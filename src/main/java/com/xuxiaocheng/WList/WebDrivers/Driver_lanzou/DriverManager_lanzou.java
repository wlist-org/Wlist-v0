package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMiscellaneousHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedFileManager;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public final class DriverManager_lanzou {
    private DriverManager_lanzou() {
        super();
    }

    // User Reader

    static void ensureLoggedIn(final @NotNull DriverConfiguration_lanzou configuration) throws IOException {
        DriverHelper_lanzou.ensureLoggedIn(configuration);
    }

    // File Reader

    static Pair.@Nullable ImmutablePair<@NotNull Iterator<@NotNull FileSqlInformation>, @NotNull Runnable> syncFilesList(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation directoryInformation = FileManager.selectFile(configuration.getName(), directoryId, connectionId.get());
            if (directoryInformation == null || directoryInformation.type() == FileSqlInterface.FileSqlType.RegularFile)
                return null;
            final List<FileSqlInformation> directoriesInformation = DriverHelper_lanzou.listAllDirectory(configuration, directoryId);
            if (directoriesInformation == null) {
                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
                connection.commit();
                return null;
            }
            final Set<FileSqlInformation> filesInformation = DriverHelper_lanzou.listFilesInPage(configuration, directoryId, 0);
            if (directoriesInformation.isEmpty() && filesInformation.isEmpty()) {
                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
                if (directoryInformation.type() == FileSqlInterface.FileSqlType.Directory)
                    FileManager.updateDirectoryType(configuration.getName(), directoryId, true, connectionId.get());
                connection.commit();
                return Pair.ImmutablePair.makeImmutablePair(HMiscellaneousHelper.getEmptyIterator(), RunnableE.EmptyRunnable);
            }
            if (directoryInformation.type() == FileSqlInterface.FileSqlType.EmptyDirectory)
                FileManager.updateDirectoryType(configuration.getName(), directoryId, false, connectionId.get());
            final Set<Long> deletedIds = ConcurrentHashMap.newKeySet();
            deletedIds.addAll(FileManager.selectFileIdByParentId(configuration.getName(), directoryId, _connectionId));
            deletedIds.remove(configuration.getWebSide().getRootDirectoryId());
            FileManager.getConnection(configuration.getName(), connectionId.get(), null);
            return DriverUtil.wrapSuppliersInPages(page -> {
                final Collection<FileSqlInformation> list = switch (page.intValue()) {
                    case 0 -> directoriesInformation;
                    case 1 -> filesInformation;
                    default -> DriverHelper_lanzou.listFilesInPage(configuration, directoryId, page.intValue() - 1);
                };
                if (page.intValue() > 1 && list.isEmpty())
                    return null;
                final Set<Long> ids = list.stream().map(FileSqlInformation::id).collect(Collectors.toSet());
                deletedIds.removeAll(ids);
                FileManager.mergeFiles(configuration.getName(), list, ids, connectionId.get());
                return list;
            }, HExceptionWrapper.wrapConsumer(e -> {
                if (e == null) {
                    FileManager.deleteFilesRecursively(configuration.getName(), deletedIds, connectionId.get());
                    FileManager.calculateDirectorySizeRecursively(configuration.getName(), directoryId, connectionId.get());
                    connection.commit();
                } else if (!(e instanceof CancellationException))
                    throw e;
            }, connection::close));
        }
    }

    static void waitSyncComplete(final Pair.@Nullable ImmutablePair<? extends @NotNull Iterator<@NotNull FileSqlInformation>, ? extends @NotNull Runnable> result) {
        if (result == null)
            return;
        HMiscellaneousHelper.consumeIterator(result.getFirst(), result.getSecond());
    }

    static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_lanzou configuration, final long id, final @Nullable Long parentId, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        if (id == configuration.getWebSide().getRootDirectoryId()) return RootDriver.getDriverInformation(configuration);
        if (id == -1) return null; // Out of Root File Tree.
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation cachedInformation = FileManager.selectFile(configuration.getName(), id, connectionId.get());
            if (cachedInformation != null) return cachedInformation;
            if (parentId == null)
                throw new UnsupportedOperationException("Cannot get an uncached file information without parent id." + ParametersMap.create().add("id", id));
            final long count = FileManager.selectFileCountByParentId(configuration.getName(), parentId.longValue(), connectionId.get());
            if (count > 0)
                return null;
            final Pair.ImmutablePair<Iterator<FileSqlInformation>, Runnable> list = DriverManager_lanzou.syncFilesList(configuration, parentId.longValue(), connectionId.get());
            if (list == null)
                return null;
            while (list.getFirst().hasNext()) {
                final FileSqlInformation information = list.getFirst().next();
                if (information.id() == id) {
                    BackgroundTaskManager.background(new BackgroundTaskManager.BackgroundTaskIdentify(BackgroundTaskManager.BackgroundTaskType.Driver,
                            configuration.getName(), "Sync files list.", parentId.toString()), () ->
                            HMiscellaneousHelper.consumeIterator(list.getFirst(), list.getSecond()), false, e -> {
                        if (e != null)
                            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
                    });
                    connection.commit();
                    return information;
                }
            }
            list.getSecond().run();
            return null;
        }
    }

    static void refreshDirectoryRecursively(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final @NotNull Set<@NotNull CompletableFuture<?>> futures, final @NotNull AtomicLong runningFutures, final @NotNull AtomicBoolean interruptFlag) throws IOException, SQLException, InterruptedException {
        try {
            final Pair.ImmutablePair<Iterator<FileSqlInformation>, Runnable> list = DriverManager_lanzou.syncFilesList(configuration, directoryId, null);
            if (list == null)
                return;
            final Collection<Long> directoryIdList = new LinkedList<>();
            try {
                while (list.getFirst().hasNext()) {
                    final FileSqlInformation information = list.getFirst().next();
                    if (information.isDirectory())
                        directoryIdList.add(information.id());
                }
            } finally {
                list.getSecond().run();
            }
            if (interruptFlag.get()) return;
            runningFutures.addAndGet(directoryIdList.size());
            for (final Long id: directoryIdList)
                futures.add(CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> DriverManager_lanzou.refreshDirectoryRecursively(configuration,
                        id.longValue(), futures, runningFutures, interruptFlag)), WListServer.IOExecutors));
        } finally {
            synchronized (runningFutures) {
                if (runningFutures.decrementAndGet() == 0)
                    runningFutures.notifyAll();
            }
        }
    }

    static Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final Options.@NotNull DirectoriesOrFiles filter, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation directoryInformation = DriverManager_lanzou.getFileInformation(configuration, directoryId, null, connectionId.get());
            if (directoryInformation == null || directoryInformation.type() == FileSqlInterface.FileSqlType.RegularFile) return null;
            if (directoryInformation.type() == FileSqlInterface.FileSqlType.EmptyDirectory)
                return Triad.ImmutableTriad.makeImmutableTriad(0L, 0L, List.of());
            final Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> cachedList = FileManager.selectFilesByParentIdInPage(configuration.getName(), directoryId, filter, limit, (long) page * limit, direction, policy, connectionId.get());
            if (cachedList.getA().longValue() > 0) return cachedList;
            DriverManager_lanzou.waitSyncComplete(DriverManager_lanzou.syncFilesList(configuration, directoryId, connectionId.get()));
            final Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list = FileManager.selectFilesByParentIdInPage(configuration.getName(), directoryId, filter, limit, (long) page * limit, direction, policy, connectionId.get());
            connection.commit();
            return list;
        }
    }

    static @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> getDownloadMethods(final @NotNull DriverConfiguration_lanzou configuration, final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final FileSqlInformation info = DriverManager_lanzou.getFileInformation(configuration, fileId, null, _connectionId);
        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        final String url = DriverHelper_lanzou.getFileDownloadUrl(configuration, fileId);
        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, DriverHelper_lanzou.headers.newBuilder())));
    }

    // File Writer

    static void trashFile(final @NotNull DriverConfiguration_lanzou configuration, final @NotNull FileSqlInformation information, final @Nullable String _connectionId, final @Nullable String _trashConnectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        final AtomicReference<String> trashConnectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId); final Connection trashConnection = TrashedFileManager.getConnection(configuration.getName(), _trashConnectionId, trashConnectionId)) {
            final LocalDateTime time;
            if (information.isDirectory()) {
                Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list;
                do {
                    list = DriverManager_lanzou.listFiles(configuration, information.id(), Options.DirectoriesOrFiles.Both, DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
                    if (list == null)
                        return;
                    for (final FileSqlInformation f: list.getC())
                        DriverManager_lanzou.trashFile(configuration, f, connectionId.get(), trashConnectionId.get());
                } while (list.getA().longValue() > 0);
                time = LocalDateTime.now();
                DriverHelper_lanzou.trashDirectories(configuration, information.id());
            } else {
                time = LocalDateTime.now();
                DriverHelper_lanzou.trashFile(configuration, information.id());
            }
            FileManager.deleteFileRecursively(configuration.getName(), information.id(), connectionId.get());
            final TrashedSqlInformation trashed = TrashedSqlInformation.fromFileSqlInformation(information, time, null);
            TrashedFileManager.insertOrUpdateFile(configuration.getName(), trashed, connectionId.get());
            trashConnection.commit();
            connection.commit();
        }
    }

    static @NotNull UnionPair<@NotNull UnionPair<@NotNull String/*new name*/, @NotNull FileSqlInformation/*for directory*/>, @NotNull FailureReason> getDuplicatePolicyName(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final boolean requireDirectory, final Options.@NotNull DuplicatePolicy policy, final @NotNull String duplicateErrorMessage, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation parentInformation = DriverManager_lanzou.getFileInformation(configuration, parentId, null, connectionId.get());
            if (parentInformation == null || parentInformation.type() != FileSqlInterface.FileSqlType.Directory) return UnionPair.ok(UnionPair.ok(name));
            if (FileManager.selectFileCountByParentId(configuration.getName(), parentId, connectionId.get()) == 0) {
                DriverManager_lanzou.waitSyncComplete(DriverManager_lanzou.syncFilesList(configuration, parentId, connectionId.get()));
                connection.commit();
            }
            FileSqlInformation information = FileManager.selectFileInDirectory(configuration.getName(), parentId, name, connectionId.get());
            if (information == null)
                return UnionPair.ok(UnionPair.ok(name));
            if (requireDirectory && information.isDirectory())
                return UnionPair.ok(UnionPair.fail(information));
            switch (policy) {
                case ERROR:
                    return UnionPair.fail(FailureReason.byDuplicateError(duplicateErrorMessage, new FileLocation(configuration.getName(), parentId), name));
                case OVER:
                    DriverManager_lanzou.trashFile(configuration, information, connectionId.get(), null);
                    connection.commit();
                    return UnionPair.ok(UnionPair.ok(name));
                case KEEP:
                    int retry = 0;
                    final Pair.ImmutablePair<String, String> wrapper = DriverUtil.getRetryWrapper(name);
                    while (information != null && !(requireDirectory && information.isDirectory()))
                        information = FileManager.selectFileInDirectory(configuration.getName(), parentId, wrapper.getFirst() + (++retry) + wrapper.getSecond(), connectionId.get());
                    return information == null ? UnionPair.ok(UnionPair.ok(wrapper.getFirst() + retry + wrapper.getSecond())) : UnionPair.ok(UnionPair.fail(information));
            }
            throw new RuntimeException("Unreachable!");
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, true, policy, "Creating directory.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            if (duplicate.getT().isFailure()) {connection.commit();return UnionPair.ok(duplicate.getT().getE());}
            final String realName = duplicate.getT().getT();
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_lanzou.createDirectory(configuration, realName, parentId);
            if (information.isSuccess()) FileManager.insertFileForce(configuration.getName(), information.getT(), connectionId.get()); // TODO
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> getUploadMethods(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final @NotNull String md5, final long size, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        if (!md5.isEmpty() && !HMessageDigestHelper.MD5.pattern.matcher(md5).matches())
            throw new IllegalStateException("Invalid md5." + ParametersMap.create().add("md5", md5));
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
        if (size > configuration.getWebSide().getMaxSizePerFile())
            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, configuration.getWebSide().getMaxSizePerFile(),  new FileLocation(configuration.getName(), parentId), name));
        final int intSize = Math.toIntExact(size);
        final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, false, policy, "Uploading.", _connectionId);
        if (duplicate.isFailure()) return UnionPair.fail(duplicate.getE());
        final String realName = duplicate.getT().getT();
        final AtomicReference<UnionPair<FileSqlInformation, FailureReason>> reference = new AtomicReference<>(null);
        final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> methods = DriverUtil.splitUploadMethodEveryFileTransferBufferSize(b ->
                reference.set(DriverHelper_lanzou.uploadFile(configuration, realName, parentId, b, md5)), intSize);
        return UnionPair.ok(new UploadMethods(methods.getFirst(), () -> {
            final UnionPair<FileSqlInformation, FailureReason> result = reference.get();
            if (result == null) return null;
            final FileSqlInformation information = result.getT();
            FileManager.insertFileForce(configuration.getName(), information, _connectionId);
            // TODO
            return information;
        }, HExceptionWrapper.wrapRunnable(() -> methods.getSecond().run())));
    }
