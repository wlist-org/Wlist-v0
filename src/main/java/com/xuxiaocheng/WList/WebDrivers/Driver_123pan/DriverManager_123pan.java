package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Polymers.DownloadMethods;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import okio.BufferedSink;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

// TODO check useCache when use twice.
@SuppressWarnings("SameParameterValue")
public final class DriverManager_123pan {
    private DriverManager_123pan() {
        super();
    }

    private static final @NotNull Map<@NotNull Long, @NotNull FileSqlInformation> RootInformationCache = new ConcurrentHashMap<>();
    private static @NotNull FileSqlInformation getRootInformation(final long id) {
        return DriverManager_123pan.RootInformationCache.computeIfAbsent(id, k -> new FileSqlInformation(k.longValue(), new DrivePath("/"), true, 0, null, null, "", null));
    }

    // User

    static void resetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        DriverHelper_123pan.resetUserInformation(configuration);
    }

    // File Information Getter

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> data = DriverHelper_123pan.listFiles(configuration, directoryId, directoryPath, limit, page, policy, direction);
        FileManager.insertOrUpdateFiles(configuration.getLocalSide().getName(), data.getSecond(), _connectionId);
        return data;
    }

    static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull FileSqlInformation>, @NotNull Runnable> listAllFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId);
        final Set<Long> allIds = ConcurrentHashMap.newKeySet();
        try {
            allIds.addAll(FileManager.selectFileIdByParentPath(configuration.getLocalSide().getName(), directoryPath, connectionId.get()));
        } catch (final SQLException | RuntimeException exception) {
            connection.close();
            throw exception;
        }
        return DriverUtil.wrapAllFilesListerInPages(page -> {
                    final Pair.ImmutablePair<@NotNull Long, @NotNull List<FileSqlInformation>> list = DriverManager_123pan.listFilesNoCache(configuration, directoryId, directoryPath,
                            configuration.getWebSide().getDefaultLimitPerPage(), page.intValue(), DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
                    allIds.removeAll(list.getSecond().stream().map(FileSqlInformation::id).collect(Collectors.toSet()));
                    return list;
                }, configuration.getWebSide().getDefaultLimitPerPage(), HExceptionWrapper.wrapConsumer(e -> {
                    try (connection) {
                        if (e != null) throw e;
                        FileManager.deleteFilesRecursively(configuration.getLocalSide().getName(), allIds, connectionId.get());
                        connection.commit();
                    }
                }), _threadPool);
    }

    static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean requireDirectory, final boolean useCache, final boolean forceCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws SQLException {
        if (path.getDepth() == 0)
            return DriverManager_123pan.getRootInformation(configuration.getWebSide().getRootDirectoryId());
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final FileSqlInformation information = FileManager.selectFileByPath(configuration.getLocalSide().getName(), path, connectionId.get());
                if (information != null)
                    return requireDirectory && !information.isDir() ? null : information;
            }
            if (forceCache)
                return null;
            final String name = path.getName();
            final DrivePath parentPath = path.getParent();
            final FileSqlInformation parentInformation = DriverManager_123pan.getFileInformation(configuration, parentPath, true, useCache, false, connectionId.get(), _threadPool);
            if (parentInformation == null) {
                connection.commit();
                return null;
            }
            FileSqlInformation information = null;
            final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, parentInformation.id(), parentPath, connectionId.get(), _threadPool);
            try {
                while (lister.getB().hasNext()) {
                    final FileSqlInformation info = lister.getB().next();
                    if (name.equals(info.path().getName())) {
                        information = info;
                        if (useCache) // Sync from web.
                            BackgroundTaskManager.backgroundOptionally("Driver_123pan: " + configuration.getLocalSide().getName(), "Sync directory: " + parentPath.getPath(),
                                    () -> new AtomicLong(0), AtomicLong.class,
                                    lock -> lock.get() != lister.getA().longValue(), RunnableE.EmptyRunnable, () -> {
                                        while (lister.getB().hasNext()) lister.getB().next();
                                    }, RunnableE.EmptyRunnableE);
                        else lister.getC().run();
                        break;
                    }
                }
            } catch (final NoSuchElementException exception) {
                assert exception.getCause() instanceof InterruptedException;
            } catch (final RuntimeException exception) {
                if (!(exception.getCause() instanceof CancellationException))
                    throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
            connection.commit();
            return information == null || (requireDirectory && !information.isDir()) ? null : information;
        }
    }

    static void refreshDirectoryRecursively(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, directoryPath, connectionId.get(), _threadPool);
            final Collection<DrivePath> directoryNameList = new LinkedList<>();
            final Collection<Long> directoryIdList = new LinkedList<>();
            final Iterator<FileSqlInformation> iterator = lister.getB();
            try {
                while (iterator.hasNext()) {
                    final FileSqlInformation info = iterator.next();
                    if (info.isDir()) {
                        directoryNameList.add(info.path());
                        directoryIdList.add(info.id());
                    }
                }
            } catch (final NoSuchElementException exception) {
                assert exception.getCause() instanceof InterruptedException;
            } catch (final RuntimeException exception) {
                if (!(exception.getCause() instanceof CancellationException))
                    throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            }
            assert directoryNameList.size() == directoryIdList.size();
            final Iterator<DrivePath> pathIterator = directoryNameList.iterator();
            final Iterator<Long> idIterator = directoryIdList.iterator();
            while (pathIterator.hasNext() && idIterator.hasNext())
                DriverManager_123pan.refreshDirectoryRecursively(configuration, idIterator.next().longValue(), pathIterator.next(), connectionId.get(), _threadPool);
            assert !pathIterator.hasNext() && !idIterator.hasNext();
            connection.commit();
        }
    }

    static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = FileManager.selectFilesByParentPathInPage(configuration.getLocalSide().getName(),
                        directoryPath, limit, (long) page * limit, direction, policy, connectionId.get());
                if (list.getFirst().longValue() > 0)
                    return list;
                assert list.getSecond().isEmpty();
            }
            final FileSqlInformation directoryInformation = DriverManager_123pan.getFileInformation(configuration, directoryPath, true, useCache, false, connectionId.get(), _threadPool);
            if (directoryInformation == null) {
                connection.commit();
                return null;
            }
            final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = DriverManager_123pan.listFilesNoCache(configuration, directoryInformation.id(), directoryPath,
                    limit, page, policy, direction, connectionId.get());
            final long cached = useCache ? 0 : FileManager.selectFileCountByParentPath(configuration.getLocalSide().getName(), directoryPath, connectionId.get());
            if (cached != list.getFirst().longValue()) {
                if (list.getFirst().longValue() == list.getSecond().size())
                    FileManager.insertOrUpdateFiles(configuration.getLocalSide().getName(), list.getSecond(), connectionId.get());
                else {
                    final AtomicReference<DrivePath> path = new AtomicReference<>();
                    BackgroundTaskManager.backgroundOptionally("Driver_123pan: " + configuration.getLocalSide().getName(), "Sync directory: " + directoryPath.getPath(),
                            () -> new AtomicLong(0), AtomicLong.class,
                            lock -> lock.get() != list.getFirst().longValue(), HExceptionWrapper.wrapRunnable(() -> {
                                FileManager.getDatabaseUtil().getExplicitConnection(connectionId.get()); // retain
                                path.set(new DrivePath(directoryPath));
                            }), () -> {
                                final Iterator<FileSqlInformation> iterator = DriverManager_123pan.listAllFilesNoCache(configuration, directoryInformation.id(), path.get(), connectionId.get(), _threadPool).getB();
                                while (iterator.hasNext()) iterator.next();
                                connection.commit();
                            }, connection::close);
                }
            }
            connection.commit();
            return list;
        }
    }

    // File Manager

    static @Nullable DownloadMethods getDownloadMethods(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, path, false, useCache, false, _connectionId, _threadPool);
        if (info == null || info.isDir())
            return null;
        final String url = DriverHelper_123pan.getFileDownloadUrl(configuration, info);
        if (url == null)
            return null;
        return DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(DriverHelper_123pan.fileClient,
                Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null));
    }

    static void trashFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            throw new IllegalStateException("Cannot trash root.");
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation information = DriverManager_123pan.getFileInformation(configuration, path, false, useCache, false, connectionId.get(), _threadPool);
            if (information == null) {
                connection.commit();
                return;
            }
            final Set<Long> ids = DriverHelper_123pan.trashFiles(configuration, List.of(information.id()), true);
            if (!ids.isEmpty()) // assert ids.size() == 1 && ids.contains(fileId);
                FileManager.deleteFileRecursively(configuration.getLocalSide().getName(), information.id(), connectionId.get());
            connection.commit();
        }
    }

    static @NotNull UnionPair<@NotNull UnionPair<@NotNull FileSqlInformation, @NotNull String>, @NotNull FailureReason> getDuplicatePolicyName(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy, final boolean requireDirectory, final @NotNull String duplicateErrorMethod, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, path, requireDirectory, useCache, true, connectionId.get(), _threadPool);
            if (info == null) {
                connection.commit();
                return UnionPair.ok(UnionPair.fail(path.getName()));
            }
            if (requireDirectory && info.isDir()) {
                connection.commit();
                return UnionPair.ok(UnionPair.ok(info));
            }
            if (policy == Options.DuplicatePolicy.ERROR) {
                connection.commit();
                return UnionPair.fail(FailureReason.byDuplicateError(duplicateErrorMethod, path));
            }
            if (policy == Options.DuplicatePolicy.OVER)
                DriverManager_123pan.trashFile(configuration, path, useCache, connectionId.get(), _threadPool);
            assert policy == Options.DuplicatePolicy.KEEP;
            final String name = path.getName();
            int retry = 0;
            final Pair.ImmutablePair<String, String> wrapper = DriverUtil.getRetryWrapper(path.getName());
            while (info != null && !(requireDirectory && info.isDir()))
                info = DriverManager_123pan.getFileInformation(configuration, path.parent().child(wrapper.getFirst() + (++retry) + wrapper.getSecond()), false, true, true, connectionId.get(), _threadPool);
            final String duplicated = path.getName();
            path.parent().child(name);
            connection.commit();
            return UnionPair.ok(UnionPair.fail(duplicated));
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectoriesRecursively(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            return UnionPair.ok(DriverManager_123pan.getRootInformation(configuration.getWebSide().getRootDirectoryId()));
        final String name = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, path));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final UnionPair<UnionPair<FileSqlInformation, String>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, path, policy, true, "Creating directories recursively. path=" + path, useCache, connectionId.get(), _threadPool);
            if (duplicate.isFailure()) {
                connection.commit();
                return UnionPair.fail(duplicate.getE());
            }
            if (duplicate.getT().isSuccess()) {
                connection.commit();
                return UnionPair.ok(duplicate.getT().getT());
            }
            final UnionPair<FileSqlInformation, FailureReason> parentInformation;
            try {
                parentInformation = DriverManager_123pan.createDirectoriesRecursively(configuration, path.parent(), Options.DuplicatePolicy.ERROR, true, connectionId.get(), _threadPool);
            } finally {
                path.child(name);
            }
            if (parentInformation.isFailure()) {
                if (parentInformation.getE().extra() instanceof DrivePath p)
                    p.child(name);
                connection.commit();
                return parentInformation;
            }
            final UnionPair<FileSqlInformation, FailureReason> information;
            try {
                information = DriverHelper_123pan.createDirectory(configuration, parentInformation.getT().id(), path.parent().child(duplicate.getT().getE()), policy);
                if (information.isFailure()) {
                    connection.commit();
                    return information;
                }
                FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information.getT(), connectionId.get());
            } finally {
                path.child(name);
            }
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> getUploadMethods(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String md5, final long size, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!MiscellaneousUtil.md5Pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", md5);
        final String newFileName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newFileName))
            return UnionPair.fail(FailureReason.byInvalidName(newFileName, path));
        final DrivePath parentPath = path.getParent();
        final UnionPair<FileSqlInformation, FailureReason> parentDirectory = DriverManager_123pan.createDirectoriesRecursively(configuration, parentPath, policy, useCache, _connectionId, _threadPool);
        if (parentDirectory.isFailure())
            return UnionPair.fail(parentDirectory.getE());
        final DrivePath realPath = parentDirectory.getT().path().getChild(newFileName);
        final UnionPair<UnionPair<FileSqlInformation, String>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, realPath, policy, false, "Uploading file. path=" + realPath.getPath(), useCache, _connectionId, _threadPool);
        if (duplicate.isFailure())
            return UnionPair.fail(duplicate.getE());
        realPath.parent().child(duplicate.getT().getE());
        final UnionPair<UnionPair<FileSqlInformation, DriverHelper_123pan.UploadIdentifier_123pan>, FailureReason> requestUploadData = DriverHelper_123pan.uploadRequest(configuration, parentDirectory.getT().id(), realPath, size, md5, policy);
        if (requestUploadData.isFailure())
            return UnionPair.fail(requestUploadData.getE());
        if (requestUploadData.getT().isSuccess()) {
            final FileSqlInformation information = requestUploadData.getT().getT();
            return UnionPair.ok(new UploadMethods(List.of(), () -> {
                FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information, _connectionId);
                return information;
            }, RunnableE.EmptyRunnable));
        }
        final int partCount = MiscellaneousUtil.calculatePartCount(size, DriverHelper_123pan.UploadPartSize);
        final List<String> urls = DriverHelper_123pan.uploadPare(configuration, requestUploadData.getT().getE(), partCount);
        long readSize = 0;
        final List<ConsumerE<ByteBuf>> consumers = new ArrayList<>(partCount);
        final Collection<Runnable> finishers = new ArrayList<>(partCount);
        final AtomicInteger countDown = new AtomicInteger(urls.size());
        for (final String url: urls) {
            //noinspection NumericCastThatLosesPrecision
            final int len = (int) Math.min(DriverHelper_123pan.UploadPartSize, (size - readSize));
            readSize += len;
            final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> split = DriverUtil.splitUploadMethod(b -> {
                DriverNetworkHelper.callRequestWithBody(DriverHelper_123pan.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
                        new DriverUtil.OctetStreamRequestBody(len) {
                            @Override
                            public void writeTo(final @NotNull BufferedSink bufferedSink) throws IOException {
                                assert b.readableBytes() == len;
                                final int bufferSize = Math.min(len, 2 << 20);
                                for (final byte[] buffer = new byte[bufferSize]; b.readableBytes() > 0; ) {
                                    final int len = Math.min(bufferSize, b.readableBytes());
                                    b.readBytes(buffer, 0, len);
                                    bufferedSink.write(buffer, 0, len);
                                }
                            }
                        }
                ).execute().close();
                countDown.getAndDecrement();
            }, len);
            consumers.addAll(split.getFirst());
            finishers.add(split.getSecond());
        }
        return UnionPair.ok(new UploadMethods(consumers, () -> {
            if (countDown.get() > 0)
                return null;
            final FileSqlInformation information = DriverHelper_123pan.uploadComplete(configuration, requestUploadData.getT().getE(), partCount);
            if (information != null)
                FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information, _connectionId);
            return information;
        }, () -> finishers.forEach(Runnable::run)));
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, path.getParent().child(name)));
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation source = DriverManager_123pan.getFileInformation(configuration, path, false, useCache, false, connectionId.get(), _threadPool);
            if (source == null) {
                connection.commit();
                return UnionPair.fail(FailureReason.byNoSuchFile("Renaming file.", path));
            }
            final DrivePath newFilePath = path.getParent().child(name);
            final UnionPair<UnionPair<FileSqlInformation, String>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, newFilePath, policy, false, "Renaming file.", useCache, connectionId.get(), _threadPool);
            if (duplicate.isFailure()) {
                connection.commit();
                return UnionPair.fail(duplicate.getE());
            }
            final String newName = duplicate.getT().getE();
            newFilePath.parent().child(newName);
            final UnionPair<FileSqlInformation, FailureReason> information = DriverHelper_123pan.renameFile(configuration, source.id(), newFilePath);
            if (information.isFailure()) {
                connection.commit();
                return information;
            }
            FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information.getT(), connectionId.get());
            final int index = source.path().getDepth() - 1;
            FileManager.updateForEach(configuration.getLocalSide().getName(),
                    FileManager.selectFileIdByParentPathRecursively(configuration.getLocalSide().getName(), path, connectionId.get()),
                    t -> {
                        t.path().replace(index, newName);
                        return t;
                    }, connectionId.get());
            connection.commit();
            return information;
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> moveFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetParent, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation sourceId = DriverManager_123pan.getFileInformation(configuration, sourceFile, false, useCache, false, connectionId.get(), _threadPool);
            if (sourceId == null) {
                connection.commit();
                return UnionPair.fail(FailureReason.byNoSuchFile("Moving file. source", sourceFile));
            }
            final UnionPair<FileSqlInformation, FailureReason> targetId = DriverManager_123pan.createDirectoriesRecursively(configuration, targetParent, Options.DuplicatePolicy.ERROR, true, connectionId.get(), _threadPool);
            if (targetId.isFailure()) {
                connection.commit();
                return targetId;
            }
            final DrivePath realPath = targetId.getT().path();
            final UnionPair<UnionPair<FileSqlInformation, String>, FailureReason> duplicate = DriverManager_123pan.getDuplicatePolicyName(configuration, realPath, policy, true, "Moving file. target" , useCache, connectionId.get(), _threadPool);
            if (duplicate.isFailure()) {
                connection.commit();
                return UnionPair.fail(duplicate.getE());
            }
            final FileSqlInformation information = DriverHelper_123pan.moveFiles(configuration, List.of(sourceId.id()), targetId.getT().id(), targetParent).get(sourceId.id());
            if (information == null)
                throw new IllegalStateException("Failed to move file. [Unknown]. sourceFile: " + sourceFile + ", sourceId: " + sourceId + ", targetParent: " + targetParent + ", targetId: " + targetId + ", policy: " + policy);
            FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information, connectionId.get());
            connection.commit();
            return UnionPair.ok(information);
        }
    }
}
