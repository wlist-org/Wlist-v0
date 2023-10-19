package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.BiConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
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
import com.xuxiaocheng.WList.Server.Operations.Helpers.ProgressBar;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Records.RefreshRequirements;
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
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@SuppressWarnings({"OverlyBroadThrowsClause", "SameParameterValue"})
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


    protected final @NotNull AtomicReference<ZonedDateTime> loginExpireTime = new AtomicReference<>();

    protected abstract @Nullable ZonedDateTime loginIfNot0() throws Exception;

    protected void loginIfNot() throws Exception {
        synchronized (this.loginExpireTime) {
            if (this.loginExpireTime.get() == null || MiscellaneousUtil.now().isAfter(this.loginExpireTime.get())) {
                BroadcastManager.onProviderLogin(this.getConfiguration().getName(), true);
                this.loginExpireTime.set(this.loginIfNot0());
                BroadcastManager.onProviderLogin(this.getConfiguration().getName(), false);
            }
        }
    }


    private <T> boolean checkBarrier(final @NotNull AtomicBoolean barrier, final T t, final @NotNull String caller, final @NotNull UnaryOperator<? super @NotNull ParametersMap> parameters) {
        if (!barrier.compareAndSet(true, false)) {
            AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, new RuntimeException("Duplicate message when '" + caller + "'." + parameters.apply(ParametersMap.create()
                    .add("configuration", this.getConfiguration()).add("msg", t))));
            return true;
        }
        return false;
    }

    private <T, U> boolean transferException(final @NotNull UnionPair<T, ? extends Throwable> t, final BackgroundTaskManager.@Nullable BackgroundTaskIdentifier identifier, final @NotNull Consumer<? super @NotNull UnionPair<U, Throwable>> consumer) {
        if (t.isFailure()) {
            if (identifier != null)
                BackgroundTaskManager.remove(identifier);
            consumer.accept(UnionPair.fail(t.getE()));
            return true;
        }
        return false;
    }

    private <T> void consume(final @NotNull T result, final @NotNull Consumer<? super @NotNull T> consumer) {
        WListServer.ServerExecutors.submit(() -> consumer.accept(result)).addListener(MiscellaneousUtil.exceptionListener());
    }

    private <T> @NotNull Runnable wrapComplete(final @NotNull RunnableE runnable, final @NotNull Consumer<? super @NotNull UnionPair<T, Throwable>> consumer) {
        return HExceptionWrapper.wrapRunnable(runnable, e -> {
            if (e != null)
                 consumer.accept(UnionPair.fail(e));
        }, true);
    }


    protected void onTrash(final long id, final boolean isDirectory) throws SQLException {
        if (!isDirectory)
            ProviderUtil.removeDownloadUrlCache(this.getLocation(id));
        this.manager.getInstance().deleteFileOrDirectory(id, isDirectory, null);
        WListServer.IOExecutors.execute(() -> BroadcastManager.onFileTrash(this.getLocation(id), isDirectory));
    }

    protected void onUpdate(final @NotNull FileInformation information) throws SQLException {
        this.manager.getInstance().updateOrInsertFileOrDirectory(information, null);
        WListServer.IOExecutors.execute(() -> BroadcastManager.onFileUpdate(this.getLocation(information.id()), information.isDirectory()));
    }

    protected void onUpload(final @NotNull FileInformation information) throws SQLException {
        this.manager.getInstance().insertFileOrDirectory(information, null);
        WListServer.IOExecutors.execute(() -> BroadcastManager.onFileUpload(this.getConfiguration().getName(), information));
    }


    public static final @NotNull UnionPair<Optional<Iterator<FileInformation>>, Throwable> ListNotExisted = UnionPair.ok(Optional.empty());
    /**
     * List the files in the directory.
     * @param progress {@link ProgressBar#progress(int, long)} or {@link ProgressBar#addStage(long)} when iterating.
     * @param consumer empty: directory is not existed. present: list of files.
     * @throws Exception: Any iterating exception can be wrapped in {@link NoSuchElementException} and then thrown in method {@code next()}.
     * @see #ListNotExisted
     * @see ProviderUtil#wrapSuppliersInPages(ConsumerE, Executor, BiConsumerE, Consumer)
     */
    protected abstract void list0(final long directoryId, final @Nullable ProgressBar progress, final @NotNull Consumer<? super UnionPair<Optional<Iterator<FileInformation>>, Throwable>> consumer) throws Exception;

    @Override
    public void list(final long directoryId, final Options.@NotNull FilterPolicy filter,
                     final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders,
                     final long position, final int limit,
                     final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FilesListInformation, RefreshRequirements>>, Throwable>> consumer) throws Exception {
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
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(information))));
            return;
        }
        // Not indexed.
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.refreshDirectory(directoryId, t -> {
            if (this.checkBarrier(barrier, t, "list#refreshDirectory", p -> p.add("directoryId", directoryId))
                    || this.transferException(t, null, consumer)) return;
            this.consume(t.getT().isPresent() ? UnionPair.ok(Optional.of(UnionPair.fail(t.getT().get()))) : ProviderInterface.ListNotExisted, consumer);
        });
    }


    @Contract(pure = true)
    protected boolean doesRequireUpdate(final @NotNull FileInformation information) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> UpdateNoRequired = UnionPair.ok(UnionPair.fail(Boolean.FALSE));
    public static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> UpdateNotExisted = UnionPair.ok(UnionPair.fail(Boolean.TRUE));
    /**
     * Try to update the file/directory information. (Should call {@link #loginIfNot()} manually.)
     * @param consumer false: needn't updated. true: file is not existed. success: updated.
     * @see #doesRequireUpdate(FileInformation)
     * @see #UpdateNotExisted
     */ // TODO: in recycler.
    protected abstract void update0(final @NotNull FileInformation oldInformation, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) throws Exception;

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
                this.update0(information, t -> {
                    if (this.checkBarrier(barrier, t, "info#update0", p -> p.add("information", information))
                        || this.transferException(t, identifier, consumer)) return;
                    UnionPair<Optional<FileInformation>, Throwable> result = null;
                    try {
                        if ((t.getT().isFailure() && !t.getT().getE().booleanValue()) || (t.getT().isSuccess() && information.equals(t.getT().getT()))) {
                            result = UnionPair.ok(Optional.of(information));
                            return;
                        }
                        if (t.getT().isFailure()) { // && t.getT().getE().booleanValue()
                            this.onTrash(id, isDirectory);
                            result = ProviderInterface.InfoNotExisted;
                            return;
                        }
                        final FileInformation updated = t.getT().getT();
                        assert updated.isDirectory() == isDirectory;
                        final FileInformation realInfo;
                        final FileManager manager = this.manager.getInstance();
                        final AtomicReference<String> connectionId = new AtomicReference<>();
                        try (final Connection connection = manager.getConnection(null, connectionId)) {
                            manager.updateOrInsertFileOrDirectory(updated, connectionId.get());
                            realInfo = manager.selectInfo(id, isDirectory, connectionId.get());
                            connection.commit();
                        }
                        WListServer.IOExecutors.execute(() -> BroadcastManager.onFileUpdate(this.getLocation(updated.id()), updated.isDirectory()));
                        if (realInfo == null) {
                            result = ProviderInterface.InfoNotExisted;
                            return;
                        }
                        result = UnionPair.ok(Optional.of(realInfo));
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result != null;
                        this.consume(result, consumer);
                    }
                });
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.remove(identifier);
                this.consume(UnionPair.fail(exception), consumer);
            }
        }, false, this.wrapComplete(() -> this.info(id, isDirectory, consumer), consumer));
    }


    @Contract(pure = true)
    protected boolean doesSupportInfo(final boolean isDirectory) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<Optional<FileInformation>, Throwable> InfoNotExist = UnionPair.ok(Optional.empty());
    /**
     * Try to get the file/directory information by id. (not care size.)
     * @param consumer empty: not existed / unsupported. present: information.
     * @see #doesSupportInfo(boolean)
     * @see #InfoNotExisted
     */
    protected abstract void info0(final long id, final boolean isDirectory, final @NotNull Consumer<? super UnionPair<Optional<FileInformation>, Throwable>> consumer) throws Exception;

    @Override
    public void refreshDirectory(final long directoryId,
                                 final @NotNull Consumer<? super @NotNull UnionPair<Optional<RefreshRequirements>, Throwable>> c) throws Exception {
        final FileInformation d = this.manager.getInstance().selectInfo(directoryId, true, null);
        if (d == null) {
            c.accept(ProviderInterface.RefreshNotExisted);
            return;
        }
        c.accept(UnionPair.ok(Optional.of(new RefreshRequirements((consumer, progress) -> {
            final FileInformation directory = this.manager.getInstance().selectInfo(directoryId, true, null);
            if (directory == null) {
                consumer.accept(null);
                return;
            }
            final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                    this.getConfiguration().getName(), BackgroundTaskManager.Directory, String.valueOf(directoryId));
            BackgroundTaskManager.background(identifier, () -> {
                Throwable result = null;
                boolean flag = true;
                try {
                    this.loginIfNot();
                    final AtomicBoolean barrier = new AtomicBoolean(true);
                    this.list0(directoryId, progress, t -> {
                        if (this.checkBarrier(barrier, t, "refreshDirectory#list0", p -> p.add("directoryId", directoryId))) return;
                        Throwable result1 = null;
                        boolean flag1 = true;
                        try {
                            if (t.isFailure()) {
                                result1 = t.getE();
                                return;
                            }
                            if (t.getT().isPresent()) {
                                final Iterator<FileInformation> iterator = t.getT().get();
                                final Set<Long> extraFiles, extraDirectories;
                                final Collection<Long> updatedFiles = new HashSet<>(), updatedDirectories = new HashSet<>();
                                final FileManager manager = this.manager.getInstance();
                                final AtomicReference<String> connectionId = new AtomicReference<>();
                                try (final Connection connection = manager.getConnection(null, connectionId)) {
                                    final Pair.ImmutablePair<Set<Long>, Set<Long>> old = manager.selectIdsInDirectory(directoryId, connectionId.get());
                                    extraFiles = old.getFirst();
                                    extraDirectories = old.getSecond();
                                    if (directory.size() == -1 && extraFiles.isEmpty() && extraDirectories.isEmpty()) // Not indexed.
                                        manager.calculateDirectorySize(directoryId, connectionId.get());
                                    try {
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
                                    } catch (final NoSuchElementException exception) {
                                        if (exception.getCause() != null) {
                                            result1 = exception.getCause() instanceof Exception e ? e : exception;
                                            return;
                                        }
                                        AbstractIdBaseProvider.logger.log(HLogLevel.MISTAKE, "No more elements when 'refreshDirectory#list0'.", ParametersMap.create().add("configuration", this.getConfiguration())
                                                .add("t", t).add("directoryId", directoryId), exception);
                                    }
                                    connection.commit();
                                }
                                if (extraFiles.isEmpty() && extraDirectories.isEmpty()) {
                                    WListServer.IOExecutors.submit(() -> {
                                        BackgroundTaskManager.remove(identifier);
                                        for (final Long id: updatedDirectories)
                                            BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), true);
                                        for (final Long id: updatedFiles)
                                            BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), false);
                                        consumer.accept(null);
                                    }).addListener(MiscellaneousUtil.exceptionListener());
                                    flag1 = false;
                                    return;
                                }
                                final Collection<Long> deleteFiles = new HashSet<>(), deleteDirectories = new HashSet<>();
                                final AtomicLong total = new AtomicLong(1);
                                final Runnable finisher = () -> {
                                    if (total.getAndDecrement() <= 1) {
                                        BackgroundTaskManager.remove(identifier);
                                        WListServer.IOExecutors.submit(() -> {
                                            for (final Long id: deleteDirectories)
                                                BroadcastManager.onFileTrash(this.getLocation(id.longValue()), true);
                                            for (final Long id: deleteFiles)
                                                BroadcastManager.onFileTrash(this.getLocation(id.longValue()), false);
                                            for (final Long id: updatedDirectories)
                                                BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), true);
                                            for (final Long id: updatedFiles)
                                                BroadcastManager.onFileUpdate(this.getLocation(id.longValue()), false);
                                            consumer.accept(null);
                                        }).addListener(MiscellaneousUtil.exceptionListener());
                                    }
                                };
                                try (final Connection connection = manager.getConnection(null, connectionId)) {
                                    if (this.doesSupportInfo(false)) {
                                        total.getAndAdd(extraFiles.size());
                                        for (final Long id: extraFiles) {
                                            final AtomicBoolean barrier1 = new AtomicBoolean(true);
                                            final Consumer<UnionPair<Optional<FileInformation>, Throwable>> handler = u -> {
                                                if (this.checkBarrier(barrier1, u, "refreshDirectory#info0", p -> p.add("directoryId", directoryId).add("id", id).add("isDirectory", false))) return;
                                                try (connection) {
                                                    if (u.isFailure())
                                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to get the information after refreshing.", ParametersMap.create()
                                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", false), u.getE());
                                                    if (u.isSuccess() && u.getT().isPresent()) {
                                                        manager.updateOrInsertFile(u.getT().get(), connectionId.get());
                                                        updatedFiles.add(id);
                                                    } else {
                                                        manager.deleteFile(id.longValue(), connectionId.get());
                                                        deleteFiles.add(id);
                                                    }
                                                    connection.commit();
                                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                                    AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to update the information after refreshing and getting.", ParametersMap.create()
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
                                    } else {
                                        for (final Long id: extraFiles)
                                            manager.deleteFile(id.longValue(), connectionId.get());
                                        deleteFiles.addAll(extraFiles);
                                    }
                                    if (this.doesSupportInfo(true)) {
                                        total.getAndAdd(extraDirectories.size());
                                        for (final Long id: extraDirectories) {
                                            final AtomicBoolean barrier1 = new AtomicBoolean(true);
                                            final Consumer<UnionPair<Optional<FileInformation>, Throwable>> handler = u -> {
                                                if (this.checkBarrier(barrier1, u, "refreshDirectory#info0", p -> p.add("directoryId", directoryId).add("id", id).add("isDirectory", true))) return;
                                                try (connection) {
                                                    if (u.isFailure())
                                                        AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to get the information after refreshing.", ParametersMap.create()
                                                                .add("directoryId", directoryId).add("id", id).add("isDirectory", true), u.getE());
                                                    if (u.isSuccess() && u.getT().isPresent()) {
                                                        manager.updateOrInsertDirectory(u.getT().get(), connectionId.get());
                                                        updatedDirectories.add(id);
                                                    } else {
                                                        manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                                                        deleteDirectories.add(id);
                                                    }
                                                    connection.commit();
                                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                                    AbstractIdBaseProvider.logger.log(HLogLevel.WARN, "Failed to update the information after refreshing and getting.", ParametersMap.create()
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
                                    } else {
                                        for (final Long id: extraDirectories)
                                            manager.deleteDirectoryRecursively(id.longValue(), connectionId.get());
                                        deleteDirectories.addAll(extraDirectories);
                                    }
                                    connection.commit();
                                }
                                finisher.run();
                                flag1 = false;
                            } else
                                this.onTrash(directoryId, true);
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            result1 = exception;
                        } finally {
                            if (flag1) {
                                BackgroundTaskManager.remove(identifier);
                                final Throwable res = result1;
                                WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                            }
                        }
                    });
                    flag = false;
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    result = exception;
                } finally {
                    if (flag) {
                        BackgroundTaskManager.remove(identifier);
                        final Throwable res = result;
                        WListServer.ServerExecutors.submit(() -> consumer.accept(res)).addListener(MiscellaneousUtil.exceptionListener());
                    }
                }
            }, false, () -> consumer.accept(null));
        }, RunnableE.EmptyRunnable))));
    }


    @Contract(pure = true)
    protected boolean doesSupportTrashNotEmptyDirectory() {
        return false;
    }
    public static final @NotNull UnionPair<Boolean, Throwable> TrashNotSupport = UnionPair.ok(Boolean.FALSE);
    public static final @NotNull UnionPair<Boolean, Throwable> TrashSuccess = UnionPair.ok(Boolean.TRUE);
    /**
     * Trash a file/directory.
     * @param consumer false: not support. true: success.
     * @see #doesSupportTrashNotEmptyDirectory()
     * @see #TrashNotSupport
     * @see #TrashSuccess
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
            this.refreshDirectory(id, t -> {
                if (this.checkBarrier(barrier, t, "trash#refresh", p -> p.add("id", id)) || this.transferException(t, null, consumer)) return;
                UnionPair<Optional<Boolean>, Throwable> result = null;
                boolean flag = true;
                try {
                    if (t.getT().isPresent()) {
                        final AtomicBoolean barrier1 = new AtomicBoolean(true);
                        t.getT().get().runner().accept((Consumer<? super Throwable>) h -> {
                            if (this.checkBarrier(barrier1, h, "trash#refresh#runner", p -> p.add("id", id))) return;
                            if (h != null) {
                                this.consume(UnionPair.fail(h), consumer);
                                return;
                            }
                            try {
                                final AtomicBoolean barrier2 = new AtomicBoolean(true);
                                this.list(id, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 1, u -> {
                                    if (this.checkBarrier(barrier2, u, "trash#list", p -> p.add("id", id)) || this.transferException(u, null, consumer)) return;
                                    UnionPair<Optional<Boolean>, Throwable> result1 = null;
                                    boolean flag1 = true;
                                    try {
                                        if (u.getT().isPresent()) {
                                            if (u.getT().get().isFailure()) {
                                                u.getT().get().getE().canceller().run();
                                                result1 = ProviderInterface.TrashTooComplex;
                                                return;
                                            }
                                            if (u.getT().get().getT().total() != 0) {
                                                result1 = ProviderInterface.TrashTooComplex;
                                                return;
                                            }
                                            final FileInformation info = this.manager.getInstance().selectInfo(id, true, null);
                                            if (info == null) {
                                                result1 = ProviderInterface.TrashNotExisted;
                                                return;
                                            }
                                            this.loginIfNot();
                                            final AtomicBoolean barrier3 = new AtomicBoolean(true);
                                            this.trash0(info, n -> {
                                                if (this.checkBarrier(barrier3, n, "trash#trash0", p -> p.add("information", info)) || this.transferException(n, null, consumer)) return;
                                                UnionPair<Optional<Boolean>, Throwable> result2 = null;
                                                try {
                                                    if (!n.getT().booleanValue()) {
                                                        result2 = ProviderInterface.TrashTooComplex;
                                                        return;
                                                    }
                                                    this.onTrash(id, true);
                                                    result2 = ProviderInterface.TrashSuccess;
                                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                                    result2 = UnionPair.fail(exception);
                                                } finally {
                                                    assert result2 != null;
                                                    this.consume(result2, consumer);
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
                                            assert result1 != null;
                                            this.consume(result1, consumer);
                                        }
                                    }
                                });
                            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                this.consume(UnionPair.fail(exception), consumer);
                            }
                        }, null);
                        flag = false;
                    } else result = ProviderInterface.TrashNotExisted;
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    result = UnionPair.fail(exception);
                } finally {
                    if (flag) {
                        assert result != null;
                        this.consume(result, consumer);
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
                this.trash0(information, t -> {
                    if (this.checkBarrier(barrier, t, "trash#trash0", p -> p.add("information", information)) || this.transferException(t, identifier, consumer)) return;
                    UnionPair<Optional<Boolean>, Throwable> result = null;
                    try {
                        if (!t.getT().booleanValue()) {
                            result = ProviderInterface.TrashTooComplex;
                            return;
                        }
                        this.onTrash(id, isDirectory);
                        result = ProviderInterface.TrashSuccess;
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result != null;
                        this.consume(result, consumer);
                    }
                });
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                BackgroundTaskManager.remove(identifier);
                this.consume(UnionPair.fail(exception), consumer);
            }
        }, false, this.wrapComplete(() -> this.trash(id, isDirectory, consumer), consumer));
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
                this.download0(information, start, end, t -> {
                    if (this.checkBarrier(barrier, t, "downloadFile#download0", p -> p.add("information", information).add("start", start).add("end", end))) return;
                    BackgroundTaskManager.remove(identifier);
                    this.consume(t, consumer);
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                this.consume(UnionPair.fail(exception), consumer);
            } finally {
                if (flag)
                    BackgroundTaskManager.remove(identifier);
            }
        }, false, this.wrapComplete(() -> this.downloadFile(fileId, from, to, consumer), consumer));
    }


    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> DefaultRetryBracketPair = Pair.ImmutablePair.makeImmutablePair(" (", ")");
    @Contract(pure = true)
    protected Pair.ImmutablePair<@NotNull String, @NotNull String> retryBracketPair() {
        return AbstractIdBaseProvider.DefaultRetryBracketPair;
    }

    private <R> void getDuplicatedName(final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull UnaryOperator<@NotNull ParametersMap> parameters, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<R, FailureReason>, Throwable>> consumer, final @NotNull BiConsumerE<? super @NotNull String, ? super BackgroundTaskManager.@NotNull BackgroundTaskIdentifier> runnable) throws Exception {
        final AtomicBoolean barrier = new AtomicBoolean(true);
        this.list(parentId, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0, t -> {
            if (this.checkBarrier(barrier, t, "getDuplicatedName#list", p -> parameters.apply(p.add("parentId", parentId))) || this.transferException(t, null, consumer)) return;
            UnionPair<UnionPair<R, FailureReason>, Throwable> result = null;
            boolean flag = true;
            try {
                if (t.getT().isPresent()) {
                    final Runnable handler = () -> {
                        UnionPair<UnionPair<R, FailureReason>, Throwable> result1 = null;
                        boolean flag1 = true;
                        try {
                            final FileManager manager = this.manager.getInstance();
                            final BackgroundTaskManager.BackgroundTaskIdentifier identifier = new BackgroundTaskManager.BackgroundTaskIdentifier(
                                    this.getConfiguration().getName(), BackgroundTaskManager.Name, String.format("%d: %s", parentId, name));
                            final FileInformation duplicate = manager.selectInfoInDirectoryByName(parentId, name, null);
                            if (duplicate == null && BackgroundTaskManager.createIfNot(identifier)) {
                                runnable.accept(name, identifier);
                                flag1 = false;
                                return;
                            }
                            if (policy == Options.DuplicatePolicy.ERROR) {
                                result1 = UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(this.getLocation(parentId), name)));
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
                                flag1 = false;
                                return;
                            }
                            assert policy == Options.DuplicatePolicy.OVER;
                            if (duplicate == null) {
                                result1 = UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(this.getLocation(parentId), name)));
                                return;
                            }
                            final ConsumerE<FileInformation> trashing = new ConsumerE<>() {
                                @Override
                                public void accept(final @NotNull FileInformation duplicate) throws Exception {
                                    final AtomicBoolean barrier = new AtomicBoolean(true);
                                    AbstractIdBaseProvider.this.trash(duplicate.id(), duplicate.isDirectory(), t -> {
                                        if (AbstractIdBaseProvider.this.checkBarrier(barrier, t, "getDuplicatedName#trash", p -> parameters.apply(p.add("duplicate", duplicate)))
                                                || AbstractIdBaseProvider.this.transferException(t, null, consumer))
                                            return;
                                        UnionPair<UnionPair<R, FailureReason>, Throwable> result = null;
                                        boolean flag = true;
                                        try {
                                            if (t.getT().isPresent()) {
                                                final FileInformation d = manager.selectInfoInDirectoryByName(parentId, name, null);
                                                if (d != null) {
                                                    this.accept(d);
                                                    flag = false;
                                                    return;
                                                }
                                                if (BackgroundTaskManager.createIfNot(identifier)) {
                                                    runnable.accept(name, identifier);
                                                    flag = false;
                                                    return;
                                                }
                                            }
                                            result = UnionPair.ok(UnionPair.fail(FailureReason.byDuplicateError(AbstractIdBaseProvider.this.getLocation(parentId), name)));
                                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                            result = UnionPair.fail(exception);
                                        } finally {
                                            if (flag) {
                                                assert result != null;
                                                AbstractIdBaseProvider.this.consume(result, consumer);
                                            }
                                        }
                                    });
                                }
                            };
                            trashing.accept(duplicate);
                            flag1 = false;
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            result1 = UnionPair.fail(exception);
                        } finally {
                            if (flag1) {
                                assert result1 != null;
                                this.consume(result1, consumer);
                            }
                        }
                    };
                    if (t.getT().get().isFailure()) {
                        t.getT().get().getE().runner().accept((Consumer<? super Throwable>) h -> {
                            if (h != null)
                                this.consume(UnionPair.fail(h), consumer);
                            else
                                handler.run();
                        }, new ProgressBar()); // TODO: progress when CMR... and ensure user's permission.
                    } else handler.run();
                    flag = false;
                } else result = UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(parentId), true)));
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    assert result != null;
                    this.consume(result, consumer);
                }
            }
        });
    }


    @Contract(pure = true)
    protected abstract @NotNull CheckRule<@NotNull String> directoryNameChecker();

    /**
     * Create an empty directory. {@code size == 0}
     */
    protected abstract void create0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) throws Exception;

    @Override
    public void createDirectory(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) throws Exception {
        final CheckRule<String> nameChecker = this.directoryNameChecker();
        if (!nameChecker.test(directoryName)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), directoryName, nameChecker.description()))));
            return;
        }
        this.getDuplicatedName(parentId, directoryName, policy, p -> p.add("caller", "createDirectory"), consumer, (name, identifier) -> {
            UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> result = null;
            boolean flag = true;
            try {
                if (!nameChecker.test(name)) {
                    result = UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), name, nameChecker.description())));
                    return;
                }
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.create0(parentId, name, Options.DuplicatePolicy.ERROR, t -> {
                    if (this.checkBarrier(barrier, t, "createDirectory#create0", p -> p.add("parentId", parentId))) return;
                    UnionPair<UnionPair<FileInformation, FailureReason>, Throwable> result1 = null;
                    try {
                        if (t.isSuccess() && t.getT().isSuccess()) {
                            final FileInformation information = t.getT().getT();
                            assert information.isDirectory() && information.size() == 0 && information.parentId() == parentId;
                            this.onUpload(information);
                        }
                        result1 = t;
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result1 = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result1 != null;
                        this.consume(result1, consumer);
                    }
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    BackgroundTaskManager.remove(identifier);
                    assert result != null;
                    this.consume(result, consumer);
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
    protected abstract void upload0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) throws Exception;

    @Override
    public void uploadFile(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) throws Exception {
        final CheckRule<String> nameChecker = this.fileNameChecker();
        if (!nameChecker.test(filename)) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), filename, nameChecker.description()))));
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
                if (!nameChecker.test(name)) {
                    result = UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), name, nameChecker.description())));
                    return;
                }
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.upload0(parentId, name, size, Options.DuplicatePolicy.ERROR, t -> {
                    if (this.checkBarrier(barrier, t, "uploadFile#upload0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size))) return;
                    if (t.isSuccess() && t.getT().isSuccess()) {
                        final UploadRequirements requirements = t.getT().getT();
                        this.consume(UnionPair.ok(UnionPair.ok(new UploadRequirements(requirements.checksums(), c -> {
                            final UploadRequirements.UploadMethods methods = requirements.transfer().apply(c);
                            return new UploadRequirements.UploadMethods(methods.parallelMethods(), o -> {
                                final AtomicBoolean barrier1 = new AtomicBoolean(true);
                                methods.supplier().accept((Consumer<? super UnionPair<Optional<FileInformation>, Throwable>>) u -> {
                                    if (this.checkBarrier(barrier1, u, "uploadFile#supplier", p -> p.add("parentId", parentId).add("filename", filename).add("size", size))) return;
                                    try {
                                        if (u.isSuccess() && u.getT().isPresent()) {
                                            final FileInformation information = u.getT().get();
                                            assert !information.isDirectory() && information.size() == size && information.parentId() == parentId;
                                            this.onUpload(information);
                                        }
                                        o.accept(u);
                                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                        o.accept(UnionPair.fail(exception));
                                    }
                                });
                            }, () -> {
                                BackgroundTaskManager.remove(identifier);
                                methods.finisher().run();
                            });
                        }, () -> BackgroundTaskManager.remove(identifier)))), consumer);
                        return;
                    }
                    BackgroundTaskManager.remove(identifier);
                    this.consume(t, consumer);
                });
                flag = false;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                result = UnionPair.fail(exception);
            } finally {
                if (flag) {
                    BackgroundTaskManager.remove(identifier);
                    assert result != null;
                    this.consume(result, consumer);
                }
            }
        });
    }


    private boolean checkCMNameAvailable(final @NotNull String name, final boolean isDirectory, final long parentId, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) {
        final CheckRule<String> nameChecker = isDirectory ? this.directoryNameChecker() : this.fileNameChecker();
        if (!nameChecker.test(name)) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(FailureReason.byInvalidName(this.getLocation(parentId), name, nameChecker.description()))))));
            return true;
        }
        return false;
    }
    private @Nullable FileInformation checkCMAvailable(final long id, final boolean isDirectory, final long parentId, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) throws SQLException {
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(FailureReason.byNoSuchFile(this.getLocation(id), false))))));
            return null;
        }
        assert isDirectory || information.size() >= 0;
        if (isDirectory && this.manager.getInstance().isInDirectoryRecursively(parentId, true, id, null)) {
            consumer.accept(ProviderInterface.CMToInside);
            return null;
        }
        return information;
    }
    private @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> transferCMDuplicateNameConsumer(final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) {
        return p -> {
            if (p.isFailure()) {
                consumer.accept(UnionPair.fail(p.getE()));
                return;
            }
            if (p.getT().isFailure()) {
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(p.getT().getE())))));
                return;
            }
