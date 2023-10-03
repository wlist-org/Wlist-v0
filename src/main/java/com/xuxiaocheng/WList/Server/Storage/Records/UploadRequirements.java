package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.BiConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Operations.Helpers.ProgressBar;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public record UploadRequirements(@NotNull @Unmodifiable List<@NotNull UploadChecksum> checksums,
                                 @NotNull FunctionE<@NotNull @Unmodifiable List<@NotNull String>, @NotNull UploadMethods> transfer,
                                 @Nullable Runnable canceller) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.UploadConfirm
     */
    @Contract("_, _ -> param1")
    public @NotNull ByteBuf dumpConfirm(final @NotNull ByteBuf buffer, final @NotNull String id) throws IOException {
        ByteBufIOUtil.writeVariableLenInt(buffer, this.checksums.size());
        for (final UploadChecksum checksum: this.checksums)
            checksum.dump(buffer);
        ByteBufIOUtil.writeUTF(buffer, id);
        return buffer;
    }

    public record UploadMethods(@NotNull @Unmodifiable List<@NotNull OrderedConsumers> parallelMethods,
                                @NotNull Consumer<@NotNull Consumer<? super @NotNull UnionPair<Optional<FileInformation>, Throwable>>> supplier,
                                @NotNull Runnable finisher) {
        /**
         * @see com.xuxiaocheng.WList.Commons.Beans.UploadConfirm.UploadInformation
         */
        @Contract("_ -> param1")
        public @NotNull ByteBuf dumpInformation(final @NotNull ByteBuf buffer) throws IOException {
            ByteBufIOUtil.writeVariableLenInt(buffer, this.parallelMethods.size());
            for (final OrderedConsumers consumers: this.parallelMethods) {
                ByteBufIOUtil.writeVariable2LenLong(buffer, consumers.start());
                ByteBufIOUtil.writeVariable2LenLong(buffer, consumers.end());
            }
            return buffer;
        }
    }

    /**
     * @param start Start byte position of the whole file.
     * @param end End byte position of the whole file.
     * @param consumersLink Each consumer will be supplied a {@code ByteBuf} whose {@code .readableBytes() <= }{@link com.xuxiaocheng.Rust.NetworkTransmission#FileTransferBufferSize}.
     */
    public record OrderedConsumers(long start, long end, @NotNull OrderedNode consumersLink) {
    }

    @FunctionalInterface
    public interface OrderedNode {
        @Nullable OrderedNode apply(final @NotNull ByteBuf content, final @NotNull Consumer<? super @Nullable Throwable> consumer, final ProgressBar.@NotNull ProgressListener listener);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull List<@NotNull OrderedConsumers>, @NotNull Runnable> splitUploadBuffer(final @NotNull BiConsumerE<? super @NotNull ByteBuf, ? super ProgressBar.@NotNull ProgressListener> sourceMethod, final long start, final int size) {
        assert size >= 0;
        if (size <= NetworkTransmission.FileTransferBufferSize)
            return Pair.ImmutablePair.makeImmutablePair(List.of(new OrderedConsumers(start, start + size, (content, consumer, listener) -> {
                try {
                    sourceMethod.accept(content, listener);
                    consumer.accept(null);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    consumer.accept(exception);
                }
                return null;
            })), RunnableE.EmptyRunnable);
        final int full = size / NetworkTransmission.FileTransferBufferSize - (size % NetworkTransmission.FileTransferBufferSize == 0 ? 1 : 0);
        final List<OrderedConsumers> list = new ArrayList<>(full + 1);
        final AtomicBoolean leaked = new AtomicBoolean(true);
        final ByteBuf[] cacher = new ByteBuf[full + 1];
        final long[] sizes = new long[full + 1];
        final ProgressBar.ProgressListener[] listeners = new ProgressBar.ProgressListener[full + 1];
        int i = 0;
        final AtomicInteger countDown = new AtomicInteger(full);
        for (long b = start; b < start + size; b += NetworkTransmission.FileTransferBufferSize) {
            final long e = Math.min(start + size, b + NetworkTransmission.FileTransferBufferSize);
            final int length = Math.toIntExact(e - b);
            final int k = i++;
            list.add(new OrderedConsumers(b, e, (c, consumer, listener) -> {
                try {
                    assert c.readableBytes() == length;
                    cacher[k] = c;
                    sizes[k] = length;
                    listeners[k] = listener;
                    if (countDown.getAndDecrement() == 0) {
                        final CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer(full + 1).addComponents(true, cacher);
                        try {
                            leaked.set(false);
                            final AtomicInteger index = new AtomicInteger(0);
                            sourceMethod.accept(buf, (ProgressBar.ProgressListener) delta -> {
                                synchronized (index) {
                                    long left = delta;
                                    while (left > 0) {
                                        final long current = Math.min(left, sizes[index.get()]);
                                        listeners[index.get()].onProgress(current);
                                        sizes[index.get()] -= current;
                                        left -= current;
                                        if (sizes[index.get()] <= 0)
                                            index.getAndIncrement();
                                    }
                                }
                            });
                            assert index.get() == listeners.length;
                        } finally {
                            buf.release();
                        }
                    }
                    consumer.accept(null);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    consumer.accept(exception);
                }
                return null;
            }));
        }
        return Pair.ImmutablePair.makeImmutablePair(list, () -> {
            if (leaked.compareAndSet(true, false))
                for (final ByteBuf buffer: cacher)
                    if (buffer != null)
                        buffer.release();
        });
    }
}
