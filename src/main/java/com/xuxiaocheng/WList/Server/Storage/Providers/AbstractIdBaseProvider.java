package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
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
        if (configuration != null && dropIndex)
            FileManager.quicklyUninitialize(configuration.getName(), null);
    }

    public abstract @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws Exception;

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
                    final Iterator<FileInformation> iterator = this.list0(directoryId);
                    if (iterator == null) {
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

    @Override
    public void refreshDirectory(final long directoryId, final Consumer<? super @Nullable UnionPair<Boolean, Throwable>> consumer) {
        try {
            final FileManager manager = this.manager.getInstance();
            final FileInformation directory = manager.selectInfo(directoryId, true, null);
            if (directory == null) {
                consumer.accept(null);
                return;
            }
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
                    final AtomicReference<String> connectionId = new AtomicReference<>();
                    try (final Connection connection = manager.getConnection(null, connectionId)) {
                        manager.deleteDirectoryRecursively(directoryId, connectionId.get()); // TODO: Only update delta.
                        manager.insertFileOrDirectory(new FileInformation(directoryId, directory.parentId(), directory.name(), true, -1, directory.createTime(), directory.updateTime(), directory.others()), connectionId.get());
                        manager.insertIterator(iterator, directoryId, connectionId.get());
                        connection.commit();
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

    @Override
    public @Nullable FileInformation info(final long id, final boolean isDirectory) throws Exception {
        return this.manager.getInstance().selectInfo(id, isDirectory, null);
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
//
    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
