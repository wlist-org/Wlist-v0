package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Exceptions.NetworkException;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
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

public final class HttpNetworkHelper {
    private HttpNetworkHelper() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("NetworkLogger");

    public static final @NotNull EventExecutorGroup CountDownExecutors = new DefaultEventExecutorGroup(2, new DefaultThreadFactory("CountDownExecutors"));
    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String DefaultWebAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.41";
    public static final @NotNull String DefaultAgent = "WList/0.3.1";

    private static final @NotNull Dispatcher Dispatcher = new Dispatcher(WListServer.IOExecutors);
    private static final @NotNull @Unmodifiable Set<@NotNull String> PrivateFormNames = Set.of("password", "pwd", "passwd", "pw");
    private static final @NotNull Interceptor NetworkLoggerInterceptor = chain -> {
        final Request request = chain.request();
        HttpNetworkHelper.logger.log(HLogLevel.NETWORK, "Sending: ", request.method(), ' ', request.url(),
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
                        if (HttpNetworkHelper.PrivateFormNames.contains(name))
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
            HttpNetworkHelper.logger.log(HLogLevel.NETWORK, "Received. Totally cost time: ", time2 - time1, "ms.",
                    successFlag ? "" : " But something went wrong.");
        }
        return response;
    };
    /**
     * @see BrowserUtil#WebClientCore
     */
    public static OkHttpClient.@NotNull Builder newHttpClientBuilder(){
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .dispatcher(HttpNetworkHelper.Dispatcher)
                .addInterceptor(HttpNetworkHelper.NetworkLoggerInterceptor);
    }
    public static final @NotNull OkHttpClient DefaultHttpClient = HttpNetworkHelper.newHttpClientBuilder().build();
    public static final @NotNull OkHttpClient DefaultNoRedirectHttpClient = HttpNetworkHelper.newHttpClientBuilder().followRedirects(false).followSslRedirects(false).build();

    public static class FrequencyControlPolicy {
        protected final int limit;
        protected final int per;
        protected final @NotNull TimeUnit unit;
        protected final @NotNull AtomicInteger accepted = new AtomicInteger(0);

        public FrequencyControlPolicy(final int limit, final int per, final @NotNull TimeUnit unit) {
            super();
            this.limit = limit;
            this.per = per;
            this.unit = unit;
        }

        protected void tryLock() throws InterruptedException {
            synchronized (this.accepted) {
                boolean first = true;
                while (this.accepted.get() > this.limit) {
                    if (first) {
                        HLog.DefaultLogger.log(HLogLevel.NETWORK, "At frequency control: ", this.limit, " requests every ", this.unit.toMillis(this.per), " ms.");
                        first = false;
                    }
                    this.accepted.wait();
                }
                this.accepted.getAndIncrement();
            }
        }

        protected void release() {
            HttpNetworkHelper.CountDownExecutors.schedule(() -> {
                synchronized (this.accepted) {
                    if (this.accepted.getAndDecrement() > 1)
                        this.accepted.notify();
                    else
                        this.accepted.notifyAll();
                }
            }, this.per, this.unit);
        }

