package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Exceptions.NetworkException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import io.netty.buffer.ByteBuf;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public record DownloadRequirements(boolean acceptedRange, long downloadingSize, @NotNull SupplierE<@NotNull DownloadMethods> supplier) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm
     */
    @Contract("_, _ -> param1")
    public @NotNull ByteBuf dumpConfirm(final @NotNull ByteBuf buffer, final @NotNull String id) throws IOException {
        ByteBufIOUtil.writeBoolean(buffer, this.acceptedRange);
        ByteBufIOUtil.writeVariable2LenLong(buffer, this.downloadingSize);
        ByteBufIOUtil.writeUTF(buffer, id);
        return buffer;
    }

    public record DownloadMethods(@NotNull @Unmodifiable List<@NotNull OrderedSuppliers> parallelMethods, @NotNull Runnable finisher, @Nullable ZonedDateTime expireTime) {
        /**
         * @see com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm.DownloadInformation
         */
        @Contract("_ -> param1")
        public @NotNull ByteBuf dumpInformation(final @NotNull ByteBuf buffer) throws IOException {
            ByteBufIOUtil.writeVariableLenInt(buffer, this.parallelMethods.size());
            for (final DownloadRequirements.OrderedSuppliers suppliers: this.parallelMethods) {
                ByteBufIOUtil.writeVariable2LenLong(buffer, suppliers.start());
                ByteBufIOUtil.writeVariable2LenLong(buffer, suppliers.end());
            }
            ByteBufIOUtil.writeNullableDataTime(buffer, this.expireTime, DateTimeFormatter.ISO_DATE_TIME);
            return buffer;
        }
    }

    /**
     * @param start Start byte position of the whole file.
     * @param end End byte position of the whole file.
     * @param suppliersLink Each supplier should accept a {@code ByteBuf} whose {@code .readableBytes() <= }{@link com.xuxiaocheng.Rust.NetworkTransmission#FileTransferBufferSize}.
     */
    public record OrderedSuppliers(long start, long end, @NotNull OrderedNode suppliersLink) {
    }

    @FunctionalInterface
    public interface OrderedNode extends Function<@NotNull Consumer<@NotNull UnionPair<ByteBuf, Throwable>>, @Nullable OrderedNode> {
    }

    public static final @NotNull DownloadRequirements EmptyDownloadRequirements = new DownloadRequirements(true, 0, () -> DownloadRequirements.EmptyDownloadMethods);
    public static final @NotNull DownloadMethods EmptyDownloadMethods = new DownloadMethods(List.of(), RunnableE.EmptyRunnable, null);


    private static boolean isSupportedRange(final @NotNull Headers headers) {
        return Objects.requireNonNullElse(headers.get("Accept-Ranges"), "").contains("bytes");
    }

    private static @Nullable ZonedDateTime getExpireTime(final @Nullable ZonedDateTime expires, final @NotNull Headers headers) {
        if (expires != null)
            return expires;
        final Instant instant = headers.getInstant("Expires"); // TODO: Expires?
        return instant == null ? null : ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }


    private static Pair.@Nullable ImmutablePair<@NotNull Headers, Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull Long>> getUrlHeaders(final @NotNull OkHttpClient client, final @NotNull HttpUrl url, final @Nullable Headers testedResponseHeader, final @Nullable Long totalSize, final Headers.@Nullable Builder requestHeaderBuilder, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IOException {
        if (from < 0 || to < 0 || from > to)
            return null;
        long size = 0, start = 0, end = 0;
        if (totalSize != null) {
            size = totalSize.longValue();
            start = Math.min(from, size);
            end = Math.min(to, size);
            if (start >= end || size <= 0)
                return null;
        }
        final Headers urlHeaders;
        if (testedResponseHeader == null)
            try (final Response response = HttpNetworkHelper.getWithParameters(client, Pair.ImmutablePair.makeImmutablePair(url, "HEAD"), requestHeaderBuilder == null ? null : requestHeaderBuilder.build(), null).execute()) {
                urlHeaders = response.headers();
                if (!response.isSuccessful())
                    throw new NetworkException(response.code(), response.message(), url);
            }
        else urlHeaders = testedResponseHeader;
        if (totalSize == null) {
            final String length = urlHeaders.get("Content-Length");
            if (length == null)
                throw new IOException("No file size found in parameters or response." + ParametersMap.create().add("url", url).add("from", from).add("to", to).add("urlHeaders", urlHeaders.toMultimap()));
            try {
                size = Long.parseLong(length);
            } catch (final NumberFormatException exception) {
                throw new IOException("Unreachable!", exception);
            }
            start = Math.min(from, size);
            end = Math.min(to, size);
            if (start >= end || size <= 0)
                return null;
        }
        return Pair.ImmutablePair.makeImmutablePair(urlHeaders, Triad.ImmutableTriad.makeImmutableTriad(size, start, end));
    }

    private static @NotNull DownloadRequirements getDownloadMethodsByRangedUrl(final @NotNull OkHttpClient client, final @NotNull HttpUrl url, final @Nullable Headers testedResponseHeader, final @Nullable Long totalSize, final Headers.@Nullable Builder requestHeaderBuilder, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable ZonedDateTime expireTime) throws IOException {
        final Pair.ImmutablePair<Headers, Triad.ImmutableTriad<Long, Long, Long>> result = DownloadRequirements.getUrlHeaders(client, url, testedResponseHeader, totalSize, requestHeaderBuilder, from, to);
        if (result == null)
            return DownloadRequirements.EmptyDownloadRequirements;
        final Headers urlHeaders = result.getFirst();
        if (!DownloadRequirements.isSupportedRange(urlHeaders))
            throw new IllegalStateException("File cannot download by range header." + ParametersMap.create().add("url", url).add("from", from).add("to", to).add("urlHeaders", urlHeaders.toMultimap()));
        final long size = result.getSecond().getA().longValue();
        final long start = result.getSecond().getB().longValue();
        final long end = result.getSecond().getC().longValue();
        final ZonedDateTime expires = DownloadRequirements.getExpireTime(expireTime, urlHeaders);
        return new DownloadRequirements(true, end - start, () -> {
            final int count = MiscellaneousUtil.calculatePartCount(size, NetworkTransmission.FileTransferBufferSize);
            final List<OrderedSuppliers> list = new ArrayList<>(count);
            final Collection<Call> calls = new ArrayList<>(count);
            final Pair.ImmutablePair<HttpUrl, String> getUrl = Pair.ImmutablePair.makeImmutablePair(url, "GET");
            final Headers.Builder builder = Objects.requireNonNullElseGet(requestHeaderBuilder, Headers.Builder::new);
            for (long b = start; b < end; b += NetworkTransmission.FileTransferBufferSize) {
                final long e = Math.min(end, b + NetworkTransmission.FileTransferBufferSize);
                final int length = Math.toIntExact(e - b);
                final Call call = HttpNetworkHelper.getWithParameters(client, getUrl, builder.set("Range", String.format("bytes=%d-%d", b, e - 1)).build(), null);
                calls.add(call);
                list.add(new OrderedSuppliers(b - start, e - start, consumer -> {
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(final @NotNull Call call, final @NotNull IOException exception) {
                            consumer.accept(UnionPair.fail(exception));
                        }

                        @Override
                        public void onResponse(final @NotNull Call call, final @NotNull Response response) {
                            try (final InputStream stream = HttpNetworkHelper.extraResponseBody(response).byteStream()) {
                                consumer.accept(UnionPair.ok(HttpNetworkHelper.receiveDataFromStream(stream, length)));
                            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                consumer.accept(UnionPair.fail(exception));
                            }
                        }
                    });
                    return null;
                }));
            }
            return new DownloadMethods(list, () -> calls.forEach(Call::cancel), expires);
        });
    }

    public static @NotNull DownloadRequirements tryGetDownloadFromUrl(final @NotNull OkHttpClient client, final @NotNull HttpUrl url, final @Nullable Headers testedResponseHeader, final @Nullable Long totalSize, final Headers.@Nullable Builder requestHeaderBuilder, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable ZonedDateTime expireTime) throws IOException {
        final Pair.ImmutablePair<Headers, Triad.ImmutableTriad<Long, Long, Long>> result = DownloadRequirements.getUrlHeaders(client, url, testedResponseHeader, totalSize, requestHeaderBuilder, from, to);
        if (result == null)
            return DownloadRequirements.EmptyDownloadRequirements;
        final Headers urlHeaders = result.getFirst();
        final long size = result.getSecond().getA().longValue();
        final long start = result.getSecond().getB().longValue();
        final long end = result.getSecond().getC().longValue();
        final ZonedDateTime expires = DownloadRequirements.getExpireTime(expireTime, urlHeaders);
        if (DownloadRequirements.isSupportedRange(urlHeaders))
            return DownloadRequirements.getDownloadMethodsByRangedUrl(client, url, urlHeaders, size, requestHeaderBuilder, start, end, expires);
        return new DownloadRequirements(false, size, () -> {
            final Call call = HttpNetworkHelper.getWithParameters(client, Pair.ImmutablePair.makeImmutablePair(url, "GET"), requestHeaderBuilder == null ? null : requestHeaderBuilder.build(), null);
            final InputStream body = HttpNetworkHelper.extraResponseBody(call.execute()).byteStream();
            return new DownloadMethods(List.of(new OrderedSuppliers(0, size, new OrderedNode() {
                private long current = 0;
                @Override
                public @Nullable OrderedNode apply(final @NotNull Consumer<@NotNull UnionPair<ByteBuf, Throwable>> consumer) {
                    final long start = this.current;
                    final long end = Math.min(start + NetworkTransmission.FileTransferBufferSize, size);
                    final int length = Math.toIntExact(end - start);
                    try {
                        consumer.accept(UnionPair.ok(HttpNetworkHelper.receiveDataFromStream(body, length)));
                    } catch (final IOException exception) {
                        consumer.accept(UnionPair.fail(exception));
                        return null;
                    }
                    this.current += length;
                    return this.current >= size ? null : this;
                }
            })), HExceptionWrapper.wrapRunnable(body::close, call::cancel), expires);
        });
    }
}
