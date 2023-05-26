package com.xuxiaocheng.WList.Driver.Helpers;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import java.util.function.Function;
import java.util.regex.Pattern;

public final class DriverUtil {
    private DriverUtil() {
        super();
    }

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    public static @NotNull InputStream getDownloadStream(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IOException {
        if (from >= to)
            return InputStream.nullInputStream();
        final InputStream link = DriverNetworkHelper.extraResponse(DriverNetworkHelper.sendRequestJson(client, url, headers, body)).byteStream();
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

    public static Pair.@NotNull ImmutablePair<@NotNull InputStream, @NotNull Long> getDownloadStreamByRangeHeader(final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull Long> url, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final Headers.@Nullable Builder builder) throws IOException {
        final long size = url.getSecond().longValue();
        final long end = Math.min(to, size);
        final long len = end - from;
        if (from >= size || len < 0)
            return Pair.ImmutablePair.makeImmutablePair(InputStream.nullInputStream(), 0L);
        return Pair.ImmutablePair.makeImmutablePair(DriverUtil.getDownloadStream(DriverNetworkHelper.httpClient,
                Pair.ImmutablePair.makeImmutablePair(url.getFirst(), "GET"),
                Objects.requireNonNullElseGet(builder, Headers.Builder::new).set("Range", String.format("bytes=%d-%d", from, end - 1)).build(),
                null, 0, len), len);
    }

    public static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull FileSqlInformation>, @NotNull Runnable> wrapAllFilesListerInPages(final @NotNull FunctionE<? super @NotNull Integer, ? extends Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>>> fileSupplierInPage, final @NotNull Function<? super @NotNull Long, @NotNull Integer> pageCountCalculator, final @NotNull Consumer<? super @Nullable Exception> finisher, final @NotNull ExecutorService threadPool) {
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> firstPage;
        try {
            firstPage = fileSupplierInPage.apply(0);
        } catch (final Exception exception) {
            finisher.accept(exception);
            return Triad.ImmutableTriad.makeImmutableTriad(0L, MiscellaneousUtil.getEmptyIterator(), () -> {});
        }
        final long fileCount = firstPage.getFirst().intValue();
        if (fileCount <= 0 || firstPage.getSecond().isEmpty()) {
            finisher.accept(null);
            return Triad.ImmutableTriad.makeImmutableTriad(0L, MiscellaneousUtil.getEmptyIterator(), () -> {});
        }
        final int pageCount = pageCountCalculator.apply(fileCount).intValue();
        if (pageCount <= 1) {
            finisher.accept(null);
            return Triad.ImmutableTriad.makeImmutableTriad(fileCount, firstPage.getSecond().iterator(), () -> {});
        }
        final BlockingQueue<FileSqlInformation> allFiles = new LinkedBlockingQueue<>(Math.max((int) fileCount, firstPage.getSecond().size()));
        allFiles.addAll(firstPage.getSecond());
        final AtomicInteger finishedPage = new AtomicInteger(0);
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
                    allFiles.addAll(infos.getSecond());
                    futures[current - 1].complete(null);
                    if (finishedPage.incrementAndGet() == pageCount && calledFinisher.compareAndSet(false, true))
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
        return Triad.ImmutableTriad.makeImmutableTriad(fileCount, MiscellaneousUtil.wrapBlockingQueueCounted(allFiles, fileCount,
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
}
