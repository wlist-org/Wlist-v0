package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
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

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileSqlInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final @NotNull DrivePath directoryPath, final @Nullable String connectionId) throws IllegalParametersException, IOException, SQLException {
        final JSONObject data = DriverHelper_123pan.doListFiles(configuration, directoryId, limit, page);
        final JSONArray info = data.getJSONArray("InfoList");
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'InfoList'.", data);
        final List<FileSqlInformation> list = new LinkedList<>(info.toList(JSONObject.class).stream()
                .map(j -> FileInformation_123pan.create(directoryPath, j)).filter(Objects::nonNull).toList());
        FileSqlHelper.insertFiles(configuration.getLocalSide().getName(), list, connectionId);
        return Pair.ImmutablePair.makeImmutablePair(data.getIntValue("Total", 0), list);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull Iterator<@NotNull FileSqlInformation>> listAllFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        boolean noThread = true;
        final Connection connection = DatabaseUtil.requireConnection(connectionId, DatabaseUtil.getInstance());
        try {
            connection.setAutoCommit(false);
            FileSqlHelper.deleteFileByParentPath(configuration.getLocalSide().getName(), directoryPath, connectionId);
            final Pair.ImmutablePair<Integer, List<FileSqlInformation>> firstPage = DriverManager_123pan.listFilesNoCache(configuration, directoryId, configuration.getWebSide().getDefaultLimitPerPage(), 1, directoryPath, connectionId);
            final int fileCount = firstPage.getFirst().intValue();
            if (fileCount <= 0 || firstPage.getSecond().isEmpty()) {
                connection.commit();
                return Pair.ImmutablePair.makeImmutablePair(0, MiscellaneousUtil.getEmptyIterator());
            }
            final int pageCount = (int) Math.ceil(((double) fileCount) / configuration.getWebSide().getDefaultLimitPerPage());
            if (pageCount <= 1) {
                connection.commit();
                return Pair.ImmutablePair.makeImmutablePair(fileCount, firstPage.getSecond().iterator());
            }
            noThread = false;
            final AtomicInteger finishedPageCount = new AtomicInteger(1);
            final BlockingQueue<FileSqlInformation> allFiles = new LinkedBlockingQueue<>(firstPage.getSecond());
            for (int page = 2; page <= pageCount; ++page) {
                final int current = page;
                threadPool.submit(() -> {
                    final Pair.ImmutablePair<Integer, List<FileSqlInformation>> infos = DriverManager_123pan.listFilesNoCache(configuration, directoryId, configuration.getWebSide().getDefaultLimitPerPage(), current, directoryPath, connectionId);
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
                        if (connectionId == null)
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
                public synchronized @NotNull FileSqlInformation next() {
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
            if (noThread)
                connection.close();
        }
    }

    static long getFileId(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Predicate<? super FileSqlInformation> infoPredicate, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            return configuration.getWebSide().getRootDirectoryId();
        if (useCache) {
            final FileSqlInformation info = FileSqlHelper.selectFile(configuration.getLocalSide().getName(), path, connectionId);
            if (info != null)
                return infoPredicate.test(info) ? info.id() : -1;
        }
        final String name = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentId = DriverManager_123pan.getFileId(configuration, parentPath, FileSqlInformation::is_dir, useCache, connectionId, threadPool);
        if (parentId < 0)
            return -1;
        final Iterator<FileSqlInformation> iterator = DriverManager_123pan.listAllFilesNoCache(configuration, parentId, parentPath, connectionId, threadPool).getSecond();
        try {
            while (iterator.hasNext()) {
                final FileSqlInformation info = iterator.next();
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

    static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (useCache) {
            final FileSqlInformation info = FileSqlHelper.selectFile(configuration.getLocalSide().getName(), path, connectionId);
            if (info != null)
                return info;
        }
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, false, connectionId, threadPool);
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
        final FileSqlInformation file = FileInformation_123pan.create(path.getParent(), info);
        if (file == null)
            throw new WrongResponseException("Abnormal data of 'infoList'.", data);
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(), file, connectionId);
        return file;
    }

    static void recursiveRefreshDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final Connection connection = DatabaseUtil.requireConnection(connectionId, DatabaseUtil.getInstance());
        try {
            connection.setAutoCommit(false);
            final Pair.ImmutablePair<Integer, Iterator<FileSqlInformation>> lister = DriverManager_123pan.listAllFilesNoCache(configuration, directoryId, directoryPath, connectionId, threadPool);
            final Collection<String> directoryNameList = new LinkedList<>();
            final Collection<Long> directoryIdList = new LinkedList<>();
            final Iterator<FileSqlInformation> iterator = lister.getSecond();
            try {
                while (iterator.hasNext()) {
                    final FileSqlInformation info = iterator.next();
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
                    DriverManager_123pan.recursiveRefreshDirectory(configuration, idIterator.next().longValue(), directoryPath, connectionId, threadPool);
                } finally {
                    directoryPath.parent();
                }
            }
            assert !nameIterator.hasNext() && !idIterator.hasNext();
            connection.commit();
        } finally {
            connection.close();
        }
    }

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> listFilesWithCache(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final @Nullable OrderDirection direction, final @Nullable OrderPolicy policy, final @Nullable String connectionId) throws SQLException {
        return FileSqlHelper.selectFileByParentPathInPage(configuration.getLocalSide().getName(), directoryPath, limit, (long) page * limit,
                Objects.requireNonNullElse(direction, configuration.getWebSide().getDefaultOrderDirection()),
                Objects.requireNonNullElse(policy, configuration.getWebSide().getDefaultOrderPolicy()),
                connectionId);
    }

    // File Manager.

    static Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> getDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = DriverManager_123pan.getFileInformation(configuration, path, useCache, connectionId, threadPool);
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

    private static long prepareUpload(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath parentPath, final @NotNull String name, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            throw new IllegalParametersException("Invalid file name.", name);
        final long parentDirectoryId = DriverManager_123pan.getFileId(configuration, parentPath, FileSqlInformation::is_dir, true, connectionId, threadPool);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", parentPath.getChildPath(name));
        return parentDirectoryId;
    }

    static @NotNull FileSqlInformation createDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final String newDirectoryName = path.getName();
        // Always called by mkdirs and this is checked.
//        if (!DriverHelper_123pan.filenamePredication.test(newDirectoryName)) return null;
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newDirectoryName, connectionId, threadPool);
        final JSONObject data = DriverHelper_123pan.doCreateDirectory(configuration, parentDirectoryId, newDirectoryName);
        final FileSqlInformation obj = FileInformation_123pan.create(parentPath, data.getJSONObject("Info"));
        if (obj == null)
            throw new WrongResponseException("Abnormal data of 'data/Info'.", data);
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(), obj, connectionId);
        return obj;
    }

    static @Nullable UploadMethods getUploadMethods(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String md5, final long size, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!MiscellaneousUtil.md5Pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", md5);
        final String newFileName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newFileName))
            return null;
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newFileName, connectionId, threadPool);
        final JSONObject requestUploadData = DriverHelper_123pan.doUploadRequest(configuration, parentDirectoryId, newFileName, size, md5);
        final Boolean reuse = requestUploadData.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
        if (reuse.booleanValue()) {
            final JSONObject fileInfo = requestUploadData.getJSONObject("Info");
            if (fileInfo == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            final FileSqlInformation info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            return new UploadMethods(List.of(), () -> {
                FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
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
            final FileSqlInformation info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'completeUploadData'.", completeUploadData);
            FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
            return info;
        }, UploadMethods.EmptyFinisher);
    }

    static void trashFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, useCache, connectionId, threadPool);
        final JSONObject data = DriverHelper_123pan.doTrashFiles(configuration, List.of(id));
        assert data.getJSONArray("InfoList") != null && data.getJSONArray("InfoList").size() == 1;
        assert data.getJSONArray("InfoList").getJSONObject(0) != null
                && Long.valueOf(id).equals(data.getJSONArray("InfoList").getJSONObject(0).getLong("FileId"));
        FileSqlHelper.deleteFile(configuration.getLocalSide().getName(), id, connectionId);
    }

    static @NotNull FileSqlInformation renameFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String name, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            throw new IllegalParametersException("Invalid file name.", name);
        final long id = DriverManager_123pan.getFileId(configuration, path, f -> true, useCache, connectionId, threadPool);
        final JSONObject data = DriverHelper_123pan.doRenameFile(configuration, id, name);
        final JSONArray infos = data.getJSONArray("Info");
        if (infos == null || infos.isEmpty())
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert infos.size() == 1;
        final JSONObject fileInfo = infos.getJSONObject(0);
        final FileSqlInformation info = FileInformation_123pan.create(path.getParent(), fileInfo);
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert info.id() == id;
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
        return info;
    }

    static @NotNull FileSqlInformation moveFile(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetParent, final boolean useCache, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IllegalParametersException, IOException, SQLException {
        final long sourceId = DriverManager_123pan.getFileId(configuration, sourceFile, f -> true, useCache, connectionId, threadPool);
        final long targetId = DriverManager_123pan.getFileId(configuration, sourceFile, FileSqlInformation::is_dir, useCache, connectionId, threadPool);
        final JSONObject data = DriverHelper_123pan.doMoveFiles(configuration, List.of(sourceId), targetId);
        final JSONArray infos = data.getJSONArray("Info");
        if (infos == null || infos.isEmpty())
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert infos.size() == 1;
        final JSONObject fileInfo = infos.getJSONObject(0);
        final FileSqlInformation info = FileInformation_123pan.create(targetParent, fileInfo);
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        assert info.id() == sourceId;
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(), info, connectionId);
        return info;
    }
}
