package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operation;
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
