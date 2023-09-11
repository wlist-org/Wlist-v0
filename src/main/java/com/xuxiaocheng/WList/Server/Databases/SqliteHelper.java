package com.xuxiaocheng.WList.Server.Databases;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Locale;

public final class SqliteHelper {
    private SqliteHelper() {
        super();
    }

    public static @NotNull LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }

    public static byte @NotNull [] toOrdered(final @NotNull String name) {
        return name.toLowerCase(Locale.ROOT).getBytes(Charset.forName("GBK"));
    }

    public static @NotNull String likeName(final @NotNull String name) {
        return '%' + name.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + '%';
    }
}
