package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@SuppressWarnings("SameParameterValue")
public final class DriverUtil_123pan {
    private DriverUtil_123pan() {
        super();
    }

    private static final @NotNull HLog logger = HLog.getInstance("DefaultLogger");

    // Token Refresher

    private static void handleLoginData(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull JSONObject data) throws WrongResponseException {
        final String token = data.getString("token");
        if (token == null)
            throw new WrongResponseException("No token in response.");
        configuration.getCacheSide().setToken(token);
        final String expire = data.getString("expire");
        if (expire == null)
            throw new WrongResponseException("No expire time in response.");
        configuration.getCacheSide().setTokenExpireTime(LocalDateTime.parse(expire, DateTimeFormatter.ISO_ZONED_DATE_TIME));
        final Long refresh = data.getLong("refresh_token_expire_time");
        if (refresh == null)
            throw new WrongResponseException("No refresh time in response.");
        // TODO: time zone ?
        //noinspection UseOfObsoleteDateTimeApi
        configuration.getCacheSide().setRefreshExpireTime(new Date(refresh.longValue() * 1000).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private static void doGetToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final Map<String, Object> requestBody = new LinkedHashMap<>(4);
        requestBody.put("type", configuration.getWebSide().getLoginPart().getLoginType());
        requestBody.put(switch (configuration.getWebSide().getLoginPart().getLoginType()) {
                case 1 -> "passport";
                case 2 -> "mail";
                default -> throw new IllegalParametersException("Unknown login type.", configuration.getWebSide().getLoginPart().getLoginType());
            }, configuration.getWebSide().getLoginPart().getPassport());
        requestBody.put("password", configuration.getWebSide().getLoginPart().getPassword());
        requestBody.put("remember", false);
        final JSONObject json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(DriverHelper_123pan.LoginURL,
                null, DriverUtil.createJsonRequestBody(requestBody))).string());
        final JSONObject data = DriverHelper_123pan.extractResponseData(json, 200, "success");
        DriverUtil_123pan.logger.log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverUtil_123pan.handleLoginData(configuration, data);
    }

    private static boolean doRefreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getCacheSide().getToken() == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final JSONObject json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(DriverHelper_123pan.RefreshTokenURL,
                new Headers.Builder().add("authorization", "Bearer " + configuration.getCacheSide().getToken()).build(),
                RequestBody.create("".getBytes(StandardCharsets.UTF_8)))).string());
        final JSONObject data;
        try {
            data = DriverHelper_123pan.extractResponseData(json, 200, "success");
        } catch (final WrongResponseException exception) {
            if (exception.getMessage().startsWith("Code: 401"))
                return true; // throw new TokenExpiredException();
            throw exception;
        }
        DriverUtil_123pan.logger.log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverUtil_123pan.handleLoginData(configuration, data);
        return false;
    } // true means failed.

    private static void forceRetrieveToken(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull LocalDateTime time) throws IllegalParametersException, IOException {
        if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getRefreshExpireTime() == null || time.isAfter(configuration.getCacheSide().getRefreshExpireTime())
                || DriverUtil_123pan.doRefreshToken(configuration))
            DriverUtil_123pan.doGetToken(configuration);
    }

    static synchronized void doRetrieveToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now();
        if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getTokenExpireTime() == null || time.isAfter(configuration.getCacheSide().getTokenExpireTime()))
            DriverUtil_123pan.forceRetrieveToken(configuration, time);
    }

    // Tokenized Network Manager

    @SuppressWarnings("TypeMayBeWeakened")
    private static @NotNull JSONObject sendJsonWithToken(final @NotNull Pair.ImmutablePair<@NotNull String, @NotNull String> url, final @NotNull DriverConfiguration_123Pan configuration, final @Nullable JSONObject request) throws IllegalParametersException, IOException {
        DriverUtil_123pan.doRetrieveToken(configuration);
        final Headers headers = new Headers.Builder().add("authorization", "Bearer " + configuration.getCacheSide().getToken())
                .add("user-agent", "123pan/1.0.100" + DriverUtil.defaultUserAgent)
                .add("app-version", "101") // .add("app-version", "2")
                .add("platform", "pc") // .add("platform", "web")
                .build();
        final Response response;
        if ("GET".equals(url.getSecond()) && request != null)
            response = DriverUtil.sendRequestWithParameters(url, headers, request);
        else
            response = DriverUtil.sendRequest(url, headers, request == null ? null : DriverUtil.createJsonRequestBody(request));
        final JSONObject json = JSON.parseObject(DriverUtil.checkResponseSuccessful(response).bytes());
        response.close();
        try {
            return DriverHelper_123pan.extractResponseData(json, 0, "ok");
        } catch (final WrongResponseException exception) {
            if (!exception.getMessage().startsWith("Code: 401"))
                throw exception;
            DriverUtil_123pan.forceRetrieveToken(configuration, LocalDateTime.now());
            final Headers newHeaders = headers.newBuilder().set("authorization", "Bearer " + configuration.getCacheSide().getToken()).build();
            final JSONObject newJson;
            if ("GET".equals(url.getSecond()) && request != null)
                newJson = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequestWithParameters(url, newHeaders, request)).bytes());
            else
                newJson = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(url, newHeaders, "GET".equals(url.getSecond()) ? null : DriverUtil.createJsonRequestBody(request))).bytes());
            return DriverHelper_123pan.extractResponseData(newJson, 0, "ok");
        }
    }

    // User Information Getter

    static void doGetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final JSONObject data = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.UserInformationURL, configuration, null);
        configuration.getCacheSide().setNickname(data.getString("Nickname"));
        configuration.getCacheSide().setImageLink(data.getString("HeadImage"));
        configuration.getCacheSide().setVip(data.getBooleanValue("Vip", false));
        configuration.getCacheSide().setSpaceAll(data.getLongValue("SpacePermanent", 0) + data.getLongValue("SpaceTemp", 0));
        configuration.getCacheSide().setSpaceUsed(data.getLongValue("SpaceUsed", 0));
        configuration.getCacheSide().setFileCount(data.getLongValue("FileCount", 0));
    }

    // File Information Getter

    static @NotNull Pair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> doListFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final JSONObject request = new JSONObject(7);
        request.put("driveId", 0);
        request.put("limit", limit);
        request.put("orderBy", DriverHelper_123pan.getOrderPolicy(configuration.getWebSide().getFilePart().getOrderPolicy()));
        request.put("orderDirection", DriverHelper_123pan.getOrderDirection(configuration.getWebSide().getFilePart().getOrderDirection()));
        request.put("parentFileId", directoryId);
        request.put("Page", page);
        request.put("trashed", false);
        final JSONObject data = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.ListFilesURL, configuration, request);
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
        final long parentId = DriverUtil_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, useCache, _connection);
        if (parentId < 0)
            return -1;
        int page = 1;
        List<FileInformation> list;
        do {
            list = DriverUtil_123pan.doListFiles(configuration, parentId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, parentPath, _connection).getSecond();
            for (final FileInformation info: list)
                if (name.equals(info.path().getName()))
                    return infoPredicate.test(info) ? info.id() : -1;
        } while (!list.isEmpty());
        return -1;
    }

    public static @Nullable FileInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return info;
        }
        final long id = DriverUtil_123pan.getFileId(configuration, path, (f) -> true, useCache, _connection);
        final JSONObject request = DriverHelper_123pan.buildFileIdList(List.of(id));
        final JSONObject data;
        try {
            data = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.FilesInfoURL, configuration, request);
        } catch (final WrongResponseException exception) {
            if (exception.getMessage().startsWith("Code: 400"))
                return null;
            throw exception;
        }
        final JSONArray list = data.getJSONArray("infoList");
        if (list == null || list.isEmpty())
            return null;
        final JSONObject info = list.getJSONObject(0);
        DriverHelper_123pan.checkForFileNecessaryInfo(info);
        final FileInformation file = FileInformation_123pan.create(path.getParent(), info);
        if (file == null)
            throw new WrongResponseException("Abnormal data of 'infoList'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), file, _connection);
        return file;
    }

    static @NotNull Iterator<@NotNull FileInformation> listAllFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final long directoryId, final @NotNull Connection connection, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final ExecutorService threadPool = Objects.requireNonNullElseGet(_threadPool, () -> Executors.newFixedThreadPool(GlobalConfiguration.getInstance().getThread_count()));
        final Pair<Integer, List<FileInformation>> firstPage = DriverUtil_123pan.doListFiles(configuration, directoryId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), 1, directoryPath, connection);
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
                allFiles.addAll(DriverUtil_123pan.doListFiles(configuration, directoryId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), current, directoryPath, connection).getSecond());
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
            final Iterator<FileInformation> lister = DriverUtil_123pan.listAllFiles(configuration, directoryPath, directoryId, connection, threadPool);
            while (lister.hasNext()) {
                final FileInformation info = lister.next();
                if (info.is_dir())
                    directoryList.add(Pair.makePair(info.path().getName(), info.id()));
            }
            for (final Pair<String, Long> name: directoryList) {
                DriverUtil_123pan.recursiveRefreshDirectory(configuration, directoryPath.child(name.getFirst()), name.getSecond().longValue(), connection, threadPool);
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
    
    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final String newDirectoryName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new IllegalParametersException("Invalid directory name.", newDirectoryName);
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverUtil_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", path);
        final JSONObject request = new JSONObject(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", newDirectoryName);
        request.put("parentFileId", parentDirectoryId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(configuration.getWebSide().getFilePart().getDuplicatePolicy()));
        final JSONObject data = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.UploadRequestURL, configuration, request);
        final FileInformation obj = FileInformation_123pan.create(parentPath, data.getJSONObject("Info"));
        if (obj == null)
            throw new WrongResponseException("Abnormal data of 'fata/Info'.", data);
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(), obj, _connection);
    }

    static @Nullable String doGetDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
        if (info == null) {
            final DrivePath parentPath = path.getParent();
            final long id = DriverUtil_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
            if (id < 0)
                return null;
            int page = 1;
            List<FileInformation> list;
            do {
                list = DriverUtil_123pan.doListFiles(configuration, id, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, parentPath, _connection).getSecond();
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
        final JSONObject request = new JSONObject(8);
        request.put("driveId", 0);
        request.put("fileId", info.id());
        request.put("fileName", info.path().getName());
        request.put("size", info.size());
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeOther(info);
        request.put("s3keyFlag",extra.s3key());
        request.put("etag", info.tag());
        final JSONObject data = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.SingleFileDownloadURL, configuration, request);
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

    static final long PartSize = 16 * 1024 * 1024;
    public static @NotNull FileInformation doUpload(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull InputStream stream, final long size, final @NotNull String hexMd5, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        if (!DriverHelper_123pan.etagPattern.matcher(hexMd5).matches())
            throw new IllegalParametersException("Invalid etag (md5).", hexMd5);
        final String newDirectoryName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new IllegalParametersException("Invalid directory name.", newDirectoryName);
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverUtil_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.", path);
        final JSONObject requestUploadRequest = new JSONObject(7);
        requestUploadRequest.put("driveId", 0);
        requestUploadRequest.put("etag", hexMd5);
        requestUploadRequest.put("fileName", newDirectoryName);
        requestUploadRequest.put("parentFileId", parentDirectoryId);
        requestUploadRequest.put("size", size);
        requestUploadRequest.put("type", 0);
        requestUploadRequest.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(configuration.getWebSide().getFilePart().getDuplicatePolicy()));
        final JSONObject requestUploadData = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.UploadRequestURL, configuration, requestUploadRequest);
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
            final int partCount = (int) Math.ceil(((double) size) / DriverUtil_123pan.PartSize);
            final JSONObject s3PareRequest = new JSONObject(6);
            s3PareRequest.put("bucket", bucket);
            s3PareRequest.put("StorageNode", node);
            s3PareRequest.put("key", key);
            s3PareRequest.put("uploadId", uploadId);
            s3PareRequest.put("partNumberStart", 1);
            s3PareRequest.put("partNumberEnd", partCount);
            final JSONObject s3PareData = DriverUtil_123pan.sendJsonWithToken(
                    partCount <= 1 ? DriverHelper_123pan.S3AuthPartURL : DriverHelper_123pan.S3ParePartsURL, configuration, s3PareRequest);
            final JSONObject urls = s3PareData.getJSONObject("presignedUrls");
            if (urls == null)
                throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
            final Buffer buffer = new Buffer();
            final long[] readSize = {0};
            for (int i = 1; i <= urls.size(); ++i) {
                final String url = urls.getString(String.valueOf(i));
                if (url == null)
                    throw new WrongResponseException("Abnormal data of 'presignedUrls'.", s3PareData);
                DriverUtil.sendRequest(Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
                        new RequestBody() {
                            @Override
                            public @Nullable MediaType contentType() {
                                return MediaType.parse("application/octet-stream");
                            }

                            private final long len = Math.min(DriverUtil_123pan.PartSize, size - readSize[0]);

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
                ).close();
            }
            final JSONObject completeUploadRequest = new JSONObject(7);
            completeUploadRequest.put("bucket", bucket);
            completeUploadRequest.put("StorageNode", node);
            completeUploadRequest.put("key", key);
            completeUploadRequest.put("uploadId", uploadId);
            completeUploadRequest.put("isMultipart", partCount != 1);
            completeUploadRequest.put("fileSize", size);
            completeUploadRequest.put("fileId", fileId);
            final JSONObject completeUploadData = DriverUtil_123pan.sendJsonWithToken(DriverHelper_123pan.UploadCompleteURL, configuration, completeUploadRequest);
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
