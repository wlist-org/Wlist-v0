package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @param location File location. {@link FileLocation}
 * @param parentId Parent file id.
 * @param type File type. {@link FileSqlInterface.FileSqlType}
 * @param size File size. 0 means a directory, -1 means unknown.
 * @param createTime File first create time. Null means unknown.
 * @param updateTime File the latest update time. Null means unknown.
 * @param md5 File md5.
 * @param others Something extra for driver.
 */
public record FileSqlInformation(@NotNull FileLocation location, long parentId, @NotNull String name, @NotNull FileSqlInterface.FileSqlType type, long size,
                                 @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                 @NotNull String md5, @Nullable String others) {
    public long id() {
        return this.location.id();
    }

    public boolean isDirectory() {
        return this.type != FileSqlInterface.FileSqlType.RegularFile;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSqlInformation that)) return false;
        return this.parentId == that.parentId && this.size == that.size && this.location.equals(that.location) && this.name.equals(that.name) &&
                ((this.type == FileSqlInterface.FileSqlType.RegularFile) == (that.type == FileSqlInterface.FileSqlType.RegularFile)) &&
                Objects.equals(this.createTime, that.createTime) && Objects.equals(this.updateTime, that.updateTime) && this.md5.equals(that.md5) && Objects.equals(this.others, that.others);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.parentId, this.name, this.size, this.createTime, this.updateTime, this.md5, this.others);
    }

    @Deprecated // only for client
    public record VisibleFileInformation(long id, long parentId, @NotNull String name, boolean isDirectory, long size,
                                         @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                         @NotNull String md5) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull FileSqlInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.location.id());
        ByteBufIOUtil.writeVariableLenLong(buffer, information.parentId);
        ByteBufIOUtil.writeUTF(buffer, information.name);
        ByteBufIOUtil.writeBoolean(buffer, information.isDirectory());
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size);
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.updateTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.md5);
    }

    @Deprecated // only for client
    public static @NotNull VisibleFileInformation parseVisible(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final long parentId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final LocalDateTime updateTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        return new VisibleFileInformation(id, parentId, name, isDirectory, size, createTime, updateTime, md5);
    }
}
