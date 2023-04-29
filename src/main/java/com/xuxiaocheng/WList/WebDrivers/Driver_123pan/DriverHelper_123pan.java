package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import okhttp3.Headers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    static final @NotNull Pair.ImmutablePair<String, String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> FilesInfoURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/info", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> SingleFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/download_info", "POST");
//    static final @NotNull Pair.ImmutablePair<String, String> BatchFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/batch_download_info", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> S3AuthPartURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_upload_object/auth", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> S3ParePartsURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_repare_upload_parts_batch", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UploadCompleteURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_complete/v2", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> TrashFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/trash", "POST");

    private static final @NotNull DuplicatePolicy defaultDuplicatePolicy = DuplicatePolicy.KEEP;
    private static final @NotNull OrderPolicy defaultOrderPolicy = OrderPolicy.FileName;
    private static final @NotNull OrderDirection defaultOrderDirection = OrderDirection.ASCEND;
    @Contract(pure = true) static int getDuplicatePolicy(final @Nullable DuplicatePolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
            default -> DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        };
    }
    @Contract(pure = true) static @NotNull String getOrderPolicy(final @Nullable OrderPolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderPolicy(DriverHelper_123pan.defaultOrderPolicy);
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
//            default -> DriverHelper_123pan.getOrderPolicy(DriverHelper_123pan.defaultOrderPolicy);
        };
    }
    @Contract(pure = true) static @NotNull String getOrderDirection(final @Nullable OrderDirection policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
//            default -> DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        };
    }

    static final @NotNull Pattern etagPattern = Pattern.compile("^[a-z0-9]{32}$");
    static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };
    static final long UploadPartSize = 16 << 20;

    static @NotNull Headers buildHeaders(final @Nullable String token) {
        final Headers.Builder builder = new Headers.Builder();
        if (token != null)
            builder.add("authorization", "Bearer " + token);
        builder.add("user-agent", "123pan/1.0.100" + DriverUtil.defaultUserAgent)
                .add("platform", "web").add("app-version", "3");
        return builder.build();
    }

    static @NotNull JSONObject extractResponseData(final @NotNull JSONObject json, final int successCode, final @NotNull String successMessage) throws WrongResponseException {
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != successCode || !successMessage.equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        return data;
    }

    // Token

    static @NotNull JSONObject doLogin(final int loginType, final @NotNull String passport, final @NotNull String password) throws IllegalParametersException, IOException {
        final Map<String, Object> requestBody = new LinkedHashMap<>(4);
        requestBody.put("type", loginType);
        requestBody.put(switch (loginType) {
            case 1 -> "passport";
            case 2 -> "mail";
            default -> throw new IllegalParametersException("Unknown login type.", loginType);
        }, passport);
        requestBody.put("password", password);
        requestBody.put("remember", false);
        final JSONObject json = DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.LoginURL, DriverHelper_123pan.buildHeaders(null), requestBody);
        return DriverHelper_123pan.extractResponseData(json, 200, "success");
    }

    static @NotNull JSONObject doRefresh(final @NotNull String token) throws IOException {
        final JSONObject json = DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.RefreshTokenURL, DriverHelper_123pan.buildHeaders(token), null);
        return DriverHelper_123pan.extractResponseData(json, 200, "success");
    }

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

    private static void login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final JSONObject data = DriverHelper_123pan.doLogin(configuration.getWebSide().getLoginPart().getLoginType(),
                configuration.getWebSide().getLoginPart().getPassport(),
                configuration.getWebSide().getLoginPart().getPassword());
        HLog.getInstance("DefaultLogger").log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
    }

    private static boolean refreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getCacheSide().getToken() == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final JSONObject data;
        try {
            data = DriverHelper_123pan.doRefresh(configuration.getCacheSide().getToken());
        } catch (final WrongResponseException exception) {
            if (exception.getMessage().startsWith("Code: 401"))
                return true; // throw new TokenExpiredException();
            throw exception;
        }
        HLog.getInstance("DefaultLogger").log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
        return false;
    } // true means failed.

    static @NotNull String ensureToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now().minusMinutes(3);
        if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getTokenExpireTime() == null
                || time.isAfter(configuration.getCacheSide().getTokenExpireTime()))
            if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getRefreshExpireTime() == null
                || time.isAfter(configuration.getCacheSide().getRefreshExpireTime())
                || DriverHelper_123pan.refreshToken(configuration))
                DriverHelper_123pan.login(configuration);
        return configuration.getCacheSide().getToken();
    }

    // Network

    static @NotNull JSONObject doGetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.UserInformationURL,
                DriverHelper_123pan.buildHeaders(token), null), 0, "ok");
    }

    static @NotNull JSONObject doListFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("driveId", 0);
        request.put("limit", limit);
        request.put("orderBy", DriverHelper_123pan.getOrderPolicy(configuration.getWebSide().getFilePart().getOrderPolicy()));
        request.put("orderDirection", DriverHelper_123pan.getOrderDirection(configuration.getWebSide().getFilePart().getOrderDirection()));
        request.put("parentFileId", directoryId);
        request.put("Page", page);
        request.put("trashed", false);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.ListFilesURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doGetFilesInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        final Collection<Object> list = new JSONArray(idList.size());
        for (final Long id: idList) {
            final Map<String, Object> pair = new JSONObject(1);
            pair.put("fileId", id.longValue());
            list.add(pair);
        }
        request.put("fileIdList", list);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.FilesInfoURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doGetFileDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull FileInformation info) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("driveId", 0);
        request.put("fileId", info.id());
        request.put("fileName", info.path().getName());
        request.put("size", info.size());
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeOther(info);
        request.put("s3keyFlag",extra.s3key());
        request.put("etag", info.tag());
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.SingleFileDownloadURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String name) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", name);
        request.put("parentFileId", parentId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(configuration.getWebSide().getFilePart().getDuplicatePolicy()));
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.UploadRequestURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doUploadRequest(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String name, final long size, final @NotNull String etag) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("driveId", 0);
        request.put("etag", etag);
        request.put("fileName", name);
        request.put("parentFileId", parentId);
        request.put("size", size);
        request.put("type", 0);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(configuration.getWebSide().getFilePart().getDuplicatePolicy()));
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.UploadRequestURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doUploadPare(final @NotNull DriverConfiguration_123Pan configuration,
                                            final @NotNull String bucket, final @NotNull String node, final @NotNull String key, final @NotNull String uploadId, final int partCount) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("bucket", bucket);
        request.put("StorageNode", node);
        request.put("key", key);
        request.put("uploadId", uploadId);
        request.put("partNumberStart", 1);
        request.put("partNumberEnd", partCount);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient,
                partCount <= 1 ? DriverHelper_123pan.S3AuthPartURL : DriverHelper_123pan.S3ParePartsURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doUploadComplete(final @NotNull DriverConfiguration_123Pan configuration,
                                                final @NotNull String bucket, final @NotNull String node, final @NotNull String key, final @NotNull String uploadId,
                                                final int partCount, final long size, final @NotNull Long fileId) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("bucket", bucket);
        request.put("StorageNode", node);
        request.put("key", key);
        request.put("uploadId", uploadId);
        request.put("isMultipart", partCount != 1);
        request.put("fileSize", size);
        request.put("fileId", fileId);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.UploadCompleteURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }

    static @NotNull JSONObject doTrashFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("driveId", 0);
        request.put("operation", true);
        final Collection<Object> list = new JSONArray(idList.size());
        for (final Long id: idList) {
            final Map<String, Object> pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            list.add(pair);
        }
        request.put("fileTrashInfoList", list);
        return DriverHelper_123pan.extractResponseData(DriverUtil.sendRequestReceiveJson(DriverUtil.httpClient, DriverHelper_123pan.TrashFileURL,
                DriverHelper_123pan.buildHeaders(token), request), 0, "ok");
    }
}
