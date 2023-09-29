package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
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
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("OverlyBroadThrowsClause")
public abstract class AbstractIdBaseProvider<C extends StorageConfiguration> implements ProviderInterface<C> {
    protected final @NotNull HInitializer<C> configuration = new HInitializer<>("ProviderConfiguration");
    protected final @NotNull HInitializer<FileManager> manager = new HInitializer<>("ProviderManager");

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
        final FileInformation information = this.manager.getInstance().selectInfo(configuration.getRootDirectoryId(), true, null);
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
    public void list(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @NotNull Consumer<@NotNull UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> consumer) throws Exception {
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
            consumer.accept(ProviderInterface.ListNotAvailable);
            return;
        }
        if (information != null) {
            consumer.accept(UnionPair.ok(UnionPair.ok(information)));
            return;
        }
        // Not indexed.
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.SyncDirectory, String.valueOf(directoryId));
        if (BackgroundTaskManager.background(identifier, () -> {
            try {
                this.loginIfNot();
                final Iterator<FileInformation> iterator = this.list0(directoryId);
                if (iterator == null) {
                    if (directoryId != this.getConfiguration().getRootDirectoryId())
                        manager.deleteDirectoryRecursively(directoryId, null);
                    BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                            ProviderInterface.ListNotExisted));
                    return;
                }
                manager.insertIterator(iterator, directoryId, null);
                final FilesListInformation list = manager.selectInfosInDirectory(directoryId, filter, orders, position, limit, null);
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.ok(UnionPair.ok(list))));
            } catch (final NoSuchElementException exception) {
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.fail(exception.getCause() instanceof Exception e ? e : exception)));
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.fail(exception)));
            }
        }, true) == null) {
            BackgroundTaskManager.onFinally(identifier, HExceptionWrapper.wrapRunnable(() -> this.list(directoryId, filter, orders, position, limit, consumer), e -> {
                if (e != null)
                    consumer.accept(UnionPair.fail(e));
            }, true));
        }
    }

    public static final @NotNull UnionPair<FileInformation, Boolean> UpdateNotExisted = UnionPair.fail(Boolean.FALSE);
    public static final @NotNull UnionPair<FileInformation, Boolean> UpdateNoRequired = UnionPair.fail(Boolean.TRUE);
    /**
     * Try to update file/directory information. (Should call {@link #loginIfNot()} manually.)
     * @return false: file is not existed. true: needn't updated. success: updated.
     */ // TODO: in recycler.
    protected @NotNull UnionPair<FileInformation, Boolean> update0(final @NotNull FileInformation oldInformation) throws Exception {
        return AbstractIdBaseProvider.UpdateNoRequired;
    }

    @Override
    public void info(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<Pair.ImmutablePair<@NotNull FileInformation, @NotNull Boolean>, Boolean>, Throwable>> consumer) throws Exception {
        final FileManager manager = this.manager.getInstance();
        final FileInformation information = manager.selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(ProviderInterface.InfoNotAvailable);
            return;
        }
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.SyncInfo, (isDirectory ? "d" : "f") + id);
        if (BackgroundTaskManager.background(identifier, () -> {
            try {
                final UnionPair<FileInformation, Boolean> update = this.update0(information);
                if ((update.isFailure() && update.getE().booleanValue()) || (update.isSuccess() && information.equals(update.getT()))) {
                    BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                            UnionPair.ok(UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(information, Boolean.FALSE)))));
                    return;
                }
                if (update.isFailure()) { // && !update.getE().booleanValue()
                    manager.deleteFileOrDirectory(id, isDirectory, null);
                    BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                            ProviderInterface.InfoNotExisted));
                    return;
                }
                assert update.getT().isDirectory() == isDirectory;
                final FileInformation realInfo;
                final AtomicReference<String> connectionId = new AtomicReference<>();
                try (final Connection connection = manager.getConnection(null, connectionId)) {
                    if (manager.selectInfo(update.getT().parentId(), true, connectionId.get()) != null) {
                        manager.updateOrInsertFileOrDirectory(update.getT(), connectionId.get());
                        realInfo = manager.selectInfo(id, isDirectory, connectionId.get());
                    } else {
                        manager.deleteFileOrDirectory(id, isDirectory, connectionId.get());
                        realInfo = null;
                    }
                    connection.commit();
                }
                final FileInformation callback = Objects.requireNonNullElse(realInfo, update.getT());
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.ok(UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(callback, Boolean.TRUE)))));
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.fail(exception)));
            }
        }, true) == null)
            BackgroundTaskManager.onFinally(identifier, HExceptionWrapper.wrapRunnable(() -> this.info(id, isDirectory, consumer), e -> {
                if (e != null)
                    consumer.accept(UnionPair.fail(e));
            }, true));
    }

    @Contract(pure = true)
    protected abstract boolean isSupportedInfo();
    /**
     * Try to get file/directory information by id.
     * @return null: unsupported / not existed. !null: information.
     * @see #isSupportedInfo()
     */
    protected @Nullable FileInformation info0(final long id, final boolean isDirectory) throws Exception {
        return null;
    }

    @Override
    public void refreshDirectory(final long directoryId, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<Pair.ImmutablePair<@NotNull Set<Long>, @NotNull Set<Long>>, Boolean>, Throwable>> consumer) throws Exception {
        final FileManager manager = this.manager.getInstance();
        final FileInformation directory = manager.selectInfo(directoryId, true, null);
        if (directory == null) {
            consumer.accept(ProviderInterface.RefreshNotAvailable);
            return;
        }
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.SyncDirectory, String.valueOf(directoryId));
        if (BackgroundTaskManager.background(identifier, () -> {
            try {
                this.loginIfNot();
                final Iterator<FileInformation> iterator = this.list0(directoryId);
                if (iterator == null) {
                    manager.deleteDirectoryRecursively(directoryId, null);
                    BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                            ProviderInterface.RefreshNotExisted));
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
                final Map<Long, FileInformation> files = new ConcurrentHashMap<>();
                final Map<Long, FileInformation> directories = new ConcurrentHashMap<>();
                if (this.isSupportedInfo())
                    try {
                        HMultiRunHelper.runConsumers(WListServer.IOExecutors, deleteFiles, HExceptionWrapper.wrapConsumer(id -> {
                            final FileInformation info = this.info0(id.longValue(), false);
                            if (info != null) files.put(id, info);
                        }));
                        HMultiRunHelper.runConsumers(WListServer.IOExecutors, deleteDirectories, HExceptionWrapper.wrapConsumer(id -> {
                            final FileInformation info = this.info0(id.longValue(), true);
                            if (info != null) directories.put(id, info);
                        }));
                    } catch (final RuntimeException exception) {
                        throw HExceptionWrapper.unwrapException(exception, Exception.class);
                    }
                if (files.isEmpty() && directories.isEmpty()) {
                    if (!deleteFiles.isEmpty() || !deleteDirectories.isEmpty())
                        try (final Connection connection = manager.getConnection(null, connectionId)) {
                            for (final Long id: deleteFiles)
                                manager.deleteFile(id.longValue(), connectionId.get());
                            for (final Long id: deleteDirectories)
                                manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                            connection.commit();
                        }
//                    else assert directory.size() != 0;
                    BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                            ProviderInterface.RefreshNoUpdater));
                    return;
                }
                final Set<Long> insertedFiles = new HashSet<>(), insertedDirectories = new HashSet<>();
                try (final Connection connection = manager.getConnection(null, connectionId)) {
                    for (final Long id: deleteFiles) {
                        final FileInformation info = files.get(id);
                        if (info == null || manager.selectInfo(info.parentId(), true, connectionId.get()) == null)
                            manager.deleteFile(id.longValue(), connectionId.get());
                        else {
                            manager.updateOrInsertFile(info, connectionId.get());
                            insertedFiles.add(id);
                        }
                    }
                    for (final Long id: deleteDirectories) {
                        final FileInformation info = directories.get(id);
                        if (info == null || manager.selectInfo(info.parentId(), true, connectionId.get()) == null)
                            manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                        else {
                            manager.updateOrInsertDirectory(info, connectionId.get());
                            insertedDirectories.add(id);
                        }
                    }
                    connection.commit();
                }
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.ok(UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(insertedFiles, insertedDirectories)))));
            } catch (final NoSuchElementException exception) {
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.fail(exception.getCause() instanceof Exception e ? e : exception)));
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.BackgroundExecutors.submit(() -> consumer.accept(
                        UnionPair.fail(exception)));
            }
        }, true) == null)
            BackgroundTaskManager.onFinally(identifier, () -> consumer.accept(ProviderInterface.RefreshNoUpdater));
    }

    @Contract(pure = true)
    protected abstract boolean isSupportedNotEmptyDirectoryTrash();
    /**
     * Trash file/directory.
     */
    protected abstract void trash0(final @NotNull FileInformation information) throws Exception;

    @Override // TODO: delete root. // TODO: into recycler.
    public synchronized void trash(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(ProviderInterface.TrashNotAvailable);
            return;
        }
        if (isDirectory && id == this.getConfiguration().getRootDirectoryId()) {
            consumer.accept(ProviderInterface.TrashNotAvailable);
            return;
        }
        this.loginIfNot();
        if (!isDirectory || this.isSupportedNotEmptyDirectoryTrash()) {
            this.trash0(information);
            this.manager.getInstance().deleteFileOrDirectory(id, isDirectory, null);
            consumer.accept(ProviderInterface.TrashSuccess);
            return;
        }
        this.trashInside(information, false, consumer);
    }
    // TODO: no inside
    private synchronized void trashInside(final @NotNull FileInformation parent, final boolean refreshed, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception {
        if (!parent.isDirectory()) {
            this.trash0(parent);
            this.manager.getInstance().deleteFile(parent.id(), null);
            consumer.accept(ProviderInterface.TrashSuccess);
            return;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parent.id(), Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 50, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trashInside#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                        .add("p", p).add("parent", parent).add("refreshed", refreshed)));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isFailure()) {
                    consumer.accept(ProviderInterface.TrashSuccess);
                    return;
                }
                if (p.getT().getT().total() == 0) {
                    if (refreshed) {
                        this.trash0(parent);
                        this.manager.getInstance().deleteDirectoryRecursively(parent.id(), null);
                        consumer.accept(ProviderInterface.TrashSuccess);
                        return;
                    }
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.refreshDirectory(parent.id(), u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trashInside#refresh'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("parent", parent)));
                            return;
                        }
                        try {
                            if (u.isFailure()) {
                                consumer.accept(UnionPair.fail(p.getE()));
                                return;
                            }
                            if (u.getT().isFailure()) {
                                consumer.accept(ProviderInterface.TrashSuccess);
                                return;
                            }
                            this.trashInside(parent, true, consumer);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        }
                    });
                    return;
                }
                final AtomicLong total = new AtomicLong(p.getT().getT().informationList().size());
                for (final FileInformation info: p.getT().getT().informationList()) {
                    final AtomicBoolean barrier2 = new AtomicBoolean(true);
                    this.trashInside(info, false, u -> {
                        if (!barrier2.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trashInside#recursively'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("parent", parent).add("info", info)));
                            return;
                        }
                        final boolean last = total.decrementAndGet() == 0;
                        try {
                            if (u.isFailure()) {
                                consumer.accept(UnionPair.fail(u.getE()));
                                return;
                            }
                            if (last)
                                this.trashInside(parent, false, consumer);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        }
                    });
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }

    protected boolean isRequiredLoginDownloading(final @NotNull FileInformation information) throws Exception {
        return true;
    }

    /**
     * Get download methods of a specific file.
     * @param location Only by used to create {@code FailureReason}.
     * @see DownloadRequirements#tryGetDownloadFromUrl(OkHttpClient, HttpUrl, Headers, Long, Headers.Builder, long, long, ZonedDateTime)
     */
    protected abstract void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) throws Exception;

    @Override
    public void downloadFile(final long fileId, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(fileId, false, null);
        if (information == null) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(location, false))));
            return;
        }
        assert information.size() >= 0;
        final long start = Math.min(Math.max(from, 0), information.size());
        final long end = Math.min(Math.max(to, 0), information.size());
        if (start >= end) {
            consumer.accept(UnionPair.ok(UnionPair.ok(DownloadRequirements.EmptyDownloadRequirements)));
            return;
        }
        if (this.isRequiredLoginDownloading(information))
            this.loginIfNot();
        this.download0(information, start, end, consumer, location);
    }

    private static final @NotNull Pair.ImmutablePair<@NotNull String, @NotNull String> DefaultRetryBracketPair = Pair.ImmutablePair.makeImmutablePair(" (", ")");
    @Contract(pure = true)
    protected Pair.ImmutablePair<@NotNull String, @NotNull String> retryBracketPair() {
        return AbstractIdBaseProvider.DefaultRetryBracketPair;
    }

    private Pair.@Nullable ImmutablePair<@NotNull String, BackgroundTaskManager.@NotNull BackgroundTaskIdentifier> getDuplicatedName(final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final FileManager manager = this.manager.getInstance();
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.Uploading, String.format("%d: %s", parentId, name));
        FileInformation duplicate = manager.selectInfoInDirectoryByName(parentId, name, null);
        if (duplicate != null || BackgroundTaskManager.createIfNot(identifier))
            switch (policy) {
                case ERROR -> {
                    return null;
                }
                case OVER -> {
                    if (duplicate == null)
                        return null;
                    while (duplicate != null) {
                        final CountDownLatch latch = new CountDownLatch(1);
                        this.trash(duplicate.id(), duplicate.isDirectory(), u -> latch.countDown());
                        latch.await();
                        duplicate = manager.selectInfoInDirectoryByName(parentId, name, null);
                    }
                }
                case KEEP -> {
                    final int index = name.lastIndexOf('.');
                    final Pair.ImmutablePair<String, String> bracket = this.retryBracketPair();
                    final String left = (index < 0 ? name : name.substring(0, index)) + bracket.getFirst();
                    final String right = bracket.getSecond() + (index < 0 ? "" : name.substring(index));
                    String n;
                    BackgroundTaskManager.BackgroundTaskIdentifier i;
                    int retry = 0;
                    do {
                        n = left + (++retry) + right;
                        i = new BackgroundTaskManager.BackgroundTaskIdentifier(this.getConfiguration().getName(),
                                BackgroundTaskManager.Uploading, String.format("%d: %s", parentId, n));
                    } while (manager.selectInfoInDirectoryByName(parentId, n, null) != null || BackgroundTaskManager.createIfNot(i));
                    return Pair.ImmutablePair.makeImmutablePair(n, i);
                }
            }
        return Pair.ImmutablePair.makeImmutablePair(name, identifier);
    }

    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> directoryNameChecker();

    /**
     * Create an empty directory. {size == 0}
     * @param parentLocation Only by used to create {@code FailureReason}.
     */
    @SuppressWarnings("SameParameterValue")
    protected abstract void createDirectory0(final long parentId, final @NotNull String directoryName, final @NotNull Options.DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void createDirectory(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception {
        if (!this.directoryNameChecker().test(directoryName)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, directoryName, this.directoryNameChecker().description()))));
            return;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'createDirectory#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                        .add("p", p).add("parentId", parentId).add("directoryName", directoryName).add("policy", policy)));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isFailure()) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true))));
                    return;
                }
                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, directoryName, policy);
                if (name == null) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, directoryName))));
                    return;
                }
                boolean flag = true;
                try {
                    if (!this.directoryNameChecker().test(name.getFirst())) {
                        consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.directoryNameChecker().description()))));
                        return;
                    }
                    this.loginIfNot();
                    flag = false;
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.createDirectory0(parentId, name.getFirst(), Options.DuplicatePolicy.ERROR, u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'createDirectory0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("parentId", parentId).add("directoryName", directoryName).add("policy", policy)));
                            return;
                        }
                        try {
                            if (u.isSuccess() && u.getT().isSuccess()) {
                                final FileInformation information = u.getT().getT();
                                assert information.isDirectory() && information.size() == 0 && information.parentId() == parentId;
                                this.manager.getInstance().insertFileOrDirectory(information, null);
                            }
                            consumer.accept(u);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        } finally {
                            BackgroundTaskManager.remove(name.getSecond());
                        }
                    }, parentLocation);
                } finally {
                    if (flag)
                        BackgroundTaskManager.remove(name.getSecond());
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }

    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> fileNameChecker();

    /**
     * Upload a file. {size >= 0}
     * @param parentLocation Only by used to create {@code FailureReason}.
     * @see UploadRequirements#splitUploadBuffer(ConsumerE, long, int)
     */
    @SuppressWarnings("SameParameterValue")
    protected abstract void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void uploadFile(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception {
        if (!this.fileNameChecker().test(filename)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, filename, this.fileNameChecker().description()))));
            return;
        }
        if (size > this.getConfiguration().getMaxSizePerFile()) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byExceedMaxSize(parentLocation, size, this.getConfiguration().getMaxSizePerFile()))));
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'uploadFile#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                        .add("p", p).add("parentId", parentId).add("filename", filename).add("size", size).add("policy", policy)));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isFailure()) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true))));
                    return;
                }
                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, filename, policy);
                if (name == null) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, filename))));
                    return;
                }
                boolean flag = true;
                try {
                    if (!this.fileNameChecker().test(name.getFirst())) {
                        consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.fileNameChecker().description()))));
                        return;
                    }
                    this.loginIfNot();
                    flag = false;
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.uploadFile0(parentId, name.getFirst(), size, Options.DuplicatePolicy.ERROR, u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'uploadFile0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("parentId", parentId).add("filename", filename).add("name", name.getFirst()).add("size", size).add("policy", policy)));
                            return;
                        }
                        if (u.isSuccess() && u.getT().isSuccess()) {
                            final UploadRequirements requirements = u.getT().getT();
                            consumer.accept(UnionPair.ok(UnionPair.ok(new UploadRequirements(requirements.checksums(), c -> {
                                final UploadRequirements.UploadMethods methods = requirements.transfer().apply(c);
                                return new UploadRequirements.UploadMethods(methods.parallelMethods(), o -> {
                                    final AtomicBoolean barrier2 = new AtomicBoolean(true);
                                    methods.supplier().accept(t -> {
                                        if (!barrier2.compareAndSet(true, false)) {
                                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'uploadFile0#supplier'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                                    .add("t", t).add("parentId", parentId).add("filename", filename).add("name", name.getFirst()).add("size", size).add("policy", policy)));
                                            return;
                                        }
                                        try {
                                            if (t.isSuccess() && t.getT().isPresent()) {
                                                final FileInformation information = t.getT().get();
                                                assert !information.isDirectory() && information.size() >= size && information.parentId() == parentId;
                                                this.manager.getInstance().insertFileOrDirectory(information, null);
                                            }
                                            o.accept(t);
                                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                            o.accept(UnionPair.fail(exception));
                                        }
                                    });
                                }, () -> {
                                    BackgroundTaskManager.remove(name.getSecond());
                                    methods.finisher().run();
                                });
                            }, () -> BackgroundTaskManager.remove(name.getSecond())))));
                            return;
                        }
                        BackgroundTaskManager.remove(name.getSecond());
                        consumer.accept(u);
                    }, parentLocation);
                } finally {
                    if (flag)
                        BackgroundTaskManager.remove(name.getSecond());
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }

    protected abstract boolean isSupportedCopyFileDirectly(final @NotNull FileInformation information, final long parentId) throws Exception;

    /**
     * Copy a file.
     * @param location Only by used to create {@code FailureReason}.
     * @param parentLocation Only by used to create {@code FailureReason}.
     * @see ProviderInterface#CopyNotSupported
     */
    protected abstract void copyFileDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void copyFileDirectly(final long fileId, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception {
        if (!this.fileNameChecker().test(filename)) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, filename, this.fileNameChecker().description())))));
            return;
        }
        final FileInformation information = this.manager.getInstance().selectInfo(fileId, false, null);
        if (information == null) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(location, false)))));
            return;
        }
        assert information.size() >= 0;
        if (!this.isSupportedCopyFileDirectly(information, parentId)) {
            consumer.accept(ProviderInterface.CopyNotSupported);
            return;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'copyFileDirectly#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                        .add("p", p).add("information", information).add("parentId", parentId).add("filename", filename).add("policy", policy)));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isFailure()) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true)))));
                    return;
                }
                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, filename, policy);
                if (name == null) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, filename)))));
                    return;
                }
                boolean flag = true;
                try {
                    if (!this.fileNameChecker().test(name.getFirst())) {
                        consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.fileNameChecker().description())))));
                        return;
                    }
                    if (parentId == information.parentId() && name.getFirst().equals(information.name())) {
                        consumer.accept(ProviderInterface.CopySelf);
                        return;
                    }
                    this.loginIfNot();
                    flag = false;
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.copyFileDirectly0(information, parentId, name.getFirst(), policy, u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'copyFileDirectly0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("information", information).add("parentId", parentId).add("filename", filename).add("name", name.getFirst()).add("policy", policy)));
                            return;
                        }
                        try {
                            if (u.isSuccess() && u.getT().isPresent() && u.getT().get().isSuccess() && u.getT().get().getT().isPresent()) {
                                final FileInformation file = u.getT().get().getT().get();
                                assert !file.isDirectory() && file.size() == information.size() && file.parentId() == parentId;
                                this.manager.getInstance().insertFileOrDirectory(file, null);
                            }
                            consumer.accept(u);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        } finally {
                            BackgroundTaskManager.remove(name.getSecond());
                        }
                    }, location, parentLocation);
                } finally {
                    if (flag)
                        BackgroundTaskManager.remove(name.getSecond());
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }

    protected abstract boolean isSupportedMoveDirectly(final @NotNull FileInformation information, final long parentId) throws Exception;

    /**
     * Move a file / directory. (.size == information.size)
     * @param location Only by used to create {@code FailureReason}.
     * @param parentLocation Only by used to create {@code FailureReason}.
     * @see ProviderInterface#CopyNotSupported
     */
    protected abstract void moveDirectly0(final @NotNull FileInformation information, final long parentId, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void moveDirectly(final long id, final boolean isDirectory, final long parentId, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception {
        final FileInformation information;
        UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> res = null;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.manager.getInstance().getConnection(null, connectionId)) {
            information = this.manager.getInstance().selectInfo(id, isDirectory, connectionId.get());
            //noinspection VariableNotUsedInsideIf
            if (information == null) {
                res = UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(location, isDirectory))));
            } else
                if (isDirectory && this.manager.getInstance().isInDirectoryRecursively(parentId, true, id, connectionId.get()))
                    res = ProviderInterface.MoveSelf;
            connection.commit();
        }
        if (res != null) {
            consumer.accept(res);
            return;
        }
        if (!this.isSupportedMoveDirectly(information, parentId)) {
            consumer.accept(ProviderInterface.MoveNotSupported);
            return;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'moveDirectly#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                        .add("p", p).add("information", information).add("parentId", parentId).add("policy", policy)));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isFailure()) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true)))));
                    return;
                }
                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, information.name(), policy);
                if (name == null) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, information.name())))));
                    return;
                }
                boolean flag = true;
                try {
                    if (!this.directoryNameChecker().test(name.getFirst())) {
                        consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.directoryNameChecker().description())))));
                        return;
                    }
                    if (!name.getFirst().equals(information.name())) { // TODO temp dir. (require move twice.)
                        consumer.accept(ProviderInterface.MoveNotSupported);
                        return;
                    }
                    this.loginIfNot();
                    flag = false;
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.moveDirectly0(information, parentId, policy, u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            HLog.getInstance("ProviderLogger").log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'moveDirectly0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("information", information).add("parentId", parentId).add("name", name.getFirst()).add("policy", policy)));
                            return;
                        }
                        try {
                            if (u.isSuccess() && u.getT().isPresent() && u.getT().get().isSuccess() && u.getT().get().getT().isPresent()) {
                                final FileInformation file = u.getT().get().getT().get();
                                assert file.isDirectory() == isDirectory && file.size() == information.size() && file.parentId() == parentId;
                                this.manager.getInstance().updateOrInsertFileOrDirectory(file, null);
                            }
                            consumer.accept(u);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        } finally {
                            BackgroundTaskManager.remove(name.getSecond());
                        }
                    }, location, parentLocation);
                } finally {
                    if (flag)
                        BackgroundTaskManager.remove(name.getSecond());
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }

