package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.BiConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@SuppressWarnings("OverlyBroadThrowsClause")
public abstract class AbstractIdBaseProvider<C extends StorageConfiguration> implements ProviderInterface<C> {
    protected static final @NotNull HLog logger = HLog.create("ProviderLogger");

    protected final @NotNull HInitializer<C> configuration = new HInitializer<>("ProviderConfiguration");
    protected final @NotNull HInitializer<FileManager> manager = new HInitializer<>("ProviderManager");

    @Override
    public @NotNull C getConfiguration() {
        return this.configuration.getInstance();
    }

    protected @NotNull FileLocation getLocation(final long id) {
        return new FileLocation(this.getConfiguration().getName(), id);
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
        boolean modified = false;
        if (information.size() != configuration.getSpaceUsed()) {
            configuration.setSpaceUsed(information.size());
            modified = true;
        }
        if (!information.createTime().equals(configuration.getCreateTime())) {
            configuration.setCreateTime(information.createTime());
            modified = true;
        }
        if (!information.updateTime().equals(configuration.getUpdateTime())) {
            configuration.setUpdateTime(information.updateTime());
            modified = true;
        }
        if (modified)
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
    public void list(final long directoryId, final Options.@NotNull FilterPolicy filter,
                     final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders,
                     final long position, final int limit,
                     final @NotNull Consumer<? super @NotNull UnionPair<Optional<FilesListInformation>, Throwable>> consumer) throws Exception {
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
        if (indexed == null) {
            consumer.accept(ProviderInterface.ListNotExisted);
            return;
        }
        if (information != null) {
            consumer.accept(UnionPair.ok(Optional.of(information)));
            return;
        }
        // Not indexed.
        BackgroundTaskManager.background(new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.Directory, String.valueOf(directoryId)), () -> {
            UnionPair<Optional<FilesListInformation>, Throwable> result = null;
            try {
                this.loginIfNot();
                final Iterator<FileInformation> iterator = this.list0(directoryId);
                if (iterator == null) {
                    manager.deleteDirectoryRecursively(directoryId, null);
                    BroadcastManager.onFileTrash(this.getLocation(directoryId), true);
                    result = ProviderInterface.ListNotExisted;
                    return;
                }
                final FilesListInformation list;
                try (final Connection connection = manager.getConnection(null, connectionId)) {
                    manager.insertIterator(iterator, directoryId, connectionId.get());
                    list = manager.selectInfosInDirectory(directoryId, filter, orders, position, limit, connectionId.get());
                    connection.commit();
                }
                result = UnionPair.ok(Optional.of(list));
            } catch (final NoSuchElementException exception) {
                result = UnionPair.fail(exception.getCause() instanceof Exception e ? e : exception);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                final UnionPair<Optional<FilesListInformation>, Throwable> res = result;
                assert res != null;
                WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
            }
        }, true, HExceptionWrapper.wrapRunnable(() ->
                this.list(directoryId, filter, orders, position, limit, consumer), e -> {
            if (e != null)
                consumer.accept(UnionPair.fail(e));
        }, true));
    }


    @Contract(pure = true)
    protected boolean doesRequireUpdate(final @NotNull FileInformation information) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> UpdateNoRequired = UnionPair.ok(UnionPair.fail(Boolean.FALSE));
    public static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> UpdateNotExisted = UnionPair.ok(UnionPair.fail(Boolean.TRUE));
    /**
     * Try to update file/directory information. (Should call {@link #loginIfNot()} manually.)
     * @param consumer false: needn't updated. true: file is not existed. success: updated.
     * @see #doesRequireUpdate(FileInformation)
     */ // TODO: in recycler.
    protected void update0(final @NotNull FileInformation oldInformation, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) throws Exception {
        consumer.accept(AbstractIdBaseProvider.UpdateNoRequired);
    }

