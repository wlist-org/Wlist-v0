package com.xuxiaocheng.WList.Drivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Drivers.HttpUtil;
import com.xuxiaocheng.WList.Internal.Drives.DrivePath;
import com.xuxiaocheng.WList.Internal.Drives.Exceptions.WrongResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DriverUtil_123pan {
    private DriverUtil_123pan() {
        super();
    }

    public static final @NotNull URL LoginURL;
    public static final @NotNull URL RefreshTokenURL;

    static {
        try {
            LoginURL = new URL("https://www.123pan.com/api/user/sign_in");
            RefreshTokenURL = new URL("https://www.123pan.com/api/user/refresh_token");
        } catch (final MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static JSONObject postHttpData(final @NotNull URL url, final @NotNull String token, final @Nullable JSONObject request) throws IOException {
        final Map<String, String> property = new HashMap<>(1);
        property.put("Content-Type", "application/json;charset=utf-8");
        final JSONObject requests = Objects.requireNonNullElseGet(request, JSONObject::new);
        requests.put("authorization", "Bearer " + token);
        final JSONObject json = HttpUtil.sendJsonHttp((HttpURLConnection) url.openConnection(), "POST", property, requests);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != 0 || !"ok".equals(message))
            throw new WrongResponseException(code, message);
        return json.getJSONObject("data");
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
