package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Locale;

public final class SqliteHelper {
    private SqliteHelper() {
        super();
    }

    public static @NotNull ZonedDateTime now() {
        return MiscellaneousUtil.now().withNano(0);
    }

    @Contract(pure = true)
    public static byte @NotNull [] toOrdered(final @NotNull String name) {
        return name.toLowerCase(Locale.ROOT).getBytes(Charset.forName("GBK"));
    }

    @Contract(pure = true)
    public static @NotNull String likeName(final @NotNull String name) {
        return '%' + name.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + '%';
    }
}
