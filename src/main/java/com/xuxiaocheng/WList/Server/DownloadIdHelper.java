package com.xuxiaocheng.WList.Server;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

public final class DownloadIdHelper {
    private DownloadIdHelper() {
        super();
    }

    public static long generateId(final @NotNull InputStream stream) {
        // TODO
        return 0;
    }

    public static @Nullable ByteBuf download(final long id) {
        // TODO
        return null;
    }

    public static boolean cancel(final long id) {
        // TODO
        return false;
    }
}
