package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

/**
 * @see VisibleUserInformation
 */
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

    public static long groupId(final @NotNull VisibleUserInformation information) {
        return information.groupId();
    }

    public static @NotNull String groupName(final @NotNull VisibleUserInformation information) {
        return information.groupName();
    }

    public static @NotNull ZonedDateTime createTime(final @NotNull VisibleUserInformation information) {
        return information.createTime();
    }

    public static @NotNull ZonedDateTime updateTime(final @NotNull VisibleUserInformation information) {
        return information.updateTime();
    }
}
