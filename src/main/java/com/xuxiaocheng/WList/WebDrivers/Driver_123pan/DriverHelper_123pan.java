package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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

    // Token Refresher

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

    private static void doGetToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        // Quick response.
        if (configuration.getLocalSide().getStrictMode())
            switch (configuration.getWebSide().getLoginType()) {
                case 1 -> {
                    if (!DriverUtil.phoneNumberPattern.matcher(configuration.getWebSide().getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801");
                }
                case 2 -> {
                    if (!DriverUtil.mailAddressPattern.matcher(configuration.getWebSide().getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u548c\u5bc6\u7801");
                }
            }
        final JSONObject request = new JSONObject(4);
        request.put("type", configuration.getWebSide().getLoginType());
        request.put(switch (configuration.getWebSide().getLoginType()) {
                case 1 -> "passport";
                case 2 -> "mail";
                default -> throw new IllegalParametersException("Unknown login type.", configuration.getWebSide().getLoginType());
            }, configuration.getWebSide().getPassport());
        request.put("password", configuration.getWebSide().getPassword());
        request.put("remember", false);
        final JSONObject json = DriverUtil.sendJsonHttp(DriverHelper_123pan.LoginURL.getFirst(), DriverHelper_123pan.LoginURL.getSecond(), null, request);
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
            if (data.getIntValue("login_type", configuration.getWebSide().getLoginType() - 1) != configuration.getWebSide().getLoginType())
                throw new WrongResponseException("Abnormal data of 'login_type'.", data);
            if (data.getIntValue("refresh_token_expire_time", -1) < 0)
                throw new WrongResponseException("Abnormal data of 'refresh_token_expire_time'.", data);
            if (data.getString("token") == null)
                throw new WrongResponseException("Abnormal data of 'token'.", data);
        }
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
    }

    private static boolean doRefreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getCacheSide().getToken() == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final Map<String, String> property = new HashMap<>(1);
        property.put("authorization", "Bearer " + configuration.getCacheSide().getToken());
        final JSONObject json = DriverUtil.sendJsonHttp(DriverHelper_123pan.RefreshTokenURL.getFirst(), DriverHelper_123pan.RefreshTokenURL.getSecond(), property, null);
        if  (configuration.getLocalSide().getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response json items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 200 || !"success".equals(message)) {
            if (code == 401 && "token is expired".equals(message))
                return true; // throw new TokenExpiredException();
            throw new WrongResponseException(code, message);
        }
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
        return false;
    }

    static synchronized void doEnsureToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        final long time = System.currentTimeMillis();
        if (configuration.getCacheSide().getTokenExpire() < time || configuration.getCacheSide().getToken() == null) {
            if (configuration.getCacheSide().getRefreshExpire() >= time && configuration.getCacheSide().getToken() != null) {
                if (DriverHelper_123pan.doRefreshToken(configuration))
                    DriverHelper_123pan.doGetToken(configuration);
            } else
                DriverHelper_123pan.doGetToken(configuration);
        }
    }

    // Information Getter.

    private static @NotNull JSONObject sendJsonWithToken(final @NotNull Pair.ImmutablePair<String, String> url, final @NotNull DriverConfiguration_123Pan configuration, final @Nullable JSONObject request) throws IOException, IllegalParametersException {
        DriverHelper_123pan.doEnsureToken(configuration);
        final Map<String, String> property = new HashMap<>(3);
        property.put("authorization", "Bearer " + configuration.getCacheSide().getToken());
        // Version: 1.0.101 (2023-03)
        property.put("app-version", "101"); // property.put("app-version", "2");
        property.put("platform", "pc"); // property.put("platform", "web");
        JSONObject json = DriverUtil.sendJsonHttp(url.getFirst(), url.getSecond(), property, request);
        if  (configuration.getLocalSide().getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 0 || !"ok".equals(message)) {
            if (code == 401) {
                // Token Expired Exception.
                // Force: DriverHelper_123pan.doEnsureToken(configuration);
                if (configuration.getCacheSide().getRefreshExpire() >= System.currentTimeMillis() && configuration.getCacheSide().getToken() != null) {
                    if (DriverHelper_123pan.doRefreshToken(configuration))
                        DriverHelper_123pan.doGetToken(configuration);
                } else
                    DriverHelper_123pan.doGetToken(configuration);
                property.put("authorization", "Bearer " + configuration.getCacheSide().getToken());
                final JSONObject newJson = DriverUtil.sendJsonHttp(url.getFirst(), url.getSecond(), property, request);
                if  (configuration.getLocalSide().getStrictMode() && newJson.size() != 3)
                    throw new WrongResponseException("Abnormal count of response items.", newJson);
                final int newCode = newJson.getIntValue("code", -1);
                final String newMessage = newJson.getString("message");
                if (newCode != 0 || !"ok".equals(newMessage)) {
                    if (newCode == 401)
                        throw new IllegalParametersException("Failed to refresh token. Message: " + message);
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

    static long getDirectoryId(final @NotNull DrivePath path, final boolean useCache, final @NotNull String name) {
        if ("/".equals(path.getPath()))
            return 0;
        if (useCache) {
            return DriverHelper_123pan.getDirectoryId(path, false, name); // TODO use cache.
        }
        //TODO getDirectoryId
        return -1;
    }

    // Files manager.

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
    
    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull DrivePath path, final @NotNull String newDirectoryName, final @Nullable DuplicatePolicy policy) throws IOException, SQLException, IllegalParametersException {
        if (configuration.getLocalSide().getStrictMode() && !DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new WrongResponseException("Invalid directory name.", newDirectoryName);
        final long parentDirectoryId = DriverHelper_123pan.getDirectoryId(path, true, configuration.getLocalSide().getName());
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
            if (data.getLong("FileId") == null)
                throw new WrongResponseException("Abnormal data/info of 'FileId'.", info);
            if (!newDirectoryName.equals(data.getString("FileName")))
                throw new WrongResponseException("Abnormal data/info of 'FileName'.", info);
            if (data.getIntValue("Type", 0) != 1)
                throw new WrongResponseException("Abnormal data/info of 'Type'.", info);
            if (data.getIntValue("Size", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Size'.", info);
            if (data.getIntValue("ContentType", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'ContentType'.", info);
            if (data.getString("S3KeyFlag") == null)
                throw new WrongResponseException("Abnormal data/info of 'S3KeyFlag'.", info);
            if (data.getString("CreateAt") == null)
                throw new WrongResponseException("Abnormal data/info of 'CreateAt'.", info);
            if (data.getString("UpdateAt") == null)
                throw new WrongResponseException("Abnormal data/info of 'UpdateAt'.", info);
            if (data.getBooleanValue("Hidden", true))
                throw new WrongResponseException("Abnormal data/info of 'Hidden'.", info);
            if (!"".equals(data.getString("Etag")))
                throw new WrongResponseException("Abnormal data of 'Etag'.", data);
            if (data.getIntValue("Status", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Status'.", info);
            if (data.getLongValue("ParentFileId", parentDirectoryId - 1) != parentDirectoryId)
                throw new WrongResponseException("Abnormal data/info of 'ParentFileId'.", info);
            if (data.getIntValue("Category", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'Category'.", info);
            if (data.getIntValue("PunishFlag", 1) != 0)
                throw new WrongResponseException("Abnormal data/info of 'PunishFlag'.", info);
            if (!"".equals(data.getString("ParentName")))
                throw new WrongResponseException("Abnormal data of 'ParentName'.", data);
            if (!"".equals(data.getString("DownloadUrl")))
                throw new WrongResponseException("Abnormal data of 'DownloadUrl'.", data);
            if (data.getIntValue("AbnormalAlert", 0) != 1)
                throw new WrongResponseException("Abnormal data/info of 'AbnormalAlert'.", info);
            if (data.getBooleanValue("Trashed", true))
                throw new WrongResponseException("Abnormal data/info of 'Trashed'.", info);
            if (data.getString("TrashedExpire") == null)
                throw new WrongResponseException("Abnormal data/info of 'TrashedExpire'.", info);
            if (data.getString("TrashedAt") == null)
                throw new WrongResponseException("Abnormal data/info of 'TrashedAt'.", info);
            if (data.getString("StorageNode") == null)
                throw new WrongResponseException("Abnormal data/info of 'StorageNode'.", info);
        }
        DriverSQLHelper_123pan.insertFile(configuration.getLocalSide().getName(), path, info);
    }
}
