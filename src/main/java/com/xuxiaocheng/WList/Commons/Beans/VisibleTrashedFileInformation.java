package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record VisibleTrashedFileInformation(long id, @NotNull String name, boolean isDirectory, long size,
                                            @Nullable LocalDateTime createTime, @Nullable LocalDateTime trashedTime, @Nullable LocalDateTime expireTime,
                                            @NotNull String md5) {
    public static @NotNull VisibleTrashedFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final LocalDateTime trashedTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final LocalDateTime expireTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        return new VisibleTrashedFileInformation(id, name, isDirectory, size, createTime, trashedTime, expireTime, md5);
    }

    public @NotNull String createTimeString(final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return this.createTime == null ? Objects.requireNonNullElse(unknown, "unknown") : this.createTime.format(formatter);
    }

    public @NotNull String trashedTimeString(final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return this.trashedTime == null ? Objects.requireNonNullElse(unknown, "unknown") : this.trashedTime.format(formatter);
    }

    public @NotNull String expireTimeString(final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return this.expireTime == null ? Objects.requireNonNullElse(unknown, "unknown") : this.expireTime.format(formatter);
    }
}