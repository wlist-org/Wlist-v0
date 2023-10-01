package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @see FileLocation
 */
public final class FileLocationGetter {
    private FileLocationGetter() {
        super();
    }

    public static @NotNull String storage(final @NotNull FileLocation location) {
        return location.storage();
    }

    public static long id(final @NotNull FileLocation location) {
        return location.id();
    }
}
