package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @param location File location. {@link FileLocation}
 * @param parentId Parent file id.
 * @param isDir If true this is a directory else is a regular file.
 * @param size File size. 0 means a directory, -1 means unknown.
 * @param createTime File first create time. Null means unknown.
 * @param updateTime File the latest update time. Null means unknown.
 * @param md5 File md5.
 * @param others Something extra for driver.
 */
public record FileSqlInformation(@NotNull FileLocation location, long parentId, @NotNull String name, boolean isDir, long size,
                                 @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                 @NotNull String md5, @Nullable String others) {
    public long id() {
        return this.location.id();
    }

    @Deprecated // only for client
    public record VisibleFileInformation(@NotNull FileLocation location, long parentId, @NotNull String name, boolean isDir, long size,
                                         @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                         @NotNull String md5) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull FileSqlInformation information) throws IOException {
        FileLocation.dump(buffer, information.location);
        ByteBufIOUtil.writeVariableLenLong(buffer, information.parentId);
        ByteBufIOUtil.writeUTF(buffer, information.name);
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
        final FileLocation location = FileLocation.parse(buffer);
        final long parentId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDir = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final LocalDateTime updateTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        return new VisibleFileInformation(location, parentId, name, isDir, size, createTime, updateTime, md5);
    }
}
