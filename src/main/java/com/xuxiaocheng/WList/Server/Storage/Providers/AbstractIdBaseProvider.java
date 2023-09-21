package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("OverlyBroadThrowsClause")
public abstract class AbstractIdBaseProvider<C extends ProviderConfiguration> implements ProviderInterface<C> {
    protected @NotNull HInitializer<C> configuration = new HInitializer<>("ProviderConfiguration");
    protected @NotNull HInitializer<FileManager> manager = new HInitializer<>("ProviderManager");

    @Override
    public @NotNull C getConfiguration() {
        return this.configuration.getInstance();
    }

    @Override
    public void initialize(final @NotNull C configuration) throws Exception {
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(StorageManager.getStorageDatabaseFile(configuration.getName()));
        FileManager.quicklyInitialize(configuration.getName(), database, configuration.getRootDirectoryId(), null);
        this.configuration.reinitialize(configuration);
        this.manager.reinitialize(FileManager.getInstance(configuration.getName()));
        final FileInformation information = this.manager.getInstance().selectInfo(this.configuration.getInstance().getRootDirectoryId(), true, null);
        assert information != null;
        assert information.createTime() != null;
        assert information.updateTime() != null;
        configuration.setSpaceUsed(information.size());
        configuration.setCreateTime(information.createTime());
        configuration.setUpdateTime(information.updateTime());
        configuration.markModified();
    }

    @Override
    public void uninitialize(final boolean dropIndex) throws Exception {
        final C configuration = this.configuration.uninitializeNullable();
        this.manager.uninitializeNullable();
        if (configuration != null) {
            if (dropIndex)
                FileManager.quicklyUninitialize(configuration.getName(), null);
            SqlDatabaseManager.quicklyClose(StorageManager.getStorageDatabaseFile(configuration.getName()));
        }
    }

    protected abstract void loginIfNot() throws Exception;

    /**
     * List the directory.
     * @return null: directory is not existed. !null: list of files.
     * @exception Exception: Any iterating exception can be wrapped in {@link NoSuchElementException} and then thrown in method {@code next()}.
     */
    protected abstract @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws Exception;

    @Override
    public void list(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @NotNull Consumer<@Nullable UnionPair<FilesListInformation, Throwable>> consumer) {
        try {
            final FileManager manager = this.manager.getInstance();
            final FilesListInformation information, indexed;
            final AtomicReference<String> connectionId = new AtomicReference<>();
            try (final Connection connection = manager.getConnection(null, connectionId)) {
                final FileInformation directory = manager.selectInfo(directoryId, true, connectionId.get());
                if (directory == null) {
                    indexed = null;
                    information = null;
                } else {
                    indexed = manager.selectInfosInDirectory(directoryId, filter, orders, position, limit, connectionId.get());
                    information = indexed.total() > 0 || directory.size() == 0 ? indexed : null;
                }
                connection.commit();
            }
            if (information == null && indexed == null) {
                consumer.accept(null);
                return;
            }
            if (information != null) {
                consumer.accept(UnionPair.ok(information));
                return;
            }
            // Not indexed.
            final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                    this.configuration.getInstance().getName(), BackgroundTaskManager.SyncTask, String.valueOf(directoryId));
            if (BackgroundTaskManager.background(identifier, () -> {
                try  {
                    this.loginIfNot();
                    final Iterator<FileInformation> iterator = this.list0(directoryId);
                    if (iterator == null) {
                        if (directoryId != this.getConfiguration().getRootDirectoryId())
                            manager.deleteDirectoryRecursively(directoryId, null);
                        consumer.accept(null);
                        return;
                    }
                    manager.insertIterator(iterator, directoryId, null);
                    consumer.accept(UnionPair.ok(manager.selectInfosInDirectory(directoryId, filter, orders, position, limit, null)));
                } catch (final NoSuchElementException exception) {
                    consumer.accept(exception.getCause() instanceof Exception e ? UnionPair.fail(e) : UnionPair.fail(exception));
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    consumer.accept(UnionPair.fail(exception));
                }
            }) == null)
                BackgroundTaskManager.onFinally(identifier, () -> this.list(directoryId, filter, orders, position, limit, consumer));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * Try to update file/directory information.
     * @return false: file is not existed. true: needn't updated. success: updated.
     */
    protected @NotNull UnionPair<FileInformation, Boolean> update0(final @NotNull FileInformation oldInformation) throws Exception {
        return UnionPair.fail(Boolean.TRUE);
    }

    @Override
    public @Nullable FileInformation info(final long id, final boolean isDirectory) throws Exception {
        final FileManager manager = this.manager.getInstance();
        final FileInformation information = manager.selectInfo(id, isDirectory, null);
        if (information == null) return null;
        this.loginIfNot();
        final UnionPair<FileInformation, Boolean> update = this.update0(information);
        if (update.isFailure()) {
            if (update.getE().booleanValue())
                return information;
            manager.deleteFileOrDirectory(id, isDirectory, null);
            return null;
        }
        if (information.equals(update.getT()))
            return information;
        assert update.getT().isDirectory() == isDirectory;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = manager.getConnection(null, connectionId)) {
            if (manager.selectInfo(update.getT().parentId(), true, connectionId.get()) != null)
                manager.updateOrInsertFileOrDirectory(update.getT(), connectionId.get());
            connection.commit();
        }
        return update.getT();
    }

