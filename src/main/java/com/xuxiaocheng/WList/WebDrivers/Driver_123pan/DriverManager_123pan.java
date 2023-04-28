package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    static @NotNull Pair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final JSONObject data = DriverHelper_123pan.doListFiles(configuration, directoryId, limit, page);
        final JSONArray info = data.getJSONArray("InfoList");
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'InfoList'.", data);
        final List<FileInformation> list = new ArrayList<>(info.size());
        for (int i = 0; i < info.size(); ++i) {
            final FileInformation obj = FileInformation_123pan.create(directoryPath, info.getJSONObject(i));
            if (obj != null)
                list.add(obj);
        }
        DriverSqlHelper.insertFiles(configuration.getLocalSide().getName(), list, _connection);
        return Pair.makePair(data.getIntValue("Total", 0), list);
    }

    static long getFileId(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final Predicate<? super FileInformation> infoPredicate, final boolean useCache, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            return configuration.getWebSide().getFilePart().getRootDirectoryId();
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return infoPredicate.test(info) ? info.id() : -1;
        }
        final String name = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentId = DriverManager_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, useCache, _connection);
        if (parentId < 0)
            return -1;
        int page = 1;
        List<FileInformation> list;
        do {
            list = DriverManager_123pan.listFiles(configuration, parentId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, parentPath, _connection).getSecond();
            for (final FileInformation info: list)
                if (name.equals(info.path().getName()))
                    return infoPredicate.test(info) ? info.id() : -1;
        } while (!list.isEmpty());
        return -1;
    }

    static @Nullable FileInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return info;
        }
        final long id = DriverManager_123pan.getFileId(configuration, path, (f) -> true, useCache, _connection);
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
        final JSONObject info = list.getJSONObject(0);
        final FileInformation file = FileInformation_123pan.create(path.getParent(), info);
        if (file == null)
            throw new WrongResponseException("Abnormal data of 'infoList'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), file, _connection);
        return file;
    }

    static @NotNull Iterator<@NotNull FileInformation> listAllFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final long directoryId, final @NotNull Connection connection, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final ExecutorService threadPool = Objects.requireNonNullElseGet(_threadPool, () -> Executors.newFixedThreadPool(GlobalConfiguration.getInstance().getThread_count()));
        final Pair<Integer, List<FileInformation>> firstPage = DriverManager_123pan.listFiles(configuration, directoryId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), 1, directoryPath, connection);
        final int fileCount = firstPage.getFirst().intValue();
        if (fileCount <= 0)
            return MiscellaneousUtil.getEmptyIterator();
        final int pageCount = (int) Math.ceil(((double) fileCount) / configuration.getWebSide().getFilePart().getDefaultLimitPerPage());
        final AtomicInteger finishedPageCount = new AtomicInteger(1);
        final BlockingQueue<FileInformation> allFiles = new LinkedBlockingQueue<>(fileCount);
        allFiles.addAll(firstPage.getSecond());
        for (int page = 2; page <= pageCount; ++page) {
            final int current = page;
            threadPool.submit(() -> {
                allFiles.addAll(DriverManager_123pan.listFiles(configuration, directoryId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), current, directoryPath, connection).getSecond());
                return finishedPageCount.incrementAndGet();
            });
        }
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                final boolean has = finishedPageCount.get() < pageCount || !allFiles.isEmpty();
                if (!has && _threadPool == null)
                    threadPool.shutdown();
                return has;
            }

            @Override
            public @NotNull FileInformation next() {
                if (!this.hasNext())
                    throw new NoSuchElementException();
                try {
                    return allFiles.take();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        };
    }

    static void recursiveRefreshDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final long directoryId, final @Nullable Connection _connection, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        final ExecutorService threadPool = Objects.requireNonNullElseGet(_threadPool, () -> Executors.newFixedThreadPool(GlobalConfiguration.getInstance().getThread_count()));
        try {
            if (_connection == null)
                connection.setAutoCommit(false);
            DriverSqlHelper.deleteFileByParentPath(configuration.getLocalSide().getName(), directoryPath, connection);
            final Collection<Pair<String, Long>> directoryList = new ArrayList<>();
            final Iterator<FileInformation> lister = DriverManager_123pan.listAllFiles(configuration, directoryPath, directoryId, connection, threadPool);
            while (lister.hasNext()) {
                final FileInformation info = lister.next();
                if (info.is_dir())
                    directoryList.add(Pair.makePair(info.path().getName(), info.id()));
            }
            for (final Pair<String, Long> name: directoryList) {
                DriverManager_123pan.recursiveRefreshDirectory(configuration, directoryPath.child(name.getFirst()), name.getSecond().longValue(), connection, threadPool);
                directoryPath.parent();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
            if (_threadPool == null)
                threadPool.shutdown();
        }
    }

    // Files manager.

    static @Nullable String doGetDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
        if (info == null) {
            final DrivePath parentPath = path.getParent();
            final long id = DriverManager_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
            if (id < 0)
                return null;
            int page = 1;
            List<FileInformation> list;
            do {
                list = DriverManager_123pan.listFiles(configuration, id, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, parentPath, _connection).getSecond();
                for (final FileInformation obj: list)
                    if (path.getName().equals(obj.path().getName())) {
                        info = obj;
                        list.clear(); // for break again.
                        break;
                    }
            } while (!list.isEmpty());
            if (info == null)
                return null;
        }
        final JSONObject data = DriverHelper_123pan.doGetFileDownloadUrl(configuration, info);
        String url = data.getString("DownloadUrl");
        if (url == null)
            throw new WrongResponseException("Abnormal data of 'DownloadUrl'.", data);
        while (url.contains("params=")) {
            final int pIndex = url.indexOf("params=") + "params=".length();
            final int aIndex = url.indexOf('&', pIndex);
            final String base64 = url.substring(pIndex, aIndex < 0 ? url.length() : aIndex);
            url = new String(Base64.getDecoder().decode(base64));
        }
        return url;
    }

    static long prepareUpload(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath parentPath, final @NotNull String name, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            throw new IllegalParametersException("Invalid directory name.", name);
        final long parentDirectoryId = DriverManager_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", parentPath.getChildPath(name));
        return parentDirectoryId;
    }

    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final String newDirectoryName = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newDirectoryName, _connection);
        final JSONObject data = DriverHelper_123pan.doCreateDirectory(configuration, parentDirectoryId, newDirectoryName);
        final FileInformation obj = FileInformation_123pan.create(parentPath, data.getJSONObject("Info"));
        if (obj == null)
            throw new WrongResponseException("Abnormal data of 'fata/Info'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), obj, _connection);
    }

    static final long PartSize = 16 * 1024 * 1024;
    static @NotNull FileInformation doUpload(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull InputStream stream, final @NotNull String hexMd5, final long size, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.etagPattern.matcher(hexMd5).matches())
            throw new IllegalParametersException("Invalid etag (md5).", hexMd5);
        final String newDirectoryName = path.getName();
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverManager_123pan.prepareUpload(configuration, parentPath, newDirectoryName, _connection);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", path);
        final JSONObject requestUploadData = DriverHelper_123pan.doUploadRequest(configuration, parentDirectoryId, newDirectoryName, size, hexMd5);
        final FileInformation info;
        final Boolean reuse = requestUploadData.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
        if (reuse.booleanValue()) {
            final JSONObject fileInfo = requestUploadData.getJSONObject("Info");
            if (fileInfo == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
        } else {
            final String bucket = requestUploadData.getString("Bucket");
            final String node = requestUploadData.getString("StorageNode");
            final String key = requestUploadData.getString("Key");
            final String uploadId = requestUploadData.getString("UploadId");
            final Long fileId = requestUploadData.getLong("FileId");
            if (bucket == null || key == null || uploadId == null || fileId == null)
                throw new WrongResponseException("Abnormal data of 'requestUploadData'.", requestUploadData);
            final int partCount = (int) Math.ceil(((double) size) / DriverManager_123pan.PartSize);
            final JSONObject s3PareData = DriverHelper_123pan.doUploadPare(configuration, bucket, node, key, uploadId, partCount);
            final JSONObject urls = s3PareData.getJSONObject("presignedUrls");
            if (urls == null)
                throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
            final Buffer buffer = new Buffer();
            final long[] readSize = {0};
            for (int i = 1; i <= urls.size(); ++i) {
                final String url = urls.getString(String.valueOf(i));
                if (url == null)
                    throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
                DriverUtil.callRequestWithBody(DriverUtil.httpClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
                        new RequestBody() {
                            @Override
                            public @Nullable MediaType contentType() {
                                return MediaType.parse("application/octet-stream");
                            }

                            private final long len = Math.min(DriverManager_123pan.PartSize, size - readSize[0]);

                            @Override
                            public long contentLength() {
                                return this.len;
                            }

                            @Override
                            public void writeTo(final @NotNull BufferedSink bufferedSink) throws IOException {
                                buffer.readFrom(stream, this.len);
                                bufferedSink.write(buffer, this.len);
                                readSize[0] += this.len;
                            }
                        }
                ).execute().close();
            }
            final JSONObject completeUploadData = DriverHelper_123pan.doUploadComplete(configuration, bucket, node, key, uploadId, partCount, size, fileId);
            final JSONObject fileInfo = completeUploadData.getJSONObject("file_info");
            if (fileInfo == null)
                throw new WrongResponseException("Abnormal data of 'completeUploadData'.", completeUploadData);
            info = FileInformation_123pan.create(parentPath, fileInfo);
            if (info == null)
                throw new WrongResponseException("Abnormal data of 'completeUploadData'.", completeUploadData);
        }
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), info, _connection);
        return info;
    }
}