    @Override
    public void info(final long id, final boolean isDirectory,
                     final @NotNull Consumer<? super @NotNull UnionPair<Optional<FileInformation>, Throwable>> consumer) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(ProviderInterface.InfoNotExisted);
            return;
        }
        if (!this.doesRequireUpdate(information)) {
            consumer.accept(UnionPair.ok(Optional.of(information)));
            return;
        }
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.File, (isDirectory ? "d" : "f") + id);
        BackgroundTaskManager.background(identifier, () -> {
            try {
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.update0(information, p -> {
                    if (!barrier.compareAndSet(true, false)) {
                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'info#update0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                .add("p", p).add("information", information)));
                        return;
                    }
                    UnionPair<Optional<FileInformation>, Throwable> result = null;
                    try {
                        if (p.isFailure()) {
                            result = UnionPair.fail(p.getE());
                            return;
                        }
                        if ((p.getT().isFailure() && !p.getT().getE().booleanValue()) || (p.getT().isSuccess() && information.equals(p.getT().getT()))) {
                            result = UnionPair.ok(Optional.of(information));
                            return;
                        }
                        if (p.getT().isFailure()) { // && p.getT().getE().booleanValue()
                            this.manager.getInstance().deleteFileOrDirectory(id, isDirectory, null);
                            BroadcastManager.onFileTrash(this.getLocation(id), isDirectory);
                            result = ProviderInterface.InfoNotExisted;
                            return;
                        }
                        final FileInformation updated = p.getT().getT();
                        assert updated.isDirectory() == isDirectory;
                        final FileInformation realInfo;
                        final FileManager manager = this.manager.getInstance();
                        final AtomicReference<String> connectionId = new AtomicReference<>();
                        try (final Connection connection = manager.getConnection(null, connectionId)) {
                            manager.updateOrInsertFileOrDirectory(updated, connectionId.get());
                            realInfo = manager.selectInfo(id, isDirectory, connectionId.get());
                            connection.commit();
                        }
                        if (realInfo == null) {
                            result = ProviderInterface.InfoNotExisted;
                            return;
                        }
                        result = UnionPair.ok(Optional.of(realInfo));
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        final UnionPair<Optional<FileInformation>, Throwable> res = result;
                        assert res != null;
                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                });
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.remove(identifier);
                WListServer.ServerExecutors.submit(() -> consumer.accept(UnionPair.fail(exception))).addListener(MiscellaneousUtil.exceptionListener());
            }
        }, false, HExceptionWrapper.wrapRunnable(() -> this.info(id, isDirectory, consumer), e -> {
            if (e != null)
                consumer.accept(UnionPair.fail(e));
        }, true));
    }


    @Contract(pure = true)
    protected boolean doesSupportInfo(final boolean isDirectory) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<Optional<FileInformation>, Throwable> InfoNotExist = UnionPair.ok(Optional.empty());
    /**
     * Try to get file/directory information by id.
     * @param consumer empty: not existed / unsupported. present: information.
     * @see #doesSupportInfo(boolean)
     */
    protected void info0(final long id, final boolean isDirectory, final @NotNull Consumer<? super UnionPair<Optional<FileInformation>, Throwable>> consumer) throws Exception {
        consumer.accept(AbstractIdBaseProvider.InfoNotExist);
    }

    @Override
    public void refreshDirectory(final long directoryId,
                                 final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception {
        final FileInformation directory = this.manager.getInstance().selectInfo(directoryId, true, null);
        if (directory == null) {
            consumer.accept(ProviderInterface.RefreshNotExisted);
            return;
        }
        if (directory.size() == -1 && this.manager.getInstance().selectInfosInDirectory(directoryId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, null).total() == 0) { // Not indexed.
            final AtomicBoolean barrier = new AtomicBoolean(true);
            this.list(directoryId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
                if (!barrier.compareAndSet(true, false)) {
                    AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'refreshDirectory#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                            .add("p", p).add("directoryId", directoryId)));
                    return;
                }
                UnionPair<Boolean, Throwable> result = null;
                try {
                    if (p.isFailure()) {
                        result = UnionPair.fail(p.getE());
                        return;
                    }
                    result = p.getT().isPresent() ? ProviderInterface.RefreshSuccess : ProviderInterface.RefreshNotExisted;
                } catch (final Throwable exception) {
                    result = UnionPair.fail(exception);
                } finally {
                    final UnionPair<Boolean, Throwable> res = result;
                    assert res != null;
                    WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                }
            });
            return;
        }
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.Directory, String.valueOf(directoryId));
        BackgroundTaskManager.background(identifier, () -> {
            UnionPair<Boolean, Throwable> result = null;
            boolean flag = true;
            try {
                this.loginIfNot();
                final Iterator<FileInformation> iterator = this.list0(directoryId);
                if (iterator == null) {
                    this.manager.getInstance().deleteDirectoryRecursively(directoryId, null);
                    BroadcastManager.onFileTrash(this.getLocation(directoryId), true);
                    result = ProviderInterface.RefreshNotExisted;
                    return;
                }
                final Set<Long> extraFiles, extraDirectories;
                final Collection<Long> updatedFiles = new HashSet<>(), updatedDirectories = new HashSet<>();
                final FileManager manager = this.manager.getInstance();
                final AtomicReference<String> connectionId = new AtomicReference<>();
                try (final Connection connection = manager.getConnection(null, connectionId)) {
                    final Pair.ImmutablePair<Set<Long>, Set<Long>> old = manager.selectIdsInDirectory(directoryId, connectionId.get());
                    extraFiles = old.getFirst();
                    extraDirectories = old.getSecond();
                    while (iterator.hasNext()) {
                        final FileInformation information = iterator.next();
                        if (information.isDirectory()) {
                            updatedDirectories.add(information.id());
                            extraDirectories.remove(information.id());
                            manager.updateOrInsertDirectory(information, connectionId.get());
                        } else {
                            updatedFiles.add(information.id());
                            extraFiles.remove(information.id());
                            manager.updateOrInsertFile(information, connectionId.get());
                        }
                    }
                    connection.commit();
                }
                result = ProviderInterface.RefreshSuccess;
                if (extraFiles.isEmpty() && extraDirectories.isEmpty()) {
                    WListServer.CodecExecutors.submit(() -> {
                        for (final Long id: updatedFiles)
                            BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), false);
                        for (final Long id: updatedDirectories)
                            BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), true);
                    }).addListener(MiscellaneousUtil.exceptionListener());
                    return;
                }
                final Collection<Long> deleteFiles = new HashSet<>(), deleteDirectories = new HashSet<>();
                final AtomicBoolean consumed = new AtomicBoolean(false);
                final AtomicLong total = new AtomicLong(1 + (this.doesSupportInfo(false) ? extraFiles.size() : 0) + (this.doesSupportInfo(true) ? extraDirectories.size() : 0));
                final Runnable finisher = () -> {
                    if (total.getAndDecrement() <= 1) {
                        BackgroundTaskManager.remove(identifier);
                        WListServer.CodecExecutors.submit(() -> {
                            for (final Long id: updatedFiles)
                                BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), false);
                            for (final Long id: updatedDirectories)
                                BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), true);
                            for (final Long id: deleteFiles)
                                BroadcastManager.onFileTrash(this.getLocation(id.longValue()), false);
                            for (final Long id: deleteDirectories)
                                BroadcastManager.onFileTrash(this.getLocation(id.longValue()), true);
                        }).addListener(MiscellaneousUtil.exceptionListener());
                        if (consumed.compareAndSet(false, true))
                            WListServer.ServerExecutors.submit(() -> consumer.accept(ProviderInterface.RefreshSuccess)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                };
                try (final Connection connection = manager.getConnection(null, connectionId)) {
                    if (this.doesSupportInfo(false))
                        for (final Long id: extraFiles) {
                            final AtomicBoolean barrier = new AtomicBoolean(true);
                            final Consumer<UnionPair<Optional<FileInformation>, Throwable>> handler = p -> {
                                if (!barrier.compareAndSet(true, false)) {
                                    AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'refreshDirectory#info0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                            .add("p", p).add("directoryId", directoryId).add("id", id).add("isDirectory", false)));
                                    return;
                                }
                                try (connection) {
                                    if (p.isFailure())
                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to get file information after refreshing.", ParametersMap.create()
                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", false), p.getE());
                                    if (p.isSuccess() && p.getT().isPresent()) {
                                        manager.updateOrInsertFile(p.getT().get(), connectionId.get());
                                        updatedFiles.add(id);
                                    } else {
                                        manager.deleteFile(id.longValue(), connectionId.get());
                                        deleteFiles.add(id);
                                    }
                                    connection.commit();
                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                    if (consumed.compareAndSet(false, true))
                                        consumer.accept(UnionPair.fail(exception));
                                    else
                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to update file information after refreshing and getting.", ParametersMap.create()
                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", false), exception);
                                } finally {
                                    finisher.run();
                                }
                            };
                            manager.getConnection(connectionId.get(), null);
                            try {
                                this.info0(id.longValue(), false, handler);
                            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                handler.accept(UnionPair.fail(exception));
                            }
                        }
                    else {
                        for (final Long id: extraFiles)
                            manager.deleteFile(id.longValue(), connectionId.get());
                        deleteFiles.addAll(extraFiles);
                    }
                    if (this.doesSupportInfo(true))
                        for (final Long id: extraDirectories) {
                            final AtomicBoolean barrier = new AtomicBoolean(true);
                            final Consumer<UnionPair<Optional<FileInformation>, Throwable>> handler = p -> {
                                if (!barrier.compareAndSet(true, false)) {
                                    AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'refreshDirectory#info0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                            .add("p", p).add("directoryId", directoryId).add("id", id).add("isDirectory", true)));
                                    return;
                                }
                                try (connection) {
                                    if (p.isFailure())
                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to get directory information after refreshing.", ParametersMap.create()
                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", true), p.getE());
                                    if (p.isSuccess() && p.getT().isPresent()) {
                                        manager.updateOrInsertDirectory(p.getT().get(), connectionId.get());
                                        updatedDirectories.add(id);
                                    } else {
                                        manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                                        deleteDirectories.add(id);
                                    }
                                    connection.commit();
                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                    if (consumed.compareAndSet(false, true))
                                        consumer.accept(UnionPair.fail(exception));
                                    else
                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to update directory information after refreshing and getting.", ParametersMap.create()
                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", true), exception);
                                } finally {
                                    finisher.run();
                                }
                            };
                            manager.getConnection(connectionId.get(), null);
                            try {
                                this.info0(id.longValue(), true, handler);
                            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                handler.accept(UnionPair.fail(exception));
                            }
                        }
                    else {
                        for (final Long id: extraDirectories)
                            manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                        deleteDirectories.addAll(extraDirectories);
                    }
                    connection.commit();
                }
                finisher.run();
                flag = false;
            } catch (final NoSuchElementException exception) {
                result = UnionPair.fail(exception.getCause() instanceof Exception e ? e : exception);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    BackgroundTaskManager.remove(identifier);
                    final UnionPair<Boolean, Throwable> res = result;
                    assert res != null;
                    WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                }
            }
        }, false, () -> consumer.accept(ProviderInterface.RefreshSuccess));
    }


    @Contract(pure = true)
    protected boolean doesSupportTrashNotEmptyDirectory() {
        return false;
    }
    public static final @NotNull UnionPair<Boolean, Throwable> TrashNotSupport = UnionPair.ok(Boolean.FALSE);
    public static final @NotNull UnionPair<Boolean, Throwable> TrashSuccess = UnionPair.ok(Boolean.TRUE);
    /**
     * Trash file/directory.
     * @param consumer false: not support. true: success.
     * @see #doesSupportTrashNotEmptyDirectory()
     */
    protected abstract void trash0(final @NotNull FileInformation information, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception;

    @Override // TODO: into recycler.
    public void trash(final long id, final boolean isDirectory,
                      final @NotNull Consumer<? super @NotNull UnionPair<Optional<Boolean>, Throwable>> consumer) throws Exception {
        if (isDirectory && id == this.getConfiguration().getRootDirectoryId()) {
            consumer.accept(ProviderInterface.TrashTooComplex);
            return;
        }
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(ProviderInterface.TrashNotExisted);
            return;
        }
        if (isDirectory && !this.doesSupportTrashNotEmptyDirectory()) {
            final FileManager manager = this.manager.getInstance();
            final FilesListInformation indexed = manager.selectInfosInDirectory(id, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, null);
            if (indexed.total() > 0) {
                consumer.accept(ProviderInterface.TrashTooComplex);
                return;
            }
            final AtomicBoolean barrier = new AtomicBoolean(true);
            this.refreshDirectory(id, p -> {
                if (!barrier.compareAndSet(true, false)) {
                    AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trash#refresh'." + ParametersMap.create().add("configuration", this.getConfiguration())
                            .add("p", p).add("id", id).add("isDirectory", true)));
                    return;
                }
                UnionPair<Optional<Boolean>, Throwable> result = null;
                boolean flag = true;
                try {
                    if (p.isFailure()) {
                        result = UnionPair.fail(p.getE());
                        return;
                    }
                    if (!p.getT().booleanValue()) {
                        result = ProviderInterface.TrashNotExisted;
                        return;
                    }
                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
                    this.list(id, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 1, u -> {
                        if (!barrier1.compareAndSet(true, false)) {
                            AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trash#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                    .add("u", u).add("id", id).add("isDirectory", true)));
                            return;
                        }
                        UnionPair<Optional<Boolean>, Throwable> result1 = null;
                        boolean flag1 = true;
                        try {
                            if (u.isFailure()) {
                                result1 = UnionPair.fail(u.getE());
                                return;
                            }
                            if (u.getT().isPresent()) {
                                if (u.getT().get().total() != 0) {
                                    result1 = ProviderInterface.TrashTooComplex;
                                    return;
                                }
                                final FileInformation info = this.manager.getInstance().selectInfo(id, true, null);
                                if (info == null) {
                                    result1 = ProviderInterface.TrashNotExisted;
                                    return;
                                }
                                this.loginIfNot();
                                final AtomicBoolean barrier2 = new AtomicBoolean(true);
                                this.trash0(info, n -> {
                                    if (!barrier2.compareAndSet(true, false)) {
                                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trash#trash0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                                .add("n", n).add("id", id).add("isDirectory", true)));
                                        return;
                                    }
                                    UnionPair<Optional<Boolean>, Throwable> result2 = null;
                                    try {
                                        if (n.isFailure()) {
                                            result2 = UnionPair.fail(n.getE());
                                            return;
                                        }
                                        if (!n.getT().booleanValue()) {
                                            result2 = ProviderInterface.TrashTooComplex;
                                            return;
                                        }
                                        this.manager.getInstance().deleteDirectoryRecursively(id, null);
                                        BroadcastManager.onFileTrash(this.getLocation(id), true);
                                        result2 = ProviderInterface.TrashSuccess;
                                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                        result2 = UnionPair.fail(exception);
                                    } finally {
                                        final UnionPair<Optional<Boolean>, Throwable> res = result2;
                                        assert res != null;
                                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                                    }
                                });
                                flag1 = false;
                                return;
                            }
                            result1 = ProviderInterface.TrashSuccess;
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            result1 = UnionPair.fail(exception);
                        } finally {
                            if (flag1) {
                                final UnionPair<Optional<Boolean>, Throwable> res = result1;
                                assert res != null;
                                WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                            }
                        }
                    });
                    flag = false;
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    result = UnionPair.fail(exception);
                } finally {
                    if (flag) {
                        final UnionPair<Optional<Boolean>, Throwable> res = result;
                        assert res != null;
                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                }
            });
            return;
        }
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.File, (isDirectory ? "d" : "f") + id);
        BackgroundTaskManager.background(identifier, () -> {
            try {
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.trash0(information, p -> {
                    if (!barrier.compareAndSet(true, false)) {
                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'trash#trash0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                .add("p", p).add("id", id).add("isDirectory", false)));
                        return;
                    }
                    UnionPair<Optional<Boolean>, Throwable> result = null;
                    try {
                        if (p.isFailure()) {
                            result = UnionPair.fail(p.getE());
                            return;
                        }
                        if (!p.getT().booleanValue()) {
                            result = ProviderInterface.TrashTooComplex;
                            return;
                        }
                        this.manager.getInstance().deleteFileOrDirectory(id, isDirectory, null);
                        BroadcastManager.onFileTrash(this.getLocation(id), isDirectory);
                        result = ProviderInterface.TrashSuccess;
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        final UnionPair<Optional<Boolean>, Throwable> res = result;
                        assert res != null;
                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                });
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.remove(identifier);
                WListServer.ServerExecutors.submit(() -> consumer.accept(UnionPair.fail(exception))).addListener(MiscellaneousUtil.exceptionListener());
            }
        }, false, HExceptionWrapper.wrapRunnable(() -> this.trash(id, isDirectory, consumer), e -> {
            if (e != null)
                consumer.accept(UnionPair.fail(e));
        }, true));
    }


    protected boolean doesRequireLoginDownloading(final @NotNull FileInformation information) throws Exception {
        return true;
    }

    /**
     * Get download methods of a specific file.
     * @see #doesRequireLoginDownloading(FileInformation)
     * @see #getLocation(long) to create {@code FailureReason}.
     * @see DownloadRequirements#tryGetDownloadFromUrl(OkHttpClient, HttpUrl, Headers, Long, Headers.Builder, long, long, ZonedDateTime)
     */
    protected abstract void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) throws Exception;

    @Override
    public void downloadFile(final long fileId, final long from, final long to,
                             final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(fileId, false, null);
        if (information == null) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(fileId), false))));
            return;
        }
        assert information.size() >= 0;
        final long start = Math.min(Math.max(from, 0), information.size());
        final long end = Math.min(Math.max(to, 0), information.size());
        if (start >= end) {
            consumer.accept(UnionPair.ok(UnionPair.ok(DownloadRequirements.EmptyDownloadRequirements)));
            return;
        }
        //noinspection StringConcatenationMissingWhitespace
        final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                this.getConfiguration().getName(), BackgroundTaskManager.File, "f" + fileId);
        BackgroundTaskManager.background(identifier, () -> {
            boolean flag = true;
            try {
                if (this.doesRequireLoginDownloading(information))
                    this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.download0(information, start, end, p -> {
                    if (!barrier.compareAndSet(true, false)) {
                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'downloadFile#download0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                .add("p", p).add("fileId", fileId).add("from", from).add("to", to)));
                        return;
                    }
                    BackgroundTaskManager.remove(identifier);
                    WListServer.ServerExecutors.submit(() -> consumer.accept(p)).addListener(MiscellaneousUtil.exceptionListener());
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                WListServer.ServerExecutors.submit(() -> consumer.accept(UnionPair.fail(exception))).addListener(MiscellaneousUtil.exceptionListener());
            } finally {
                if (flag)
                    BackgroundTaskManager.remove(identifier);
            }
        }, false, HExceptionWrapper.wrapRunnable(() -> this.downloadFile(fileId, from, to, consumer), e -> {
            if (e != null)
                consumer.accept(UnionPair.fail(e));
        }, true));
    }


    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> DefaultRetryBracketPair = Pair.ImmutablePair.makeImmutablePair(" (", ")");
    @Contract(pure = true)
    protected Pair.ImmutablePair<@NotNull String, @NotNull String> retryBracketPair() {
        return AbstractIdBaseProvider.DefaultRetryBracketPair;
    }

    private <R> void getDuplicatedName(final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull UnaryOperator<? super @NotNull ParametersMap> parameters, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<R, FailureReason>, Throwable>> consumer, final @NotNull BiConsumerE<? super @NotNull String, ? super BackgroundTaskManager.@NotNull BackgroundTaskIdentifier> runnable) throws Exception {
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
            if (!barrier.compareAndSet(true, false)) {
                AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'prepareNewFiles#list'." + parameters.apply(ParametersMap.create()
                        .add("configuration", this.getConfiguration()).add("p", p).add("parentId", parentId).add("name", name).add("policy", policy))));
                return;
            }
            try {
                if (p.isFailure()) {
                    consumer.accept(UnionPair.fail(p.getE()));
                    return;
                }
                if (p.getT().isEmpty()) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(parentId), true))));
                    return;
                }
                final FileManager manager = this.manager.getInstance();
                final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                        this.getConfiguration().getName(), BackgroundTaskManager.Name, String.format("%d: %s", parentId, name));
                final FileInformation duplicate = manager.selectInfoInDirectoryByName(parentId, name, null);
                if (duplicate == null && BackgroundTaskManager.createIfNot(identifier)) {
                    runnable.accept(name, identifier);
                    return;
                }
                if (policy == Options.DuplicatePolicy.ERROR) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(this.getLocation(parentId), name))));
                    return;
                }
                if (policy == Options.DuplicatePolicy.KEEP) {
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
                                BackgroundTaskManager.Name, String.format("%d: %s", parentId, n));
                    } while (manager.selectInfoInDirectoryByName(parentId, n, null) != null || !BackgroundTaskManager.createIfNot(i));
                    runnable.accept(n, i);
                    return;
                }
                assert policy == Options.DuplicatePolicy.OVER;
                if (duplicate == null) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(this.getLocation(parentId), name))));
                    return;
                }
                final Consumer<FileInformation> trashing = new Consumer<>() {
                    @Override
                    public void accept(final @NotNull FileInformation duplicate) {
                        try {
                            final AtomicBoolean barrier = new AtomicBoolean(true);
                            AbstractIdBaseProvider.this.trash(duplicate.id(), duplicate.isDirectory(), p -> {
                                if (!barrier.compareAndSet(true, false)) {
                                    AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'prepareNewFiles#trash'." + parameters.apply(ParametersMap.create()
                                            .add("configuration", AbstractIdBaseProvider.this.getConfiguration()).add("p", p).add("parentId", parentId).add("name", name).add("policy", policy).add("duplicate", duplicate))));
                                    return;
                                }
                                try {
                                    if (p.isFailure()) {
                                        consumer.accept(UnionPair.fail(p.getE()));
                                        return;
                                    }
                                    if (p.getT().isPresent()) {
                                        final FileInformation d = manager.selectInfoInDirectoryByName(parentId, name, null);
                                        if (d != null) {
                                            this.accept(d);
                                            return;
                                        }
                                        if (BackgroundTaskManager.createIfNot(identifier)) {
                                            runnable.accept(name, identifier);
                                            return;
                                        }
                                    }
                                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(AbstractIdBaseProvider.this.getLocation(parentId), name))));
                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                    consumer.accept(UnionPair.fail(exception));
                                }
                            });
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            consumer.accept(UnionPair.fail(exception));
                        }
                    }
                };
                trashing.accept(duplicate);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            }
        });
    }


    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> directoryNameChecker();

    /**
     * Create an empty directory. {@code size == 0}
     */
    @SuppressWarnings("SameParameterValue")
    protected abstract void createDirectory0(final long parentId, final @NotNull String directoryName, final @NotNull Options.DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) throws Exception;

    @Override
    public void createDirectory(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) throws Exception {
        if (!this.directoryNameChecker().test(directoryName)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), directoryName, this.directoryNameChecker().description()))));
            return;
        }
        this.getDuplicatedName(parentId, directoryName, policy, p -> p.add("caller", "createDirectory"), consumer, (name, identifier) -> {
            UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> result = null;
            boolean flag = true;
            try {
                if (!this.directoryNameChecker().test(name)) {
                    result = UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), name, this.directoryNameChecker().description())));
                    return;
                }
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.createDirectory0(parentId, name, Options.DuplicatePolicy.ERROR, p -> {
                    if (!barrier.compareAndSet(true, false)) {
                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'createDirectory#createDirectory0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                .add("p", p).add("parentId", parentId).add("directoryName", directoryName).add("name", name).add("policy", policy)));
                        return;
                    }
                    UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> result1 = null;
                    try {
                        if (p.isSuccess() && p.getT().isSuccess()) {
                            final FileInformation information = p.getT().getT();
                            assert information.isDirectory() && information.size() == 0 && information.parentId() == parentId;
                            this.manager.getInstance().insertFileOrDirectory(information, null);
                            BroadcastManager.onFileUpload(this.getConfiguration().getName(), information);
                        }
                        result1 = p;
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result1 = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        final UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> res = result1;
                        assert res != null;
                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    BackgroundTaskManager.remove(identifier);
                    final UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> res = result;
                    assert res != null;
                    WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                }
            }
        });
    }


    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> fileNameChecker();

    /**
     * Upload a file. {@code size >= 0}
     * @see UploadRequirements#splitUploadBuffer(BiConsumerE, long, int)
     */
    @SuppressWarnings("SameParameterValue")
    protected abstract void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) throws Exception;

    @Override
    public void uploadFile(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) throws Exception {
        if (!this.fileNameChecker().test(filename)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), filename, this.fileNameChecker().description()))));
            return;
        }
        if (size > this.getConfiguration().getMaxSizePerFile() || size < 0) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byExceedMaxSize(this.getLocation(parentId), size, this.getConfiguration().getMaxSizePerFile()))));
            return;
        }
        this.getDuplicatedName(parentId, filename, policy, p -> p.add("size", size).add("caller", "uploadFile"), consumer, (name, identifier) -> {
            UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable> result = null;
            boolean flag = true;
            try {
                if (!this.fileNameChecker().test(name)) {
                    result = UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), name, this.fileNameChecker().description())));
                    return;
                }
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.uploadFile0(parentId, name, size, Options.DuplicatePolicy.ERROR, p -> {
                    if (!barrier.compareAndSet(true, false)) {
                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'uploadFile#uploadFile0'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                .add("p", p).add("parentId", parentId).add("filename", filename).add("name", name).add("size", size).add("policy", policy)));
                        return;
                    }
                    if (p.isSuccess() && p.getT().isSuccess()) {
                        final UploadRequirements requirements = p.getT().getT();
                        WListServer.ServerExecutors.submit(() -> consumer.accept(UnionPair.ok(UnionPair.ok(new UploadRequirements(requirements.checksums(), c -> {
                            final UploadRequirements.UploadMethods methods = requirements.transfer().apply(c);
                            return new UploadRequirements.UploadMethods(methods.parallelMethods(), o -> {
                                final AtomicBoolean barrier1 = new AtomicBoolean(true);
                                methods.supplier().accept((Consumer<? super UnionPair<Optional<FileInformation>, Throwable>>) t -> {
                                    if (!barrier1.compareAndSet(true, false)) {
                                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'uploadFile0#supplier'." + ParametersMap.create().add("configuration", this.getConfiguration())
                                                .add("t", t).add("parentId", parentId).add("filename", filename).add("name", name).add("size", size).add("policy", policy)));
                                        return;
                                    }
                                    try {
                                        if (t.isSuccess() && t.getT().isPresent()) {
                                            final FileInformation information = t.getT().get();
                                            assert !information.isDirectory() && information.size() == size && information.parentId() == parentId;
                                            this.manager.getInstance().insertFileOrDirectory(information, null);
                                            BroadcastManager.onFileUpload(this.getConfiguration().getName(), information);
                                        }
                                        o.accept(t);
                                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                        o.accept(UnionPair.fail(exception));
                                    }
                                });
                            }, () -> {
                                BackgroundTaskManager.remove(identifier);
                                methods.finisher().run();
                            });
                        }, () -> BackgroundTaskManager.remove(identifier)))))).addListener(MiscellaneousUtil.exceptionListener());
                        return;
                    }
                    BackgroundTaskManager.remove(identifier);
                    WListServer.ServerExecutors.submit(() -> consumer.accept(p)).addListener(MiscellaneousUtil.exceptionListener());
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    BackgroundTaskManager.remove(identifier);
                    final UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable> res = result;
                    assert res != null;
                    WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                }
            }
        });
    }

