package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.DataAccessObjects.FileInformation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
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

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final JSONObject data = DriverHelper_123pan.doListFiles(configuration, directoryId, limit, page);
        final JSONArray info = data.getJSONArray("InfoList");
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'InfoList'.", data);
        final List<FileInformation> list = new LinkedList<>(info.toList(JSONObject.class).stream()
                .map(j -> FileInformation_123pan.create(directoryPath, j)).filter(Objects::nonNull).toList());
        DriverSqlHelper.insertFiles(configuration.getLocalSide().getName(), list, _connection);
        return Pair.ImmutablePair.makeImmutablePair(data.getIntValue("Total", 0), list);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull Iterator<@NotNull FileInformation>> listAllFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        boolean noThread = true;
        final Connection connection = DataBaseUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try {
            if (_connection == null)
                connection.setAutoCommit(false);
            DriverSqlHelper.deleteFileByParentPath(configuration.getLocalSide().getName(), directoryPath, connection);
            final Pair.ImmutablePair<Integer, List<FileInformation>> firstPage = DriverManager_123pan.listFilesNoCache(configuration, directoryId, configuration.getWebSide().getDefaultLimitPerPage(), 1, directoryPath, connection);
            final int fileCount = firstPage.getFirst().intValue();
            if (fileCount <= 0 || firstPage.getSecond().isEmpty()) {
                if (_connection == null)
                    connection.commit();
                return Pair.ImmutablePair.makeImmutablePair(0, MiscellaneousUtil.getEmptyIterator());
            }
            final int pageCount = (int) Math.ceil(((double) fileCount) / configuration.getWebSide().getDefaultLimitPerPage());
            if (pageCount <= 1) {
                if (_connection == null)
                    connection.commit();
                return Pair.ImmutablePair.makeImmutablePair(fileCount, firstPage.getSecond().iterator());
            }
            noThread = false;
            final AtomicInteger finishedPageCount = new AtomicInteger(1);
            final BlockingQueue<FileInformation> allFiles = new LinkedBlockingQueue<>(firstPage.getSecond());
            for (int page = 2; page <= pageCount; ++page) {
                final int current = page;
                threadPool.submit(() -> {
                    final Pair.ImmutablePair<Integer, List<FileInformation>> infos = DriverManager_123pan.listFilesNoCache(configuration, directoryId, configuration.getWebSide().getDefaultLimitPerPage(), current, directoryPath, connection);
                    assert infos.getFirst().intValue() == fileCount;
                    // TODO interrupt.
                    allFiles.addAll(infos.getSecond());
                    return finishedPageCount.incrementAndGet();
                });
            }
            return Pair.ImmutablePair.makeImmutablePair(fileCount, new Iterator<>() {
                @Override
                public boolean hasNext() {
                    final boolean have = finishedPageCount.get() < pageCount || !allFiles.isEmpty();
                    if (!have) {
                        if (_connection == null)
                            try {
                                connection.commit();
                                connection.close();
                            } catch (final SQLException exception) {
                                throw new RuntimeException(exception);
                            }
                    }
                    return have;
                }

                @Override
                public synchronized @NotNull FileInformation next() {
                    if (!this.hasNext())
                        throw new NoSuchElementException();
                    try {
                        return allFiles.take();
                    } catch (final InterruptedException exception) {
                        throw new NoSuchElementException(exception);
                    }
                }
            });
        } finally {
            if (noThread && _connection == null)
                connection.close();
        }
    }

    static long getFileId(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Predicate<? super FileInformation> infoPredicate, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            return configuration.getWebSide().getRootDirectoryId();
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return infoPredicate.test(info) ? info.id() : -1;
        }
        final String name = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentId = DriverManager_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, useCache, _connection, threadPool);
        if (parentId < 0)
            return -1;
        final Iterator<FileInformation> iterator = DriverManager_123pan.listAllFilesNoCache(configuration, parentId, parentPath, _connection, threadPool).getSecond();
        try {
            while (iterator.hasNext()) {
                final FileInformation info = iterator.next();
                if (name.equals(info.path().getName())) {
                    while (iterator.hasNext())
                        iterator.next();
                    return infoPredicate.test(info) ? info.id() : -1;
                }
            }
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof SQLException sqlException)
                throw sqlException;
            throw exception;
        }
        return -1;
    }

    static @Nullable FileInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return info;
        }
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, false, _connection, threadPool);
        if (id < 0)
            return null;
        final JSONObject data;
        try {
            data = DriverHelper_123pan.doGetFilesInformation(configuration, List.of(id));
        } catch (final WrongResponseException exception) {
            if (exception.getMessage().startsWith("Code: 400"))
                return null;
            throw exception;
        }
        final JSONArray list = data.getJSONArray("infoList");
        if (list == null || list.isEmpty())
            return null;
        assert list.size() == 1;
        final JSONObject info = list.getJSONObject(0);
        final FileInformation file = FileInformation_123pan.create(path.getParent(), info);
        if (file == null)
            throw new WrongResponseException("Abnormal data of 'infoList'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), file, _connection);
        return file;
    }

    static void recursiveRefreshDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final Connection connection = DataBaseUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try {
            if (_connection == null)
                connection.setAutoCommit(false);
            final Pair.ImmutablePair<Integer, Iterator<FileInformation>> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, directoryPath, connection, threadPool);
            final Collection<String> directoryNameList = new LinkedList<>();
            final Collection<Long> directoryIdList = new LinkedList<>();
            final Iterator<FileInformation> iterator = lister.getSecond();
            try {
                while (iterator.hasNext()) {
                    final FileInformation info = iterator.next();
                    if (info.is_dir()) {
                        directoryNameList.add(info.path().getName());
                        directoryIdList.add(info.id());
                    }
                }
            } catch (final RuntimeException exception) {
                if (exception.getCause() instanceof SQLException sqlException)
                    throw sqlException;
                throw exception;
            }
            assert directoryNameList.size() == directoryIdList.size();
            final Iterator<String> nameIterator = directoryNameList.iterator();
            final Iterator<Long> idIterator = directoryIdList.iterator();
            while (nameIterator.hasNext() && idIterator.hasNext()) {
                directoryPath.child(nameIterator.next());
                try {
                    DriverManager_123pan.recursiveRefreshDirectory(configuration, idIterator.next().longValue(), directoryPath, connection, threadPool);
                } finally {
                    directoryPath.parent();
                }
            }
            assert !nameIterator.hasNext() && !idIterator.hasNext();
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull @UnmodifiableView List<@NotNull FileInformation>> listFilesWithCache(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final @Nullable OrderDirection direction, final @Nullable OrderPolicy policy, final @Nullable Connection _connection) throws SQLException {
        return DriverSqlHelper.getFileByParentPathS(configuration.getLocalSide().getName(), directoryPath, limit, (page - 1) * limit,
                Objects.requireNonNullElse(direction, configuration.getWebSide().getDefaultOrderDirection()),
                Objects.requireNonNullElse(policy, configuration.getWebSide().getDefaultOrderPolicy()),
                _connection);
    }

    // File Manager.

    static Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> getDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverManager_123pan.getFileInformation(configuration, path, useCache, _connection, threadPool);
        if (info == null)
            return null;
        if (info.is_dir())
            return null;
        final JSONObject data = DriverHelper_123pan.doGetFileDownloadUrl(configuration, info);
        final String url = data.getString("DownloadUrl");
        if (url == null)
            throw new WrongResponseException("Abnormal data of 'DownloadUrl'.", data);
        return Pair.ImmutablePair.makeImmutablePair(DriverHelper_123pan.extractDownloadUrl(url), info.size());
    }

    private static long prepareUpload(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath parentPath, final @NotNull String name, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            throw new IllegalParametersException("Invalid file name.", name);
        final long parentDirectoryId = DriverManager_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection, threadPool);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", parentPath.getChildPath(name));
        return parentDirectoryId;
    }

    static @NotNull FileInformation createDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final String newDirectoryName = path.getName();
        // Always called by mkdirs and this is checked.
