package com.xuxiaocheng.WList.Commons.Options;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum OrderDirection {
    ASCEND, DESCEND,
    ;

    public static @Nullable OrderDirection of(final @NotNull String policy) {
        try {
            return OrderDirection.valueOf(policy);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