//            assert false;
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(p.getT().getT()))));
        };
    }


    protected boolean doesSupportCopyDirectly(final @NotNull FileInformation information, final long parentId) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable> CopyNotSupport = UnionPair.ok(Optional.empty());
    /**
     * Copy a file/directory. (usually provided by upload same file.) {@code size == (isDirectory ? -1 : information.size())}
     * @see #doesSupportCopyDirectly(FileInformation, long)
     * @see #CopyNotSupport
     */
    protected abstract void copyDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) throws Exception;

    @Override
    public void copyDirectly(final long id, final boolean isDirectory, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) throws Exception {
        if (this.checkCMNameAvailable(name, isDirectory, parentId, consumer))
            return;
        final FileInformation information = this.checkCMAvailable(id, isDirectory, parentId, consumer);
        if (information == null)
            return;
        if (!this.doesSupportCopyDirectly(information, parentId)) {
            consumer.accept(ProviderInterface.CMTooComplex);
            return;
        }
        this.getDuplicatedName(parentId, name, policy, p -> p.add("information", information).add("caller", "copyDirectly"), this.transferCMDuplicateNameConsumer(consumer), (duplicatedName, identifier) -> {
            boolean flag = true;
            try {
                if (this.checkCMNameAvailable(duplicatedName, isDirectory, parentId, consumer))
                    return;
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.copyDirectly0(information, parentId, duplicatedName, Options.DuplicatePolicy.ERROR, t -> {
                    if (this.checkBarrier(barrier, t, "copyDirectly#copyDirectly0", p -> p.add("information", information).add("parentId", parentId).add("name", duplicatedName))
                            || this.transferException(t, identifier, consumer)) return;
                    UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable> result = null;
                    try {
                        if (t.getT().isPresent()) {
                            if (t.getT().get().isFailure()) {
                                result = UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(t.getT().get().getE()))));
                                return;
                            }
                            final FileInformation info = t.getT().get().getT();
                            assert info.isDirectory() == isDirectory && info.size() == (isDirectory ? -1 : information.size()) && info.parentId() == parentId;
                            // this.onUpload(info, null);
                            this.manager.getInstance().updateOrInsertFileOrDirectory(info, null); // size != 0.
                            BroadcastManager.onFileUpload(this.getConfiguration().getName(), info);
                            result = UnionPair.ok(Optional.of(UnionPair.ok(info)));
                        } else result = UnionPair.ok(Optional.empty());
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result != null;
                        this.consume(result, consumer);
                    }
                });
                flag = false;
            } finally {
                if (flag)
                    BackgroundTaskManager.remove(identifier);
            }
        });
    }


    protected boolean doesSupportMoveDirectly(final @NotNull FileInformation information, final long parentId) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable> MoveNotSupport = UnionPair.ok(Optional.empty());
    /**
     * Move a file / directory. {@code size == information.size()}
     * @see #doesSupportMoveDirectly(FileInformation, long)
     * @see #MoveNotSupport
     */
    protected abstract void moveDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) throws Exception;

    @Override
    public void moveDirectly(final long id, final boolean isDirectory, final long parentId, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) throws Exception {
        final FileInformation information = this.checkCMAvailable(id, isDirectory, parentId, consumer);
        if (information == null)
            return;
        if (information.parentId() == parentId) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(information))));
            return;
        }
        if (this.checkCMNameAvailable(information.name(), isDirectory, parentId, consumer))
            return;
        if (!this.doesSupportMoveDirectly(information, parentId)) {
            consumer.accept(ProviderInterface.CMTooComplex);
            return;
        }
        this.getDuplicatedName(parentId, information.name(), policy, p -> p.add("information", information).add("caller", "moveDirectly"), this.transferCMDuplicateNameConsumer(consumer), (name, identifier) -> {
            boolean flag = true;
            try {
                if (this.checkCMNameAvailable(name, isDirectory, parentId, consumer))
                    return;
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.moveDirectly0(information, parentId, name, Options.DuplicatePolicy.ERROR, t -> {
                    if (this.checkBarrier(barrier, t, "moveDirectly#moveDirectly0", p -> p.add("information", information).add("parentId", parentId).add("name", name))
                            || this.transferException(t, identifier, consumer)) return;
                    UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable> result = null;
                    try {
                        if (t.getT().isPresent()) {
                            if (t.getT().get().isFailure()) {
                                result = UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(t.getT().get().getE()))));
                                return;
                            }
                            final FileInformation info = t.getT().get().getT();
                            assert info.id() == id;
                            assert info.isDirectory() == isDirectory && info.size() == information.size() && info.parentId() == parentId;
                            this.onUpdate(info);
                            result = UnionPair.ok(Optional.of(UnionPair.ok(info)));
                            return;
                        }
                        result = UnionPair.ok(Optional.empty());
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result != null;
                        this.consume(result, consumer);
                    }
                });
                flag = false;
            } finally {
                if (flag)
                    BackgroundTaskManager.remove(identifier);
            }
        });
    }


    protected boolean doesSupportRenameDirectly(final @NotNull FileInformation information, final @NotNull String name) throws Exception {
        return false;
    }
    public static final @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable> RenameNotSupport = UnionPair.ok(Optional.empty());
    /**
     * Rename a file / directory. {@code size == information.size()}
     * @see #doesSupportRenameDirectly(FileInformation, String)
     * @see #RenameNotSupport
     */
    protected abstract void renameDirectly0(final @NotNull FileInformation information, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) throws Exception;

    @Override
    public void renameDirectly(final long id, final boolean isDirectory, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) throws Exception {
        final FileInformation information = this.manager.getInstance().selectInfo(id, isDirectory, null);
        if (information == null) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(id), false)))));
            return;
        }
        if (information.name().equals(name)) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(information))));
            return;
        }
        final CheckRule<String> nameChecker = isDirectory ? this.directoryNameChecker() : this.fileNameChecker();
        if (!nameChecker.test(name)) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(information.parentId()), name, nameChecker.description())))));
            return;
        }
        if (!this.doesSupportRenameDirectly(information, name)) {
            consumer.accept(ProviderInterface.RenameTooComplex);
            return;
        }
        this.getDuplicatedName(information.parentId(), name, policy, p -> p.add("id", id).add("isDirectory", isDirectory).add("caller", "renameDirectly"), p -> {
            if (p.isFailure()) {
                consumer.accept(UnionPair.fail(p.getE()));
                return;
            }
            if (p.getT().isFailure()) {
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(p.getT().getE()))));
                return;
            }
