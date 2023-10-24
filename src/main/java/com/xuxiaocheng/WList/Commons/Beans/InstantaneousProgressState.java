package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.Helpers.ProgressBar
 */
public record InstantaneousProgressState(@NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> stages) {
    public static @NotNull InstantaneousProgressState parse(final @NotNull ByteBuf buffer) throws IOException {
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final List<Pair.ImmutablePair<Long, Long>> stages = new ArrayList<>(length);
        for (int i = 0; i < length; ++i)
            stages.add(Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readVariableLenLong(buffer), ByteBufIOUtil.readVariableLenLong(buffer)));
        return new InstantaneousProgressState(stages);
    }

    private static final Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long> EmptyMerge = Pair.ImmutablePair.makeImmutablePair(0L, 0L);
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long> merge() {
        if (this.stages.isEmpty()) return InstantaneousProgressState.EmptyMerge;
        if (this.stages.size() == 1) return this.stages.get(0);
        long current = 0, total = 0;
        for (final Pair.ImmutablePair<Long, Long> pair: this.stages) {
            current += pair.getFirst().longValue();
            total += pair.getSecond().longValue();
        }
        return Pair.ImmutablePair.makeImmutablePair(current, total);
    }
}
