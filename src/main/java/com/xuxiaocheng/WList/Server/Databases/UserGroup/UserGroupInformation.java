package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;

public record UserGroupInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.@NotNull Permission> permissions) {
    public record Inserter(@NotNull String name, @NotNull EnumSet<Operation.@NotNull Permission> permissions) {
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation
     */
    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull UserGroupInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.id);
        ByteBufIOUtil.writeUTF(buffer, information.name);
        ByteBufIOUtil.writeUTF(buffer, Operation.dumpPermissions(information.permissions()));
    }
}
