package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

public record VisibleUserGroupInformation(long id, @NotNull String name, @NotNull Set<@NotNull UserPermission> permissions,
                                          @NotNull LocalDateTime createTime, @NotNull LocalDateTime updateTime) {
    public static @NotNull VisibleUserGroupInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Set<UserPermission> permissions = Objects.requireNonNullElse(UserPermission.parse(ByteBufIOUtil.readUTF(buffer)), UserPermission.Empty);
        final LocalDateTime createTime = LocalDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
        final LocalDateTime updateTime = LocalDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
        return new VisibleUserGroupInformation(id, name, permissions, createTime, updateTime);
    }

    /**
     * @see UserPermission
     */
    public enum Order implements Options.OrderPolicy {
        Id, Name, CreateTime, UpdateTime,
        Permissions_ServerOperate,
        Permissions_Broadcast,
        Permissions_UsersList,
        Permissions_GroupsOperate,
        Permissions_UsersOperate,
        Permissions_DriverOperate,
        Permissions_FilesBuildIndex,
        Permissions_FilesList,
        Permissions_FileDownload,
        Permissions_FileUpload,
        Permissions_FileDelete,
    }

    public static @Nullable Order orderBy(final @NotNull String orderBy) {
        try {
            return Order.valueOf(orderBy);
        } catch (final IllegalArgumentException ignore) {
            return null;
        }
    }
}
