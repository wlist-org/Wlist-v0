package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;

public record VisibleUserGroupInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.@NotNull Permission> permissions) {
    public static @NotNull VisibleUserGroupInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final EnumSet<Operation.Permission> permissions = Operation.parsePermissionsNotNull(ByteBufIOUtil.readUTF(buffer));
        return new VisibleUserGroupInformation(id, name, permissions);
    }
}
