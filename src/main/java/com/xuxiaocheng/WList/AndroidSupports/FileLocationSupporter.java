package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @see FileLocation
 */
public final class FileLocationSupporter {
    private FileLocationSupporter() {
        super();
    }

    public static @NotNull String storage(final @NotNull FileLocation location) {
        return location.storage();
    }

    public static long id(final @NotNull FileLocation location) {
        return location.id();
    }

    public static @NotNull FileLocation create(final @NotNull String storage, final long id) {
        return new FileLocation(storage, id);
    }
}
