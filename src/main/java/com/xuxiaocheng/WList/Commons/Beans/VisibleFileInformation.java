package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Objects;

public record VisibleFileInformation(long id, long parentId, @NotNull String name, boolean isDirectory, long size,
                                     @Nullable ZonedDateTime createTime, @Nullable ZonedDateTime updateTime) {
    /**
     * @see com.xuxiaocheng.WList.Server.Databases.File.FileInformation
     */
    public static @NotNull VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final long parentId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final ZonedDateTime createTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final ZonedDateTime updateTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
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
        ;
        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final @NotNull @Unmodifiable LinkedHashMap<@NotNull Order, Options.@NotNull OrderDirection> ListEmptyOrder = new LinkedHashMap<>(0);
    public static @NotNull @Unmodifiable LinkedHashMap<@NotNull Order, Options.@NotNull OrderDirection> emptyOrder() {
        return VisibleFileInformation.ListEmptyOrder;
    }
}
