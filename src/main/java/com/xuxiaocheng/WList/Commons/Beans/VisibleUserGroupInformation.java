package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

public record VisibleUserGroupInformation(long id, @NotNull String name, @NotNull Set<@NotNull UserPermission> permissions,
                                          @NotNull ZonedDateTime createTime, @NotNull ZonedDateTime updateTime) {
    /**
     * @see com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation
     */
    public static @NotNull VisibleUserGroupInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Set<UserPermission> permissions = Objects.requireNonNullElse(UserPermission.parse(ByteBufIOUtil.readUTF(buffer)), UserPermission.Empty);
        final ZonedDateTime createTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
        final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
        return new VisibleUserGroupInformation(id, name, permissions, createTime, updateTime);
    }

    /**
     * @see UserPermission
     */
    public enum Order implements OrderPolicies.OrderPolicy {
        Id, Name, CreateTime, UpdateTime,
        Permissions_ServerOperate,
        Permissions_Broadcast,
        Permissions_UsersList,
        Permissions_GroupsOperate,
        Permissions_UsersOperate,
        Permissions_ProvidersOperate,
        Permissions_FilesList,
        Permissions_FilesRefresh,
        Permissions_FileDownload,
        Permissions_FileUpload,
        Permissions_FileTrash,
        Permissions_FileCopy,
        Permissions_FileMove,
        ;
        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final @NotNull @Unmodifiable LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> ListEmptyOrder = new LinkedHashMap<>(0);
    public static @NotNull @Unmodifiable LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> emptyOrder() {
        return VisibleUserGroupInformation.ListEmptyOrder;
    }
}
