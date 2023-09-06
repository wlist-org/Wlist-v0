package com.xuxiaocheng.WListClient.AndroidSupports;

import com.xuxiaocheng.WListClient.Server.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;

public final class UserInformationGetter {
    private UserInformationGetter() {
        super();
    }

    public static long id(final @NotNull VisibleUserInformation information) {
        return information.id();
    }

    public static @NotNull String username(final @NotNull VisibleUserInformation information) {
        return information.username();
    }

    public static @NotNull String group(final @NotNull VisibleUserInformation information) {
        return information.group();
    }
}
