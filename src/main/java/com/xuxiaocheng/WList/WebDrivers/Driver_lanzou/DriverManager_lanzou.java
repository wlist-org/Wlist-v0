package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    static @Nullable @UnmodifiableView List<@NotNull FileSqlInformation> syncFilesList(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation directoryInformation = FileManager.selectFile(configuration.getName(), directoryId, connectionId.get());
            if (directoryInformation == null || directoryInformation.type() == FileSqlInterface.FileSqlType.RegularFile)
                return null;
            if (directoryInformation.type() == FileSqlInterface.FileSqlType.EmptyDirectory)
                return List.of();
            final CompletableFuture<List<FileSqlInformation>> directoriesFuture = CompletableFuture.supplyAsync(HExceptionWrapper.wrapSupplier(() ->
                    DriverHelper_lanzou.listAllDirectory(configuration, directoryId)), WListServer.IOExecutors);
            final CompletableFuture<List<FileSqlInformation>> filesFuture = CompletableFuture.supplyAsync(HExceptionWrapper.wrapSupplier(() ->
                    DriverHelper_lanzou.listAllFiles(configuration, directoryId)), WListServer.IOExecutors);
            final List<FileSqlInformation> information;
            try {
                information = directoriesFuture.get();
                if (information == null) {
                    FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
                    connection.commit();
                    return null;
                }
                information.addAll(filesFuture.get());
            } catch (final ExecutionException exception) {
                if (exception.getCause() instanceof RuntimeException runtimeException)
                    throw HExceptionWrapper.unwrapException(HExceptionWrapper.unwrapException(runtimeException, IOException.class), InterruptedException.class);
                throw new RuntimeException(exception.getCause());
            } finally {
                directoriesFuture.cancel(true);
                filesFuture.cancel(true);
            }
            if (information.isEmpty()) {
                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
                FileManager.insertOrUpdateFile(configuration.getName(), directoryInformation.getAsEmptyDirectory(), connectionId.get());
            } else {
                final Set<Long> deletedIds = FileManager.selectFileIdByParentId(configuration.getName(), directoryId, connectionId.get());
                deletedIds.removeAll(information.stream().map(FileSqlInformation::id).collect(Collectors.toSet()));
                deletedIds.remove(-1L);
                FileManager.deleteFilesRecursively(configuration.getName(), deletedIds, connectionId.get());
                FileManager.insertOrUpdateFiles(configuration.getName(), information, connectionId.get());
            }
            connection.commit();
            return information;
        }
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
            final List<FileSqlInformation> list = DriverManager_lanzou.syncFilesList(configuration, parentId.longValue(), connectionId.get());
            connection.commit();
            if (list == null || list.isEmpty())
                return null;
            for (final FileSqlInformation information: list)
                if (information.id() == id)
                    return information;
            return null;
        }
    }

    static void refreshDirectoryRecursively(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId) throws IOException, SQLException, InterruptedException {
        final List<FileSqlInformation> list = DriverManager_lanzou.syncFilesList(configuration, directoryId, null);
        if (list == null)
            return;
        final Collection<Long> directoryIdList = new LinkedList<>();
        for (final FileSqlInformation information: list)
            if (information.isDirectory())
                directoryIdList.add(information.id());
        for (final Long id: directoryIdList)
            DriverManager_lanzou.refreshDirectoryRecursively(configuration, id.longValue());
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
            DriverManager_lanzou.syncFilesList(configuration, directoryId, connectionId.get());
            final Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list = FileManager.selectFilesByParentIdInPage(configuration.getName(), directoryId, filter, limit, (long) page * limit, direction, policy, connectionId.get());
            connection.commit();
            return list;
        }
    }
