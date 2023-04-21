package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import okhttp3.Headers;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .add("app-version", "101") // .add("app-version", "2")
                .add("platform", "pc") // .add("platform", "web")
                .build();
        final JSONObject json;
        if ("GET".equals(url.getSecond()) && request != null)
            json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequestWithParameters(url, headers, request)).bytes());
        else
            json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(url, headers, "GET".equals(url.getSecond()) ? null : DriverUtil.createJsonRequestBody(request))).bytes());
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

    // File Info Getter

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

    static @Nullable FileInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
        if (info != null)
            return info;
        final long id = DriverUtil_123pan.getFileId(configuration, path.getParent(), FileInformation::is_dir, true, _connection);
        int page = 1;
        List<FileInformation> list;
        do {
            list = DriverUtil_123pan.doListFiles(configuration, id, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, path, _connection).getSecond();
            for (final FileInformation obj: list)
                if (path.getName().equals(obj.path().getName()))
                    return obj;
        } while (!list.isEmpty());
        return null;
    }

    static void recursiveRefreshDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final Connection connection = DriverSqlHelper.requireConnection(_connection);
        try {
            if (_connection == null)
                connection.setAutoCommit(false);
            final long directoryId = DriverUtil_123pan.getFileId(configuration, directoryPath, FileInformation::is_dir, true, connection);
            DriverSqlHelper.deleteFileByParentPath(configuration.getLocalSide().getName(), directoryPath, connection);
            int page = 1;
            List<FileInformation> list;
            do {
                list = DriverUtil_123pan.doListFiles(configuration, directoryId, configuration.getWebSide().getFilePart().getDefaultLimitPerPage(), page++, directoryPath, connection).getSecond();
                for (final FileInformation info: list)
                    if (info.is_dir())
                        DriverUtil_123pan.recursiveRefreshDirectory(configuration, info.path(), connection);
            } while (!list.isEmpty());
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    // Files manager.
    
    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @Nullable DuplicatePolicy policy, final @Nullable Connection _connection) throws IllegalParametersException, IOException, SQLException {
        final String newDirectoryName = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new IllegalParametersException("Invalid directory name.", newDirectoryName);
        final DrivePath parentPath = path.getParent();
        final long parentDirectoryId = DriverUtil_123pan.getFileId(configuration, parentPath, FileInformation::is_dir, true, _connection);
        if (parentDirectoryId < 0)
            throw new IllegalParametersException("Parent directory is nonexistent.");
        final JSONObject request = new JSONObject(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", newDirectoryName);
        request.put("parentFileId", parentDirectoryId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
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
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeJson(info);
        request.put("s3keyFlag",extra.s3key());
        request.put("etag", extra.etag());
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


}
