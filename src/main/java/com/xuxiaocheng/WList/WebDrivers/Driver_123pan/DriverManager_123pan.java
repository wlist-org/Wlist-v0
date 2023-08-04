package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public final class DriverManager_123pan {
    private DriverManager_123pan() {
        super();
    }

    // User Reader

    static void resetUserInformation(final @NotNull DriverConfiguration_123pan configuration) throws IllegalParametersException, IOException {
        DriverHelper_123pan.resetUserInformation(configuration);
    }

    // File Reader

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123pan configuration, final long directoryId, final @IntRange(minimum = 1) int limit, final @IntRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> data = DriverHelper_123pan.listFiles(configuration, directoryId, limit, page, policy, direction);
        FileManager.insertOrUpdateFiles(configuration.getName(), data.getSecond(), _connectionId);
        return data;
    }

    static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull FileSqlInformation>, @NotNull Runnable> listAllFilesNoCache(final @NotNull DriverConfiguration_123pan configuration, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId);
        final Set<Long> allIds = ConcurrentHashMap.newKeySet();
        try {
            allIds.addAll(FileManager.selectFileIdByParentId(configuration.getName(), directoryId, connectionId.get()));
        } catch (final SQLException | RuntimeException exception) {
            connection.close();throw exception;
        }
        return DriverUtil.wrapAllFilesListerInPages(page -> {
                    final Pair.ImmutablePair<@NotNull Long, @NotNull List<FileSqlInformation>> list = DriverManager_123pan.listFilesNoCache(configuration, directoryId, DriverUtil.DefaultLimitPerRequestPage, page.intValue(), DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
                    allIds.removeAll(list.getSecond().stream().map(FileSqlInformation::id).collect(Collectors.toSet()));
                    return list;
                }, DriverUtil.DefaultLimitPerRequestPage, HExceptionWrapper.wrapConsumer(e -> {
                    try (connection) {
                        if (e != null) throw e;
                        FileManager.deleteFilesRecursively(configuration.getName(), allIds, connectionId.get());
                        connection.commit();
                    }
                }));
    }

    static boolean refreshDirectoryInBackground(final @NotNull DriverConfiguration_123pan configuration, final long directoryId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), null, connectionId)) {
            connection.setAutoCommit(false);
            final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, connectionId.get());
            FileManager.updateDirectoryType(configuration.getName(), directoryId, lister.getA().longValue() == 0, connectionId.get());
            // Hint: type and name is linking to #tryGetFileInDirectory
            BackgroundTaskManager.backgroundWithLock(new BackgroundTaskManager.BackgroundTaskIdentify("Driver_123pan: " + configuration.getName(), "Sync directory: " + directoryId),
                    () -> new AtomicLong(0), AtomicLong.class, HExceptionWrapper.wrapPredicate(lock -> {
                        if (lock.get() == lister.getA().longValue()) {lister.getC().run();return false;}
                        FileManager.getConnection(configuration.getName(), connectionId.get(), null); // retain
                        return true;
                    }), () -> {
                        while (lister.getB().hasNext()) lister.getB().next();
                        connection.commit();
                    }, connection::close);
            connection.commit();
            return lister.getA().longValue() == 0;
        }
    }

    static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_123pan configuration, final long id, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (id == configuration.getWebSide().getRootDirectoryId()) return RootDriver.getDriverInformation(configuration);
        if (id == 0) return null; // Out of Root File Tree.
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation cachedInformation = FileManager.selectFile(configuration.getName(), id, connectionId.get());
            if (cachedInformation != null) return cachedInformation;
            final FileSqlInformation information = DriverHelper_123pan.getFilesInformation(configuration, List.of(id)).get(id);
            if (information == null || !information.isDirectory()) {connection.commit();return information;}
            final boolean empty = DriverManager_123pan.refreshDirectoryInBackground(configuration, information.parentId());
            connection.commit();
            return empty ? new FileSqlInformation(information.location(), information.parentId(), information.name(), FileSqlInterface.FileSqlType.EmptyDirectory, information.size(), information.createTime(), information.updateTime(), information.md5(), information.others()) : information;
        }
    }

    static @Nullable FileSqlInformation tryGetFileInDirectory(final @NotNull DriverConfiguration_123pan configuration, final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation parentInformation = DriverManager_123pan.getFileInformation(configuration, parentId, connectionId.get());
            if (parentInformation == null || parentInformation.type() != FileSqlInterface.FileSqlType.Directory) return null;
            if (FileManager.selectFileCountByParentId(configuration.getName(), parentId, connectionId.get()) == 0) {
                DriverManager_123pan.refreshDirectoryInBackground(configuration, parentId);
                BackgroundTaskManager.wait(new BackgroundTaskManager.BackgroundTaskIdentify("Driver_123pan: " + configuration.getName(), "Sync directory: " + parentId));
            }
            return FileManager.selectFileInDirectory(configuration.getName(), parentId, name, connectionId.get());
        }
    }

    static void refreshDirectoryRecursively(final @NotNull DriverConfiguration_123pan configuration, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, connectionId.get());
            if (lister.getA().longValue() == 0) { // Empty directory
                FileManager.updateDirectoryType(configuration.getName(), directoryId, true, connectionId.get());
                connection.commit();return;
            }
            final Collection<Long> directoryIdList = new LinkedList<>();
            final Iterator<FileSqlInformation> iterator = lister.getB();
            try {
                while (iterator.hasNext()) {
                    final FileSqlInformation info = iterator.next();
                    if (info.isDirectory()) directoryIdList.add(info.id());
                }
            } catch (final NoSuchElementException exception) {
                assert exception.getCause() instanceof InterruptedException;
            } catch (final RuntimeException exception) {
                if (!(exception.getCause() instanceof CancellationException))
                    throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            }
            for (final Long id: directoryIdList) DriverManager_123pan.refreshDirectoryRecursively(configuration, id.longValue(), connectionId.get());
            //noinspection CommentedOutCode
            { // Due to frequency control, refreshing directory in threads in unnecessary.
//                final CompletableFuture<?>[] futures = new CompletableFuture<?>[directoryIdList.size()];
//                int i = 0;
//                for (final Long id : directoryIdList)
//                    futures[i++] = CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> DriverManager_123pan.refreshDirectoryRecursively(configuration, id.longValue(), connectionId.get(), _threadPool)), WListServer.IOExecutors);
//                try {
//                    CompletableFuture.allOf(futures).join();
//                } catch (final CompletionException exception) {
//                    throw HExceptionWrapper.unwrapException(exception, SQLException.class);
//                }
            }
            connection.commit();
        }
    }

    static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_123pan configuration, final long directoryId, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation directoryInformation = DriverManager_123pan.getFileInformation(configuration, directoryId, connectionId.get());
            if (directoryInformation == null) {connection.commit();return null;}
            switch (directoryInformation.type()) {
                case RegularFile: return null;
                case Directory: break;
                case EmptyDirectory: return Pair.ImmutablePair.makeImmutablePair(0L, List.of());
            }
            final Pair.ImmutablePair<Long, List<FileSqlInformation>> cachedList = FileManager.selectFilesByParentIdInPage(configuration.getName(), directoryId, limit, (long) page * limit, direction, policy, connectionId.get());
            if (cachedList.getFirst().longValue() > 0) return cachedList;
            assert cachedList.getSecond().isEmpty();
            final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = DriverManager_123pan.listFilesNoCache(configuration, directoryId, limit, page, policy, direction, connectionId.get());
            if (!cachedList.getFirst().equals(list.getFirst()))
                if (list.getFirst().longValue() == list.getSecond().size()) {
                    FileManager.updateDirectoryType(configuration.getName(), directoryId, list.getFirst().longValue() == 0, connectionId.get());
                    FileManager.insertOrUpdateFiles(configuration.getName(), list.getSecond(), connectionId.get());
                } else DriverManager_123pan.refreshDirectoryInBackground(configuration, directoryId);
            connection.commit();
            return list;
        }
    }

    static @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> getDownloadMethods(final @NotNull DriverConfiguration_123pan configuration, final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, fileId, _connectionId);
        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        final String url = DriverHelper_123pan.getFileDownloadUrl(configuration, info);
        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(DriverHelper_123pan.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null)));
    }

    // File Writer

    static void trashFile(final @NotNull DriverConfiguration_123pan configuration, final long id, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation information = DriverManager_123pan.getFileInformation(configuration, id, connectionId.get());
            if (information == null) {
                connection.commit();
                return;
            }
            final Set<Long> ids = DriverHelper_123pan.trashFiles(configuration, List.of(id), true);
            if (!ids.contains(id)) {
                connection.commit();
                return;
            }
            FileManager.deleteFileRecursively(configuration.getName(), id, connectionId.get());
            // TODO: waiting trash.
//            final TrashedSqlInformation trashed = TrashHelper_123pan.getFilesInformation(configuration, ids).get(id);
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

    static @NotNull UnionPair<@NotNull UnionPair<@NotNull String/*new name*/, @NotNull FileSqlInformation/*for directory*/>, @NotNull FailureReason> getDuplicatePolicyName(final @NotNull DriverConfiguration_123pan configuration, final long parentId, final @NotNull String name, final boolean requireDirectory, final Options.@NotNull DuplicatePolicy policy, final @NotNull String duplicateErrorMessage, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            FileSqlInformation information = DriverManager_123pan.tryGetFileInDirectory(configuration, parentId, name, connectionId.get());
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
                DriverManager_123pan.trashFile(configuration, information.id(), connectionId.get());
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

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_123pan configuration, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, parentId, name, true, policy, "Creating directory.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            if (duplicate.getT().isFailure()) {connection.commit();return UnionPair.ok(duplicate.getT().getE());}
            final String realName = duplicate.getT().getT();
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_123pan.createDirectory(configuration, parentId, realName, policy);
            if (information.isSuccess()) FileManager.insertOrUpdateFile(configuration.getName(), information.getT(), connectionId.get());
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> getUploadMethods(final @NotNull DriverConfiguration_123pan configuration, final long parentId, final @NotNull String name, final @NotNull CharSequence md5, final long size, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!HMessageDigestHelper.MD5.pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", ParametersMap.create().add("md5", md5));
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, parentId, name, false, policy, "Uploading file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            final String realName = duplicate.getT().getT();
            final UnionPair<UnionPair<FileSqlInformation, DriverHelper_123pan.UploadIdentifier_123pan>, FailureReason> requestUploadData = DriverHelper_123pan.uploadRequest(configuration, parentId, realName, size, md5, policy);
            if (requestUploadData.isFailure()) {connection.commit();return UnionPair.fail(requestUploadData.getE());}
            if (requestUploadData.getT().isSuccess()) {
                final FileSqlInformation information = requestUploadData.getT().getT();
                FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
                connection.commit();return UnionPair.ok(new UploadMethods(List.of(), () -> information, RunnableE.EmptyRunnable));
            }
            final int partCount = MiscellaneousUtil.calculatePartCount(size, DriverHelper_123pan.UploadPartSize);
            final List<String> urls = DriverHelper_123pan.uploadPare(configuration, requestUploadData.getT().getE(), partCount);
            long readSize = 0;
            final List<ConsumerE<ByteBuf>> consumers = new ArrayList<>(partCount);
            final Collection<Runnable> finishers = new ArrayList<>(partCount);
            final AtomicInteger countDown = new AtomicInteger(urls.size());
            for (final String url: urls) {
                //noinspection NumericCastThatLosesPrecision
                final int len = (int) Math.min(DriverHelper_123pan.UploadPartSize, (size - readSize));readSize += len;
                final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> split = DriverUtil.splitUploadMethod(b -> {
                    DriverNetworkHelper.postWithBody(DriverHelper_123pan.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"),
                            null, DriverNetworkHelper.createOctetStreamRequestBody(b)).execute().close();
                    countDown.getAndDecrement();
                }, len);
                consumers.addAll(split.getFirst());
                finishers.add(split.getSecond());
            }
            FileManager.getConnection(configuration.getName(), connectionId.get(), null);
            return UnionPair.ok(new UploadMethods(consumers, () -> {
                if (countDown.get() > 0) return null;
                final FileSqlInformation information = DriverHelper_123pan.uploadComplete(configuration, requestUploadData.getT().getE(), partCount);
                if (information != null) FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
                connection.commit();return information;
            }, HExceptionWrapper.wrapRunnable(() -> {finishers.forEach(Runnable::run);connection.close();})));
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_123pan configuration, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, new FileLocation(configuration.getName(), id), name));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation source = DriverManager_123pan.getFileInformation(configuration, id, connectionId.get());
            if (source == null) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Renaming file.", new FileLocation(configuration.getName(), id)));}
            if (name.equals(source.name())) return UnionPair.ok(source);
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, source.parentId(), name, false, policy, "Renaming file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_123pan.renameFile(configuration, id, duplicate.getT().getT(), policy);
            if (information.isFailure()) {connection.commit();return information;}
            FileManager.insertOrUpdateFile(configuration.getName(), information.getT(), connectionId.get());
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> moveFile(final @NotNull DriverConfiguration_123pan configuration, final long id, final long targetId, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation source = DriverManager_123pan.getFileInformation(configuration, id, connectionId.get());
            if (source == null) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Moving file (source).", new FileLocation(configuration.getName(), id)));}
            if (source.parentId() == targetId) return UnionPair.ok(source);
            final FileSqlInformation target = DriverManager_123pan.getFileInformation(configuration, targetId, connectionId.get());
            if (target == null || !target.isDirectory()) {connection.commit();return UnionPair.fail(FailureReason.byNoSuchFile("Moving file (target).", new FileLocation(configuration.getName(), targetId)));}
            final UnionPair<UnionPair<String, FileSqlInformation>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, targetId, source.name(), true, policy, "Moving file.", connectionId.get());
            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
            // TODO the same code as rename file.
            final FileSqlInformation information = DriverHelper_123pan.moveFiles(configuration, List.of(id), targetId, policy).get(id);
            if (information == null) throw new IllegalStateException("Failed to move file." + ParametersMap.create().add("source", id).add("target", targetId).add("policy", policy));
            FileManager.insertOrUpdateFile(configuration.getName(), information, connectionId.get());
            connection.commit();
            return UnionPair.ok(information);
        }
    }
}