        @Override
        public @NotNull String toString() {
            return "FrequencyControlPolicy{" +
                    "limit=" + this.limit +
                    ", duration=" + this.per +
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
        protected final @NotNull Collection<@NotNull FrequencyControlPolicy> policies = new HashSet<>();

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

    private static @NotNull HttpUrl getRealUrl(final @NotNull HttpUrl url, final @Nullable Map<@NotNull String, @NotNull String> parameters) {
        if (parameters == null)
            return url;
        final HttpUrl.Builder builder = url.newBuilder();
        for (final Map.Entry<String, String> entry: parameters.entrySet())
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static final @NotNull Headers EmptyHeaders = new Headers.Builder().build();

    public static @NotNull Call getWithParameters(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull String> parameters) {
        assert !HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(HttpNetworkHelper.getRealUrl(url.getFirst(), parameters))
                .headers(Objects.requireNonNullElse(headers, HttpNetworkHelper.EmptyHeaders))
                .method(url.getSecond(), null)
                .build());
    }

    public static @NotNull Call postWithBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final @Nullable Headers headers, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(url.getFirst())
                .headers(Objects.requireNonNullElse(headers, HttpNetworkHelper.EmptyHeaders))
                .method(url.getSecond(),
                        Objects.requireNonNullElseGet(body, () -> RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Call postWithParametersAndBody(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull String> parameters, final @Nullable RequestBody body) {
        assert HttpMethod.requiresRequestBody(url.getSecond());
        return client.newCall(new Request.Builder().url(HttpNetworkHelper.getRealUrl(url.getFirst(), parameters))
                .headers(Objects.requireNonNullElse(headers, HttpNetworkHelper.EmptyHeaders))
                .method(url.getSecond(), Objects.requireNonNullElseGet(body, () ->
                        RequestBody.create("".getBytes(StandardCharsets.UTF_8))))
                .build());
    }

    public static @NotNull Call callWithJson(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body) {
        return HttpMethod.requiresRequestBody(url.getSecond()) ?
                HttpNetworkHelper.postWithBody(client, url, headers, HttpNetworkHelper.createJsonRequestBody(body)) :
                HttpNetworkHelper.getWithParameters(client, url, headers, body == null ? null : body.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
    }

    public static @NotNull ResponseBody extraResponseBody(final @NotNull Response response) throws IOException {
        if (!response.isSuccessful())
            throw new NetworkException(response.code(), response.message(), response.request().url());
        final ResponseBody body = response.body();
        if (body == null)
            throw new NetworkException("Null response body.");
        return body;
    }

    public static @NotNull JSONObject extraJsonResponseBody(final @NotNull Response response) throws IOException {
        try (final InputStream stream = HttpNetworkHelper.extraResponseBody(response).byteStream()) {
            return JSON.parseObject(stream);
        }
    }

    public static @NotNull RequestBody createJsonRequestBody(final @Nullable Object obj) {
        return RequestBody.create(JSON.toJSONBytes(obj), MediaType.parse("application/json;charset=utf-8"));
    }

    @FunctionalInterface
    public interface UploadProgressListener {
        void onProgress(long current, long total);
    }

    public static @NotNull RequestBody createOctetStreamRequestBody(final @NotNull ByteBuf content, final @Nullable UploadProgressListener listener) {
        final long length = content.readableBytes();
        final RequestBody body = new RequestBody() {
            @Override
            public @Nullable MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return length;
            }

            @Override
            public void writeTo(final @NotNull BufferedSink sink) throws IOException {
                if (content.readableBytes() == 0)
                    return;
                if (content.nioBufferCount() < 0) {
                    HLog.getInstance("NetworkLogger").log(HLogLevel.MISTAKE, "Rewrite data from netty to okhttp by default algorithm without nio buffer.",
                            ParametersMap.create().add("content", content)); // Reachable?
                    final int bufferSize = (int) Math.min(length, 2 << 20);
                    for (final byte[] buffer = new byte[bufferSize]; content.readableBytes() > 0; ) {
                        final int len = Math.min(bufferSize, content.readableBytes());
                        content.readBytes(buffer, 0, len);
                        sink.write(buffer, 0, len);
                    }
                    return;
                }
                for (final ByteBuffer buffer: content.nioBuffers())
                    sink.write(buffer);
            }


            @Override
            public @NotNull String toString() {
                return "ByteBufOctetStreamRequestBody{" +
                        "length=" + length +
                        ", content=" + content +
                        ", super=" + super.toString() +
                        "}";
            }
        };
        return listener == null ? body : new ProgressRequestBody(body, listener);
    }

    public static class ProgressRequestBody extends RequestBody {
        protected final @NotNull RequestBody requestBody;
        protected final @NotNull UploadProgressListener listener;

        public ProgressRequestBody(final @NotNull RequestBody requestBody, final @NotNull UploadProgressListener listener) {
            super();
            this.requestBody = requestBody;
            this.listener = listener;
        }

        @Override
        public @Nullable MediaType contentType() {
            return this.requestBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return this.requestBody.contentLength();
        }

        @Override
        public void writeTo(final @NotNull BufferedSink sink) throws IOException {
            final BufferedSink forwardingSink = Okio.buffer(new ForwardingSink(sink) {
                private final long length = ProgressRequestBody.this.requestBody.contentLength();
                private long written = 0;

                @Override
                public void write(final @NotNull Buffer source, final long byteCount) throws IOException {
                    super.write(source, byteCount);
                    this.written += byteCount;
                    ProgressRequestBody.this.listener.onProgress(this.written, this.length);
                }
            });
            this.requestBody.writeTo(forwardingSink);
            forwardingSink.flush();
        }

        @Override
        public @NotNull String toString() {
            return "ProgressRequestBody{" +
                    "requestBody=" + this.requestBody +
                    ", listener=" + this.listener +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Deprecated
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

    public static @NotNull ByteBuf receiveDataFromStream(final @NotNull InputStream stream, final int length) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(length, length);
        try {
            // stream.transferTo(new ByteBufOutputStream(buffer));
            int read = 0;
            while (read < length) {
                final int current = buffer.writeBytes(stream, length - read);
                if (current < 0)
                    break;
                if (current == 0)
                    AndroidSupporter.onSpinWait();
                read += current;
            }
            return buffer.retain();
        } finally {
            buffer.release();
        }
    }
}
