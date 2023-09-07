package com.xuxiaocheng.WList.Server.Handlers.Helpers;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DownloadIdHelper {
    private DownloadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull DownloaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull DownloadMethods methods, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed , resource // In cleaner.
        return new DownloaderData(methods, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) {
        final DownloaderData data = DownloadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    public static @Nullable ByteBuf download(final @NotNull String id, final @NotNull String username, final int chunk) throws Exception {
        final DownloaderData data = DownloadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return null;
        return data.get(chunk);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static class DownloaderData implements Closeable {
        private final @NotNull DownloadMethods methods;
        private final int count;
        private final @NotNull String username;
        private final @NotNull String id;
        private final int rest;
        private final @NotNull Collection<@NotNull Integer> calledSet = new HashSet<>();
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
        private final @NotNull ReadWriteLock closerLock = new ReentrantReadWriteLock();

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            DownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private DownloaderData(final @NotNull DownloadMethods methods, final @NotNull String username) {
            super();
            this.methods = methods;
            this.count = methods.methods().size();
            this.username = username;
            final int mod = (int) (methods.total() % NetworkTransmission.FileTransferBufferSize);
            this.rest = mod == 0 ? NetworkTransmission.FileTransferBufferSize : mod;
            this.id = MiscellaneousUtil.randomKeyAndPut(DownloadIdHelper.buffers,
                    () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, ConstantManager.DefaultRandomChars), this);
            if (methods.expireTime() != null)
                DownloadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(methods.expireTime(), this));
            this.appendExpireTime();
        }

        @Override
        public void close() {
            this.closerLock.writeLock().lock();
            try {
                if (!this.closed.compareAndSet(false, true))
                    return;
                DownloadIdHelper.buffers.remove(this.id, this);
                this.methods.finisher().run();
            } finally {
                this.closerLock.writeLock().unlock();
            }
        }

        public @Nullable ByteBuf get(final int chunk) throws Exception {
            boolean last = false;
            this.closerLock.readLock().lock();
            try {
                if (this.closed.get() || chunk >= this.count || chunk < 0)
                    return null;
                this.appendExpireTime();
                synchronized (this.calledSet) {
                    if (this.calledSet.contains(chunk))
                        return null;
                    this.calledSet.add(chunk);
                    if (this.calledSet.size() == this.count)
                        last = true;
                }
                final ByteBuf buffer = this.methods.methods().get(chunk).get();
                final int readableBytes = buffer.readableBytes();
                if (readableBytes != (chunk + 1 == this.count ? this.rest : NetworkTransmission.FileTransferBufferSize)) {
                    buffer.release();
                    throw new IllegalStateException("Invalid buffer size. readableBytes: " + readableBytes +
                            ", require: " + (chunk + 1 == this.count ? this.rest : NetworkTransmission.FileTransferBufferSize));
                }
                return buffer;
            } finally {
                this.closerLock.readLock().unlock();
                if (last)
                    this.close();
            }
        }

        @Override
        public @NotNull String toString() {
            return "DownloaderData{" +
                    "count=" + this.count +
                    ", username='" + this.username + '\'' +
                    ", id='" + this.id + '\'' +
                    ", rest=" + this.rest +
                    ", calledSet=" + this.calledSet +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    '}';
        }
    }

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull DownloaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, DownloaderData> check = DownloadIdHelper.checkTime.take();
                if (check.getSecond().closed.get())
                    continue;
                final LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(check.getFirst()))
                    TimeUnit.MILLISECONDS.sleep(Duration.between(now, check.getFirst()).toMillis());
                if (check.getSecond().closed.get())
                    continue;
                if (LocalDateTime.now().isAfter(check.getSecond().expireTime))
                    check.getSecond().close();
            } catch (final InterruptedException ignore) {
                break;
            }
        }
    }, "DownloadData Cleaner");
    static {
        DownloadIdHelper.cleaner.setDaemon(true);
        DownloadIdHelper.cleaner.start();
    }
}
