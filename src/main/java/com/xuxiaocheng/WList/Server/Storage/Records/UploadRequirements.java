package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// TODO
public record UploadRequirements(@NotNull @Unmodifiable List<@NotNull UploadChecksum> checksums, @NotNull Function<@NotNull @Unmodifiable List<@NotNull String>, @NotNull UploadMethods> transfer) {
    public record UploadMethods(@NotNull @Unmodifiable List<@NotNull OrderedFrame> parallelMethods,
                                @NotNull SupplierE<@Nullable FileInformation> supplier,
                                @NotNull Runnable finisher) {
    }

    public record OrderedFrame(
            long start,
            long end,
            @NotNull @Unmodifiable List<@NotNull ConsumerE<@NotNull ByteBuf>> consumers
    ) {
    }

    public static Pair.@NotNull ImmutablePair<@NotNull List<@NotNull ConsumerE<@NotNull ByteBuf>>, @NotNull Runnable> splitUploadMethodEveryFileTransferBufferSize(final @NotNull ConsumerE<@NotNull ByteBuf> sourceMethod, final int totalSize) {
        assert totalSize >= 0;
        if (totalSize < NetworkTransmission.FileTransferBufferSize)
            return Pair.ImmutablePair.makeImmutablePair(List.of(sourceMethod), RunnableE.EmptyRunnable);
        final int mod = totalSize % NetworkTransmission.FileTransferBufferSize;
        final int count = totalSize / NetworkTransmission.FileTransferBufferSize - (mod == 0 ? 1 : 0);
        final int rest = mod == 0 ? NetworkTransmission.FileTransferBufferSize : mod;
        final List<ConsumerE<ByteBuf>> list = new ArrayList<>(count + 1);
        final AtomicBoolean leaked = new AtomicBoolean(true);
        final ByteBuf[] cacher = new ByteBuf[count + 1];
        final AtomicInteger countDown = new AtomicInteger(count);
        for (int i = 0; i < count + 1; ++i) {
            final int c = i;
            list.add(b -> {
                assert b.readableBytes() == (c == count ? rest : NetworkTransmission.FileTransferBufferSize);
                cacher[c] = b;
                if (countDown.getAndDecrement() == 0) {
                    leaked.set(false);
                    final CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer(count + 1).addComponents(true, cacher);
                    try {
                        sourceMethod.accept(buf);
                    } finally {
                        buf.release();
                    }
                }
            });
        }
        return Pair.ImmutablePair.makeImmutablePair(list, () -> {
            if (leaked.get())
                for (final ByteBuf buffer: cacher)
                    if (buffer != null)
                        buffer.release();
        });
    }
}
