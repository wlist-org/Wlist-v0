package com.xuxiaocheng.WListClient.AndroidSupports;

import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.VisibleUserGroupInformation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

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

    public static @NotNull EnumSet<Operation.Permission> permissions(final @NotNull VisibleUserGroupInformation information) {
        return information.permissions();
    }
}
