package com.xuxiaocheng.WList.Commons.Options;

import org.jetbrains.annotations.Nullable;

public enum FilterPolicy {
    OnlyDirectories,
    OnlyFiles,
    Both,
    ;

    public static @Nullable FilterPolicy of(final byte policy) {
        return switch (policy) {
            case 0 -> FilterPolicy.OnlyDirectories;
            case 1 -> FilterPolicy.OnlyFiles;
            case 2 -> FilterPolicy.Both;
            default -> null;
        };
    }
}
