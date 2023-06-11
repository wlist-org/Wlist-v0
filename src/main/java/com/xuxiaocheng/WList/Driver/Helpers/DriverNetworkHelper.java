package com.xuxiaocheng.WList.Driver.Helpers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
import okhttp3.Interceptor;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class DriverNetworkHelper {
    private DriverNetworkHelper() {
        super();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String defaultWebAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.37";
    public static final @NotNull String defaultAgent = "WList/0.2.0";
    public static final OkHttpClient.@NotNull Builder httpClientBuilder = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .dispatcher(new Dispatcher(WListServer.IOExecutors))
            .addInterceptor(chain -> {
                final Request request = chain.request();
                if (WList.DebugMode)
                    HLog.DefaultLogger.log(HLogLevel.NETWORK, "Sending: ", request.method(), ' ', request.url(),
                        request.header("Range") == null ? "" : ("Range: " + request.header("Range")));
                final long time1 = System.currentTimeMillis();
                final Response response;
                try {
                    response = chain.proceed(request);
                } catch (final RuntimeException exception) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
                    throw exception;
                } finally {
                    final long time2 = System.currentTimeMillis();
                    if (WList.DebugMode)
                        HLog.DefaultLogger.log(HLogLevel.NETWORK, "Received. Totally cost time: ", time2 - time1, "ms.");
                }
                return response;
            });

    public static class FrequencyControlInterceptor implements Interceptor {
        protected final int perSecond;
        protected final @NotNull AtomicInteger frequencyControlSecond = new AtomicInteger(0);
        protected static final Executor threadPoolSecond = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

        protected final int perMinute;
        protected final @NotNull AtomicInteger frequencyControlMinute = new AtomicInteger(0);
        protected static final Executor threadPoolMinute = CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES);

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
                FrequencyControlInterceptor.threadPoolSecond.execute(() -> {
                    synchronized (this.frequencyControlSecond) {
                        if (this.frequencyControlSecond.getAndDecrement() > 1)
                            this.frequencyControlSecond.notify();
                        else
                            this.frequencyControlSecond.notifyAll();
                    }
                });
                FrequencyControlInterceptor.threadPoolMinute.execute(() -> {
                    synchronized (this.frequencyControlMinute) {
                        if (this.frequencyControlMinute.getAndDecrement() > 1)
                            this.frequencyControlMinute.notify();
                        else
                            this.frequencyControlMinute.notifyAll();
                    }
                });
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

    public static @NotNull RequestBody createJsonRequestBody(final @Nullable Object obj) {
        return RequestBody.create(JSON.toJSONBytes(obj),
                MediaType.parse("application/json;charset=utf-8"));
    }

    public static @NotNull Call callRequestWithParameters(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> parameters) {
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

    public static @NotNull Call callRequestWithBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(url.getFirst())
                .headers(Objects.requireNonNullElseGet(headers, () -> new Headers.Builder().build()))
                .method(url.getSecond(),
                        Objects.requireNonNullElseGet(body, () -> RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Response sendRequestJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) throws IOException {
        return (HttpMethod.requiresRequestBody(url.getSecond()) ?
                DriverNetworkHelper.callRequestWithBody(client, url, headers, DriverNetworkHelper.createJsonRequestBody(body)) :
                DriverNetworkHelper.callRequestWithParameters(client, url, headers, body)).execute();
    }

    public static void sendRequestAsync(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body, final @NotNull Callback callback) {
        (HttpMethod.requiresRequestBody(url.getSecond()) ?
                DriverNetworkHelper.callRequestWithBody(client, url, headers, DriverNetworkHelper.createJsonRequestBody(body)) :
                DriverNetworkHelper.callRequestWithParameters(client, url, headers, body)).enqueue(callback);
    }

    public static @NotNull ResponseBody extraResponse(final @NotNull Response response) throws NetworkException {
        if (!response.isSuccessful())
            throw new NetworkException(response.code(), response.message());
        final ResponseBody responseBody = response.body();
        if (responseBody == null)
            throw new NetworkException("Null response body.");
        return responseBody;
    }

    public static @NotNull JSONObject sendRequestReceiveJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) throws IOException {
        return JSON.parseObject(DriverNetworkHelper.extraResponse(DriverNetworkHelper.sendRequestJson(client, url, headers, body)).byteStream());
    }
}
