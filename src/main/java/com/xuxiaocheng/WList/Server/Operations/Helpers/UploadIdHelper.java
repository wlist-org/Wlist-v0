package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class UploadIdHelper {
    private UploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploadRequirements> requirements = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull UploadRequirements requirements) {
        requirements.checksums().forEach(c -> {
            assert c.start() < c.end();
            UploadChecksum.requireRegisteredAlgorithm(c.algorithm());
        });
        final String id = MiscellaneousUtil.randomKeyAndPut(UploadIdHelper.requirements, IdsHelper::randomTimerId, requirements);
        IdsHelper.CleanerExecutors.schedule(() -> UploadIdHelper.requirements.remove(id, requirements), ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS)
                .addListener(IdsHelper.noCancellationExceptionListener());
        return id;
    }

    public static boolean cancel(final @NotNull String id) {
        final UploadRequirements requirements = UploadIdHelper.requirements.remove(id);
        if (requirements == null)
            return false;
        if (requirements.canceller() != null)
            requirements.canceller().run();
        return true;
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> data = new ConcurrentHashMap<>();

    private static final @NotNull UnionPair<UploadRequirements.UploadMethods, Integer> ChecksumsInvalidSize = UnionPair.fail(-1);
    public static @Nullable UnionPair<UploadRequirements.UploadMethods, Integer> confirm(final @NotNull String id, final @NotNull List<@NotNull String> checksums) throws Exception {
        final UploadRequirements requirements = UploadIdHelper.requirements.remove(id);
        if (requirements == null)
            return null;
        if (requirements.checksums().size() != checksums.size())
            return UploadIdHelper.ChecksumsInvalidSize;
        final Iterator<UploadChecksum> require = requirements.checksums().iterator();
        final Iterator<String> checker = checksums.iterator();
        int i = 0;
        while (require.hasNext() && checker.hasNext()) {
            final Pattern pattern = UploadChecksum.getAlgorithm(require.next().algorithm()).pattern();
            final String checksum = checker.next();
            if (!pattern.matcher(checksum).matches())
                return UnionPair.fail(i);
            ++i;
        }
        assert !require.hasNext() && !checker.hasNext();
        final UploadRequirements.UploadMethods methods = requirements.transfer().apply(checksums);
        UploadIdHelper.data.put(id, new UploaderData(id, methods));
        return UnionPair.ok(methods);
    }

    public static boolean upload(final @NotNull String id, final @NotNull ByteBuf buf, final int index, final @NotNull Consumer<? super @Nullable Throwable> consumer) {
        final UploaderData data = UploadIdHelper.data.get(id);
        if (data == null)
            return false;
        return data.put(buf, index, consumer);
    }

    private static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> FinishNoId = UnionPair.ok(UnionPair.fail(Boolean.FALSE));
    private static final @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable> FinishFailure = UnionPair.ok(UnionPair.fail(Boolean.TRUE));
    public static void finish(final @NotNull String id, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) {
        final UploaderData data = UploadIdHelper.data.get(id);
        if (data == null) {
            consumer.accept(UploadIdHelper.FinishNoId);
            return;
        }
        data.finishAndClose(consumer);
    }

    private static class UploaderData implements AutoCloseable {
        private final @NotNull String id;
        private final UploadRequirements.@NotNull UploadMethods methods;
        private final @NotNull ProgressBar progress;
        private final UploadRequirements.@Nullable OrderedNode @NotNull [] nodes;
        private final @NotNull Object @NotNull [] locks;
        private final AtomicInteger counter = new AtomicInteger();
        private @NotNull ZonedDateTime expireTime;
        private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);

        private UploaderData(final @NotNull String id, final UploadRequirements.@NotNull UploadMethods methods) {
            super();
            this.id = id;
            this.methods = methods;
            this.progress = IdsHelper.setProgressBar(id);
            for (final UploadRequirements.OrderedConsumers parallel: this.methods.parallelMethods())
                this.progress.addStage(parallel.end() - parallel.start());
            this.nodes = new UploadRequirements.OrderedNode[this.methods.parallelMethods().size()];
            this.locks = new Object[this.methods.parallelMethods().size()];
            for (int i = 0; i < this.methods.parallelMethods().size(); ++i) {
                this.nodes[i] = this.methods.parallelMethods().get(i).consumersLink();
                this.locks[i] = new Object();
            }
            this.counter.set(this.methods.parallelMethods().size());
            this.expireTime = MiscellaneousUtil.now().plusSeconds(ServerConfiguration.get().idIdleExpireTime());
            IdsHelper.CleanerExecutors.schedule(() -> {
                if (MiscellaneousUtil.now().isAfter(this.expireTime))
                    this.close();
            }, ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS)
                    .addListener(IdsHelper.noCancellationExceptionListener());
        }

        @Override
        public void close() {
            if (!this.closed.compareAndSet(false, true))
                return;
            //noinspection resource
            final UploaderData old = UploadIdHelper.data.remove(this.id);
            assert old == this;
            IdsHelper.removeProgressBar(this.id);
            this.methods.finisher().run();
        }

        public boolean put(final @NotNull ByteBuf buf, final int index, final @NotNull Consumer<? super @Nullable Throwable> consumer) {
            if (this.closed.get() || index >= this.methods.parallelMethods().size())
                return false;
            this.expireTime = MiscellaneousUtil.now().plusSeconds(ServerConfiguration.get().idIdleExpireTime());
            IdsHelper.CleanerExecutors.schedule(() -> {
                if (MiscellaneousUtil.now().isAfter(this.expireTime))
                    this.close();
            }, ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS)
                    .addListener(IdsHelper.noCancellationExceptionListener());
            synchronized (this.locks[index]) {
                if (this.nodes[index] == null)
                    return false;
                this.nodes[index] = this.nodes[index].apply(buf, consumer, delta -> this.progress.progress(index, delta));
                if (this.nodes[index] == null)
                    this.counter.getAndDecrement();
            }
            return true;
        }

        public void finishAndClose(final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) {
            if (this.closed.get()) {
                consumer.accept(UploadIdHelper.FinishNoId);
                return;
            }
            try {
                if (this.counter.get() > 0) {
                    consumer.accept(UploadIdHelper.FinishFailure);
                    return;
                }
                this.methods.supplier().accept((Consumer<? super UnionPair<Optional<FileInformation>, Throwable>>) p -> {
                    if (p.isFailure()) {
                        consumer.accept(UnionPair.fail(p.getE()));
                        return;
                    }
                    //noinspection SimplifyOptionalCallChains
                    if (!p.getT().isPresent()) { // AndroidSupporter
                        consumer.accept(UploadIdHelper.FinishFailure);
                        return;
                    }
                    consumer.accept(UnionPair.ok(UnionPair.ok(p.getT().get())));
                });
            } catch (final Throwable exception) {
                consumer.accept(UnionPair.fail(exception));
            } finally {
                IdsHelper.CleanerExecutors.submit(this::close).addListener(MiscellaneousUtil.exceptionListener());
            }
        }

        @Override
        public @NotNull String toString() {
            return "UploaderData{" +
                    "id='" + this.id + '\'' +
                    ", methods=" + this.methods +
                    ", nodes=" + Arrays.toString(this.nodes) +
                    ", expireTime=" + this.expireTime +
                    ", closed=" + this.closed +
                    '}';
        }
    }
}
