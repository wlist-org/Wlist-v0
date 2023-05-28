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
    public static void dump(final @NotNull ByteBuf buffer, final @NotNull VisibleFileInformation information) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, information.path.getPath());
        ByteBufIOUtil.writeBoolean(buffer, information.is_dir);
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size);
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.updateTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.md5);
    }

    public static @NotNull VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        return new VisibleFileInformation(new DrivePath(ByteBufIOUtil.readUTF(buffer)),
                ByteBufIOUtil.readBoolean(buffer), ByteBufIOUtil.readVariable2LenLong(buffer),
                ByteBufIOUtil.readObjectNullable(buffer, b ->
                        LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME)),
                ByteBufIOUtil.readObjectNullable(buffer, b ->
                        LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME)),
                ByteBufIOUtil.readUTF(buffer));
    }
}
