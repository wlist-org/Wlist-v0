package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedFileManager;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
            final List<FileSqlInformation> information = DriverHelper_lanzou.listAllDirectory(configuration, directoryId);
            if (information == null) {
                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
                connection.commit();
                return null;
            }
            information.addAll(DriverHelper_lanzou.listAllFiles(configuration, directoryId));
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

    static @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> getDownloadMethods(final @NotNull DriverConfiguration_lanzou configuration, final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final FileSqlInformation info = DriverManager_lanzou.getFileInformation(configuration, fileId, null, _connectionId);
        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        final String url = DriverHelper_lanzou.getFileDownloadUrl(configuration, fileId);
        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null)));
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
            final TrashedSqlInformation trashed = information.toTrashedSqlInformation(time, null);
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
                DriverManager_lanzou.syncFilesList(configuration, parentId, connectionId.get());
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
            if (information.isSuccess()) FileManager.insertOrUpdateFile(configuration.getName(), information.getT(), connectionId.get());
            connection.commit();
            return information;
        }
    }
/*
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
