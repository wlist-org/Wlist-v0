package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import org.jetbrains.annotations.Nullable;

public final class DatabaseSupporter {
    private DatabaseSupporter() {
        super();
    }

    public static @Nullable String getAndDeleteDefaultAdminPassword() {
        return UserManager.getInstance().getAndDeleteDefaultAdminPassword();
    }
}
