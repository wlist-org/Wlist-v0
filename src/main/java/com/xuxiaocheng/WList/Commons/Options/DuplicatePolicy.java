package com.xuxiaocheng.WList.Commons.Options;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DuplicatePolicy {
    ERROR,
    OVER,
    KEEP,
    ;

    public static @Nullable DuplicatePolicy of(final @NotNull String policy) {
        try {
            return DuplicatePolicy.valueOf(policy);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
