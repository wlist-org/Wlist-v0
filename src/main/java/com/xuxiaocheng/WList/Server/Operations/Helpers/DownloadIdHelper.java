package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class DownloadIdHelper {
    private DownloadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull DownloadRequirements> requirements = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull DownloadRequirements requirements) {
        final String id = MiscellaneousUtil.randomKeyAndPut(DownloadIdHelper.requirements, IdsHelper::randomTimerId, requirements);
        IdsHelper.CleanerExecutors.schedule(() -> DownloadIdHelper.requirements.remove(id, requirements), ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS);
        return id;
    }

    public static boolean cancel(final @NotNull String id) {
        return DownloadIdHelper.requirements.remove(id) != null;
    }

    private static final @NotNull Map<@NotNull String, @NotNull DownloaderData> data = new ConcurrentHashMap<>();

    public static DownloadRequirements.@Nullable DownloadMethods confirm(final @NotNull String id) throws Exception {
        final DownloadRequirements requirements = DownloadIdHelper.requirements.remove(id);
        if (requirements == null)
            return null;
        final DownloadRequirements.DownloadMethods methods = requirements.supplier().get();
        DownloadIdHelper.data.put(id, new DownloaderData(id, methods));
        return methods;
    }

    public static void download(final @NotNull String id, final int index, final @NotNull Consumer<@Nullable UnionPair<ByteBuf, Throwable>> consumer) {
        final DownloaderData data = DownloadIdHelper.data.get(id);
        if (data == null) {
            consumer.accept(null);
            return;
        }
        data.get(index, consumer);
    }

    public static boolean finish(final @NotNull String id) {
        final DownloaderData data = DownloadIdHelper.data.get(id);
        if (data == null)
            return false;
        data.close();
        return true;
    }

    private static class DownloaderData implements Closeable {
        private final @NotNull String id;
        private final DownloadRequirements.@NotNull DownloadMethods methods;
        private final DownloadRequirements.@Nullable OrderedNode @NotNull [] nodes;
        private final @NotNull Object @NotNull [] locks;
        private final AtomicInteger counter = new AtomicInteger();
        private @NotNull ZonedDateTime expireTime;
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);

        private DownloaderData(final @NotNull String id, final DownloadRequirements.@NotNull DownloadMethods methods) {
            super();
            this.id = id;
            this.methods = methods;
            this.nodes = new DownloadRequirements.OrderedNode[this.methods.parallelMethods().size()];
            this.locks = new Object[this.methods.parallelMethods().size()];
            for (int i = 0; i < this.methods.parallelMethods().size(); ++i) {
                this.nodes[i] = this.methods.parallelMethods().get(i).suppliersLink();
                this.locks[i] = new Object();
            }
            this.counter.set(this.methods.parallelMethods().size());
            if (this.methods.expireTime() == null) {
                this.expireTime = MiscellaneousUtil.now().plusSeconds(ServerConfiguration.get().idIdleExpireTime());
                IdsHelper.CleanerExecutors.schedule(() -> {
                    if (MiscellaneousUtil.now().isAfter(this.expireTime))
                        this.close();
                }, ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS).addListener(MiscellaneousUtil.exceptionListener());
            } else {
                this.expireTime = this.methods.expireTime();
                IdsHelper.CleanerExecutors.schedule(this::close, Duration.between(MiscellaneousUtil.now(), this.expireTime).toSeconds(), TimeUnit.SECONDS).addListener(MiscellaneousUtil.exceptionListener());
            }
        }

        @Override
        public void close() {
            if (!this.closed.compareAndSet(false, true))
                return;
            //noinspection resource
            final DownloaderData old = DownloadIdHelper.data.remove(this.id);
            assert old == this;
            this.methods.finisher().run();
        }

        public void get(final int index, final @NotNull Consumer<@Nullable UnionPair<ByteBuf, Throwable>> consumer) {
            if (this.closed.get() || index >= this.methods.parallelMethods().size()) {
                consumer.accept(null);
                return;
            }
            if (this.methods.expireTime() == null) {
                this.expireTime = MiscellaneousUtil.now().plusSeconds(ServerConfiguration.get().idIdleExpireTime());
                IdsHelper.CleanerExecutors.schedule(() -> {
                    if (MiscellaneousUtil.now().isAfter(this.expireTime))
                        this.close();
                }, ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS).addListener(MiscellaneousUtil.exceptionListener());
            }
            synchronized (this.locks[index]) {
                if (this.nodes[index] == null) {
                    consumer.accept(null);
                    return;
                }
                this.nodes[index] = this.nodes[index].apply(consumer);
                if (this.nodes[index] == null && this.counter.getAndDecrement() == 0)
                    IdsHelper.CleanerExecutors.execute(this::close);
            }
        }

        @Override
        public @NotNull String toString() {
            return "DownloaderData{" +
                    "id='" + this.id + '\'' +
                    ", methods=" + this.methods +
                    ", nodes=" + Arrays.toString(this.nodes) +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    '}';
        }
    }
}
