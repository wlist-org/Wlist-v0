package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.ConstantSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.DelayQueueInByteBufOutByteBuf;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class FileUploadIdHelper {
    private FileUploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull UploadMethods methods, final @NotNull String tag, final @NotNull String username) {
        //noinspection IOResourceOpenedButNotSafelyClosed , resource // In cleaner.
        return new UploaderData(methods, tag, username).id;
    }

    public static boolean cancel(final @NotNull String id, final @NotNull String username) throws IOException {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return false;
        data.close();
        return true;
    }

    // TODO check chunk.
    public static @Nullable SupplierE<@Nullable FileSqlInformation> upload(final @NotNull String id, final @NotNull String username, final @NotNull ByteBuf buf, final int chunk) throws Exception {
        final UploaderData data = FileUploadIdHelper.buffers.get(id);
        if (data == null || !data.username.equals(username))
            return null;
        return data.put(buf, chunk) ? data::tryGet : null;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static class UploaderData implements Closeable {
        private final @NotNull String username;
        private final @NotNull UploadMethods methods;
        private final @NotNull AtomicInteger indexer = new AtomicInteger(0);
        private final @NotNull DelayQueueInByteBufOutByteBuf bufferQueue = new DelayQueueInByteBufOutByteBuf();
        private final @NotNull String id;
        private final @NotNull String tag;
        private @NotNull LocalDateTime expireTime = LocalDateTime.now();
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
        private final @NotNull AtomicBoolean finished = new AtomicBoolean(false);
        private final @NotNull MessageDigest md5;
        private final @NotNull AtomicInteger getLock = new AtomicInteger(0);

        private void appendExpireTime() {
            this.expireTime = LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().idIdleExpireTime());
            FileUploadIdHelper.checkTime.add(Pair.ImmutablePair.makeImmutablePair(this.expireTime, this));
        }

        private UploaderData(final @NotNull UploadMethods methods, final @NotNull String tag, final @NotNull String username) {
            super();
            this.username = username;
            this.methods = methods;
            this.tag = tag;
            this.id = MiscellaneousUtil.randomKeyAndPut(FileUploadIdHelper.buffers,
                    () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, ConstantSqlHelper.DefaultRandomChars), this);
            this.md5 = MiscellaneousUtil.getMd5Digester();
            this.appendExpireTime();
        }

        @Override
        public void close() throws IOException {
            if (this.closed.get())
                return;
            this.closed.set(true);
            FileUploadIdHelper.buffers.remove(this.id, this);
            try {
                this.methods.finisher().run();
            } finally {
                this.expireTime = LocalDateTime.now();
                this.bufferQueue.close();
            }
        }

        public boolean put(final @NotNull ByteBuf buf, final int chunk) throws Exception {
            if (this.closed.get())
                return false;
            this.appendExpireTime();
            return this.bufferQueue.put(buf, chunk);
        }

        public @Nullable FileSqlInformation tryGet() throws Exception {
            if (this.finished.get())
                return null;
            this.getLock.getAndIncrement();
            try {
                final Pair.ImmutablePair<Integer, ByteBuf> pair;
                synchronized (this.indexer) {
                    if (this.indexer.get() > this.methods.methods().size() - 1)
                        return null;
                    final int length = this.methods.methods().get(this.indexer.get()).size();
                    if (this.bufferQueue.readableBytes() < length)
                        return null;
                    pair = this.bufferQueue.get(length);
                    if (pair == null)
                        return null;
                    assert pair.getFirst().intValue() == this.indexer.get();
                    this.indexer.getAndIncrement();
                    try (final InputStream inputStream = new ByteBufInputStream(pair.getSecond().markReaderIndex())) {
                        MiscellaneousUtil.updateMessageDigest(this.md5, inputStream);
                    }
                    pair.getSecond().resetReaderIndex();
                }
                this.methods.methods().get(pair.getFirst().intValue()).consumer().accept(pair.getSecond());
                pair.getSecond().release();
                this.bufferQueue.discardSomeReadBytes();
                if (this.indexer.get() < this.methods.methods().size() - 1)
                    return null;
                this.finished.set(true);
                synchronized (this.getLock) {
                    while (this.getLock.get() > 1)
                        this.getLock.wait();
                }
                return this.finish();
            } finally {
                synchronized (this.getLock) {
                    this.getLock.getAndDecrement();
                    this.getLock.notify();
                }
            }
        }

        public @Nullable FileSqlInformation finish() throws Exception {
            try {
                final BigInteger i = new BigInteger(1, this.md5.digest());
                if (!this.tag.equals(String.format("%32s", i.toString(16)).replace(' ', '0')))
                    return null;
                return this.methods.supplier().get();
            } finally {
                this.close();
            }
        }

        @Override
        public @NotNull String toString() {
            return "UploaderData{" +
                    "username='" + this.username + '\'' +
                    ", methods=" + this.methods +
                    ", indexer=" + this.indexer +
                    ", bufferQueue=" + this.bufferQueue +
                    ", id='" + this.id + '\'' +
                    ", md5='" + this.tag + '\'' +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    ", md5=" + this.md5 +
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
