package com.xuxiaocheng.WList.Drivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Drivers.HttpUtil;
import com.xuxiaocheng.WList.Internal.Drives.DrivePath;
import com.xuxiaocheng.WList.Internal.Drives.DuplicatePolicy;
import com.xuxiaocheng.WList.Internal.Drives.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.WList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DriverUtil_123pan {
    private DriverUtil_123pan() {
        super();
    }

    private static final @NotNull HLog logger = HLog.getInstance("DefaultLogger");
    private static final @NotNull Pattern tokenPattern = Pattern.compile("^([a-z]|[A-Z]|[0-9]){36}\\.([a-z]|[A-Z]|[0-9]){139}\\.([a-z]|[A-Z]|[0-9]|_|-){43}$");

    public static final @NotNull String LoginURL = "https://www.123pan.com/api/user/sign_in";
    public static final @NotNull String RefreshTokenURL = "https://www.123pan.com/api/user/refresh_token";
    public static final @NotNull String UploadRequestURL = "https://www.123pan.com/api/file/upload_request";

    private static @NotNull JSONObject postJsonWithToken(final @NotNull String url, final @NotNull String token, final @Nullable JSONObject request) throws IOException {
        final JSONObject requests = Objects.requireNonNullElseGet(request, () -> new JSONObject(1));
        requests.put("authorization", "Bearer " + token);
        final JSONObject json = HttpUtil.sendJsonHttp(url, "POST", null, requests);
        if (WList.StrictMode && json.size() != 3)
            throw new WrongResponseException("Response format error (size!=3).", json);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 0 || !"ok".equals(message))
            throw new WrongResponseException(code, message);
        if (WList.StrictMode && json.get("data") == null)
            throw new WrongResponseException("Response format error (data==null).", json);
        return json.getJSONObject("data");
    }

    private static void handleLoginData(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull JSONObject data) throws WrongResponseException {
        // token
        {
            final String token = data.getString("token");
            if (token == null)
                throw new WrongResponseException("No token response.");
            if (WList.StrictMode && !DriverUtil_123pan.tokenPattern.matcher(token).matches())
                throw new WrongResponseException("Response data format error (token doesn't match).", data);
            configuration.setToken(token);
        }
        // token expire time
        {
            //noinspection SpellCheckingInspection
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            final StringBuilder t = new StringBuilder(data.getString("expire"));
            try {
                configuration.setTokenExpire(dateFormat.parse(t.deleteCharAt(t.lastIndexOf(":")).toString()).getTime());
            } catch (final ParseException exception) {
                throw new WrongResponseException("Invalid expire time.", exception);
            }
        }
        // refresh token expire time
        {
            configuration.setRefreshExpire(data.getLongValue("refresh_token_expire_time") * 1000);
        }
    }

    static void doGetToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (WList.StrictMode) {
            if (configuration.getPassport().contains("@")) {
                if (!HttpUtil.mailAddressPattern.matcher(configuration.getPassport()).matches())
                    throw new WrongResponseException(401, "请输入正确的邮箱号");
            } else {
                if (!HttpUtil.phoneNumberPattern.matcher(configuration.getPassport()).matches())
                    throw new WrongResponseException(401, "请输入正确的手机号码");
            }
        }
        final JSONObject request = new JSONObject(4);
        request.put("passport", configuration.getPassport());
        request.put("password", configuration.getPassword());
        request.put("remember", false);
        request.put("type", configuration.getPassport().contains("@") ? 2 : 1);
        final JSONObject json = HttpUtil.sendJsonHttp(DriverUtil_123pan.LoginURL, "POST", null, request);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 200 || !"success".equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        DriverUtil_123pan.logger.log(HLogLevel.DEBUG, "Logged in: ", data);
        DriverUtil_123pan.handleLoginData(configuration, data);
    }

    static void doRefreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        // Quick response.
        if (configuration.getToken() == null)
            throw new WrongResponseException(401, "cookie token is empty");
        final JSONObject data = DriverUtil_123pan.postJsonWithToken(DriverUtil_123pan.RefreshTokenURL, configuration.getToken(), null);
        DriverUtil_123pan.logger.log(HLogLevel.DEBUG, "Refreshed token: ", data);
        DriverUtil_123pan.handleLoginData(configuration, data);
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private static int getDuplicatePolicy(final @Nullable DuplicatePolicy policy) {
        if (policy == null)
            return 1;
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
            default -> 1;
        };
    }
    
    static void doCreateDirectory(final @NotNull String token, final long parentDirectoryId, final @NotNull String name, final @NotNull DuplicatePolicy policy) throws IOException {
        final JSONObject request = new JSONObject(8);
        request.put("driveId", 0);
        request.put("etag", "");
        request.put("fileName", name);
        request.put("parentFileId", parentDirectoryId);
        request.put("size", 0);
        request.put("type", 1);
        request.put("NotReuse", true);
        request.put("duplicate", DriverUtil_123pan.getDuplicatePolicy(policy));
        final JSONObject data = DriverUtil_123pan.postJsonWithToken(DriverUtil_123pan.UploadRequestURL, token, request);
        // TODO: do cache.
    }

    public static long getDirectoryId(final @NotNull DrivePath path, final boolean useCache, final @NotNull Driver_123Pan driver) {
        if ("/".equals(path.getPath()))
            return 0;
        if (useCache) {
            return DriverUtil_123pan.getDirectoryId(path, false, driver); // TODO use cache.
        }
        //TODO getDirectoryId
        return -1;
    }
}
