package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import org.jetbrains.annotations.NotNull;

public final class FileLocationSupporter {
    private FileLocationSupporter() {
        super();
    }

    public static @NotNull String driver(final @NotNull FileLocation location) {
        return location.storage();
    }

    public static long id(final @NotNull FileLocation location) {
        return location.id();
    }

    public static @NotNull FileLocation create(final @NotNull String driver, final long id) {
        return new FileLocation(driver, id);
    }
}
