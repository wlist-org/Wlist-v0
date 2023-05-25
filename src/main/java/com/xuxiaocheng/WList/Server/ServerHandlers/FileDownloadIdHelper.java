package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.ConstantSqlHelper;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.DelayQueueInStreamOutByteBuf;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class FileDownloadIdHelper {
    private FileDownloadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull DownloaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull InputStream inputStream, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed // In cleaner.
        return new DownloaderData(inputStream, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) {
        final DownloaderData data = FileDownloadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> download(final @NotNull String id, final @NotNull String username) throws InterruptedException, IOException, ExecutionException {
        final DownloaderData data = FileDownloadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return null;
        return data.get();
    }

    private static class DownloaderData implements Closeable {
        private final @NotNull String username;
        private final @NotNull DelayQueueInStreamOutByteBuf queue;
        private final @NotNull String id;
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private boolean closed = false;

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            FileDownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private DownloaderData(final @NotNull InputStream inputStream, final @NotNull String username) {
            super();
            this.username = username;
            this.queue = new DelayQueueInStreamOutByteBuf(inputStream, WListServer.IOExecutors,
                    WListServer.FileTransferBufferSize, 3,
                    e -> HLog.getInstance("ServerLogger").log(HLogLevel.ERROR, e));
            this.id = MiscellaneousUtil.randomKeyAndPut(FileDownloadIdHelper.buffers,
                    () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, ConstantSqlHelper.DefaultRandomChars), this);
            this.appendExpireTime();
            this.queue.start();
        }

        public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> get() throws InterruptedException, IOException {
            if (this.closed)
                return null;
            this.appendExpireTime();
            final Pair.ImmutablePair<Integer, ByteBuf> t = this.queue.get();
            if (t == null || this.queue.getInputStream().available() <= 0)
                this.queue.getInputStream().close();
            return t;
        }

        @Override
        public void close() {
            if (this.closed)
                return;
            this.closed = true;
            FileDownloadIdHelper.buffers.remove(this.id, this);
            this.queue.close();
            this.expireTime = LocalDateTime.now();
        }

        @Override
        public @NotNull String toString() {
            return "DownloaderData{" +
                    "username='" + this.username + '\'' +
                    ", queue=" + this.queue +
                    ", id='" + this.id + '\'' +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    '}';
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
                    check.getSecond().close();
            } catch (final InterruptedException ignore) {
                break;
            }
        }
    }, "DownloadData Cleaner");
    static {
        FileDownloadIdHelper.cleaner.setDaemon(true);
        FileDownloadIdHelper.cleaner.start();
    }
}
