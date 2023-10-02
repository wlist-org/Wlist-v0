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
}