//    protected abstract boolean isSupportedCopyFileDirectly(final @NotNull FileInformation information, final long parentId) throws Exception;
//
//    /**
//     * Copy a file.
//     * @param location Only by used to create {@code FailureReason}.
//     * @param parentLocation Only by used to create {@code FailureReason}.
//     * @see ProviderInterface#CopyNotSupported
//     */
//    protected abstract void copyFileDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void copyFileDirectly(final long fileId, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception {
//        if (!this.fileNameChecker().test(filename)) {
//            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, filename, this.fileNameChecker().description())))));
//            return;
//        }
//        final FileInformation information = this.manager.getInstance().selectInfo(fileId, false, null);
//        if (information == null) {
//            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(location, false)))));
//            return;
//        }
//        assert information.size() >= 0;
//        if (!this.isSupportedCopyFileDirectly(information, parentId)) {
//            consumer.accept(ProviderInterface.CopyNotSupported);
//            return;
//        }
//        final AtomicBoolean barrier = new AtomicBoolean(true);
//        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
//            if (!barrier.compareAndSet(true, false)) {
//                AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'copyFileDirectly#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
//                        .add("p", p).add("information", information).add("parentId", parentId).add("filename", filename).add("policy", policy)));
//                return;
//            }
//            try {
//                if (p.isFailure()) {
//                    consumer.accept(UnionPair.fail(p.getE()));
//                    return;
//                }
//                if (p.getT().isFailure()) {
//                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true)))));
//                    return;
//                }
//                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, filename, policy);
//                if (name == null) {
//                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, filename)))));
//                    return;
//                }
//                boolean flag = true;
//                try {
//                    if (!this.fileNameChecker().test(name.getFirst())) {
//                        consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.fileNameChecker().description())))));
//                        return;
//                    }
//                    if (parentId == information.parentId() && name.getFirst().equals(information.name())) {
//                        consumer.accept(ProviderInterface.CopySelf);
//                        return;
//                    }
//                    this.loginIfNot();
//                    flag = false;
//                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
//                    this.copyFileDirectly0(information, parentId, name.getFirst(), policy, u -> {
//                        if (!barrier1.compareAndSet(true, false)) {
//                            AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'copyFileDirectly0'." + ParametersMap.create().add("configuration", this.getConfiguration())
//                                    .add("u", u).add("information", information).add("parentId", parentId).add("filename", filename).add("name", name.getFirst()).add("policy", policy)));
//                            return;
//                        }
//                        try {
//                            if (u.isSuccess() && u.getT().isPresent() && u.getT().get().isSuccess() && u.getT().get().getT().isPresent()) {
//                                final FileInformation file = u.getT().get().getT().get();
//                                assert !file.isDirectory() && file.size() == information.size() && file.parentId() == parentId;
//                                this.manager.getInstance().insertFileOrDirectory(file, null);
//                            }
//                            consumer.accept(u);
//                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
//                            consumer.accept(UnionPair.fail(exception));
//                        } finally {
//                            BackgroundTaskManager.remove(name.getSecond());
//                        }
//                    }, location, parentLocation);
//                } finally {
//                    if (flag)
//                        BackgroundTaskManager.remove(name.getSecond());
//                }
//            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
//                consumer.accept(UnionPair.fail(exception));
//            }
//        });
    }

