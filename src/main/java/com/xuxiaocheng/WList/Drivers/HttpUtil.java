package com.xuxiaocheng.WList.Drivers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Internal.Drives.Exceptions.IllegalResponseCodeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;

public final class HttpUtil {
    private HttpUtil() {
        super();
    }

    public static @NotNull JSONObject sendJsonHttp(final @NotNull HttpURLConnection connection, final @NotNull String method,
                                          final @NotNull Map<@NotNull String, @NotNull String> property, final @NotNull JSONObject message) throws IOException {
        connection.setRequestMethod(method);
        property.forEach(connection::setRequestProperty);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        final OutputStream outputStream = connection.getOutputStream();
        outputStream.write(JSON.toJSONBytes(message));
        outputStream.close();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IllegalResponseCodeException(connection.getResponseCode());
        final InputStream inputStream = connection.getInputStream();
        final JSONObject json = JSON.parseObject(inputStream.readAllBytes());
        inputStream.close();
        connection.disconnect();
        return json;
    }
}
