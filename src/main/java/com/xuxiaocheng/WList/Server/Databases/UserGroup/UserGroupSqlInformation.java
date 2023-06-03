package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

public record UserGroupSqlInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.Permission> permissions) {
    public record Inserter(@NotNull String name, @NotNull EnumSet<Operation.Permission> permissions) {
    }

    public @NotNull Inserter toInsert() {
        return new Inserter(this.name, this.permissions);
    }

    @Deprecated // only for client
    public record VisibleUserGroupInformation(long id, @NotNull String name, @NotNull EnumSet<Operation.Permission> permissions) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull UserGroupSqlInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.id);
        ByteBufIOUtil.writeUTF(buffer, information.name);
        ByteBufIOUtil.writeUTF(buffer, Operation.dumpPermissions(information.permissions()));
    }

    @Deprecated // only for client
    public static @NotNull VisibleUserGroupInformation parseVisible(final @NotNull ByteBuf buffer) throws IOException {
        return new VisibleUserGroupInformation(ByteBufIOUtil.readVariableLenLong(buffer), ByteBufIOUtil.readUTF(buffer),
                Objects.requireNonNullElseGet(Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer)),
                        Operation::emptyPermissions));
    }
}
