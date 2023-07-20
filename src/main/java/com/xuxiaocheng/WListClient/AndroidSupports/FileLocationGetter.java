package com.xuxiaocheng.WListClient.AndroidSupports;

import com.xuxiaocheng.WListClient.Server.FileLocation;
import org.jetbrains.annotations.NotNull;

public final class FileLocationGetter {
    private FileLocationGetter() {
        super();
    }

    public static @NotNull String driver(final @NotNull FileLocation location) {
        return location.driver();
    }

    public static long id(final @NotNull FileLocation location) {
        return location.id();
    }

    public static @NotNull FileLocation create(final @NotNull String driver, final long id) {
        return new FileLocation(driver, id);
    }
}
