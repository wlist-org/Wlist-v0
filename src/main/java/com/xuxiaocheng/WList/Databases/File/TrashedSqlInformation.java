package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TrashedSqlInformation(@NotNull FileLocation location, @NotNull String name, boolean isDirectory, long size,
                                    @Nullable LocalDateTime createTime, @Nullable LocalDateTime trashedTime, @NotNull LocalDateTime expireTime,
                                    @NotNull String md5, @Nullable String others) {
    public long id() {
        return this.location.id();
    }

    @Deprecated // only for client
    public record VisibleTrashedInformation(long id, @NotNull String name, boolean isDirectory, long size,
                                            @Nullable LocalDateTime createTime, @Nullable LocalDateTime trashedTime, @NotNull LocalDateTime expireTime,
                                            @NotNull String md5) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull TrashedSqlInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.location.id());
        ByteBufIOUtil.writeUTF(buffer, information.name);
        ByteBufIOUtil.writeBoolean(buffer, information.isDirectory);
        ByteBufIOUtil.writeVariable2LenLong(buffer, information.size);
        ByteBufIOUtil.writeObjectNullable(buffer, information.createTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeObjectNullable(buffer, information.trashedTime, (b, t) ->
                ByteBufIOUtil.writeUTF(b, t.format(DateTimeFormatter.ISO_DATE_TIME)));
        ByteBufIOUtil.writeUTF(buffer, information.expireTime.format(DateTimeFormatter.ISO_DATE_TIME));
        ByteBufIOUtil.writeUTF(buffer, information.md5);
    }

    @Deprecated // only for client
    public static @NotNull VisibleTrashedInformation parseVisible(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final LocalDateTime createTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final LocalDateTime trashedTime = ByteBufIOUtil.readObjectNullable(buffer, b ->
                LocalDateTime.parse(ByteBufIOUtil.readUTF(b), DateTimeFormatter.ISO_DATE_TIME));
        final LocalDateTime expireTime = LocalDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        return new VisibleTrashedInformation(id, name, isDirectory, size, createTime, trashedTime, expireTime, md5);
    }
}
