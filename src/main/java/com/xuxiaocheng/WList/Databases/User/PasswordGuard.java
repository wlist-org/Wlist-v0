package com.xuxiaocheng.WList.Databases.User;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import org.jetbrains.annotations.NotNull;

public final class PasswordGuard {
    private PasswordGuard() {
        super();
    }

    // Due to the salt being stored in the database, additional salt needs to be added to prevent password leakage.
    private static final @NotNull String ServerPasswordSlat = "WList/PasswordGuard/AdditionalSlat: " + HExceptionWrapper.wrapSupplier(() -> ConstantManager.get("PasswordSlat",
            () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 256, ConstantManager.DefaultRandomChars), "initialize")).get();

    public static @NotNull String encryptPassword(final @NotNull CharSequence password) {
        return Integer.toString(password.length(), 36) + HMessageDigestHelper.SHA256.get(password + PasswordGuard.ServerPasswordSlat);
    }
}
