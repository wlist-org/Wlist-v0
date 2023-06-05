package com.xuxiaocheng.WListClient.Server;

import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record VisibleFileInformation(@NotNull DrivePath path, boolean is_dir, long size,
                                     @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                     @NotNull String md5) {
    public static @NotNull VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final boolean isDir = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final LocalDateTime updateTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        return new VisibleFileInformation(path, isDir, size, createTime, updateTime, md5);
    }
}
