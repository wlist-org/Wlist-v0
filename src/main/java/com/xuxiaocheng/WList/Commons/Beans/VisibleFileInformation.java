package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record VisibleFileInformation(long id, long parentId, @NotNull String name, boolean isDirectory, long size,
                                     @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime) {
    public static @NotNull VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final long parentId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final LocalDateTime updateTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        return new VisibleFileInformation(id, parentId, name, isDirectory, size, createTime, updateTime);
    }

    public @NotNull String createTimeString(final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return this.createTime == null ? Objects.requireNonNullElse(unknown, "unknown") : this.createTime.format(formatter);
    }

    public @NotNull String updateTimeString(final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return this.updateTime == null ? Objects.requireNonNullElse(unknown, "unknown") : this.updateTime.format(formatter);
    }

    public enum Order implements Options.OrderPolicy {
        Id, Name, Directory, Size, CreateTime, UpdateTime,
    }

    public static @Nullable Order orderBy(final @NotNull String orderBy) {
        try {
            return Order.valueOf(orderBy);
        } catch (final IllegalArgumentException ignore) {
            return null;
        }
    }
}
