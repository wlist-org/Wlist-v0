package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @param id File id. Primary key.
 * @param path Full path. Union.
 * @param is_dir If true this is a directory else is a regular file.
 * @param size File size. 0 means a directory, -1 means unknown.
 * @param createTime File first create time. Null means unknown.
 * @param updateTime File the latest update time. Null means unknown.
 * @param md5 File md5.
 * @param availableForGroup Permission for user group.
 * @param others Something extra for driver.
 */
public record FileSqlInformation(long id, @NotNull DrivePath path, boolean is_dir, long size,
                                 @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                 @NotNull String md5, @NotNull List<@NotNull Long> availableForGroup, @Nullable String others) {

    public record VisibleFileInformation(@NotNull DrivePath path, boolean is_dir, long size,
                                         @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                         @NotNull String md5) {
    }

    public static @NotNull FileSqlInformation.VisibleFileInformation create(final @NotNull FileSqlInformation information) {
        return new VisibleFileInformation(information.path(), information.is_dir(), information.size(),
                information.createTime(), information.updateTime(), information.md5());
    }

    public static void dump(final @NotNull ByteBuf buffer, final @NotNull FileSqlInformation.VisibleFileInformation information) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, information.path.getPath());
        ByteBufIOUtil.writeBoolean(buffer, information.is_dir);
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size);
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.updateTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.md5);
    }

    public static void dump(final @NotNull ByteBuf buffer, final @NotNull FileSqlInformation information) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, information.path().getPath());
        ByteBufIOUtil.writeBoolean(buffer, information.is_dir());
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size());
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime(), (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.updateTime(), (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.md5());
    }

    public static @NotNull FileSqlInformation.VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        return new VisibleFileInformation(new DrivePath(ByteBufIOUtil.readUTF(buffer)),
                ByteBufIOUtil.readBoolean(buffer), ByteBufIOUtil.readVariable2LenLong(buffer),
                ByteBufIOUtil.readObjectNullable(buffer, b ->
                        LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME)),
                ByteBufIOUtil.readObjectNullable(buffer, b ->
                        LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME)),
                ByteBufIOUtil.readUTF(buffer));
    }
}
