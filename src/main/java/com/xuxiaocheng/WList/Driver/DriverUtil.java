package com.xuxiaocheng.WList.Driver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalResponseCodeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

public final class DriverUtil {
    private DriverUtil() {
        super();
    }

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[A-Za-z0-9]+(\\.[A-Za-z0-9]+){1,2}$");

    public static @NotNull JSONObject sendJsonHttp(final @NotNull String url, final @NotNull String method,
                                                   final @Nullable Map<@NotNull String, @NotNull String> property, final @Nullable JSONObject message) throws IOException {
        assert url.startsWith("http://") || url.startsWith("https://");
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        if (property == null)
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        else
            property.forEach(connection::setRequestProperty);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        if (message != null) {
            final OutputStream outputStream = connection.getOutputStream();
            outputStream.write(JSON.toJSONBytes(message));
            outputStream.close();
        }
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IllegalResponseCodeException(connection.getResponseCode());
        final InputStream inputStream = connection.getInputStream();
        final JSONObject json = JSON.parseObject(inputStream.readAllBytes());
        inputStream.close();
        connection.disconnect();
        return json;
    }
}
