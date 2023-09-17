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
import java.sql.SQLException;
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
        final FileInformation information = this.manager.getInstance().selectFile(this.configuration.getInstance().getRootDirectoryId(), true, null);
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

//    @Override
//    public Pair.@NotNull ImmutablePair<@Nullable FileInformation, @Nullable FileInformation> info(final long id) throws Exception {
//        final FileManager manager = this.manager.getInstance();
//        final FileInformation directory, file;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = manager.getConnection(null, connectionId)) {
//            final Pair.ImmutablePair<FileInformation, FileInformation> cachedInformation = manager.selectFile(id, connectionId.get());
//            if (cachedInformation.getFirst() == null) {
//                directory = this.infoDirectory(id);
//                if (directory != null)
//                    manager.insertFile(directory, connectionId.get());
//            } else directory = cachedInformation.getFirst();
//            if (cachedInformation.getSecond() == null) {
//                file = this.infoFile(id);
//                if (file != null)
//                    manager.insertFile(file, connectionId.get());
//            } else file = cachedInformation.getSecond();
//            if (cachedInformation != null) return cachedInformation;
//            if (parentId == null)
//                throw new UnsupportedOperationException("Cannot get an uncached file information without parent id." + ParametersMap.create().add("id", id));
//            final long count = FileManager.selectFileCountByParentId(configuration.getName(), parentId.longValue(), connectionId.get());
//            if (count > 0)
//                return null;
//            final Pair.ImmutablePair<Iterator<FileInformation>, Runnable> list = DriverManager_lanzou.syncFilesList(configuration, parentId.longValue(), connectionId.get());
//            if (list == null)
//                return null;
//            while (list.getFirst().hasNext()) {
//                final FileInformation information = list.getFirst().next();
//                if (information.id() == id) {
//                    BackgroundTaskManager.backgroundWithLock(new BackgroundTaskManager.BackgroundTaskIdentifier(BackgroundTaskManager.BackgroundTaskType.Driver,
//                            configuration.getName(), "Sync files list.", parentId.toString()), () -> new AtomicBoolean(true),
//                            AtomicBoolean.class, l -> l.compareAndSet(true, false), () -> /*Held connection id*/
//                            HMiscellaneousHelper.consumeIterator(list.getFirst(), list.getSecond()), null);
//                    connection.commit();
//                    return information;
//                }
//            }
//            list.getSecond().run();
//            return null;
//            connection.commit();
//        }
//        return Pair.ImmutablePair.makeImmutablePair(directory, file);
//    }

    public abstract @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws Exception;

    @Override
    public void list(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @NotNull Consumer<@Nullable UnionPair<FilesListInformation, Exception>> consumer) {
        final FileManager manager = this.manager.getInstance();
        final FilesListInformation information, indexed;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = manager.getConnection(null, connectionId)) {
            final FileInformation directory = manager.selectFile(directoryId, true, connectionId.get());
            if (directory == null) {
                indexed = null;
                information = null;
            } else {
                indexed = manager.selectFilesInDirectory(directoryId, filter, orders, position, limit, connectionId.get());
                information = indexed.total() > 0 || directory.size() == 0 ? indexed : null;
            }
            connection.commit();
        } catch (final SQLException exception) {
            consumer.accept(UnionPair.fail(exception));
            return;
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
            UnionPair<FilesListInformation, Exception> result = null;
            try (final Connection connection = manager.getConnection(null, connectionId)) {
                final Iterator<FileInformation> iterator = this.list0(directoryId);
                if (iterator == null)
                    manager.deleteDirectoryRecursively(directoryId, connectionId.get());
                else
                    try {
                        manager.insertFilesSameDirectory(iterator, directoryId, connectionId.get());
                        result = UnionPair.ok(manager.selectFilesInDirectory(directoryId, filter, orders, position, limit, connectionId.get()));
                    } catch (final NoSuchElementException exception) {
                        if (exception.getCause() instanceof Exception e)
                            throw e;
                        throw exception;
                    }
                connection.commit();
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Exception exception) {
                result = UnionPair.fail(exception);
            } finally {
                consumer.accept(result);
            }
        }) == null) {
            BackgroundTaskManager.join(identifier);
            this.list(directoryId, filter, orders, position, limit, consumer);
        }
    }

    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
