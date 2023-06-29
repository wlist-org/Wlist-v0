package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @param id File id. Primary key.
 * @param path Full path. Union.
 * @param isDir If true this is a directory else is a regular file.
 * @param size File size. 0 means a directory, -1 means unknown.
 * @param createTime File first create time. Null means unknown.
 * @param updateTime File the latest update time. Null means unknown.
 * @param md5 File md5.
 * @param others Something extra for driver.
 */
public record FileSqlInformation(long id, @NotNull DrivePath path, boolean isDir, long size,
                                 @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                 @NotNull String md5, @Nullable String others) {
    @Deprecated // only for client
    public record VisibleFileInformation(@NotNull DrivePath path, boolean isDir, long size,
                                         @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                         @NotNull String md5) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull FileSqlInformation information) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, information.path.getPath());
        ByteBufIOUtil.writeBoolean(buffer, information.isDir);
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size);
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.updateTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.md5);
    }

    @Deprecated // only for client
    public static @NotNull VisibleFileInformation parseVisible(final @NotNull ByteBuf buffer) throws IOException {
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
