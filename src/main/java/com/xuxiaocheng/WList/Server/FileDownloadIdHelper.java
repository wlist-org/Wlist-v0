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

public final class FileDownloadIdHelper {
    private FileDownloadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull DownloaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull InputStream inputStream) {
        final DownloaderData data = new DownloaderData(inputStream);
        String id;
        while (true) {
            //noinspection SpellCheckingInspection
            id = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*,.?|");
            final boolean[] flag = {false};
            FileDownloadIdHelper.buffers.computeIfAbsent(id, (i) -> {
                flag[0] = true;
                return data;
            });
            if (flag[0])
                break;
        }
        data.id = id;
        return id;
    }

    public static boolean cancel(final @NotNull String id) {
        final DownloaderData data = FileDownloadIdHelper.buffers.remove(id);
        if (data == null)
            return false;
        data.cancel(true);
        return true;
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> download(final @NotNull String id) throws InterruptedException, IOException, ExecutionException {
        final DownloaderData data = FileDownloadIdHelper.buffers.get(id);
        if (data == null)
            return null;
        final Pair.ImmutablePair<Integer, ByteBuf> buf = data.get();
        if (buf == null)
            FileDownloadIdHelper.cancel(id);
        return buf;
    }

    private static class DownloaderData {
        private @NotNull String id = "";
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
                        final int len = Math.min(DownloaderData.this.inputStream.available(), WListServer.FileTransferBufferSize);
                        if (len < 1)
                            return;
                        buffer = ByteBufAllocator.DEFAULT.buffer(len, WListServer.FileTransferBufferSize);
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
            FileDownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
            this.downloader = CompletableFuture.allOf(this.newDownloader(), this.newDownloader(), this.newDownloader());
        }

        public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> get() throws InterruptedException, IOException, ExecutionException {
            assert !this.id.isEmpty();
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().getDownload_id_expire_time());
            FileDownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
            synchronized (this) {
                if ((this.downloader.isDone() || this.downloader.isCancelled()) && this.bufferQueue.isEmpty()) {
                    this.cancel(true);
                    return null;
                }
            }
            final Pair.ImmutablePair<Integer, ByteBuf> cached = this.bufferQueue.take();
            synchronized (this) {
                if (this.downloader.isDone() && this.inputStream.available() <= 0 && this.bufferQueue.isEmpty())
                    this.cancel(false);
                else if (this.downloader.isDone() && this.inputStream.available() > 0)
                    this.downloader = this.newDownloader();
            }
            return cached;
        }

        public void cancel(final boolean release) {
            assert !this.id.isEmpty();
            synchronized (this) {
                this.downloader.cancel(true);
                try {
                    this.downloader.get();
                } catch (final InterruptedException | ExecutionException ignore) {
                }
            }
            try {
                this.inputStream.close();
            } catch (final IOException ignore) {
            }
            FileDownloadIdHelper.buffers.remove(this.id, this);
            if (release)
                while (!this.bufferQueue.isEmpty()) {
                    try {
                        final Pair.ImmutablePair<Integer, ByteBuf> t = this.bufferQueue.poll(0L, TimeUnit.NANOSECONDS);
                        if (t == null)
                            break;
                        t.getSecond().release();
                    } catch (final InterruptedException ignore) {
                    }
                }
        }

        @Override
        public @NotNull String toString() {
            final String id = this.id;
            synchronized (this) {
                return "DownloaderData{" +
                        "id=" + id +
                        ", downloader=" + this.downloader +
                        ", queryTime=" + this.expireTime +
                        ", buffers=" + this.bufferQueue +
                        ", counter=" + this.counter[0] +
                        ", inputStream=" + this.inputStream +
                        '}';
            }
        }
    }

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull DownloaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, DownloaderData> check = FileDownloadIdHelper.checkTime.take();
                final LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(check.getFirst()))
                    TimeUnit.MILLISECONDS.sleep(Duration.between(now, check.getFirst()).toMillis());
                if (LocalDateTime.now().isAfter(check.getSecond().expireTime))
                    check.getSecond().cancel(true);
            } catch (final InterruptedException ignore) {
                break;
            }
        }
    }, "Downloader Cleaner");
    static {
        FileDownloadIdHelper.cleaner.setDaemon(true);
        FileDownloadIdHelper.cleaner.start();
    }
}