//        if (!DriverHelper_123pan.filenamePredication.test(newDirectoryName)) return null;
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newDirectoryName, _connection, threadPool);
        final JSONObject data = DriverHelper_123pan.doCreateDirectory(configuration, parentDirectoryId, newDirectoryName);
        final FileInformation obj = FileInformation_123pan.create(parentPath, data.getJSONObject("Info"));
        if (obj == null)
            throw new WrongResponseException("Abnormal data of 'data/Info'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), obj, _connection);
        return obj;
    }

    static @Nullable UploadMethods getUploadMethods(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String md5, final long size, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverUtil.tagPredication.test(md5))
            throw new IllegalParametersException("Invalid etag (md5).", md5);
        final String newFileName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newFileName))
            return null;
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newFileName, _connection, threadPool);
        final JSONObject requestUploadData = DriverHelper_123pan.doUploadRequest(configuration, parentDirectoryId, newFileName, size, md5);
        final Boolean reuse = requestUploadData.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
        if (reuse.booleanValue()) {
            final JSONObject fileInfo = requestUploadData.getJSONObject("Info");
            if (fileInfo == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            final FileInformation info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            return new UploadMethods(List.of(), () -> {
                DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), info, _connection);
                return info;
            }, UploadMethods.EmptyFinisher);
        }
        final String bucket = requestUploadData.getString("Bucket");
        final String node = requestUploadData.getString("StorageNode");
        final String key = requestUploadData.getString("Key");
        final String uploadId = requestUploadData.getString("UploadId");
        final Long fileId = requestUploadData.getLong("FileId");
        if (bucket == null || key == null || uploadId == null || fileId == null)
            throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
        final int partCount = (int) Math.ceil(((double) size) / DriverHelper_123pan.UploadPartSize);
        final JSONObject s3PareData = DriverHelper_123pan.doUploadPare(configuration, bucket, node, key, uploadId, partCount);
        final JSONObject urls = s3PareData.getJSONObject("presignedUrls");
        if (urls == null)
            throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
        assert urls.size() == partCount;
        long readSize = 0;
        final List<UploadMethods.UploadPartMethod> list = new ArrayList<>(partCount);
        final AtomicInteger countDown = new AtomicInteger(urls.size());
        for (int i = 1; i <= urls.size(); ++i) {
            final String url = urls.getString(String.valueOf(i));
            if (url == null)
                throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
            final int len = Math.min(DriverHelper_123pan.UploadPartSize, (int) (size - readSize));
            readSize += len;
            list.add(new UploadMethods.UploadPartMethod(len, b -> {
                DriverNetworkHelper.callRequestWithBody(DriverNetworkHelper.httpClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
                        new RequestBody() {
                            @Override
                            public @Nullable MediaType contentType() {
                                return MediaType.parse("application/octet-stream");
                            }

                            @Override
                            public long contentLength() {
                                return len;
                            }

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
            final JSONObject fileInfo = completeUploadData.getJSONObject("file_info");
            if (fileInfo == null)
                throw new WrongResponseException("Abnormal data of 'completeUploadData'.", completeUploadData);
            final FileInformation info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'completeUploadData'.", completeUploadData);
            DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), info, _connection);
            return info;
        }, UploadMethods.EmptyFinisher);
    }

    static void trashFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, useCache, _connection, threadPool);
        final JSONObject data = DriverHelper_123pan.doTrashFiles(configuration, List.of(id));
        assert data.getJSONArray("InfoList") != null && data.getJSONArray("InfoList").size() == 1;
        assert data.getJSONArray("InfoList").getJSONObject(0) != null
                && Long.valueOf(id).equals(data.getJSONArray("InfoList").getJSONObject(0).getLong("FileId"));
        DriverSqlHelper.deleteFileById(configuration.getLocalSide().getName(), id, _connection);
    }

    static @NotNull FileInformation renameFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String name, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            throw new IllegalParametersException("Invalid file name.", name);
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, useCache, _connection, threadPool);
        final JSONObject data = DriverHelper_123pan.doRenameFile(configuration, id, name);
        final JSONArray infos = data.getJSONArray("Info");
        if (infos == null || infos.isEmpty())
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert infos.size() == 1;
        final JSONObject fileInfo = infos.getJSONObject(0);
        final FileInformation info = FileInformation_123pan.create(path.getParent(), fileInfo);
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert info.id() == id;
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), info, _connection);
        return info;
    }

    static @NotNull FileInformation moveFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetParent, final boolean useCache, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final long sourceId = DriverManager_123pan.getFileId(configuration, sourceFile, f -> true, useCache, _connection, threadPool);
        final long targetId = DriverManager_123pan.getFileId(configuration, sourceFile, FileInformation::is_dir, useCache, _connection, threadPool);
        final JSONObject data = DriverHelper_123pan.doMoveFiles(configuration, List.of(sourceId), targetId);
        final JSONArray infos = data.getJSONArray("Info");
        if (infos == null || infos.isEmpty())
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert infos.size() == 1;
        final JSONObject fileInfo = infos.getJSONObject(0);
        final FileInformation info = FileInformation_123pan.create(targetParent, fileInfo);
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert info.id() == sourceId;
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), info, _connection);
        return info;
    }
}
