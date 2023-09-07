package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import org.jetbrains.annotations.NotNull;

public final class PasswordGuard {
    private PasswordGuard() {
        super();
    }

    // Due to the salt being stored in the database, additional salt needs to be added to prevent password leakage.
    private static final @NotNull String ServerPasswordSlat = "WList/PasswordGuard/AdditionalSlat: " + HExceptionWrapper.wrapSupplier(() -> ConstantManager.get("PasswordSlat",
            () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 256, HRandomHelper.AnyWords), "initialize")).get();

    public static @NotNull String encryptPassword(final @NotNull String password) {
        final String sha256 = HMessageDigestHelper.SHA256.get(password);
        final String md5 = HMessageDigestHelper.MD5.get(password + PasswordGuard.ServerPasswordSlat);
        return sha256.substring(0, 32) + Integer.toString(password.length(), 16) + md5.substring(0, 16) + sha256.substring(32) + md5.substring(16);
    }
}