    /**
     * Try to get file/directory information by id.
     * @return null: unsupported / not existed. !null: information.
     */
    protected @Nullable FileInformation info0(final long id, final boolean isDirectory) throws Exception {
        return null;
    }

    @Override
    public void refreshDirectory(final long directoryId, final Consumer<? super @Nullable UnionPair<Boolean, Throwable>> consumer) {
        try {
            final FileManager manager = this.manager.getInstance();
            final FileInformation directory = manager.selectInfo(directoryId, true, null);
            if (directory == null) {
                consumer.accept(null);
                return;
            }
            this.loginIfNot();
            final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                    this.configuration.getInstance().getName(), BackgroundTaskManager.SyncTask, String.valueOf(directoryId));
            if (BackgroundTaskManager.background(identifier, () -> {
                try {
                    final Iterator<FileInformation> iterator = this.list0(directoryId);
                    if (iterator == null) {
                        manager.deleteDirectoryRecursively(directoryId, null);
                        consumer.accept(UnionPair.ok(Boolean.FALSE));
                        return;
                    }
                    final Set<Long> deleteFiles, deleteDirectories;
                    final AtomicReference<String> connectionId = new AtomicReference<>();
                    try (final Connection connection = manager.getConnection(null, connectionId)) {
                        final Pair.ImmutablePair<Set<Long>, Set<Long>> old = manager.selectIdsInDirectory(directoryId, connectionId.get());
                        deleteFiles = old.getFirst();
                        deleteDirectories = old.getSecond();
                        while (iterator.hasNext()) {
                            final FileInformation information = iterator.next();
                            if (information.isDirectory()) {
                                deleteDirectories.remove(information.id());
                                manager.updateOrInsertDirectory(information, connectionId.get());
                            } else {
                                deleteFiles.remove(information.id());
                                manager.updateOrInsertFile(information, connectionId.get());
                            }
                        }
                        connection.commit();
                    }
                    if (!deleteFiles.isEmpty() || !deleteDirectories.isEmpty()) {
                        final Map<Long, FileInformation> files = new ConcurrentHashMap<>();
                        final Map<Long, FileInformation> directories = new ConcurrentHashMap<>();
                        HMultiRunHelper.runConsumers(WListServer.IOExecutors, deleteFiles, HExceptionWrapper.wrapConsumer(id -> {
                            final FileInformation info = this.info0(id.longValue(), false);
                            if (info != null) files.put(id, info);
                        }));
                        HMultiRunHelper.runConsumers(WListServer.IOExecutors, deleteDirectories, HExceptionWrapper.wrapConsumer(id -> {
                            final FileInformation info = this.info0(id.longValue(), true);
                            if (info != null) directories.put(id, info);
                        }));
                        try (final Connection connection = manager.getConnection(null, connectionId)) {
                            for (final Long id: deleteFiles) {
                                final FileInformation info = files.get(id);
                                if (info == null)
                                    manager.deleteFile(id.longValue(), connectionId.get());
                                else if (manager.selectInfo(info.parentId(), true, connectionId.get()) != null)
                                    manager.updateOrInsertFile(info, connectionId.get());
                            }
                            for (final Long id: deleteDirectories) {
                                final FileInformation info = directories.get(id);
                                if (info == null)
                                    manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                                else if (manager.selectInfo(info.parentId(), true, connectionId.get()) != null)
                                    manager.updateOrInsertDirectory(info, connectionId.get());
                            }
                            connection.commit();
                        }
                    }
                    consumer.accept(UnionPair.ok(Boolean.TRUE));
                } catch (final NoSuchElementException exception) {
                    consumer.accept(exception.getCause() instanceof Exception e ? UnionPair.fail(e) : UnionPair.fail(exception));
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    consumer.accept(UnionPair.fail(exception));
                }
            }) == null && !BackgroundTaskManager.onComplete(identifier, (a, e) -> {
                if (e != null) {
                    consumer.accept(UnionPair.fail(e));
                    return;
                }
                try {
                    consumer.accept(UnionPair.ok(manager.selectInfo(directoryId, true, null) != null));
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    consumer.accept(UnionPair.fail(exception));
                }
            })) consumer.accept(UnionPair.ok(manager.selectInfo(directoryId, true, null) != null));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * Delete file/directory.
     */
    protected abstract void delete0(final @NotNull FileInformation information) throws Exception;

    @Override
    public boolean delete(final long id, final boolean isDirectory) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null)
            return false;
        this.loginIfNot();
        this.delete0(information);
        this.manager.getInstance().deleteFileOrDirectory(id, isDirectory, null);
        return true;
    }

    /**
     * Get download methods of a specific file.
     * @param location Only by used to create {@code FailureReason}.
     * @see DownloadRequirements#tryGetDownloadFromUrl(OkHttpClient, HttpUrl, Headers, Long, Headers.Builder, long, long, ZonedDateTime)
     */
    protected abstract @NotNull UnionPair<DownloadRequirements, FailureReason> download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull FileLocation location) throws Exception;

    @Override
    public @NotNull UnionPair<DownloadRequirements, FailureReason> download(final long fileId, final long from, final long to, final @NotNull FileLocation location) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(fileId, false, null);
        if (information == null)
            return UnionPair.fail(FailureReason.byNoSuchFile(location));
        assert information.size() >= 0;
        final long start = Math.min(Math.max(from, 0), information.size());
        final long end = Math.min(Math.max(to, 0), information.size());
        if (start >= end)
            return UnionPair.ok(DownloadRequirements.EmptyDownloadRequirements);
        this.loginIfNot();
        return this.download0(information, start, end, location);
    }

    protected boolean checkDirectoryIndexed(final long directoryId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<FilesListInformation, Throwable>> p = new AtomicReference<>();
        this.list(directoryId, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 0, l -> {
            p.set(l);
            latch.countDown();
        });
        latch.await();
        if (p.get() == null)
            return true;
        if (p.get().isFailure()) {
            if (p.get().getE() instanceof Exception exception)
                throw exception;
            throw (Error) p.get().getE();
        }
        return false;
    }

    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> nameChecker();

    /**
     * Create an empty directory. {size == 0}
     * @param parentLocation Only by used to create {@code FailureReason}.
     */
    protected abstract @NotNull UnionPair<FileInformation, FailureReason> createDirectory0(final long parentId, final @NotNull String directoryName, final @NotNull Options.DuplicatePolicy ignoredPolicy, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final long parentId, final @NotNull String directoryName, final @NotNull Options.DuplicatePolicy policy, final @NotNull FileLocation parentLocation) throws Exception {
        if (!this.nameChecker().test(directoryName))
            return UnionPair.fail(FailureReason.byInvalidName(parentLocation, directoryName, this.nameChecker().description()));
        final FileInformation information = this.manager.getInstance().selectInfo(parentId, true, null);
        if (information == null || this.checkDirectoryIndexed(parentId))
            return UnionPair.fail(FailureReason.byNoSuchFile(parentLocation));
        String name = directoryName;
        FileInformation duplicate = this.manager.getInstance().selectInfoInDirectoryByName(parentId, directoryName, null);
        if (duplicate != null)
            switch (policy) {
                case ERROR -> {
                    return UnionPair.fail(FailureReason.byDuplicateError(parentLocation));
                }
                case OVER -> {
                    while (duplicate != null) {
                        this.delete(duplicate.id(), duplicate.isDirectory());
                        duplicate = this.manager.getInstance().selectInfoInDirectoryByName(parentId, directoryName, null);
                    }
                }
                case KEEP -> {
                    final int index = name.lastIndexOf('.');
                    final String left = (index < 0 ? name: name.substring(0, index)) + '(';
                    final String right = ')' + (index < 0 ? "" : name.substring(index));
                    int retry = 0;
                    do {
                        name = left + (++retry) + right;
                    } while (this.manager.getInstance().selectInfoInDirectoryByName(parentId, name, null) != null);
                }
            }
        if (!this.nameChecker().test(name))
            return UnionPair.fail(FailureReason.byInvalidName(parentLocation, name, this.nameChecker().description()));
        final UnionPair<FileInformation, FailureReason> directory = this.createDirectory0(parentId, name, policy, parentLocation);
        if (directory.isSuccess())
            this.manager.getInstance().insertFileOrDirectory(directory.getT(), null);
        return directory;
    }

