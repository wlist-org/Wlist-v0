package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;

public record UserGroupInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.@NotNull Permission> permissions) {
    public record Inserter(@NotNull String name, @NotNull EnumSet<Operation.@NotNull Permission> permissions) {
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        ByteBufIOUtil.writeUTF(buffer, this.name);
        ByteBufIOUtil.writeUTF(buffer, Operation.dumpPermissions(this.permissions));
        return buffer;
    }
}
