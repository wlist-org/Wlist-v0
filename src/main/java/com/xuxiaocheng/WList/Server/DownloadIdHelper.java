package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class DownloadIdHelper {
    private DownloadIdHelper() {
        super();
    }

    public static final int BufferSize = 4 << 20;

    private static final @NotNull Map<@NotNull Long, @NotNull DownloaderData> buffers = new ConcurrentHashMap<>();

    public static long generateId(final @NotNull InputStream inputStream) {
        final DownloaderData data = new DownloaderData(inputStream);
        long id = HRandomHelper.DefaultSecureRandom.nextLong();
        for (; ; id = HRandomHelper.DefaultSecureRandom.nextLong()) {
            final boolean[] flag = {false};
            DownloadIdHelper.buffers.computeIfAbsent(id, (i) -> {
                flag[0] = true;
                return data;
            });
            if (flag[0])
                break;
        }
        return id;
    }

    public static boolean cancel(final long id) {
        final DownloaderData data = DownloadIdHelper.buffers.remove(id);
        if (data == null)
            return false;
        data.cancel();
        return true;
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> download(final long id) throws InterruptedException, IOException, ExecutionException {
        final DownloaderData data = DownloadIdHelper.buffers.get(id);
        if (data == null)
            return null;
        final Pair.ImmutablePair<Integer, ByteBuf> buf = data.get();
        if (buf == null)
            DownloadIdHelper.cancel(id);
        return buf;
    }

    private static class DownloaderData {
        private @NotNull Future<?> downloader;
        private volatile @NotNull LocalDateTime expireTime;
        private final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull ByteBuf>> bufferQueue = new LinkedBlockingQueue<>(3);
        private final int @NotNull [] counter = new int[] {0};
        private final @NotNull InputStream inputStream;

        private @NotNull CompletableFuture<?> newDownloader() {
            final @NotNull BlockingQueue<Pair.ImmutablePair<Integer, ByteBuf>> buffers = this.bufferQueue;
            return CompletableFuture.runAsync(() -> {
                ByteBuf buffer = null;
                try {
                    synchronized (DownloaderData.this.inputStream) {
                        final int len = Math.min(DownloaderData.this.inputStream.available(), DownloadIdHelper.BufferSize);
                        if (len < 1)
                            return;
                        buffer = ByteBufAllocator.DEFAULT.buffer(len, DownloadIdHelper.BufferSize);
                        buffer.writeBytes(DownloaderData.this.inputStream, len);
                        buffers.put(Pair.ImmutablePair.makeImmutablePair(this.counter[0]++, buffer)); // Still in synchronized block because of order.
                    }
                } catch (final IOException | InterruptedException exception) {
                    if (buffer != null)
                        buffer.release();
                    HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, exception);
                }
            }, WListServer.IOExecutors);
        }

        private DownloaderData(final @NotNull InputStream inputStream) {
            super();
            this.inputStream = inputStream;
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().getDownload_id_expire_time());
            DownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
            this.downloader = CompletableFuture.allOf(this.newDownloader(), this.newDownloader(), this.newDownloader());
        }

        public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> get() throws InterruptedException, IOException, ExecutionException {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().getDownload_id_expire_time());
            DownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
            final Pair.ImmutablePair<Integer, ByteBuf> pair;
            final boolean flag;
            synchronized (this) {
                // TODO can optimize.
                //noinspection LoopStatementThatDoesntLoop,ConstantConditions
                do {
                    if (!this.bufferQueue.isEmpty()) {
                        if (this.downloader.isDone() && this.inputStream.available() > 0)
                            this.downloader = this.newDownloader();
                        pair = this.bufferQueue.take();
                        break;
                    }
                    if (this.downloader.isDone()) {
                        if (this.inputStream.available() <= 0 && this.bufferQueue.isEmpty()) {
                            pair = null;
                            break;
                        }
                        this.downloader = this.newDownloader();
                    }
                    try {
                        this.downloader.get();
                    } catch (final ExecutionException exception) {
                        throw new IOException(exception);
                    }
                    if (this.bufferQueue.isEmpty()) {
                        pair = null;
                        break;
                    }
                    this.downloader = this.newDownloader();
                    pair = this.bufferQueue.take();
                    break;
                } while (false);
                flag = pair != null && this.downloader.isDone() && this.bufferQueue.isEmpty();
            }
            if (flag)
                DownloadIdHelper.buffers.values().remove(this);
            return pair;
        }

        public void cancel() {
            synchronized (this) {
                this.downloader.cancel(true);
            }
            try {
                this.inputStream.close();
            } catch (final IOException ignore) {
            }
            while (!this.bufferQueue.isEmpty()) {
                try {
                    this.bufferQueue.take().getSecond().release();
                } catch (final InterruptedException ignore) {
                }
            }
        }

        @Override
        public synchronized @NotNull String toString() {
            return "DownloaderData{" +
                    "downloader=" + this.downloader +
                    ", queryTime=" + this.expireTime +
                    ", buffers=" + this.bufferQueue +
                    ", counter=" + this.counter[0] +
                    ", inputStream=" + this.inputStream +
                    '}';
        }
    }

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull DownloaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, DownloaderData> check = DownloadIdHelper.checkTime.take();
                final LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(check.getFirst()))
                    TimeUnit.MILLISECONDS.sleep(Duration.between(now, check.getFirst()).toMillis());
                if (LocalDateTime.now().isAfter(check.getSecond().expireTime)) {
                    check.getSecond().cancel();
                    DownloadIdHelper.buffers.values().remove(check.getSecond());
                }
            } catch (final InterruptedException ignore) {
                break;
            }
        }
    }, "Downloader Cleaner");
    static {
        DownloadIdHelper.cleaner.setDaemon(true);
        DownloadIdHelper.cleaner.start();
    }
}
