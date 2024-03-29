package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @see InstantaneousProgressState
 */
public final class InstantaneousProgressStateGetter {
    private InstantaneousProgressStateGetter() {
        super();
    }

    public static @NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> stages(final @NotNull InstantaneousProgressState state) {
        return state.stages();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long> merge(final @NotNull InstantaneousProgressState state) {
        return state.merge();
    }
}