//            assert false;
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok((FileInformation) p.getT().getT()))));
        }, (duplicatedName, identifier) -> {
            boolean flag = true;
            try {
                if (!nameChecker.test(duplicatedName)) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(information.parentId()), duplicatedName, nameChecker.description())))));
                    return;
                }
                this.loginIfNot();
                final AtomicBoolean barrier = new AtomicBoolean(true);
                this.renameDirectly0(information, duplicatedName, Options.DuplicatePolicy.ERROR, t -> {
                    if (this.checkBarrier(barrier, t, "moveDirectly#moveDirectly0", p -> p.add("information", information).add("name", duplicatedName))
                            || this.transferException(t, identifier, consumer)) return;
                    UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable> result = null;
                    try {
                        if (t.getT().isPresent() && t.getT().get().isSuccess()) {
                            final FileInformation info = t.getT().get().getT();
                            assert info.id() == id;
                            assert info.isDirectory() == isDirectory && info.size() == information.size() && info.parentId() == information.parentId();
                            this.onUpdate(info);
                        }
                        result = t;
                    } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                        result = UnionPair.fail(exception);
                    } finally {
                        BackgroundTaskManager.remove(identifier);
                        assert result != null;
                        this.consume(result, consumer);
                    }
                });
                flag = false;
            } finally {
                if (flag)
                    BackgroundTaskManager.remove(identifier);
            }
        });
    }


    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                ", loginExpireTime=" + this.loginExpireTime +
                '}';
    }
}
