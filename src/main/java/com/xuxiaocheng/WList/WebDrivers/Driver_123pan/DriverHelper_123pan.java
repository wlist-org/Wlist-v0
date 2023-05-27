package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import okhttp3.Headers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    static final Pair.@NotNull ImmutablePair<String, String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
//    static final Pair.@NotNull ImmutablePair<String, String> TokenDelayURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/token_delay", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    static final Pair.@NotNull ImmutablePair<String, String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    static final Pair.@NotNull ImmutablePair<String, String> FilesInfoURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/info", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> SingleFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/download_info", "POST");
//    static final Pair.@NotNull ImmutablePair<String, String> BatchFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/batch_download_info", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> S3AuthPartURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_upload_object/auth", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> S3ParePartsURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_repare_upload_parts_batch", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> UploadCompleteURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_complete/v2", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> TrashFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/trash", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> RenameFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/rename", "POST");
    static final Pair.@NotNull ImmutablePair<String, String> MoveFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/mod_pid", "POST");

    static final int TokenExpireResponseCode = 401;
    static final int NoSuchFileResponseCode = 400;

    private static final Options.@NotNull DuplicatePolicy defaultDuplicatePolicy = Options.DuplicatePolicy.KEEP;
    private static final Options.@NotNull OrderPolicy defaultOrderPolicy = Options.OrderPolicy.FileName;
    private static final Options.@NotNull OrderDirection defaultOrderDirection = Options.OrderDirection.ASCEND;
    @Contract(pure = true) static int getDuplicatePolicy(final Options.@Nullable DuplicatePolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
        };
    }
    @Contract(pure = true) static @NotNull String getOrderPolicy(final Options.@Nullable OrderPolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderPolicy(DriverHelper_123pan.defaultOrderPolicy);
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
        };
    }
    @Contract(pure = true) static @NotNull String getOrderDirection(final Options.@Nullable OrderDirection policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
//            default -> DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        };
    }

    static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };

    static final int UploadPartSize = 16 << 20;

    static @NotNull Headers.Builder headerBuilder(final @Nullable String token) {
        final Headers.Builder builder = new Headers.Builder();
        if (token != null)
            builder.add("authorization", "Bearer " + token);
        builder.add("user-agent", DriverNetworkHelper.defaultAgent);
//        builder.add("user-agent", "123pan/1.0.100");
        builder.add("platform", "web").add("app-version", "3");
        return builder;
    }

    static @NotNull JSONObject extractResponseData(final @NotNull JSONObject json, final int successCode, final @NotNull String successMessage) throws WrongResponseException {
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != successCode || !successMessage.equals(message))
            throw new IllegalResponseCodeException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        return data;
    }

    static @NotNull String extractDownloadUrl(final @NotNull String url) throws IOException {
        assert url.contains("params=");
        final int pIndex = url.indexOf("params=") + "params=".length();
        final int aIndex = url.indexOf('&', pIndex);
        final String base64 = url.substring(pIndex, aIndex < 0 ? url.length() : aIndex);
        final String decodedUrl = new String(Base64.getDecoder().decode(base64));
        final JSONObject json = DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, Pair.ImmutablePair.makeImmutablePair(decodedUrl, "GET"), null, null);
        final JSONObject data = DriverHelper_123pan.extractResponseData(json, 0, "ok");
        final String redirectUrl = data.getString("redirect_url");
        if (redirectUrl == null)
            throw new WrongResponseException("Abnormal data of 'redirect_url'.", data);
        return redirectUrl;
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
        final JSONObject json = DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.LoginURL, DriverHelper_123pan.headerBuilder(null).build(), requestBody);
        return DriverHelper_123pan.extractResponseData(json, 200, "success");
    }

    static @NotNull JSONObject doRefresh(final @NotNull String token) throws IOException {
        final JSONObject json = DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.RefreshTokenURL, DriverHelper_123pan.headerBuilder(token).build(), null);
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
        configuration.getCacheSide().setTokenExpire(LocalDateTime.parse(expire, DateTimeFormatter.ISO_ZONED_DATE_TIME));
        final Long refresh = data.getLong("refresh_token_expire_time");
        if (refresh == null)
            throw new WrongResponseException("No refresh time in response.");
        configuration.getCacheSide().setRefreshExpire(LocalDateTime.ofEpochSecond(refresh.longValue(), 0, ZoneOffset.ofHours(8)));
    }

    private static void login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final JSONObject data = DriverHelper_123pan.doLogin(configuration.getWebSide().getLoginType(),
                configuration.getWebSide().getPassport(),
                configuration.getWebSide().getPassword());
        HLog.getInstance("DefaultLogger").log(HLogLevel.LESS, "Logged in: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
    }

    private static boolean refreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getCacheSide().getToken() == null)
            return true;
        final JSONObject data;
        try {
            data = DriverHelper_123pan.doRefresh(configuration.getCacheSide().getToken());
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.TokenExpireResponseCode)
                return true; // throw new TokenExpiredException();
            throw exception;
        }
        HLog.getInstance("DefaultLogger").log(HLogLevel.LESS, "Refreshed token: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
        return false;
    } // true means failed.

    static @NotNull String ensureToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now().minusMinutes(3);
        if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getTokenExpire() == null
                || time.isAfter(configuration.getCacheSide().getTokenExpire()))
            if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getRefreshExpire() == null
                || time.isAfter(configuration.getCacheSide().getRefreshExpire())
                || DriverHelper_123pan.refreshToken(configuration))
                DriverHelper_123pan.login(configuration);
        return configuration.getCacheSide().getToken();
    }

    // Network

    static @NotNull JSONObject doGetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.UserInformationURL,
                DriverHelper_123pan.headerBuilder(token).build(), null), 0, "ok");
    }

    static @NotNull JSONObject doListFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("driveId", 0);
        request.put("limit", limit);
        request.put("orderBy", DriverHelper_123pan.getOrderPolicy(policy));
        request.put("orderDirection", DriverHelper_123pan.getOrderDirection(direction));
        request.put("parentFileId", directoryId);
        request.put("Page", page);
        request.put("trashed", false);
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.ListFilesURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doGetFilesInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        request.put("fileIdList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("fileId", id.longValue());
            return pair;
        }).toList());
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.FilesInfoURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doGetFileDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull FileSqlInformation info) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("driveId", 0);
        request.put("fileId", info.id());
        request.put("fileName", info.path().getName());
        request.put("size", info.size());
        request.put("etag", info.md5());
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeOther(info);
        request.put("s3keyFlag",extra.s3key());
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.SingleFileDownloadURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", name);
        request.put("parentFileId", parentId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.UploadRequestURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doUploadRequest(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String name, final long size, final @NotNull String etag, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("driveId", 0);
        request.put("etag", etag);
        request.put("fileName", name);
        request.put("parentFileId", parentId);
        request.put("size", size);
        request.put("type", 0);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.UploadRequestURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
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
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient,
                partCount <= 1 ? DriverHelper_123pan.S3AuthPartURL : DriverHelper_123pan.S3ParePartsURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
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
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.UploadCompleteURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doTrashFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("driveId", 0);
        request.put("operation", true);
        request.put("fileTrashInfoList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        }).toList());
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.TrashFileURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doRenameFile(final @NotNull DriverConfiguration_123Pan configuration, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(4);
        request.put("driveId", 0);
        request.put("fileId", id);
        request.put("fileName", name);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.RenameFileURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }

    static @NotNull JSONObject doMoveFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList, final long parentId) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(2);
        request.put("parentFileId", parentId);
        request.put("fileIdList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        }).toList());
        return DriverHelper_123pan.extractResponseData(DriverNetworkHelper.sendRequestReceiveJson(DriverNetworkHelper.httpClient, DriverHelper_123pan.MoveFilesURL,
                DriverHelper_123pan.headerBuilder(token).build(), request), 0, "ok");
    }
}