//    protected abstract boolean isSupportedMoveDirectly(final @NotNull FileInformation information, final long parentId) throws Exception;
//
//    /**
//     * Move a file / directory. (.size == information.size)
//     * @param location Only by used to create {@code FailureReason}.
//     * @param parentLocation Only by used to create {@code FailureReason}.
//     * @see ProviderInterface#CopyNotSupported
//     */
//    protected abstract void moveDirectly0(final @NotNull FileInformation information, final long parentId, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

    @Override
    public void moveDirectly(final long id, final boolean isDirectory, final long parentId, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception {
//        final FileInformation information;
//        UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> res = null;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.manager.getInstance().getConnection(null, connectionId)) {
//            information = this.manager.getInstance().selectInfo(id, isDirectory, connectionId.get());
//            //noinspection VariableNotUsedInsideIf
//            if (information == null) {
//                res = UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(location, isDirectory))));
//            } else
//                if (isDirectory && this.manager.getInstance().isInDirectoryRecursively(parentId, true, id, connectionId.get()))
//                    res = ProviderInterface.MoveSelf;
//            connection.commit();
//        }
//        if (res != null) {
//            consumer.accept(res);
//            return;
//        }
//        if (!this.isSupportedMoveDirectly(information, parentId)) {
//            consumer.accept(ProviderInterface.MoveNotSupported);
//            return;
//        }
//        final AtomicBoolean barrier = new AtomicBoolean(true);
//        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, p -> {
//            if (!barrier.compareAndSet(true, false)) {
//                AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'moveDirectly#list'." + ParametersMap.create().add("configuration", this.getConfiguration())
//                        .add("p", p).add("information", information).add("parentId", parentId).add("policy", policy)));
//                return;
//            }
//            try {
//                if (p.isFailure()) {
//                    consumer.accept(UnionPair.fail(p.getE()));
//                    return;
//                }
//                if (p.getT().isFailure()) {
//                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(parentLocation, true)))));
//                    return;
//                }
//                final Pair.ImmutablePair<String, BackgroundTaskManager.BackgroundTaskIdentifier> name = this.getDuplicatedName(parentId, information.name(), policy);
//                if (name == null) {
//                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byDuplicateError(parentLocation, information.name())))));
//                    return;
//                }
//                boolean flag = true;
//                try {
//                    if (!this.directoryNameChecker().test(name.getFirst())) {
//                        consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(parentLocation, name.getFirst(), this.directoryNameChecker().description())))));
//                        return;
//                    }
//                    if (!name.getFirst().equals(information.name())) { // TODO temp dir. (require move twice.)
//                        consumer.accept(ProviderInterface.MoveNotSupported);
//                        return;
//                    }
//                    this.loginIfNot();
//                    flag = false;
//                    final AtomicBoolean barrier1 = new AtomicBoolean(true);
//                    this.moveDirectly0(information, parentId, policy, u -> {
//                        if (!barrier1.compareAndSet(true, false)) {
//                            AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when 'moveDirectly0'." + ParametersMap.create().add("configuration", this.getConfiguration())
//                                    .add("u", u).add("information", information).add("parentId", parentId).add("name", name.getFirst()).add("policy", policy)));
//                            return;
//                        }
//                        try {
//                            if (u.isSuccess() && u.getT().isPresent() && u.getT().get().isSuccess() && u.getT().get().getT().isPresent()) {
//                                final FileInformation file = u.getT().get().getT().get();
//                                assert file.isDirectory() == isDirectory && file.size() == information.size() && file.parentId() == parentId;
//                                this.manager.getInstance().updateOrInsertFileOrDirectory(file, null);
//                            }
//                            consumer.accept(u);
//                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
//                            consumer.accept(UnionPair.fail(exception));
//                        } finally {
//                            BackgroundTaskManager.remove(name.getSecond());
//                        }
//                    }, location, parentLocation);
//                } finally {
//                    if (flag)
//                        BackgroundTaskManager.remove(name.getSecond());
//                }
//            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
//                consumer.accept(UnionPair.fail(exception));
//            }
//        });
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
