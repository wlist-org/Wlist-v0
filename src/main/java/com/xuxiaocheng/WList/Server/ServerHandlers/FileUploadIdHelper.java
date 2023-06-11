package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FileUploadIdHelper {
    private FileUploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull UploadMethods methods, final long size, final @NotNull String md5, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed , resource // In cleaner.
        return new UploaderData(methods, size, md5, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) throws IOException {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    public static @Nullable UnionPair<@NotNull FileSqlInformation, @NotNull Boolean> upload(final @NotNull String id, final @NotNull String username, final @NotNull ByteBuf buf, final int chunk) throws Exception {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return null;
        return data.put(buf, chunk) ? data.tryGet() : null;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static class UploaderData implements Closeable {
        private final @NotNull UploadMethods methods;
        private final long count;
        private final @NotNull String md5;
        private final @NotNull String username;
        private final @NotNull String id;
        private final @NotNull MessageDigest digest = MiscellaneousUtil.getMd5Digester();
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
        private final @NotNull ReadWriteLock closerLock = new ReentrantReadWriteLock();
        private final @NotNull AtomicInteger indexer = new AtomicInteger(0);
        private final @NotNull Map<@NotNull Integer, @NotNull ByteBuf> cacheTree = new ConcurrentSkipListMap<>();

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            FileUploadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private UploaderData(final @NotNull UploadMethods methods, final long size, final @NotNull String md5, final @NotNull String username) {
            super();
            this.username = username;
            this.methods = methods;
            this.count = MiscellaneousUtil.calculatePartCount(size, WListServer.FileTransferBufferSize);
            this.md5 = md5;
            this.id = MiscellaneousUtil.randomKeyAndPut(FileUploadIdHelper.buffers,
                    () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, ConstantManager.DefaultRandomChars), this);
            this.appendExpireTime();
        }

        @Override
        public void close() throws IOException {
            this.closerLock.writeLock().lock();
            try {
                if (!this.closed.compareAndSet(false, true))
                    return;
                FileUploadIdHelper.buffers.remove(this.id, this);
                this.methods.finisher().run();
            } finally {
                this.closerLock.writeLock().unlock();
            }
        }

        public boolean put(final @NotNull ByteBuf buf, final int chunk) throws Exception {
            this.closerLock.readLock().lock();
            try {
                if (this.closed.get() || chunk < this.indexer.get() || this.count <= chunk)
                    return false;
                this.appendExpireTime();
                if (this.cacheTree.putIfAbsent(chunk, buf) != null)
                    return false;
                buf.retain();
                final boolean[] flag = {false};
                try {
                    do {
                        flag[0] = false;
                        this.cacheTree.computeIfPresent(this.indexer.get(), HExceptionWrapper.wrapBiFunction((k, o) -> {
                            if (!this.indexer.compareAndSet(k.intValue(), k.intValue() + 1))
                                //noinspection ConstantConditions,ReturnOfNull
                                return null;
                            // TODO check chunk size.
                            try (final InputStream inputStream = new ByteBufInputStream(o.markReaderIndex())) {
                                MiscellaneousUtil.updateMessageDigest(this.digest, inputStream);
                            }
                            o.resetReaderIndex();
                            try {
                                this.methods.methods().get(k.intValue()).accept(o);
                            } finally {
                                o.release();
                            }
                            flag[0] = true;
                            //noinspection ConstantConditions,ReturnOfNull
                            return null;
                        }));
                    } while (flag[0]);
                } catch (final RuntimeException exception) {
                    throw HExceptionWrapper.unwrapException(exception, Exception.class);
                }
                return true;
            } finally {
                this.closerLock.readLock().unlock();
            }
        }

        public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull Boolean> tryGet() throws Exception {
            this.closerLock.readLock().lock();
            try {
                if (this.closed.get() || !this.cacheTree.isEmpty() || this.indexer.get() < this.count)
                    return UnionPair.fail(false);
                try {
                    if (!this.md5.equals(MiscellaneousUtil.getMd5(this.digest)))
                        return UnionPair.fail(true);
                    final FileSqlInformation information = this.methods.supplier().get();
                    return information == null ? UnionPair.fail(true) : UnionPair.ok(information);
                } finally {
                    this.close();
                }
            } finally {
                this.closerLock.readLock().unlock();
            }
        }

        @Override
        public @NotNull String toString() {
            return "UploaderData{" +
                    "methods=" + this.methods +
                    ", count=" + this.count +
                    ", md5='" + this.md5 + '\'' +
                    ", username='" + this.username + '\'' +
                    ", id='" + this.id + '\'' +
                    ", digest=" + this.digest +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    ", closerLock=" + this.closerLock +
                    ", indexer=" + this.indexer +
                    ", cacheTree=" + this.cacheTree +
                    '}';
        }
    }

    private static final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull LocalDateTime, @NotNull UploaderData>> checkTime = new LinkedBlockingQueue<>();
    public static final Thread cleaner = new Thread(() -> {
        while (true) {
            try {
                final Pair.ImmutablePair<LocalDateTime, UploaderData> check = FileUploadIdHelper.checkTime.take();
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
            } catch (final IOException exception) {
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, exception);
            }
        }
    }, "UploadData Cleaner");
    static {
        FileUploadIdHelper.cleaner.setDaemon(true);
        FileUploadIdHelper.cleaner.start();
    }
}
