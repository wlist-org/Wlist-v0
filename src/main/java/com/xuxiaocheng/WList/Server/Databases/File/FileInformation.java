package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
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
public record FileInformation(@NotNull FileLocation location, long parentId, @NotNull String name, FileSqlInterface.@NotNull FileSqlType type, long size,
                              @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                              @NotNull String md5, @Nullable String others) { // TODO
    public long id() {
        return this.location.id();
    }

    public boolean isDirectory() {
        return this.type != FileSqlInterface.FileSqlType.RegularFile;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof FileInformation that)) return false;
        return this.parentId == that.parentId && this.size == that.size && this.location.equals(that.location) && this.name.equals(that.name) &&
                ((this.type == FileSqlInterface.FileSqlType.RegularFile) == (that.type == FileSqlInterface.FileSqlType.RegularFile)) &&
                Objects.equals(this.createTime, that.createTime) && Objects.equals(this.updateTime, that.updateTime) &&
                this.md5.equals(that.md5) && Objects.equals(this.others, that.others);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.parentId, this.name, this.size, this.createTime, this.updateTime, this.md5, this.others);
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.location.id());
        ByteBufIOUtil.writeVariableLenLong(buffer, this.parentId);
        ByteBufIOUtil.writeUTF(buffer, this.name);
        ByteBufIOUtil.writeBoolean(buffer, this.isDirectory());
        ByteBufIOUtil.writeVariable2LenLong(buffer, this.size);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.createTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.updateTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeUTF(buffer, this.md5);
        return buffer;
    }
}
