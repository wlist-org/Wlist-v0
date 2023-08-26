package com.xuxiaocheng.WList.Driver.Helpers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
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
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
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
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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
    public static final @NotNull String defaultAgent = "WList/0.2.3";

    private static final @NotNull Dispatcher dispatcher = new Dispatcher(WListServer.IOExecutors);
    private static final @NotNull @Unmodifiable Set<@NotNull String> privateFormNames = Set.of("password", "pwd", "passwd", "pw");
    private static final @NotNull Interceptor NetworkLoggerInterceptor = chain -> {
        final Request request = chain.request();
        DriverNetworkHelper.logger.log(HLogLevel.NETWORK, "Sending: ", request.method(), ' ', request.url(),
                request.header("Range") == null ? "" : (" (Range: " + request.header("Range") + ')'),
                (Supplier<String>) () -> {
                    final RequestBody requestBody = request.body();
                    if (!(requestBody instanceof FormBody formBody))
                        return "";
                    final StringBuilder builder = new StringBuilder();
                    builder.append(" (Form: {");
                    for (int i = 0; i < formBody.size(); i++) {
                        final String name = formBody.name(i);
                        builder.append(name);
                        if (DriverNetworkHelper.privateFormNames.contains(name))
                            builder.append(": ***");
                        else
                            builder.append("=").append(formBody.value(i));
                        if (i == formBody.size() - 1)
                            builder.append("})");
                        else
                            builder.append(", ");
                    }
                    return builder.toString();
                });
        final long time1 = System.currentTimeMillis();
        final Response response;
        boolean successFlag = false;
        try {
            response = chain.proceed(request);
            successFlag = true;
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
    public static final @NotNull OkHttpClient noRedirectHttpClient = DriverNetworkHelper.newHttpClientBuilder().followRedirects(false).followSslRedirects(false).build();

    public static class FrequencyControlPolicy {
        protected final int limit;
        protected final int amount;
        protected final @NotNull TimeUnit unit;
        protected final @NotNull AtomicInteger accepted = new AtomicInteger(0);

        public FrequencyControlPolicy(final int limit, final int amount, final @NotNull TimeUnit unit) {
            super();
            this.limit = limit;
            this.amount = amount;
            this.unit = unit;
        }

        protected void tryLock() throws InterruptedException {
            synchronized (this.accepted) {
                boolean first = true;
                while (this.accepted.get() > this.limit) {
                    if (first) {
                        HLog.DefaultLogger.log(HLogLevel.NETWORK, "At frequency control: ", this.limit, " every ", this.unit.toMillis(this.amount), " ms.");
                        first = false;
                    }
                    this.accepted.wait();
                }
                this.accepted.getAndIncrement();
            }
        }

        protected void release() {
            DriverNetworkHelper.CountDownExecutors.schedule(() -> {
                synchronized (this.accepted) {
                    if (this.accepted.getAndDecrement() > 1)
                        this.accepted.notify();
                    else
                        this.accepted.notifyAll();
                }
            }, this.amount, this.unit);
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof FrequencyControlPolicy that)) return false;
            return this.limit == that.limit && this.amount == that.amount && this.unit == that.unit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.limit, this.amount, this.unit);
        }

        @Override
        public @NotNull String toString() {
            return "FrequencyControlPolicy{" +
                    "limit=" + this.limit +
                    ", duration=" + this.amount +
                    ", timeUnit=" + this.unit +
                    ", accepted=" + this.accepted +
                    '}';
        }
    }
    public static @NotNull FrequencyControlPolicy defaultFrequencyControlPolicyPerSecond() {
        return new FrequencyControlPolicy(5, 1, TimeUnit.SECONDS);
    }
    public static @NotNull FrequencyControlPolicy defaultFrequencyControlPolicyPerMinute() {
        return new FrequencyControlPolicy(100, 1, TimeUnit.MINUTES);
    }

    public static class FrequencyControlInterceptor implements Interceptor {
        protected final Set<FrequencyControlPolicy> policies = new HashSet<>();

        public FrequencyControlInterceptor(final @NotNull FrequencyControlPolicy @NotNull... policies) {
            super();
            this.policies.addAll(List.of(policies));
        }

        @Override
        public @NotNull Response intercept(final Interceptor.@NotNull Chain chain) throws IOException {
            final Collection<FrequencyControlPolicy> locked = new LinkedList<>();
            try {
                synchronized (this.policies) {
                    for (final FrequencyControlPolicy policy: this.policies) {
                        policy.tryLock();
                        locked.add(policy);
                    }
                }
                return chain.proceed(chain.request());
            } catch (final InterruptedException exception) {
                throw new IOException(exception);
            } finally {
                locked.forEach(FrequencyControlPolicy::release);
            }
        }

        @Override
        public @NotNull String toString() {
            return "FrequencyControlInterceptor{" +
                    "policies=" + this.policies +
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

    public static @NotNull Headers getRealHeader(final @NotNull OkHttpClient client, final @NotNull String url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull String> parameters) throws IOException {
        String head = url;
        while (true) {
            try (final Response response = DriverNetworkHelper.getWithParameters(client, Pair.ImmutablePair.makeImmutablePair(head, "HEAD"), headers, parameters).execute()) {
                if (response.code() != 302)
                    return response.headers();
                head = response.header("Location");
                if (head == null)
                    throw new IOException("No redirect location." + ParametersMap.create().add("url", url));
            }
        }
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

    public static class PersistenceCookieJar implements CookieJar {
        protected final @NotNull List<@NotNull Cookie> cookies;

        public PersistenceCookieJar(final @Nullable List<@NotNull Cookie> cookies) {
            super();
            this.cookies = Objects.requireNonNullElseGet(cookies, ArrayList::new);
        }

        @Override
        public synchronized void saveFromResponse(final @NotNull HttpUrl url, final @NotNull List<@NotNull Cookie> cookies) {
            this.cookies.addAll(cookies);
        }

        @Override
        public synchronized @NotNull List<@NotNull Cookie> loadForRequest(final @NotNull HttpUrl url) {
            final Collection<Cookie> invalidCookies = new ArrayList<>();
            final List<Cookie> validCookies = new ArrayList<>();
            final long now = System.currentTimeMillis();
            for (final Cookie cookie: this.cookies)
                if (cookie.expiresAt() < now)
                    invalidCookies.add(cookie);
                else if (cookie.matches(url))
                    validCookies.add(cookie);
            this.cookies.removeAll(invalidCookies);
            return validCookies;
        }

        @Override
        public @NotNull String toString() {
            return "PersistenceCookieJar{" +
                    "cache=" + this.cookies +
                    '}';
        }
    }
}
