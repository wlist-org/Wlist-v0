package com.xuxiaocheng.WList.Driver.Helpers;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class DriverUtil {
    private DriverUtil() {
        super();
    }

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    public static final @NotNull String InvalidPhoneNumber = "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801";
    public static final @NotNull String InvalidMailAddress = "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u53f7";

    public static final int DefaultLimitPerRequestPage = 100;
    public static final Options.@NotNull OrderPolicy DefaultOrderPolicy = Options.OrderPolicy.FileName;
    public static final Options.@NotNull OrderDirection DefaultOrderDirection = Options.OrderDirection.ASCEND;

    private static final @NotNull Iterable<@NotNull Pattern> HtmlCommentsTags = List.of(Pattern.compile("<!--.*?-->"));
    public static @NotNull String removeHtmlComments(final @Nullable String html) {
        if (html == null)
            return "";
        String res = html;
        for (final Pattern pattern: DriverUtil.HtmlCommentsTags) {
            res = pattern.matcher(res).replaceAll("");
        }
        return res;
    }

    private static final @NotNull String scriptStartTag = "<script type=\"text/javascript\">";
    private static final @NotNull String scriptEndTag = "</script>";
    public static @NotNull List<@NotNull String> findScripts(final @NotNull String html) {
        final List<String> scripts = new ArrayList<>();
        int index = 0;
        while (true) {
            index = html.indexOf(DriverUtil.scriptStartTag, index);
            if (index == -1) break;
            final int endIndex = html.indexOf(DriverUtil.scriptEndTag, index);
            if (endIndex == -1) break;
            scripts.add(html.substring(index + DriverUtil.scriptStartTag.length(), endIndex));
            index = endIndex;
        }
        return scripts;
    }

    /**
     * Example: <pre>{@code
     *     int retry = 0;
     *     final Pair.ImmutablePair<String, String> wrapper = DriverUtil.getRetryWrapper(path.getName());
     *     while (...) {
     *         final String name = wrapper.getFirst() + (++retry) + wrapper.getSecond();
     *         ...
     *     }
     * }</pre>
     */
    public static Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> getRetryWrapper(final @NotNull String name) {
        final int index = name.lastIndexOf('.');
        final String left = (index < 0 ? name: name.substring(0, index)) + '(';
        final String right = ')' + (index < 0 ? "" : name.substring(index));
        return Pair.ImmutablePair.makeImmutablePair(left, right);
    }

    private static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> getIteratorWrappedSuppliers(final @NotNull Runnable supplier, final @NotNull BlockingQueue<? extends @NotNull I> filesQueue, final @NotNull AtomicBoolean noNext, final @NotNull AtomicBoolean threadRunning, final @NotNull Consumer<? super @Nullable Exception> callback) {
        return Pair.ImmutablePair.makeImmutablePair(new Iterator<>() {
            @Override
            public boolean hasNext() {
                if (filesQueue.peek() != null)
                    return true;
                if (noNext.get())
                    return false;
                supplier.run();
                synchronized (threadRunning) {
                    while (threadRunning.get() && filesQueue.peek() == null)
                        try {
                            threadRunning.wait();
                        } catch (final InterruptedException ignore) {
                            return false;
                        }
                }
                return this.hasNext();
            }

            @Override
            public @NotNull I next() {
                final I item = filesQueue.poll();
                if (item != null) {
                    if (filesQueue.size() < DriverUtil.DefaultLimitPerRequestPage >> 1)
                        supplier.run();
                    return item;
                }
                if (!this.hasNext())
                    throw new NoSuchElementException();
                return this.next();
            }
        }, () -> {
            if (noNext.compareAndSet(false, true))
                callback.accept(new CancellationException());
        });
    }

    public static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> wrapSuppliersInPages(final @NotNull FunctionE<? super @NotNull Integer, ? extends @Nullable Collection<@NotNull I>> supplierInPage, final @NotNull Consumer<? super @Nullable Exception> callback) {
        final AtomicBoolean noNext = new AtomicBoolean(false);
        final BlockingQueue<I> filesQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean threadRunning = new AtomicBoolean(false);
        final AtomicInteger nextPage = new AtomicInteger(0);
        return DriverUtil.getIteratorWrappedSuppliers(() -> {
            if (threadRunning.compareAndSet(false, true))
                CompletableFuture.runAsync(() -> {
                    try {
                        while (filesQueue.size() < DriverUtil.DefaultLimitPerRequestPage) {
                            if (noNext.get())
                                return;
                            final Collection<I> page;
                            try {
                                page = supplierInPage.apply(nextPage.getAndIncrement());
                            } catch (final Exception exception) {
                                if (noNext.compareAndSet(false, true))
                                    callback.accept(exception);
                                throw new NoSuchElementException(exception);
                            }
                            if (page == null) {
                                if (noNext.compareAndSet(false, true))
                                    callback.accept(null);
                                return;
                            }
                            synchronized (threadRunning) {
                                filesQueue.addAll(page);
                                threadRunning.notifyAll();
                            }
                            if (filesQueue.size() >= DriverUtil.DefaultLimitPerRequestPage)
                                break;
                        }
                    } finally {
                        synchronized (threadRunning) {
                            threadRunning.set(false);
                            threadRunning.notifyAll();
                        }
                    }
                }, WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
        }, filesQueue, noNext, threadRunning, callback);
    }

    @Deprecated // TODO
    public static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> wrapSuppliersInPages(final int pageCount, final @NotNull FunctionE<? super @NotNull Integer, @NotNull Collection<@NotNull I>> supplierInPage, final @NotNull Consumer<? super @Nullable Exception> callback) {
        throw new UnsupportedOperationException();
//        final AtomicBoolean noNext = new AtomicBoolean(false);
//        final BlockingQueue<I> filesQueue = new LinkedBlockingQueue<>();
//        final AtomicBoolean threadRunning = new AtomicBoolean(false);
//        final AtomicInteger nextPage = new AtomicInteger(0);
//        final Supplier<@Nullable Collection<I>> wrappedSupplier = () -> {
//            final Collection<I> page;
//            try {
//                page = supplierInPage.apply(nextPage.getAndIncrement());
//            } catch (final Exception exception) {
//                if (noNext.compareAndSet(false, true))
//                    callback.accept(exception);
//                throw new NoSuchElementException(exception);
//            }
//            return page;
//        };
//        return DriverUtil.getIteratorWrappedSuppliers(() -> {
//            if (threadRunning.compareAndSet(false, true))
//                CompletableFuture.runAsync(() -> {
//                    try {
//                        // Multi-call wapperedSupplier.
//                    } finally {
//                        threadRunning.set(false);
//                    }
//                }, WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
//        }, filesQueue, noNext, threadRunning, callback);
    }

    public static @NotNull DownloadMethods getDownloadMethodsByUrlWithRangeHeader(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final long size, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final Headers.@Nullable Builder builder) throws IOException {
        final long end = Math.min(to, size);
        final long total = end - from;
        if (from >= size || total < 0)
            return new DownloadMethods(0, List.of(), RunnableE.EmptyRunnable, null);
        final Headers headers;
        try (final Response response = DriverNetworkHelper.getWithParameters(DriverNetworkHelper.defaultHttpClient, Pair.ImmutablePair.makeImmutablePair(url.getFirst(), "HEAD"), Objects.requireNonNullElseGet(builder, Headers.Builder::new).build(), null).execute()) {
            headers = response.headers();
        }
        final Instant instant = headers.getInstant("Expires");
        if (!Objects.requireNonNullElse(headers.get("Accept-Ranges"), "").contains("bytes") || instant == null)
            throw new IllegalStateException("File cannot download by range header." + ParametersMap.create().add("url", url).add("size", size).add("from", from).add("to", to).add("headers", headers.toMultimap()));
        final LocalDateTime expires = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        final int count = MiscellaneousUtil.calculatePartCount(total, WListServer.FileTransferBufferSize);
        final List<SupplierE<ByteBuf>> list = new ArrayList<>(count);
        for (long i = from; i < end; i += WListServer.FileTransferBufferSize) {
            final long b = i;
            final long e = Math.min(end, i + WListServer.FileTransferBufferSize);
            list.add(() -> {
                //noinspection NumericCastThatLosesPrecision
                final int length = (int) (e - b);
                try (final InputStream stream = DriverNetworkHelper.extraResponseBody(DriverNetworkHelper.callWithJson(client, url,
                        Objects.requireNonNullElseGet(builder, Headers.Builder::new).set("Range", String.format("bytes=%d-%d", b, e - 1)).build(), null).execute()).byteStream()) {
                    final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(length, length);
                    try {
                        // stream.transferTo(new ByteBufOutputStream(buffer));
                        int read = 0;
                        while (read < length) {
                            final int current = buffer.writeBytes(stream, length - read);
                            if (current < 0)
                                break;
                            if (current == 0)
                                Thread.onSpinWait();
                            read += current;
                        }
                        return buffer.retain();
                    } finally {
                        buffer.release();
                    }
                }
            });
        }
        return new DownloadMethods(total, list, RunnableE.EmptyRunnable, expires);
    }

    public static @NotNull DownloadMethods toCachedDownloadMethods(final @NotNull DownloadMethods source) {
        final int count = source.methods().size();
        final int forwardDownloadCacheCount = GlobalConfiguration.getInstance().forwardDownloadCacheCount();
        if (count < 1 || forwardDownloadCacheCount == 0)
            return source;
        final List<SupplierE<ByteBuf>> list = new ArrayList<>(count);
        final AtomicBoolean closeFlag = new AtomicBoolean(false);
        final Map<Integer, CompletableFuture<ByteBuf>> cacher = new ConcurrentHashMap<>(forwardDownloadCacheCount + 1);
        for (int i = 0; i < count; ++i) {
            final int c = i;
            list.add(() -> {
                if (closeFlag.get())
                    throw new IllegalStateException("Closed download methods.");
                for (int n = c; n < Math.min(c + forwardDownloadCacheCount, count - 1); ++n)
                    cacher.computeIfAbsent(n + 1, k -> CompletableFuture.supplyAsync(
                            HExceptionWrapper.wrapSupplier(source.methods().get(k.intValue())), WListServer.IOExecutors)
                            .exceptionally(MiscellaneousUtil.exceptionHandler()));
                final ByteBuf buffer;
                if (cacher.putIfAbsent(c, CompletableFuture.completedFuture(null)) == null)
                    buffer = source.methods().get(c).get();
                else
                    buffer = cacher.get(c).get();
                cacher.remove(c);
                return buffer;
            });
        }
        cacher.computeIfAbsent(0, k -> CompletableFuture.supplyAsync(
                HExceptionWrapper.wrapSupplier(source.methods().get(k.intValue())), WListServer.IOExecutors)
                .exceptionally(MiscellaneousUtil.exceptionHandler()));
        return new DownloadMethods(source.total(), list, () -> {
            closeFlag.set(true);
            for (final CompletableFuture<?> future: cacher.values())
                future.cancel(true);
            source.finisher().run();
            for (final CompletableFuture<ByteBuf> future: cacher.values())
                if (future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally())
                    try {
                        future.get().release();
                    } catch (final InterruptedException | ExecutionException ignore) {
                    }
        }, source.expireTime());
    }

    /**
     * @see WListServer#FileTransferBufferSize
     */
    public static Pair.@NotNull ImmutablePair<@NotNull List<@NotNull ConsumerE<@NotNull ByteBuf>>, @NotNull Runnable> splitUploadMethodEveryFileTransferBufferSize(final @NotNull ConsumerE<@NotNull ByteBuf> sourceMethod, final int totalSize) {
        assert totalSize > 0;
        if (totalSize < WListServer.FileTransferBufferSize)
            return Pair.ImmutablePair.makeImmutablePair(List.of(sourceMethod), RunnableE.EmptyRunnable);
        final int mod = totalSize % WListServer.FileTransferBufferSize;
        final int count = totalSize / WListServer.FileTransferBufferSize - (mod == 0 ? 1 : 0);
        final int rest = mod == 0 ? WListServer.FileTransferBufferSize : mod;
        final List<ConsumerE<ByteBuf>> list = new ArrayList<>(count + 1);
        final AtomicBoolean leaked = new AtomicBoolean(true);
        final ByteBuf[] cacher = new ByteBuf[count + 1];
        final AtomicInteger countDown = new AtomicInteger(count);
        for (int i = 0; i < count + 1; ++i) {
            final int c = i;
            list.add(b -> {
                assert b.readableBytes() == (c == count ? rest : WListServer.FileTransferBufferSize);
                cacher[c] = b;
                if (countDown.getAndDecrement() == 0) {
                    leaked.set(false);
                    final CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer(count + 1).addComponents(true, cacher);
                    try {
                        sourceMethod.accept(buf);
                    } finally {
                        buf.release();
                    }
                }
            });
        }
        return Pair.ImmutablePair.makeImmutablePair(list, () -> {
            if (leaked.get())
                for (final ByteBuf buffer: cacher)
                    if (buffer != null)
                        buffer.release();
        });
    }
}
