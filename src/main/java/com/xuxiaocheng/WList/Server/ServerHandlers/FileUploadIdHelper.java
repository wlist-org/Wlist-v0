package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class FileUploadIdHelper {
    private FileUploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final long size, final @NotNull String tag, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed // In cleaner.
        return new UploaderData(size, tag, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    public static boolean upload(final @NotNull String id, final @NotNull String username, final @NotNull ByteBuf buf) {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        return data.put(buf);
    }

    private static class UploaderData implements Closeable {
        private final @NotNull String username;
        private final long size;
        private long read;
        private final @NotNull String tag;
        private final @NotNull String id;
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private boolean closed = false;

        private final @NotNull BlockingQueue<@NotNull ByteBuf> bufferQueue = new LinkedBlockingQueue<>();
        private final @NotNull MessageDigest md5;

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            FileUploadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private UploaderData(final long size, final @NotNull String tag, final @NotNull String username) {
            super();
            this.username = username;
            this.size = size;
            this.tag = tag;
            try {
                this.md5 = MessageDigest.getInstance("MD5");
            } catch (final NoSuchAlgorithmException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            this.id = MiscellaneousUtil.randomKeyAndPut(FileUploadIdHelper.buffers, () -> {
                //noinspection SpellCheckingInspection
                return HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-_=+[]{}\\|;:,.<>/?");
            }, this);
        }

        @Override
        public void close() {
            if (this.closed)
                return;
            this.closed = true;
            FileUploadIdHelper.buffers.remove(this.id, this);
            this.expireTime = LocalDateTime.now();
            while (!this.bufferQueue.isEmpty()) {
                try {
                    final ByteBuf buf = this.bufferQueue.poll(0L, TimeUnit.NANOSECONDS);
                    if (buf == null)
                        break;
                    buf.release();
                } catch (final InterruptedException ignore) {
                }
            }
        }

        public synchronized boolean put(final @NotNull ByteBuf buf) {
            if (this.closed)
                return false;

        }
    }

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull UploaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, UploaderData> check = FileUploadIdHelper.checkTime.take();
                final LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(check.getFirst()))
                    TimeUnit.MILLISECONDS.sleep(Duration.between(now, check.getFirst()).toMillis());
                if (LocalDateTime.now().isAfter(check.getSecond().expireTime))
                    check.getSecond().close();
            } catch (final InterruptedException ignore) {
                break;
            }
        }
    }, "UploadData Cleaner");
    static {
        FileUploadIdHelper.cleaner.setDaemon(true);
        FileUploadIdHelper.cleaner.start();
    }
}
