package com.xuxiaocheng.WList.Internal.Drives;

import org.jetbrains.annotations.NotNull;

public interface DriverConfiguration {
    default @NotNull String getName() {
        return this.getClass().getName();
    }

    default void setName(final @NotNull String name) {
    }
}