/*
    static @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> getDownloadMethods(final @NotNull DriverConfiguration_lanzou configuration, final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = DriverManager_lanzou.getFileInformation(configuration, fileId, _connectionId);
        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        final String url = DriverHelper_lanzou.getFileDownloadUrl(configuration, info);
        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(DriverHelper_lanzou.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null)));
    }

    // File Writer

    static void trashFile(final @NotNull DriverConfiguration_lanzou configuration, final long id, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation information = DriverManager_lanzou.getFileInformation(configuration, id, connectionId.get());
            if (information == null) {
                connection.commit();
                return;
            }
            final Set<Long> ids = DriverHelper_lanzou.trashFiles(configuration, List.of(id), true);
            if (!ids.contains(id)) {
                connection.commit();
                return;
            }
            FileManager.deleteFileRecursively(configuration.getName(), id, connectionId.get());
            // TODO: waiting trash.
//            final TrashedSqlInformation trashed = TrashHelper_lanzou.getFilesInformation(configuration, ids).get(id);
//            if (trashed == null)
//                throw new IllegalStateException("Failed to get trashed file information. [Unreachable]. id: " + id);
//            try (final Connection trashedConnection = TrashedFileManager.getConnection(configuration.getName(), FileManager.sqlInstances.getInstance(configuration.getName()) == TrashedFileManager.sqlInstances.getInstance(configuration.getName()) ? _connectionId : null, connectionId)) {
//                trashedConnection.setAutoCommit(false);
//                TrashedFileManager.insertOrUpdateFile(configuration.getName(), trashed, connectionId.get());
//                trashedConnection.commit();
//            }
            connection.commit();
        }
    }
*/
//    static @NotNull UnionPair<@NotNull UnionPair<@NotNull String/*new name*/, @NotNull FileSqlInformation/*for directory*/>, @NotNull FailureReason> getDuplicatePolicyName(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final boolean requireDirectory, final Options.@NotNull DuplicatePolicy policy, final @NotNull String duplicateErrorMessage, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        /*final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            FileSqlInformation information = DriverManager_lanzou.tryGetFileInDirectory(configuration, parentId, name, connectionId.get());
            if (information == null) {
                connection.commit();
                return UnionPair.ok(UnionPair.ok(name));
            }
            if (requireDirectory && information.isDirectory()) {
                connection.commit();
                return UnionPair.ok(UnionPair.fail(information));
            }
            if (policy == Options.DuplicatePolicy.ERROR) {
                connection.commit();
                return UnionPair.fail(FailureReason.byDuplicateError(duplicateErrorMessage, new FileLocation(configuration.getName(), parentId), name));
            }
            if (policy == Options.DuplicatePolicy.OVER) {
                DriverManager_lanzou.trashFile(configuration, information.id(), connectionId.get());
                // TODO waiting trash. Delete.
                connection.commit();
                return UnionPair.ok(UnionPair.ok(name));
            }
            if (policy == Options.DuplicatePolicy.KEEP) {
                int retry = 0;
                final Pair.ImmutablePair<String, String> wrapper = DriverUtil.getRetryWrapper(name);
                while (information != null && !(requireDirectory && information.isDirectory()))
                    information = FileManager.selectFileInDirectory(configuration.getName(), parentId, wrapper.getFirst() + (++retry) + wrapper.getSecond(), connectionId.get());
                connection.commit();
                return information == null ? UnionPair.ok(UnionPair.ok(wrapper.getFirst() + retry + wrapper.getSecond())) : UnionPair.ok(UnionPair.fail(information));
            }
            throw new RuntimeException("Unreachable!");
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, true, policy, "Creating directory.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            if (duplicate.getT().isFailure()) {connection.commit();return UnionPair.ok(duplicate.getT().getE());}
            final String realName = duplicate.getT().getT();
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_lanzou.createDirectory(configuration, parentId, realName, policy);
            if (information.isSuccess()) FileManager.insertOrUpdateFile(configuration.getName(), information.getT(), connectionId.get());
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> getUploadMethods(final @NotNull DriverConfiguration_lanzou configuration, final long parentId, final @NotNull String name, final @NotNull CharSequence md5, final long size, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!HMessageDigestHelper.MD5.pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", ParametersMap.create().add("md5", md5));
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, false, policy, "Uploading file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            final String realName = duplicate.getT().getT();
            final UnionPair<UnionPair<FileSqlInformation, DriverHelper_lanzou.UploadIdentifier_lanzou>, FailureReason> requestUploadData = DriverHelper_lanzou.uploadRequest(configuration, parentId, realName, size, md5, policy);
            if (requestUploadData.isFailure()) {connection.commit();return UnionPair.fail(requestUploadData.getE());}
            if (requestUploadData.getT().isSuccess()) {
                final FileSqlInformation information = requestUploadData.getT().getT();
                FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
                connection.commit();return UnionPair.ok(new UploadMethods(List.of(), () -> information, RunnableE.EmptyRunnable));
            }
            final int partCount = MiscellaneousUtil.calculatePartCount(size, DriverHelper_lanzou.UploadPartSize);
            final List<String> urls = DriverHelper_lanzou.uploadPare(configuration, requestUploadData.getT().getE(), partCount);
            long readSize = 0;
            final List<ConsumerE<ByteBuf>> consumers = new ArrayList<>(partCount);
            final Collection<Runnable> finishers = new ArrayList<>(partCount);
            final AtomicInteger countDown = new AtomicInteger(urls.size());
            for (final String url: urls) {
                //noinspection NumericCastThatLosesPrecision
                final int len = (int) Math.min(DriverHelper_lanzou.UploadPartSize, (size - readSize));readSize += len;
                final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> split = DriverUtil.splitUploadMethod(b -> {
                    DriverNetworkHelper.postWithBody(DriverHelper_lanzou.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"),
                            null, DriverNetworkHelper.createOctetStreamRequestBody(b)).execute().close();
                    countDown.getAndDecrement();
                }, len);
                consumers.addAll(split.getFirst());
                finishers.add(split.getSecond());
            }
            FileManager.getConnection(configuration.getName(), connectionId.get(), null);
            return UnionPair.ok(new UploadMethods(consumers, () -> {
                if (countDown.get() > 0) return null;
                final FileSqlInformation information = DriverHelper_lanzou.uploadComplete(configuration, requestUploadData.getT().getE(), partCount);
                if (information != null) FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
                connection.commit();return information;
            }, HExceptionWrapper.wrapRunnable(() -> {finishers.forEach(Runnable::run);connection.close();})));
        }
    }

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
