package com.xuxiaocheng.WList.Driver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Exceptions.NetworkException;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class DriverUtil {
    private DriverUtil() {
        super();
    }

    public static final @NotNull OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .dispatcher(new Dispatcher(WListServer.IOExecutors))
            .addNetworkInterceptor(chain -> {
                final Request request = chain.request();
                if (WList.DeepDebugMode) {
                    HLog.DefaultLogger.log(HLogLevel.DEBUG, "Thread: ", Thread.currentThread(), " Request: ", request);
                    HLog.DefaultLogger.log(HLogLevel.VERBOSE, "Thread: ", Thread.currentThread(), new Throwable());
                }
                final long time1 = System.currentTimeMillis();
                final Response response = chain.proceed(request);
                final long time2 = System.currentTimeMillis();
                if (WList.DeepDebugMode) {
                    HLog.DefaultLogger.log(HLogLevel.DEBUG, "Thread: ", Thread.currentThread(), "Response: ", response);
                    HLog.DefaultLogger.log(HLogLevel.INFO, "Thread: ", Thread.currentThread(), "Cost time: ", time2 - time1, "ms.");
                }
                return response;
            })
            .build();

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[A-Za-z0-9]+(\\.[A-Za-z0-9]+){1,2}$");

    public static @NotNull RequestBody createJsonRequestBody(final @Nullable Object obj) {
        return RequestBody.create(JSON.toJSONBytes(obj),
                MediaType.parse("application/json;charset=utf-8"));
    }

    public static @NotNull Call callRequestWithParameters(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> parameters) {
        assert !HttpMethod.requiresRequestBody(url.getSecond());
        final Request.Builder request = new Request.Builder();
        if (parameters == null)
            request.url(url.getFirst());
        else {
            final StringBuilder builder = new StringBuilder(url.getFirst());
            builder.append('?');
            for (final Map.Entry<String, Object> entry : parameters.entrySet())
                builder.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
            builder.deleteCharAt(builder.length() - 1);
            request.url(builder.toString());
        }
        return client.newCall(request
                .headers(Objects.requireNonNullElseGet(headers, () -> new Headers.Builder().build()))
                .method(url.getSecond(), null).build());
    }

    public static @NotNull Call callRequestWithBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(url.getFirst())
                .headers(Objects.requireNonNullElseGet(headers, () -> new Headers.Builder().build()))
                .method(url.getSecond(),
                        Objects.requireNonNullElseGet(body, () -> RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Response sendRequestJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) throws IOException {
        return (HttpMethod.requiresRequestBody(url.getSecond()) ?
            DriverUtil.callRequestWithBody(client, url, headers, DriverUtil.createJsonRequestBody(body)) :
            DriverUtil.callRequestWithParameters(client, url, headers, body)).execute();
    }

    public static void sendRequestAsync(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body, final @NotNull Callback callback) {
        (HttpMethod.requiresRequestBody(url.getSecond()) ?
                DriverUtil.callRequestWithBody(client, url, headers, DriverUtil.createJsonRequestBody(body)) :
                DriverUtil.callRequestWithParameters(client, url, headers, body)).enqueue(callback);
    }

    public static @NotNull ResponseBody extraResponse(final @NotNull Response response) throws NetworkException {
        if (!response.isSuccessful())
            throw new NetworkException(response.code(), response.message());
        final ResponseBody responseBody = response.body();
        if (responseBody == null)
            throw new NetworkException("Null response body.");
        return responseBody;
    }

    public static @NotNull JSONObject sendRequestReceiveJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) throws IOException {
        return JSON.parseObject(DriverUtil.extraResponse(DriverUtil.sendRequestJson(client, url, headers, body)).byteStream());
    }

    public static @NotNull InputStream getDownloadStream(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IOException {
        if (from >= to)
            return InputStream.nullInputStream();
        final InputStream link = DriverUtil.extraResponse(DriverUtil.sendRequestJson(client, url, headers, body)).byteStream();
        final long skip = link.skip(from);
        assert skip == from;
        return new InputStream() {
            private long pos = skip;

            @Override
            public int read() throws IOException {
                if (this.pos + 1 > to) {
                    link.close();
                    return -1;
                }
                ++this.pos;
                return link.read();
            }

            @Override
            public int read(final byte @NotNull [] b, final int off, final int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0)
                    return 0;
                if (this.pos + 1 > to) {
                    link.close();
                    return -1;
                }
                final int r = link.read(b, off, (int) Math.min(len, to - this.pos));
                this.pos += r;
                if (this.pos + 1 > to)
                    link.close();
                return r;
            }

            @Override
            public int available() {
                return (int) Math.min(to - this.pos, Integer.MAX_VALUE);
            }

            @Override
            public void close() throws IOException {
                link.close();
            }
        };
    }
}
