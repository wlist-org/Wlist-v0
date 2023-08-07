package com.xuxiaocheng.WList.Driver.Helpers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import com.xuxiaocheng.WList.Exceptions.NetworkException;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class DriverNetworkHelper {
    private DriverNetworkHelper() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("NetworkLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true, HMergedStreams.getFileOutputStreamNoException(null));

    public static final @NotNull EventExecutorGroup CountDownExecutors =
            new DefaultEventExecutorGroup(2, new DefaultThreadFactory("CountDownExecutors"));
    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String defaultWebAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.37";
    public static final @NotNull String defaultAgent = "WList/0.2.1";

    private static final @NotNull Dispatcher dispatcher = new Dispatcher(WListServer.IOExecutors);
    private static final @NotNull Interceptor NetworkLoggerInterceptor = chain -> {
        final Request request = chain.request();
        DriverNetworkHelper.logger.log(HLogLevel.NETWORK, "Sending: ", request.method(), ' ', request.url(),
                request.header("Range") == null ? "" : (" (Range: " + request.header("Range") + ')'));
        final long time1 = System.currentTimeMillis();
        final Response response;
        boolean successFlag = false;
        try {
            response = chain.proceed(request);
            successFlag = true;
        } catch (final RuntimeException exception) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            throw exception;
        } finally {
            final long time2 = System.currentTimeMillis();
            DriverNetworkHelper.logger.log(HLogLevel.NETWORK, "Received. Totally cost time: ", time2 - time1, "ms.",
                    successFlag ? "" : " But something went wrong.");
        }
        return response;
    };
    public static OkHttpClient.@NotNull Builder newHttpClientBuilder(){
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .dispatcher(DriverNetworkHelper.dispatcher)
                .addInterceptor(DriverNetworkHelper.NetworkLoggerInterceptor);
    }
    public static final @NotNull OkHttpClient defaultHttpClient = DriverNetworkHelper.newHttpClientBuilder().build();

    public static class FrequencyControlInterceptor implements Interceptor {
        protected final int perSecond;
        protected final @NotNull AtomicInteger frequencyControlSecond = new AtomicInteger(0);

        protected final int perMinute;
        protected final @NotNull AtomicInteger frequencyControlMinute = new AtomicInteger(0);

        public FrequencyControlInterceptor(final int perSecond, final int perMinute) {
            super();
            this.perSecond = perSecond;
            this.perMinute = perMinute;
        }

        @Override
        public @NotNull Response intercept(final Interceptor.@NotNull Chain chain) throws IOException {
            synchronized (this.frequencyControlSecond) {
                boolean first = true;
                while (this.frequencyControlSecond.get() > this.perSecond)
                    try {
                        if (first) {
                            HLog.DefaultLogger.log(HLogLevel.NETWORK, "At frequency control: Second.");
                            first = false;
                        }
                        this.frequencyControlSecond.wait();
                    } catch (final InterruptedException exception) {
                        throw new IOException(exception);
                    }
                this.frequencyControlSecond.getAndIncrement();
            }
            synchronized (this.frequencyControlMinute) {
                boolean first = true;
                while (this.frequencyControlMinute.get() > this.perMinute)
                    try {
                        if (first) {
                            HLog.DefaultLogger.log(HLogLevel.NETWORK, "At frequency control: Minute.");
                            first = false;
                        }
                        this.frequencyControlMinute.wait();
                    } catch (final InterruptedException exception) {
                        throw new IOException(exception);
                    }
                this.frequencyControlMinute.getAndIncrement();
            }
            final Response response;
            try {
                response = chain.proceed(chain.request());
            } finally {
                DriverNetworkHelper.CountDownExecutors.schedule(() -> {
                    synchronized (this.frequencyControlSecond) {
                        if (this.frequencyControlSecond.getAndDecrement() > 1)
                            this.frequencyControlSecond.notify();
                        else
                            this.frequencyControlSecond.notifyAll();
                    }
                }, 1, TimeUnit.SECONDS);
                DriverNetworkHelper.CountDownExecutors.schedule(() -> {
                    synchronized (this.frequencyControlMinute) {
                        if (this.frequencyControlMinute.getAndDecrement() > 1)
                            this.frequencyControlMinute.notify();
                        else
                            this.frequencyControlMinute.notifyAll();
                    }
                }, 1, TimeUnit.MINUTES);
            }
            return response;
        }

        @Override
        public @NotNull String toString() {
            return "FrequencyControlInterceptor{" +
                    "perSecond=" + this.perSecond +
                    ", perMinute=" + this.perMinute +
                    ", frequencyControlSecond=" + this.frequencyControlSecond +
                    ", frequencyControlMinute=" + this.frequencyControlMinute +
                    '}';
        }
    }

    private static @NotNull HttpUrl getRealUrl(final @NotNull String url, final @Nullable Map<@NotNull String, @NotNull String> parameters) {
        if (parameters == null)
            return Objects.requireNonNull(HttpUrl.parse(url));
        final HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (final Map.Entry<String, String> entry: parameters.entrySet())
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static final Headers.@NotNull Builder emptyHeaders = new Headers.Builder();

    public static @NotNull Call getWithParameters(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull String> parameters) {
        assert !HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(DriverNetworkHelper.getRealUrl(url.getFirst(), parameters))
                .headers(Objects.requireNonNullElseGet(headers, DriverNetworkHelper.emptyHeaders::build))
                .method(url.getSecond(), null)
                .build());
    }

    public static @NotNull Call postWithBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(url.getFirst())
                .headers(Objects.requireNonNullElseGet(headers, DriverNetworkHelper.emptyHeaders::build))
                .method(url.getSecond(),
                        Objects.requireNonNullElseGet(body, () -> RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Call postWithParametersAndBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull String> parameters, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(DriverNetworkHelper.getRealUrl(url.getFirst(), parameters))
                .headers(Objects.requireNonNullElseGet(headers, DriverNetworkHelper.emptyHeaders::build))
                .method(url.getSecond(), Objects.requireNonNullElseGet(body, () ->
                        RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Call callWithJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) {
        return HttpMethod.requiresRequestBody(url.getSecond()) ?
                DriverNetworkHelper.postWithBody(client, url, headers, DriverNetworkHelper.createJsonRequestBody(body)) :
                DriverNetworkHelper.getWithParameters(client, url, headers, body == null ? null : body.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
    }

    public static @NotNull ResponseBody extraResponseBody(final @NotNull Response response) throws NetworkException {
        if (!response.isSuccessful())
            throw new NetworkException(response.code(), response.message());
        final ResponseBody body = response.body();
        if (body == null)
            throw new NetworkException("Null response body.");
        return body;
    }

    public static @NotNull JSONObject extraJsonResponseBody(final @NotNull Response response) throws IOException {
        try (final InputStream stream = DriverNetworkHelper.extraResponseBody(response).byteStream()) {
            return JSON.parseObject(stream);
        }
    }

    public static @NotNull RequestBody createJsonRequestBody(final @Nullable Object obj) {
        return RequestBody.create(JSON.toJSONBytes(obj),
                MediaType.parse("application/json;charset=utf-8"));
    }

    public static @NotNull RequestBody createOctetStreamRequestBody(final @NotNull ByteBuf content) {
        final ByteBuf _content = content;
        return new RequestBody() {
            private final int length = _content.readableBytes();
            private final @NotNull ByteBuf content = _content;

            @Override
            public @Nullable MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return this.length;
            }

            @Override
            public void writeTo(final @NotNull BufferedSink bufferedSink) throws IOException {
                final ByteBuffer nio;
                try {
                    nio = this.content.nioBuffer(this.content.readerIndex(), this.content.readableBytes());
                } catch (final RuntimeException exception) {
                    final int bufferSize = Math.min(this.length, 2 << 20);
                    for (final byte[] buffer = new byte[bufferSize]; this.content.readableBytes() > 0; ) {
                        final int len = Math.min(bufferSize, this.content.readableBytes());
                        this.content.readBytes(buffer, 0, len);
                        bufferedSink.write(buffer, 0, len);
                    }
                    return;
                }
                bufferedSink.write(nio);
            }

            @Override
            public @NotNull String toString() {
                return "ByteBufOctetStreamRequestBody{" +
                        "length=" + this.length +
                        ", super=" + super.toString() +
                        "}";
            }
        };
    }
}
