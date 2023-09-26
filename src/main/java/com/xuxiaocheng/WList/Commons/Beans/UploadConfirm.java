package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record UploadConfirm(@NotNull @Unmodifiable List<@NotNull UploadChecksum> checksums, @NotNull String id) {
    public static @NotNull UploadConfirm parse(final @NotNull ByteBuf buffer) throws IOException {
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final List<UploadChecksum> checksums = new ArrayList<>(length);
        for (int i = 0; i < length; ++i)
            checksums.add(UploadChecksum.parse(buffer));
        final String id = ByteBufIOUtil.readUTF(buffer);
        return new UploadConfirm(checksums, id);
    }

    public record UploadInformation(@NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> parallel) {
        public static @NotNull UploadInformation parse(final @NotNull ByteBuf buffer) throws IOException {
            final int size = ByteBufIOUtil.readVariableLenInt(buffer);
            final List<Pair.ImmutablePair<Long, Long>> parallel = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final long start = ByteBufIOUtil.readVariable2LenLong(buffer);
                final long end = ByteBufIOUtil.readVariable2LenLong(buffer);
                parallel.add(Pair.ImmutablePair.makeImmutablePair(start, end));
            }
            return new UploadInformation(parallel);
        }
    }
}
