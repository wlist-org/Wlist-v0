package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * @see VisibleUserGroupInformation
 */
public final class UserGroupInformationGetter {
    private UserGroupInformationGetter() {
        super();
    }

    public static long id(final @NotNull VisibleUserGroupInformation information) {
        return information.id();
    }

    public static @NotNull String name(final @NotNull VisibleUserGroupInformation information) {
        return information.name();
    }

    public static @NotNull Set<UserPermission> permissions(final @NotNull VisibleUserGroupInformation information) {
        return information.permissions();
    }

    public static @NotNull ZonedDateTime createTime(final @NotNull VisibleUserGroupInformation information) {
        return information.createTime();
    }

    public static @NotNull ZonedDateTime updateTime(final @NotNull VisibleUserGroupInformation information) {
        return information.updateTime();
    }

    public static boolean equals(final @NotNull VisibleUserGroupInformation a, final @NotNull VisibleUserGroupInformation b) {
        return a.equals(b);
    }

    public static int hashCode(final @NotNull VisibleUserGroupInformation information) {
        return information.hashCode();
    }

    public static @NotNull String toString(final @NotNull VisibleUserGroupInformation information) {
        return information.toString();
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public enum Order {
        Id(VisibleUserGroupInformation.Order.Id),
        Name(VisibleUserGroupInformation.Order.Name),
        CreateTime(VisibleUserGroupInformation.Order.CreateTime),
        UpdateTime(VisibleUserGroupInformation.Order.UpdateTime),
        Permissions_ServerOperate(VisibleUserGroupInformation.Order.Permissions_ServerOperate),
        Permissions_Broadcast(VisibleUserGroupInformation.Order.Permissions_Broadcast),
        Permissions_UsersList(VisibleUserGroupInformation.Order.Permissions_UsersList),
        Permissions_GroupsOperate(VisibleUserGroupInformation.Order.Permissions_GroupsOperate),
        Permissions_UsersOperate(VisibleUserGroupInformation.Order.Permissions_UsersOperate),
        Permissions_ProvidersOperate(VisibleUserGroupInformation.Order.Permissions_ProvidersOperate),
        Permissions_FilesList(VisibleUserGroupInformation.Order.Permissions_FilesList),
        Permissions_FilesRefresh(VisibleUserGroupInformation.Order.Permissions_FilesRefresh),
        Permissions_FileDownload(VisibleUserGroupInformation.Order.Permissions_FileDownload),
        Permissions_FileUpload(VisibleUserGroupInformation.Order.Permissions_FileUpload),
        Permissions_FileTrash(VisibleUserGroupInformation.Order.Permissions_FileTrash),
        Permissions_FileCopy(VisibleUserGroupInformation.Order.Permissions_FileCopy),
        Permissions_FileMove(VisibleUserGroupInformation.Order.Permissions_FileMove),
        ;
        private final VisibleUserGroupInformation.Order order;
        Order(final VisibleUserGroupInformation.Order order) {
            this.order = order;
        }
        public VisibleUserGroupInformation.Order order() {
            return this.order;
        }

        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }
}
