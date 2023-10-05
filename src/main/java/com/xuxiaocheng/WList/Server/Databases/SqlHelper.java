package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

public final class SqlHelper {
    private SqlHelper() {
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

    public static @NotNull ZonedDateTime toZonedDataTime(final @NotNull Timestamp timestamp) {
        return ZonedDateTime.of(AndroidSupporter.toLocalDateTime(timestamp), ZoneOffset.UTC);
    }

    @Contract(pure = true)
    public static @Nullable Timestamp getTimestamp(final @Nullable ZonedDateTime time) {
        assert time == null || ZoneOffset.UTC.equals(time.getZone());
        return time == null ? null : Timestamp.valueOf(time.toLocalDateTime());
    }
}