//    @Override
//    public void buildIndex() throws IOException, SQLException, InterruptedException {
//        final Set<CompletableFuture<?>> futures = ConcurrentHashMap.newKeySet();
//        final AtomicLong runningFutures = new AtomicLong(1);
//        final AtomicBoolean interruptFlag = new AtomicBoolean(false);
//        DriverManager_lanzou.refreshDirectoryRecursively(this.configuration, this.configuration.getRootDirectoryId(), futures, runningFutures, interruptFlag);
//        try {
//            synchronized (runningFutures) {
//                while (runningFutures.get() > 0)
//                    runningFutures.wait();
//            }
//        } catch (final InterruptedException exception) {
//            interruptFlag.set(true);
//            throw exception;
//        }
//        for (final CompletableFuture<?> future: futures)
//            try {
//                future.join();
//            } catch (final CancellationException ignore) {
//            } catch (final CompletionException exception) {
//                Throwable throwable;
//                try {
//                    throwable = HExceptionWrapper.unwrapException(exception.getCause(), IOException.class, SQLException.class, InterruptedException.class);
//                } catch (final IOException | SQLException | InterruptedException e) {
//                    throwable = e;
//                }
//                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
//            }
//        final FileInformation root = FileManager.selectFile(this.configuration.getName(), this.configuration.getRootDirectoryId(), null);
//        if (root != null)
//            this.configuration.setSpaceUsed(root.size());
//        this.configuration.setLastFileIndexBuildTime(ZonedDateTime.now());
//        this.configuration.setModified(true);
//    }

    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
