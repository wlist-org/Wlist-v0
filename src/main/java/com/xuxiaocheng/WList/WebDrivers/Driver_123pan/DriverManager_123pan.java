package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@SuppressWarnings("SameParameterValue")
public final class DriverManager_123pan {
    private DriverManager_123pan() {
        super();
    }

    static void getUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final JSONObject data = DriverHelper_123pan.doGetUserInformation(configuration);
        configuration.getCacheSide().setNickname(Objects.requireNonNullElse(data.getString("Nickname"), "undefined"));
        configuration.getCacheSide().setImageLink(data.getString("HeadImage"));
        configuration.getCacheSide().setVip(data.getBooleanValue("Vip", false));
        configuration.getCacheSide().setSpaceAll(data.getLongValue("SpacePermanent", 0) + data.getLongValue("SpaceTemp", 0));
        configuration.getCacheSide().setSpaceUsed(data.getLongValue("SpaceUsed", 0));
        configuration.getCacheSide().setFileCount(data.getLongValue("FileCount", 0));
    }

    // File Information Getter

    private static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @NotNull DrivePath directoryPath, final @Nullable String connectionId) throws IllegalParametersException, IOException, SQLException {
        final JSONObject data = DriverHelper_123pan.doListFiles(configuration, directoryId, limit, page, policy, direction);
        final Long total = data.getLong("Total");
        final JSONArray info = data.getJSONArray("InfoList");
        if (total== null || info == null)
            throw new WrongResponseException("Listing file.", data);
        final List<FileSqlInformation> list = new LinkedList<>(info.toList(JSONObject.class).stream()
                .map(j -> FileInformation_123pan.create(directoryPath, j)).filter(Objects::nonNull).toList());
        FileSqlHelper.insertFiles(configuration.getLocalSide().getName(), list, connectionId);
        return Pair.ImmutablePair.makeImmutablePair(total, list);
    }

    private static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull FileSqlInformation>, Runnable> listAllFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId);
        return DriverUtil.wrapAllFilesListerInPages(page -> DriverManager_123pan.listFilesNoCache(configuration, directoryId,
                        configuration.getWebSide().getDefaultLimitPerPage(), page.intValue(), DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, directoryPath, id.get()),
                configuration.getWebSide().getDefaultLimitPerPage(), HExceptionWrapper.wrapConsumer(e -> {
                    try (connection) {
                        if (e == null)
                            connection.commit();
                        else
                            throw e;
                    }
                }), threadPool);
    }

    private static long getFileId(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Predicate<? super FileSqlInformation> infoPredicate, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws SQLException {
        if (path.getDepth() == 0)
            return configuration.getWebSide().getRootDirectoryId();
        if (useCache) {
            final FileSqlInformation information = FileSqlHelper.selectFile(configuration.getLocalSide().getName(), path, connectionId);
            if (information != null)
                return infoPredicate.test(information) ? information.id() : -1;
        }
        final String name = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentId = DriverManager_123pan.getFileId(configuration, parentPath, FileSqlInformation::is_dir, useCache, connectionId, threadPool);
        if (parentId < 0)
            return -1;
        FileSqlInformation information = null;
        final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, parentId, parentPath, connectionId, threadPool);
        try {
            while (lister.getB().hasNext()) {
                final FileSqlInformation info = lister.getB().next();
                if (name.equals(info.path().getName())) {
                    information = info;
                    if (useCache) {
                        while (lister.getB().hasNext())
                            lister.getB().next();
                    } else lister.getC().run();
                    break;
                }
            }
        } catch (final NoSuchElementException exception) {
            assert exception.getCause() instanceof InterruptedException;
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        return information != null && infoPredicate.test(information) ? information.id() : -1;
    }

    static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (useCache) {
            final FileSqlInformation information = FileSqlHelper.selectFile(configuration.getLocalSide().getName(), path, connectionId);
            if (information != null)
                return information;
        }
        final long id = DriverManager_123pan.getFileId(configuration, path, PredicateE.truePredicate(), false, connectionId, threadPool);
        if (id < 0)
            return null;
        // Same as recursively call 'getFileInformation' with cache.
        final JSONObject data;
        try {
            data = DriverHelper_123pan.doGetFilesInformation(configuration, List.of(id));
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.NoSuchFileResponseCode)
                return null;
            throw exception;
        }
        final JSONArray list = data.getJSONArray("infoList");
        if (list == null || list.isEmpty())
            return null;
        assert list.size() == 1;
        final FileSqlInformation file = FileInformation_123pan.create(path.getParent(), list.getJSONObject(0));
        if (file == null)
            throw new WrongResponseException("Getting file information.", data);
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(), file, connectionId);
        return file;
    }

    static void refreshDirectoryRecursively(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            if (connectionId == null) { // first time called.
                assert !"refreshDirectoryRecursively".equals(new Throwable().getStackTrace()[1].getMethodName());
                FileSqlHelper.deleteFileByParentPathRecursively(configuration.getLocalSide().getName(), directoryPath, id.get());
            }
            final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, directoryPath, id.get(), threadPool);
            final Collection<DrivePath> directoryNameList = new LinkedList<>();
            final Collection<Long> directoryIdList = new LinkedList<>();
            final Iterator<FileSqlInformation> iterator = lister.getB();
            try {
                while (iterator.hasNext()) {
                    final FileSqlInformation info = iterator.next();
                    if (info.is_dir()) {
                        directoryNameList.add(info.path());
                        directoryIdList.add(info.id());
                    }
                }
            } catch (final NoSuchElementException exception) {
                assert exception.getCause() instanceof InterruptedException;
            } catch (final RuntimeException exception) {
                throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            }
            assert directoryNameList.size() == directoryIdList.size();
            final Iterator<DrivePath> pathIterator = directoryNameList.iterator();
            final Iterator<Long> idIterator = directoryIdList.iterator();
            while (pathIterator.hasNext() && idIterator.hasNext())
                DriverManager_123pan.refreshDirectoryRecursively(configuration, idIterator.next().longValue(), pathIterator.next(), id.get(), threadPool);
            assert !pathIterator.hasNext() && !idIterator.hasNext();
            connection.commit();
        }
    }

    static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = FileSqlHelper.selectFileByParentPathInPage(configuration.getLocalSide().getName(),
                        directoryPath, limit, (long) page * limit, direction, policy, id.get());
                if (list.getFirst().longValue() > 0)
                    return list;
            }
            final long directoryId = DriverManager_123pan.getFileId(configuration, directoryPath, FileSqlInformation::is_dir, useCache, id.get(), threadPool);
            if (directoryId < 0)
                return null;
            final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = DriverManager_123pan.listFilesNoCache(configuration, directoryId,
                    limit, page, policy, direction, directoryPath, id.get());
            connection.commit();
            return list;
        }
    }

    // File Manager.

    static Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> getDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, path, useCache, connectionId, threadPool);
        if (info == null || info.is_dir())
            return null;
        final JSONObject data = DriverHelper_123pan.doGetFileDownloadUrl(configuration, info);
        final String url = data.getString("DownloadUrl");
        if (url == null)
            throw new WrongResponseException("Getting download url.", data);
        return Pair.ImmutablePair.makeImmutablePair(DriverHelper_123pan.extractDownloadUrl(url), info.size());
    }

    static @Nullable FileSqlInformation createDirectoriesRecursively(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, path, true, id.get(), threadPool);
            if (info != null)
                return info.is_dir() ? info : null;
            final FileSqlInformation parentInformation;
            final String name = path.getName();
            if (!DriverHelper_123pan.filenamePredication.test(name))
                return null;
            try {
                parentInformation = DriverManager_123pan.createDirectoriesRecursively(configuration, path.parent(), policy, id.get(), threadPool);
            } finally {
                path.child(name);
            }
            if (parentInformation == null)
                return null;
            final JSONObject data = DriverHelper_123pan.doCreateDirectory(configuration, parentInformation.id(), name, policy);
            final FileSqlInformation information = FileInformation_123pan.create(path.getParent(), data.getJSONObject("Info"));
            if (information == null)
                throw new WrongResponseException("Creating directory.", data);
            FileSqlHelper.insertFile(configuration.getLocalSide().getName(), information, id.get());
            return information;
        }
    }

    static @Nullable UploadMethods getUploadMethods(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String md5, final long size, final Options.@NotNull DuplicatePolicy policy, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!MiscellaneousUtil.md5Pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", md5);
        final String newFileName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newFileName))
            return null;
        final DrivePath parentPath = path.getParent();
        final FileSqlInformation parentDirectory = DriverManager_123pan.createDirectoriesRecursively(configuration, parentPath, policy, connectionId, threadPool);
        if (parentDirectory == null)
            return null;
        final JSONObject requestUploadData = DriverHelper_123pan.doUploadRequest(configuration, parentDirectory.id(), newFileName, size, md5, policy);
        final Boolean reuse = requestUploadData.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("PreUploading file (Reuse).", requestUploadData);
        if (reuse.booleanValue()) {
            final FileSqlInformation info = FileInformation_123pan.create(parentPath, requestUploadData.getJSONObject("Info"));
            if (info == null)
                throw new WrongResponseException("PreUploading file.", requestUploadData);
            return new UploadMethods(List.of(), () -> {
                FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
                return info;
            }, RunnableE.EmptyRunnable);
        }
        final String bucket = requestUploadData.getString("Bucket");
        final String node = requestUploadData.getString("StorageNode");
        final String key = requestUploadData.getString("Key");
        final String uploadId = requestUploadData.getString("UploadId");
        final Long fileId = requestUploadData.getLong("FileId");
        if (bucket == null || key == null || uploadId == null || fileId == null)
            throw new WrongResponseException("PreUploading file.", requestUploadData);
        final int partCount = MiscellaneousUtil.calculatePartCount(size, DriverHelper_123pan.UploadPartSize);
        final JSONObject s3PareData = DriverHelper_123pan.doUploadPare(configuration, bucket, node, key, uploadId, partCount);
        final JSONObject urls = s3PareData.getJSONObject("presignedUrls");
        if (urls == null)
            throw new WrongResponseException("PareUploading file.", s3PareData);
        assert urls.size() == partCount;
        long readSize = 0;
        final List<UploadMethods.UploadPartMethod> list = new ArrayList<>(partCount);
        final AtomicInteger countDown = new AtomicInteger(urls.size());
        for (int i = 1; i <= urls.size(); ++i) {
            final String url = urls.getString(String.valueOf(i));
            if (url == null)
                throw new WrongResponseException("PareUploading file(url:" + i + ").", s3PareData);
            final int len = Math.min(DriverHelper_123pan.UploadPartSize, (int) (size - readSize));
            readSize += len;
            list.add(new UploadMethods.UploadPartMethod(len, b -> {
                DriverNetworkHelper.callRequestWithBody(DriverNetworkHelper.httpClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
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
            }));
        }
        return new UploadMethods(list, () -> {
            if (countDown.get() > 0)
                return null;
            final JSONObject completeUploadData = DriverHelper_123pan.doUploadComplete(configuration, bucket, node, key, uploadId, partCount, size, fileId);
            final FileSqlInformation info = FileInformation_123pan.create(parentPath, completeUploadData.getJSONObject("file_info"));
            if (info == null)
                throw new WrongResponseException("CompleteUploading file.", completeUploadData);
            FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
            return info;
        }, RunnableE.EmptyRunnable);
    }

    static void trashFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final long fileId = DriverManager_123pan.getFileId(configuration, path, PredicateE.truePredicate(), useCache, id.get(), threadPool);
            final JSONObject data = DriverHelper_123pan.doTrashFiles(configuration, List.of(fileId));
            final JSONArray list = data.getJSONArray("InfoList");
            assert list != null && list.size() == 1;
            final JSONObject info = list.getJSONObject(0);
            assert info != null && Long.valueOf(fileId).equals(info.getLong("FileId"));
            FileSqlHelper.deleteFile(configuration.getLocalSide().getName(), fileId, id.get());
            connection.commit();
        }
    }

    static @Nullable FileSqlInformation renameFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return null;
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final long fileId = DriverManager_123pan.getFileId(configuration, path, PredicateE.truePredicate(), useCache, connectionId, threadPool);
            final JSONObject data = DriverHelper_123pan.doRenameFile(configuration, fileId, name, policy);
            final JSONArray infos = data.getJSONArray("Info");
            if (infos == null || infos.isEmpty())
                throw new WrongResponseException("Renaming file.", data);
            assert infos.size() == 1;
            final FileSqlInformation info = FileInformation_123pan.create(path.getParent(), infos.getJSONObject(0));
            if (info == null)
                throw new WrongResponseException("Renaming file.", data);
            assert info.id() == fileId;
            FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
            connection.commit();
            return info;
        }
    }

    static @NotNull FileSqlInformation moveFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetParent, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> id = new AtomicReference<>(connectionId);
        try (final Connection connection = connectionId == null ? FileSqlHelper.DefaultDatabaseUtil.getNewConnection(id::set) : FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            final long sourceId = DriverManager_123pan.getFileId(configuration, sourceFile, PredicateE.truePredicate(), useCache, id.get(), threadPool);
            final long targetId = DriverManager_123pan.getFileId(configuration, sourceFile, FileSqlInformation::is_dir, useCache, id.get(), threadPool);
            final JSONObject data = DriverHelper_123pan.doMoveFiles(configuration, List.of(sourceId), targetId);
            final JSONArray infos = data.getJSONArray("Info");
            if (infos == null || infos.isEmpty())
                throw new WrongResponseException("Moving file.", data);
            assert infos.size() == 1;
            final FileSqlInformation info = FileInformation_123pan.create(targetParent, infos.getJSONObject(0));
            if (info == null)
                throw new WrongResponseException("Moving file.", data);
            assert info.id() == sourceId;
            FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, id.get());
            connection.commit();
            return info;
        }
    }
}