//    static @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull LanzouConfiguration configuration, final @NotNull FileInformation source, final long targetId, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        if (source.parentId() == targetId) return UnionPair.ok(source);
//        if (policy == Options.DuplicatePolicy.KEEP) // TODO: vip
//            throw new UnsupportedOperationException("Driver lanzou not support rename file while moving.");
//        if (source.isDirectory()) // TODO: directory
//            throw new UnsupportedOperationException("Driver lanzou not support move directory.");
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//            final UnionPair<UnionPair<String, FileInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, targetId, source.name(), false, policy, "Moving.", connectionId.get());
//            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
//            assert duplicate.getT().getT().equals(source.name());
//            final UnionPair<ZonedDateTime, FailureReason> information = DriverHelper_lanzou.moveFile(configuration, source.id(), targetId);
//            if (information == null) {connection.commit();return UnionPair.ok(source);}
//            if (information.isFailure()) {connection.commit();return UnionPair.fail(information.getE());}
//            FileManager.mergeFile(configuration.getName(), new FileInformation(source.location(), targetId,
//                    source.name(), source.type(), source.size(), source.createTime(), information.getT(), source.md5(), source.others()), connectionId.get());
//            FileManager.updateDirectorySize(configuration.getName(), source.parentId(), -source.size(), connectionId.get());
//            FileManager.updateDirectoryType(configuration.getName(), targetId, false, connectionId.get());
//            FileManager.updateDirectorySize(configuration.getName(), targetId, source.size(), connectionId.get());
//            connection.commit();
//            return UnionPair.ok(source);
//        }
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull LanzouConfiguration configuration, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return UnionPair.fail(FailureReason.byInvalidName(name, new FileLocation(configuration.getName(), id), name));
//        throw new UnsupportedOperationException("Driver lanzou not support rename."); // TODO: vip
//    }

    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
