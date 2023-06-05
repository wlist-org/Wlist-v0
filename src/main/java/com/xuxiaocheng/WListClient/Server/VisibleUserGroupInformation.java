package com.xuxiaocheng.WListClient.Server;

import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

public record VisibleUserGroupInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.Permission> permissions) {
    public static @NotNull VisibleUserGroupInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final EnumSet<Operation.Permission> permissions = Objects.requireNonNullElseGet(
                Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer)), Operation::emptyPermissions);
        return new VisibleUserGroupInformation(id, name, permissions);
    }
}
