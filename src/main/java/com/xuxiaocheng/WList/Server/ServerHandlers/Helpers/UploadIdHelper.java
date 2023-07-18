package com.xuxiaocheng.WList.Server.ServerHandlers.Helpers;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class UploadIdHelper {
    private UploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull UploadMethods methods, final long size, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed , resource // In cleaner.
        return new UploaderData(methods, size, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) {
        final UploaderData data = UploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    public static @Nullable UnionPair<@NotNull FileSqlInformation, @NotNull Boolean> upload(final @NotNull String id, final @NotNull String username, final @NotNull ByteBuf buf, final int chunk) throws Exception {
        final UploaderData data = UploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return null;
        return data.put(buf, chunk) ? data.tryGet() : null;
    }

    private static class UploaderData implements Closeable {
        private final @NotNull UploadMethods methods;
        private final int count;
        private final @NotNull String username;
        private final @NotNull String id;
        private final int rest;
        private final @NotNull Collection<@NotNull Integer> calledSet = ConcurrentHashMap.newKeySet();
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
        private final @NotNull ReadWriteLock closerLock = new ReentrantReadWriteLock();

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            UploadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private UploaderData(final @NotNull UploadMethods methods, final long size, final @NotNull String username) {
            super();
            this.methods = methods;
            this.count = methods.methods().size();
            this.username = username;
            final int mod = (int) (size % WListServer.FileTransferBufferSize);
            this.rest = mod == 0 ? WListServer.FileTransferBufferSize : mod;
            this.id = MiscellaneousUtil.randomKeyAndPut(UploadIdHelper.buffers,
                    () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, ConstantManager.DefaultRandomChars), this);
            this.appendExpireTime();
        }

        @Override
        public void close() {
            this.closerLock.writeLock().lock();
            try {
                if (!this.closed.compareAndSet(false, true))
                    return;
                UploadIdHelper.buffers.remove(this.id, this);
                this.methods.finisher().run();
            } finally {
                this.closerLock.writeLock().unlock();
            }
        }

        public boolean put(final @NotNull ByteBuf buf, final int chunk) throws Exception {
            this.closerLock.readLock().lock();
            try {
                if (this.closed.get() || chunk >= this.count || chunk < 0)
                    return false;
                this.appendExpireTime();
                if (buf.readableBytes() != (chunk + 1 == this.count ? this.rest : WListServer.FileTransferBufferSize))
                    return false;
                synchronized (this.calledSet) {
                    if (this.calledSet.contains(chunk))
                        return false;
                    this.calledSet.add(chunk);
                }
                this.methods.methods().get(chunk).accept(buf.retain());
                return true;
            } finally {
                this.closerLock.readLock().unlock();
            }
        }

        public @Nullable FileSqlInformation finish() throws Exception {
            this.closerLock.writeLock().lock();
            try {
                if (!this.closed.compareAndSet(false, true))
                    return null;
                return this.methods.supplier().get();
            } finally {
                this.closerLock.writeLock().unlock();
                this.close();
            }
        }

        public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull Boolean> tryGet() throws Exception {
            this.closerLock.readLock().lock();
            try {
                if (this.closed.get() || this.calledSet.size() < this.count)
                    return UnionPair.fail(false);
            } finally {
                this.closerLock.readLock().unlock();
            }
            final FileSqlInformation information = this.finish();
            return information == null ? UnionPair.fail(true) : UnionPair.ok(information);
        }

        @Override
        public @NotNull String toString() {
            return "UploaderData{" +
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

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull UploaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, UploaderData> check = UploadIdHelper.checkTime.take();
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
    }, "UploadData Cleaner");
    static {
        UploadIdHelper.cleaner.setDaemon(true);
        UploadIdHelper.cleaner.start();
    }
}