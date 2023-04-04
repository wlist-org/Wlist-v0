package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
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
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };

    public static final @NotNull String LoginURL = "https://www.123pan.com/api/user/sign_in";
    public static final @NotNull String RefreshTokenURL = "https://www.123pan.com/api/user/refresh_token";
    public static final @NotNull String UploadRequestURL = "https://www.123pan.com/api/file/upload_request";

    static long parseServerTime(final @NotNull String time) throws IllegalParametersException {
        //noinspection SpellCheckingInspection
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final StringBuilder t = new StringBuilder(time);
        try {
            return dateFormat.parse(t.deleteCharAt(t.lastIndexOf(":")).toString()).getTime();
        } catch (final ParseException exception) {
            throw new IllegalParametersException("Invalid time format.", time);
        }
    }

    private static @NotNull JSONObject postJsonWithToken(final @NotNull String url, final @Nullable String token, final @Nullable JSONObject request, final boolean strictMode) throws IOException {
        // Quick response
        if (token == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final Map<String, String> property = new HashMap<>(3);
        property.put("authorization", "Bearer " + token);
        property.put("app-version", "101"); // Version: 1.0.101 (2023-03)
        property.put("platform", "pc");
        final JSONObject json = DriverUtil.sendJsonHttp(url, "POST", property, request);
        if  (strictMode && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 0 || !"ok".equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        return data;
    }

    private static void handleLoginData(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull JSONObject data) throws WrongResponseException {
        // token
        final String token = data.getString("token");
        if (token == null)
            throw new WrongResponseException("No token response.");
        if (configuration.getStrictMode() && !DriverHelper_123pan.tokenPattern.matcher(token).matches())
            throw new WrongResponseException("Invalid token format.", data);
        configuration.setToken(token);
        // token expire time
        try {
            configuration.setTokenExpire(DriverHelper_123pan.parseServerTime(data.getString("expire")));
        } catch (final IllegalParametersException exception) {
            throw new WrongResponseException("Invalid expire time.", exception);
        }
        // refresh token expire time
        configuration.setRefreshExpire(data.getLongValue("refresh_token_expire_time") * 1000);
    }

    static void doGetToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException {
        // Quick response.
        if (configuration.getStrictMode())
            switch (configuration.getLoginType()) {
                case 1 -> {
                    if (!DriverUtil.phoneNumberPattern.matcher(configuration.getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801");
                }
                case 2 -> {
                    if (!DriverUtil.mailAddressPattern.matcher(configuration.getPassport()).matches())
                        //noinspection UnnecessaryUnicodeEscape
                        throw new WrongResponseException(401, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u548c\u5bc6\u7801");
                }
            }
        final JSONObject request = new JSONObject(4);
        request.put("type", configuration.getLoginType());
        request.put(switch (configuration.getLoginType()) {
                case 1 -> "passport";
                case 2 -> "mail";
                default -> throw new IllegalParametersException("Unknown login type.", configuration.getLoginType());
            }, configuration.getPassport());
        request.put("password", configuration.getPassword());
        request.put("remember", false);
        final JSONObject json = DriverUtil.sendJsonHttp(DriverHelper_123pan.LoginURL, "POST", null, request);
        if  (configuration.getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 200 || !"success".equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        if (configuration.getStrictMode() && data.size() != 5)
            throw new WrongResponseException("Abnormal count of data items.", data);
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
    }

    static void doRefreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getToken() == null)
            throw new WrongResponseException(401, "token contains an invalid number of segments");
        final Map<String, String> property = new HashMap<>(1);
        property.put("authorization", "Bearer " + configuration.getToken());
        final JSONObject json = DriverUtil.sendJsonHttp(DriverHelper_123pan.RefreshTokenURL, "POST", property, null);
        if  (configuration.getStrictMode() && json.size() != 3)
            throw new WrongResponseException("Abnormal count of response items.", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 200 || !"success".equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        DriverHelper_123pan.logger.log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverHelper_123pan.handleLoginData(configuration, data);
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
    
    static void doCreateDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long parentDirectoryId, final @NotNull String newDirectoryName, final @Nullable DuplicatePolicy policy) throws IOException {
        if (configuration.getStrictMode() && !DriverHelper_123pan.filenamePredication.test(newDirectoryName))
            throw new WrongResponseException("Invalid directory name.", newDirectoryName);
        final JSONObject request = new JSONObject(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", newDirectoryName);
        request.put("parentFileId", parentDirectoryId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        final JSONObject data = DriverHelper_123pan.postJsonWithToken(DriverHelper_123pan.UploadRequestURL, configuration.getToken(), request, configuration.getStrictMode());
        // TODO: do cache.
    }

    public static long getDirectoryId(final @NotNull DrivePath path, final boolean useCache, final @NotNull Driver_123Pan driver) {
        if ("/".equals(path.getPath()))
            return 0;
        if (useCache) {
            return DriverHelper_123pan.getDirectoryId(path, false, driver); // TODO use cache.
        }
        //TODO getDirectoryId
        return -1;
    }
}
