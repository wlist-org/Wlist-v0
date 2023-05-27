package com.xuxiaocheng.WList.Driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Options {
    private Options() {
        super();
    }

    public enum OrderDirection {
        ASCEND, DESCEND,
    }

    public enum OrderPolicy {
        FileName,
        Size,
        CreateTime,
        UpdateTime,
    }

    public static @Nullable OrderDirection valueOfOrderDirection(final @NotNull String direction) {
        try {
            return OrderDirection.valueOf(direction);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static @Nullable OrderPolicy valueOfOrderPolicy(final @NotNull String policy) {
        try {
            return OrderPolicy.valueOf(policy);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public enum DuplicatePolicy {
        ERROR,
        OVER,
        KEEP,
    }

    public static @Nullable DuplicatePolicy valueOfDuplicatePolicy(final @NotNull String policy) {
        try {
            return DuplicatePolicy.valueOf(policy);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
