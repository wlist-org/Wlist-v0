package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import okhttp3.Headers;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    private static final @NotNull HLog logger = HLog.getInstance("DefaultLogger");

    private static final @NotNull Pattern tokenPattern = Pattern.compile("^([a-z]|[A-Z]|[0-9]){36}\\.([a-z]|[A-Z]|[0-9]){139}\\.([a-z]|[A-Z]|[0-9]|_|-){43}$");
    private static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch : s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };

    public static final @NotNull Pair.ImmutablePair<String, String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    public static final @NotNull Pair.ImmutablePair<String, String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    public static final @NotNull Pair.ImmutablePair<String, String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
    public static final @NotNull Pair.ImmutablePair<String, String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    public static final @NotNull Pair.ImmutablePair<String, String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");

    // "1970-01-01 08:00:00"
    static long parseServerTime(final @Nullable String time) throws IllegalParametersException {
        if (time == null)
            throw new IllegalParametersException(new NullPointerException("time"));
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(time).getTime();
        } catch (final ParseException exception) {
            throw new IllegalParametersException("Invalid time format.", time);
        }
    }

    // "0001-01-01T00:00:00+00:00"
    static long parseServerTimeWithZone(final @Nullable String time) throws IllegalParametersException {
        if (time == null)
            throw new IllegalParametersException(new NullPointerException("time"));
        final int index = time.lastIndexOf(':');
        if (index != 22)
            throw new IllegalParametersException("Invalid time format.", time);
        //noinspection SpellCheckingInspection
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final StringBuilder t = new StringBuilder(time);
        try {
            return dateFormat.parse(t.deleteCharAt(t.lastIndexOf(":")).toString()).getTime();
        } catch (final ParseException exception) {
            throw new IllegalParametersException("Invalid time format.", time);
        }
    }

    private static int getDuplicatePolicy(final @Nullable DuplicatePolicy policy) {
        final int defaultPolicy = 1;
        if (policy == null)
            return defaultPolicy;
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
            default -> defaultPolicy;
        };
    }

    private static @NotNull String getOrderPolicy(final @Nullable OrderPolicy policy) {
        final String defaultPolicy = "file_name";
        if (policy == null)
            return defaultPolicy;
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
//            default -> defaultPolicy;
        };
    }

    private static @NotNull String getOrderDirection(final @Nullable OrderDirection policy) {
        final String defaultPolicy = "asc";
        if (policy == null)
            return defaultPolicy;
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
//            default -> defaultPolicy;
        };
    }

    // Token Refresher

    private static @NotNull JSONObject extractLoginResponse(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull JSONObject json) throws WrongResponseException {
        if  (configuration.getLocalSide().getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response json items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 200 || !"success".equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        if (configuration.getLocalSide().getStrictMode()) {
            if (data.size() != 4)
                throw new WrongResponseException("Abnormal count of data items.", data);
            if (data.getString("expire") == null)
                throw new WrongResponseException("Abnormal data of 'expire'.", data);
            if (data.getIntValue("login_type", configuration.getWebSide().getLoginPart().getLoginType() - 1) != configuration.getWebSide().getLoginPart().getLoginType())
                throw new WrongResponseException("Abnormal data of 'login_type'.", data);
            if (data.getIntValue("refresh_token_expire_time", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'refresh_token_expire_time'.", data);
            if (data.getString("token") == null)
                throw new WrongResponseException("Abnormal data of 'token'.", data);
        }
        return data;
    }

    private static void handleLoginData(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull JSONObject data) throws WrongResponseException {
        // token
        final String token = data.getString("token");
        if (configuration.getLocalSide().getStrictMode() && !DriverHelper_123pan.tokenPattern.matcher(token).matches())
            throw new WrongResponseException("Invalid token format.", data);
        configuration.getCacheSide().setToken(token);
        // TODO: time zone?
        // token expire time
        try {
            configuration.getCacheSide().setTokenExpire(DriverHelper_123pan.parseServerTimeWithZone(data.getString("expire")));
        } catch (final IllegalParametersException exception) {
            throw new WrongResponseException("Invalid expire time.", exception);
        }
        // refresh token expire time
        configuration.getCacheSide().setRefreshExpire(data.getLongValue("refresh_token_expire_time", 0) * 1000);
    }

    private static void getToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        // Quick response.
        if (configuration.getLocalSide().getStrictMode())
            switch (configuration.getWebSide().getLoginPart().getLoginType()) {
                case 1 -> {
                    if (!DriverUtil.phoneNumberPattern.matcher(configuration.getWebSide().getLoginPart().getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801");
                }
                case 2 -> {
                    if (!DriverUtil.mailAddressPattern.matcher(configuration.getWebSide().getLoginPart().getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u548c\u5bc6\u7801");
                }
                default -> throw new IllegalParametersException("Unknown login type.", configuration.getWebSide().getLoginPart().getLoginType());
            }
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
        final JSONObject data = DriverHelper_123pan.extractLoginResponse(configuration, json);
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
    }

    private static boolean refreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getCacheSide().getToken() == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final JSONObject json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(DriverHelper_123pan.RefreshTokenURL,
                new Headers.Builder().add("authorization", "Bearer " + configuration.getCacheSide().getToken()).build(),
                RequestBody.create(null))).string());
        final JSONObject data;
        try {
            data = DriverHelper_123pan.extractLoginResponse(configuration, json);
        } catch (final WrongResponseException exception) {
            if (exception.getMessage().startsWith("Code: 401"))
                return true; // throw new TokenExpiredException();
            throw exception;
        }
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
        return false;
    }

    private static synchronized void forceRetrieveToken(final @NotNull DriverConfiguration_123Pan configuration, final long time) throws IOException, IllegalParametersException {
        if (configuration.getCacheSide().getRefreshExpire() >= time && configuration.getCacheSide().getToken() != null) {
            if (DriverHelper_123pan.refreshToken(configuration))
                DriverHelper_123pan.getToken(configuration);
        } else
            DriverHelper_123pan.getToken(configuration);
    }

    static void doRetrieveToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        final long time = System.currentTimeMillis();
        if (configuration.getCacheSide().getTokenExpire() < time || configuration.getCacheSide().getToken() == null)
            DriverHelper_123pan.forceRetrieveToken(configuration, time);
    }

    // Information Getter.

    //   User

    @SuppressWarnings("TypeMayBeWeakened")
    private static @NotNull JSONObject sendJsonWithToken(final @NotNull Pair.ImmutablePair<String, String> url, final @NotNull DriverConfiguration_123Pan configuration, final @Nullable JSONObject request) throws IOException, IllegalParametersException {
        DriverHelper_123pan.doRetrieveToken(configuration);
        final Headers headers = new Headers.Builder().add("authorization", "Bearer " + configuration.getCacheSide().getToken())
                .add("app-version", "101") // .add("app-version", "2")
                .add("platform", "pc") // .add("platform", "web")
                .build();
        JSONObject json;
        if ("GET".equals(url.getSecond()) && request != null)
            json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequestWithParameters(url, headers, request)).bytes());
        else
            json = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(url, headers, "GET".equals(url.getSecond()) ? null : DriverUtil.createJsonRequestBody(request))).bytes());
        if  (configuration.getLocalSide().getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 0 || !"ok".equals(message)) {
            if (code == 401) {
                // Token Expired Exception.
                DriverHelper_123pan.forceRetrieveToken(configuration, System.currentTimeMillis());
                final JSONObject newJson;
                if ("GET".equals(url.getSecond()) && request != null)
                    newJson = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequestWithParameters(url, headers, request)).bytes());
                else
                    newJson = JSON.parseObject(DriverUtil.checkResponseSuccessful(DriverUtil.sendRequest(url, headers, "GET".equals(url.getSecond()) ? null : DriverUtil.createJsonRequestBody(request))).bytes());
                if (configuration.getLocalSide().getStrictMode() && newJson.size() != 3)
                    throw new WrongResponseException("Abnormal count of response items.", newJson);
                final int newCode = newJson.getIntValue("code", -1);
                final String newMessage = newJson.getString("message");
                if (newCode != 0 || !"ok".equals(newMessage)) {
                    if (newCode == 401)
                        throw new IllegalParametersException("Failed to refresh token. Message: " + newMessage);
                    throw new WrongResponseException(newCode, newMessage);
                }
                json = newJson;
            } else
                throw new WrongResponseException(code, message);
        }
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        return data;
    }

    static void doGetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        final JSONObject data = DriverHelper_123pan.sendJsonWithToken(DriverHelper_123pan.UserInformationURL, configuration, null);
        if (configuration.getLocalSide().getStrictMode()) {
            if (data.size() != 22)
                throw new WrongResponseException("Abnormal count of data items.", data);
            if (data.getLong("UID") == null)
                throw new WrongResponseException("Abnormal data of 'UID'.", data);
            if (data.getString("Nickname") == null)
                throw new WrongResponseException("Abnormal data of 'Nickname'.", data);
            if (data.getLongValue("SpaceUsed", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'SpaceUsed'.", data);
            if (data.getLongValue("SpacePermanent", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'SpacePermanent'.", data);
            if (data.getLongValue("SpaceTemp", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'SpaceTemp'.", data);
            if (data.getLongValue("FileCount", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'FileCount'.", data);
            if (data.getString("SpaceTempExpr") == null) // With zone
                throw new WrongResponseException("Abnormal data of 'SpaceTempExpr'.", data);
            if (data.getString("Mail") == null)
                throw new WrongResponseException("Abnormal data of 'Mail'.", data);
            if (data.getString("Passport") == null)
                throw new WrongResponseException("Abnormal data of 'Passport'.", data);
            if (data.getString("HeadImage") == null)
                throw new WrongResponseException("Abnormal data of 'HeadImage'.", data);
            if (data.getBoolean("BindWechat") == null)
                throw new WrongResponseException("Abnormal data of 'BindWechat'.", data);
            if (data.getBoolean("StraightLink") == null)
                throw new WrongResponseException("Abnormal data of 'StraightLink'.", data);
            if (data.getInteger("OpenLink") == null)
                throw new WrongResponseException("Abnormal data of 'OpenLink'.", data);
            if (data.getBoolean("Vip") == null)
                throw new WrongResponseException("Abnormal data of 'Vip'.", data);
            if (data.getString("VipExpire") == null) // Without zone
                throw new WrongResponseException("Abnormal data of 'VipExpire'.", data);
            if (data.getBoolean("SpaceBuy") == null)
                throw new WrongResponseException("Abnormal data of 'SpaceBuy'.", data);
            if (data.getString("VipExplain") == null)
                throw new WrongResponseException("Abnormal data of 'VipExplain'.", data);
            if (data.getInteger("SignType") == null)
                throw new WrongResponseException("Abnormal data of 'SignType'.", data);
            if (data.getBoolean("ContinuousPayment") == null)
                throw new WrongResponseException("Abnormal data of 'ContinuousPayment'.", data);
            if (data.getString("ContinuousPaymentDate") == null)
                throw new WrongResponseException("Abnormal data of 'ContinuousPaymentDate'.", data);
            if (data.getLong("ContinuousPaymentAmount") == null)
                throw new WrongResponseException("Abnormal data of 'ContinuousPaymentAmount'.", data);
            if (data.getLong("ContinuousPaymentDuration") == null)
                throw new WrongResponseException("Abnormal data of 'ContinuousPaymentDuration'.", data);
        }
        configuration.getCacheSide().setNickname(data.getString("Nickname"));
        configuration.getCacheSide().setImageLink(data.getString("HeadImage"));
        configuration.getCacheSide().setVip(data.getBooleanValue("Vip", false));
        configuration.getCacheSide().setSpaceAll(data.getLongValue("SpacePermanent", 0) + data.getLongValue("SpaceTemp", 0));
        configuration.getCacheSide().setSpaceUsed(data.getLongValue("SpaceUsed", 0));
        configuration.getCacheSide().setFileCount(data.getLongValue("FileCount", 0));
    }

    //   File

    private static void strictCheckForFileInfo(final @NotNull JSONObject info) throws WrongResponseException {
        if (info.getLong("FileId") == null)
            throw new WrongResponseException("Abnormal data/info of 'FileId'.", info);
        if (info.getString("FileName") == null)
            throw new WrongResponseException("Abnormal data/info of 'FileName'.", info);
        if (info.getInteger("Type") == null)
            throw new WrongResponseException("Abnormal data/info of 'Type'.", info);
        if (info.getLongValue("Size", -1) < 0)
            throw new WrongResponseException("Abnormal data/info of 'Size'.", info);
        if (info.getIntValue("ContentType", 1) != 0)
            throw new WrongResponseException("Abnormal data/info of 'ContentType'.", info);
        if (info.getString("S3KeyFlag") == null)
            throw new WrongResponseException("Abnormal data/info of 'S3KeyFlag'.", info);
        if (info.getString("CreateAt") == null)
            throw new WrongResponseException("Abnormal data/info of 'CreateAt'.", info);
        if (info.getString("UpdateAt") == null)
            throw new WrongResponseException("Abnormal data/info of 'UpdateAt'.", info);
        if (info.getBooleanValue("Hidden", true))
            throw new WrongResponseException("Abnormal data/info of 'Hidden'.", info);
        if (info.getString("Etag") == null)
            throw new WrongResponseException("Abnormal data of 'Etag'.", info);
        if (info.getInteger("Status") == null)
            throw new WrongResponseException("Abnormal data/info of 'Status'.", info);
        if (info.getLong("ParentFileId") == null)
            throw new WrongResponseException("Abnormal data/info of 'ParentFileId'.", info);
        if (info.getInteger("Category") == null)
            throw new WrongResponseException("Abnormal data/info of 'Category'.", info);
        if (info.getIntValue("PunishFlag", 1) != 0)
            throw new WrongResponseException("Abnormal data/info of 'PunishFlag'.", info);
        if (!"".equals(info.getString("ParentName")))
            throw new WrongResponseException("Abnormal data of 'ParentName'.", info);
        if (info.getString("DownloadUrl") == null)
            throw new WrongResponseException("Abnormal data of 'DownloadUrl'.", info);
        if (info.getInteger("AbnormalAlert") == null)
            throw new WrongResponseException("Abnormal data/info of 'AbnormalAlert'.", info);
        if (info.getBooleanValue("Trashed", true))
            throw new WrongResponseException("Abnormal data/info of 'Trashed'.", info);
        if (info.getString("TrashedExpire") == null)
            throw new WrongResponseException("Abnormal data/info of 'TrashedExpire'.", info);
        if (info.getString("TrashedAt") == null)
            throw new WrongResponseException("Abnormal data/info of 'TrashedAt'.", info);
        if (info.getString("StorageNode") == null)
            throw new WrongResponseException("Abnormal data/info of 'StorageNode'.", info);
    }

    private static @NotNull Pair<Integer, JSONArray> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final long id, final int page) throws IOException, IllegalParametersException {
        final JSONObject request = new JSONObject(7);
        request.put("driveId", 0);
        request.put("limit", configuration.getWebSide().getFilePart().getDefaultLimitPerPage());
        request.put("orderBy", DriverHelper_123pan.getOrderPolicy(configuration.getWebSide().getFilePart().getOrderPolicy()));
        request.put("orderDirection", DriverHelper_123pan.getOrderDirection(configuration.getWebSide().getFilePart().getOrderDirection()));
        request.put("parentFileId", id);
        request.put("Page", page);
        request.put("trashed", false);
        final JSONObject data = DriverHelper_123pan.sendJsonWithToken(DriverHelper_123pan.ListFilesURL, configuration, request);
        if (configuration.getLocalSide().getStrictMode()) {
            if (data.size() != 5)
                throw new WrongResponseException("Abnormal count of data items.", data);
            if (data.getString("Next") == null)
                throw new WrongResponseException("Abnormal data of 'Next'.", data);
            if (data.getIntValue("Len", 0) > configuration.getWebSide().getFilePart().getDefaultLimitPerPage())
                throw new WrongResponseException("Abnormal data of 'Len'.", data);
            if (data.getBooleanValue("IsFirst", page != 1) != (page == 1))
                throw new WrongResponseException("Abnormal data of 'IsFirst'.", data);
            if (data.getIntValue("Total", -1) < data.getIntValue("Len", 0))
                throw new WrongResponseException("Abnormal data of 'Total'.", data);
        }
        final JSONArray info = data.getJSONArray("InfoList");
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'InfoList'.", data);
        if (configuration.getLocalSide().getStrictMode()) {
            if (info.size() != data.getIntValue("Len"))
                throw new WrongResponseException("Abnormal count of data/InfoList items.", data);
            for (int i = 0; i < info.size(); ++i) {
                final JSONObject obj = info.getJSONObject(i);
                if (obj.size() != 24)
                    throw new WrongResponseException("Abnormal count of file info items. index: " + i, data);
                DriverHelper_123pan.strictCheckForFileInfo(obj);
                if (obj.getLongValue("ParentFileId") != id)
                    throw new WrongResponseException("Abnormal file info of 'ParentFileId'. index: " + i, data);
                if (obj.getInteger("EnableAppeal") == null)
                    throw new WrongResponseException("Abnormal file info of 'EnableAppeal'. index: " + i, data);
                if (obj.getString("ToolTip") == null)
                    throw new WrongResponseException("Abnormal file info of 'ToolTip'. index: " + i, data);
                if (obj.getInteger("RefuseReason") == null)
                    throw new WrongResponseException("Abnormal file info of 'RefuseReason'. index: " + i, data);
            }
        }
        return Pair.makePair(data.getIntValue("Total", 0), info);
    }

    public static long getDirectoryId(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final boolean dir, final boolean useCache) throws IOException, IllegalParametersException, SQLException {
        if (path.getDepth() == 0)
            return 0;
        if (useCache) {
            final DriverSQLHelper_123pan.FileInformation info = DriverSQLHelper_123pan.getFile(configuration.getLocalSide().getName(), path);
            if (info != null) {
                if (dir && info.is_dir() != 1)
                    return -1;
                else
                    return info.id();
            }
            return DriverHelper_123pan.getDirectoryId(configuration, path, dir, false);
        }
        final String name = path.getName();
        final long parentId = DriverHelper_123pan.getDirectoryId(configuration, path.parent(), false, false);
        try {
            if (parentId < 0)
                return -1;
            JSONArray list = DriverHelper_123pan.listFiles(configuration, parentId, 1).getSecond();
            for (int page = 2; !list.isEmpty(); ++page) {
                long id = -2;
                for (int i = 0; i < list.size(); ++i) {
                    final JSONObject info = list.getJSONObject(i);
                    DriverSQLHelper_123pan.updateFile(configuration.getLocalSide().getName(), path, info);
                    if (id == -2 && name.equals(info.getString("FileName"))) {
                        if (dir && info.getIntValue("Type") != 1)
                            id = -1;
                        else
                            id = info.getLongValue("FileId");
                    }
                }
                if (id != -2)
                    return id;
                list = DriverHelper_123pan.listFiles(configuration, parentId, page).getSecond();
            }
            return -1;
        } finally {
            path.child(name);
        }
    }

    // Files manager.
    
    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String newDirectoryName, final @Nullable DuplicatePolicy policy) throws IOException, SQLException, IllegalParametersException {
        if (configuration.getLocalSide().getStrictMode() && !DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new WrongResponseException("Invalid directory name.", newDirectoryName);
        final long parentDirectoryId = DriverHelper_123pan.getDirectoryId(configuration, path, true, true);
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
        final JSONObject data = DriverHelper_123pan.sendJsonWithToken(DriverHelper_123pan.UploadRequestURL, configuration, request);
        if (configuration.getLocalSide().getStrictMode()) {
            if (data.size() != 13)
                throw new WrongResponseException("Abnormal count of data items.", data);
            if (data.get("AccessKeyId") != null)
                throw new WrongResponseException("Abnormal data of 'AccessKeyId'.", data);
            if (data.get("SecretAccessKey") != null)
                throw new WrongResponseException("Abnormal data of 'SecretAccessKey'.", data);
            if (data.get("SessionToken") != null)
                throw new WrongResponseException("Abnormal data of 'SessionToken'.", data);
            if (data.get("Expiration") != null)
                throw new WrongResponseException("Abnormal data of 'Expiration'.", data);
            if (data.getLong("FileId") == null)
                throw new WrongResponseException("Abnormal data of 'FileId'.", data);
            if (!"".equals(data.getString("Key")))
                throw new WrongResponseException("Abnormal data of 'Key'.", data);
            if (!"".equals(data.getString("Bucket")))
                throw new WrongResponseException("Abnormal data of 'Bucket'.", data);
            if (data.getBooleanValue("Reuse", true))
                throw new WrongResponseException("Abnormal data of 'Reuse'.", data);
            if (!"".equals(data.getString("UploadId")))
                throw new WrongResponseException("Abnormal data of 'UploadId'.", data);
            if (!"".equals(data.getString("StorageNode")))
                throw new WrongResponseException("Abnormal data of 'StorageNode'.", data);
            if (!"".equals(data.getString("EndPoint")))
                throw new WrongResponseException("Abnormal data of 'EndPoint'.", data);
        }
        final JSONObject info = data.getJSONObject("Info");
        if (info == null)
            throw new WrongResponseException("Abnormal data of 'Info'.", data);
        if (configuration.getLocalSide().getStrictMode()) {
            if (info.size() != 21)
                throw new WrongResponseException("Abnormal count of data/info items.", info);
            DriverHelper_123pan.strictCheckForFileInfo(info);
            if (!newDirectoryName.equals(info.getString("FileName")))
                throw new WrongResponseException("Abnormal data/info of 'FileName'.", info);
            if (info.getIntValue("Type", 0) != 1)
                throw new WrongResponseException("Abnormal data/info of 'Type'.", info);
            if (info.getIntValue("Size", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Size'.", info);
            if (!"".equals(info.getString("Etag")))
                throw new WrongResponseException("Abnormal data of 'Etag'.", info);
            if (info.getIntValue("Status", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Status'.", info);
            if (info.getLongValue("ParentFileId", parentDirectoryId - 1) != parentDirectoryId)
                throw new WrongResponseException("Abnormal data/info of 'ParentFileId'.", info);
            if (info.getIntValue("Category", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Category'.", info);
            if (!"".equals(info.getString("DownloadUrl")))
                throw new WrongResponseException("Abnormal data of 'DownloadUrl'.", info);
        }
        DriverSQLHelper_123pan.insertFile(configuration.getLocalSide().getName(), path, info);
    }
}
