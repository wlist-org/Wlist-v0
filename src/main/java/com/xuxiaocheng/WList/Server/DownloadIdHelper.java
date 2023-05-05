package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public final class DownloadIdHelper {
    private DownloadIdHelper() {
        super();
    }

    public static final int BufferSize = 3;

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
        data.downloader.cancel(true);
        try {
            data.inputStream.close();
        } catch (final IOException ignore) {
        }
        while (!data.bufferQueue.isEmpty()) {
            try {
                data.bufferQueue.take().release();
            } catch (final InterruptedException ignore) {
            }
        }
        return true;
    }

    public static @Nullable ByteBuf download(final long id) throws InterruptedException, IOException, ExecutionException {
        final DownloaderData data = DownloadIdHelper.buffers.get(id);
        if (data == null)
            return null;
        final ByteBuf buf = data.get();
        if (buf == null)
            DownloadIdHelper.cancel(id);
        return buf;
    }

    private static class DownloaderData {
        private @NotNull Future<?> downloader;
        private @NotNull LocalDateTime queryTime;
        private final @NotNull BlockingQueue<ByteBuf> bufferQueue = new LinkedBlockingQueue<>(3);
        private final @NotNull InputStream inputStream;

        private @NotNull CompletableFuture<?> newDownloader() {
            return CompletableFuture.runAsync(new Downloader(this.inputStream, this.bufferQueue), WListServer.IOExecutors);
        }

        private DownloaderData(final @NotNull InputStream inputStream) {
            super();
            this.inputStream = inputStream;
            this.queryTime = LocalDateTime.now();
            this.downloader = CompletableFuture.allOf(this.newDownloader(), this.newDownloader(), this.newDownloader());
        }

        private synchronized @Nullable ByteBuf get() throws InterruptedException, IOException, ExecutionException {
            this.queryTime = LocalDateTime.now();
            if (!this.bufferQueue.isEmpty()) {
                if (this.downloader.isDone() && this.inputStream.available() > 0)
                    this.downloader = this.newDownloader();
                return this.bufferQueue.take();
            }
            if (this.downloader.isDone()) {
                if (this.inputStream.available() <= 0 && this.bufferQueue.isEmpty())
                    return null;
                this.downloader = this.newDownloader();
            }
            this.downloader.get();
            if (this.bufferQueue.isEmpty())
                return null;
            final ByteBuf buf = this.bufferQueue.take();
            this.downloader = this.newDownloader();
            return buf;
        }

        @Override
        public synchronized @NotNull String toString() {
            return "DownloaderData{" +
                    "downloader=" + this.downloader +
                    ", queryTime=" + this.queryTime +
                    ", buffers=" + this.bufferQueue +
                    ", inputStream=" + this.inputStream +
                    '}';
        }
    }

    private record Downloader(@NotNull InputStream stream, @NotNull BlockingQueue<ByteBuf> buffers) implements Runnable {
        @Override
        public void run() {
            ByteBuf buffer = null;
            try {
                synchronized (this.stream) {
                    final int len = Math.min(this.stream.available(), DownloadIdHelper.BufferSize);
                    if (len < 1)
                        return;
                    buffer = ByteBufAllocator.DEFAULT.buffer(len, DownloadIdHelper.BufferSize);
                    buffer.writeBytes(this.stream, len);
                    this.buffers.put(buffer);
                }
            } catch (final IOException | InterruptedException exception) {
                if (buffer != null)
                    buffer.release();
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, exception);
            }
        }
    }
}
