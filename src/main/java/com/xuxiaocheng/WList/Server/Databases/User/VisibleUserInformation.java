package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public record VisibleUserInformation(long id, @NotNull String username, @NotNull SortedSet<Operation.@NotNull Permission> permissions) {
    public static @NotNull VisibleUserInformation create(final @NotNull UserSqlInformation information) {
        return new VisibleUserInformation(information.getId(), information.getUsername(), information.getPermissions());
    }

    public static void dump(final @NotNull ByteBuf buffer, final @NotNull VisibleUserInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.id);
        ByteBufIOUtil.writeUTF(buffer, information.username);
        ByteBufIOUtil.writeUTF(buffer, Operation.dumpPermissions(information.permissions));
    }

    public static void dump(final @NotNull ByteBuf buffer, final @NotNull UserSqlInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.getId());
        ByteBufIOUtil.writeUTF(buffer, information.getUsername());
        ByteBufIOUtil.writeUTF(buffer, Operation.dumpPermissions(information.getPermissions()));
    }

    public static @NotNull VisibleUserInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        return new VisibleUserInformation(ByteBufIOUtil.readVariableLenLong(buffer),
                ByteBufIOUtil.readUTF(buffer),
                Objects.requireNonNullElse(Operation.parsePermissions(ByteBufIOUtil.readUTF(buffer)), new TreeSet<>()));
    }
}
