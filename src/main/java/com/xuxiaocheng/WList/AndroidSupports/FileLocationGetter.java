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

    public static boolean equals(final @NotNull FileLocation a, final @NotNull FileLocation b) {
        return a.equals(b);
    }

    public static int hashCode(final @NotNull FileLocation location) {
        return location.hashCode();
    }

    public static @NotNull String toString(final @NotNull FileLocation location) {
        return location.toString();
    }
}
