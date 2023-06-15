package com.xuxiaocheng.WList.Driver.Helpers;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.DownloadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    public static final Options.@NotNull OrderPolicy DefaultOrderPolicy = Options.OrderPolicy.FileName;
    public static final Options.@NotNull OrderDirection DefaultOrderDirection = Options.OrderDirection.ASCEND;


    public static @NotNull Pair.ImmutablePair<@NotNull String, @NotNull String> getRetryWrapper(final @NotNull String name) {
        final int index = name.lastIndexOf('.');
        final String left = (index < 0 ? name: name.substring(0, index)) + '(';
        final String right = ')' + (index < 0 ? "" : name.substring(index));
        return Pair.ImmutablePair.makeImmutablePair(left, right);
    }

    public static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull FileSqlInformation>, @NotNull Runnable> wrapAllFilesListerInPages(final @NotNull FunctionE<? super @NotNull Integer, ? extends Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>>> fileSupplierInPage, final int defaultLimit, final @NotNull Consumer<? super @Nullable Exception> finisher, final @Nullable ExecutorService _threadPool) {
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> firstPage;
        try {
            firstPage = fileSupplierInPage.apply(0);
        } catch (final Exception exception) {
            finisher.accept(exception);
            return Triad.ImmutableTriad.makeImmutableTriad(0L, MiscellaneousUtil.getEmptyIterator(), RunnableE.EmptyRunnable);
        }
        final long fileCount = firstPage.getFirst().intValue();
        if (fileCount <= 0 || firstPage.getSecond().isEmpty()) {
            finisher.accept(null);
            return Triad.ImmutableTriad.makeImmutableTriad(0L, MiscellaneousUtil.getEmptyIterator(), RunnableE.EmptyRunnable);
        }
        assert firstPage.getSecond().size() <= defaultLimit;
        final int pageCount = MiscellaneousUtil.calculatePartCount(fileCount, defaultLimit);
        if (pageCount <= 1) {
            finisher.accept(null);
            return Triad.ImmutableTriad.makeImmutableTriad(fileCount, firstPage.getSecond().iterator(), RunnableE.EmptyRunnable);
        }
        final ExecutorService threadPool = Objects.requireNonNullElse(_threadPool, WListServer.IOExecutors);
        final BlockingQueue<FileSqlInformation> allFiles = new LinkedBlockingQueue<>(Math.max((int) fileCount, firstPage.getSecond().size() << 1));
        allFiles.addAll(firstPage.getSecond());
        final AtomicInteger countDown = new AtomicInteger(pageCount);
        final AtomicBoolean calledFinisher = new AtomicBoolean(false);
        final AtomicBoolean cancelFlag = new AtomicBoolean(false);
        final CompletableFuture<?>[] futures = new CompletableFuture[pageCount - 1];
        for (int i = 0; i < pageCount - 1; ++i)
            futures[i] = new CompletableFuture<>();
        final CompletableFuture<?> future = CompletableFuture.allOf(futures);
        final Future<?>[] threads = new Future[pageCount - 1];
        for (int page = 1; page < pageCount; ++page) {
            final int current = page;
            threads[current - 1] = threadPool.submit(() -> {
                try {
                    final Pair.ImmutablePair<Long, List<FileSqlInformation>> infos = fileSupplierInPage.apply(current);
                    assert infos.getFirst().longValue() == fileCount;
                    assert current == pageCount - 1 ? infos.getSecond().size() <= defaultLimit : infos.getSecond().size() == defaultLimit;
                    allFiles.addAll(infos.getSecond());
                    futures[current - 1].complete(null);
                    if (countDown.decrementAndGet() == 1 && calledFinisher.compareAndSet(false, true))
                        finisher.accept(null);
                } catch (final Exception exception) {
                    cancelFlag.set(true);
                    future.completeExceptionally(exception);
                    futures[current - 1].completeExceptionally(exception);
                    if (calledFinisher.compareAndSet(false, true))
                        finisher.accept(exception);
                }
            }, threadPool);
        }
        return Triad.ImmutableTriad.makeImmutableTriad(fileCount, MiscellaneousUtil.wrapCountedBlockingQueueCancellable(allFiles, fileCount,
                cancelFlag, TimeUnit.SECONDS.toMillis(10)), () -> {
            cancelFlag.set(true);
            future.cancel(true);
            for (final CompletableFuture<?> f: futures)
                f.cancel(true);
            for (final Future<?> thread: threads)
                thread.cancel(true);
            if (calledFinisher.compareAndSet(false, true))
                finisher.accept(new CancellationException());
        });
    }

    public static @NotNull DownloadMethods getDownloadMethodsByUrlWithRangeHeader(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final long size, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final Headers.@Nullable Builder builder) {
        final long end = Math.min(to, size);
        final long total = end - from;
        if (from >= size || total < 0)
            return new DownloadMethods(0, List.of(), RunnableE.EmptyRunnable);
        final int count = MiscellaneousUtil.calculatePartCount(total, WListServer.FileTransferBufferSize);
        final List<SupplierE<ByteBuf>> list = new ArrayList<>(count);
        for (long i = from; i < end; i += WListServer.FileTransferBufferSize) {
            final long b = i;
            final long e = Math.min(end, i + WListServer.FileTransferBufferSize);
            list.add(() -> {
                //noinspection NumericCastThatLosesPrecision
                final int length = (int) (e - b);
                try (final InputStream stream = DriverNetworkHelper.extraResponse(DriverNetworkHelper.sendRequestJson(client, url,
                        Objects.requireNonNullElseGet(builder, Headers.Builder::new).set("Range", String.format("bytes=%d-%d", b, e - 1)).build(), null)).byteStream()) {
                    final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(length, length);
                    try {
                        int read = 0;
                        while (read < length) {
                            final int current = buffer.writeBytes(stream, length - read);
                            if (current < 0)
                                break;
                            read += current;
                        }
                        return buffer.retain();
                    } finally {
                        buffer.release();
                    }
                }
            });
        }
        return new DownloadMethods(total, list, RunnableE.EmptyRunnable);
    }

    public static @NotNull DownloadMethods toCachedDownloadMethods(final @NotNull DownloadMethods source) {
        // TODO
        return source;
//        final int count = source.methods().size();
//        final List<SupplierE<ByteBuf>> list = new ArrayList<>(count);
//        final AtomicBoolean closeFlag = new AtomicBoolean(false);
//        final Map<Integer, CompletableFuture<ByteBuf>> cacher = new ConcurrentHashMap<>(3);
//        for (int i = 0; i < count; ++i) {
//            final int c = i;
//            list.add(() -> {
//                if (closeFlag.get())
//                    throw new IllegalStateException("Closed download methods.");
//                for (int n = c; n < Math.min(c + 3, count); ++n)
//                    cacher.computeIfAbsent(n, k -> CompletableFuture.supplyAsync(
//                            HExceptionWrapper.wrapSupplier(source.methods().get(k.intValue())), WListServer.IOExecutors));
//                final ByteBuf buffer = cacher.get(c).get();
//                if (closeFlag.get())
//                    buffer.release(); // Needn't any ref of buffer.
//                return buffer;
//            });
//        }
//        if (count > 0) {
//            final Future<ByteBuf> future= WListServer.IOExecutors.submit(list.get(0)::get);
//            list.set(0, future::get);
//        }
//        return new DownloadMethods(source.total(), list, () -> {
//            closeFlag.set(true);
//            source.finisher().run();
//        });
    }

    public abstract static class OctetStreamRequestBody extends RequestBody {
        protected final long length;

        protected OctetStreamRequestBody(final long length) {
            super();
            this.length = length;
        }

        @Override
        public @Nullable MediaType contentType() {
            return MediaType.parse("application/octet-stream");
        }

        @Override
        public long contentLength() {
            return this.length;
        }

        @Override
        public @NotNull String toString() {
            return "OctetStreamRequestBody{" +
                    "length=" + this.length +
                    ", super=" + super.toString() +
                    "}";
        }
    }

    // WARNING: assert requireSize % WListServer.FileTransferBufferSize == 0; (expected last chunk)
    public static @NotNull List<@NotNull ConsumerE<@NotNull ByteBuf>> splitUploadMethod(final @NotNull ConsumerE<? super @NotNull ByteBuf> sourceMethod, final int requireSize) {
        assert requireSize > 0;
        final int mod = requireSize % WListServer.FileTransferBufferSize;
        final int count = requireSize / WListServer.FileTransferBufferSize - (mod == 0 ? 1 : 0);
        final int rest = mod == 0 ? WListServer.FileTransferBufferSize : mod;
        final List<ConsumerE<ByteBuf>> list = new ArrayList<>(count + 1);
        final ByteBuf[] cacher = new ByteBuf[count + 1];
        final AtomicInteger countDown = new AtomicInteger(count);
        for (int i = 0; i < count + 1; ++i) {
            final int c = i;
            list.add(b -> {
                if (c == count)
                    assert b.readableBytes() == rest;
                else
                    assert b.readableBytes() == WListServer.FileTransferBufferSize;
                cacher[c] = b;
                if (countDown.getAndDecrement() == 0) {
                    final CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer(count + 1).addComponents(true, cacher);
                    try {
                        sourceMethod.accept(buf);
                    } finally {
                        buf.release();
                    }
                }
            });
        }
        return list;
    }
}