/*
    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_lanzou configuration, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, new FileLocation(configuration.getName(), id), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation source = DriverManager_lanzou.getFileInformation(configuration, id, connectionId.get());
            if (source == null) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Renaming file.", new FileLocation(configuration.getName(), id)));}
            if (name.equals(source.name())) return UnionPair.ok(source);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, source.parentId(), name, false, policy, "Renaming file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_lanzou.renameFile(configuration, id, duplicate.getT().getT(), policy);
            if (information.isFailure()) {connection.commit();return information;}
            FileManager.insertOrUpdateFile(configuration.getName(), information.getT(), connectionId.get());
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> moveFile(final @NotNull DriverConfiguration_lanzou configuration, final long id, final long targetId, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation source = DriverManager_lanzou.getFileInformation(configuration, id, connectionId.get());
            if (source == null) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Moving file (source).", new FileLocation(configuration.getName(), id)));}
            if (source.parentId() == targetId) return UnionPair.ok(source);
            final FileSqlInformation target = DriverManager_lanzou.getFileInformation(configuration, targetId, connectionId.get());
            if (target == null || !target.isDirectory()) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Moving file (target).", new FileLocation(configuration.getName(), targetId)));}
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, targetId, source.name(), true, policy, "Moving file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            // TODO the same code as rename file.
            final FileSqlInformation information = DriverHelper_lanzou.moveFiles(configuration, List.of(id), targetId, policy).get(id);
            if (information == null) throw new IllegalStateException("Failed to move file." + ParametersMap.create().add("source", id).add("target", targetId).add("policy", policy));
            FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
            connection.commit();
            return UnionPair.ok(information);
        }
    }*/
}