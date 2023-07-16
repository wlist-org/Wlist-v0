package com.xuxiaocheng.WList.Databases.User;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public final class PasswordGuard {
    private PasswordGuard() {
        super();
    }

    // Due to the salt being stored in the database, additional salt needs to be added to prevent password leakage.
    private static final @NotNull String ServerPasswordSlat = "WList/PasswordSlat/AdditionalSlat: " + HExceptionWrapper.wrapSupplier(() -> ConstantManager.get("PasswordSlat",
            () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 256, ConstantManager.DefaultRandomChars), "initialize")).get();

    public static @NotNull String encryptPassword(final @NotNull CharSequence password) {
        return MiscellaneousUtil.getMd5((password + PasswordGuard.ServerPasswordSlat).getBytes(StandardCharsets.UTF_8)) + Integer.toString(password.length(), 36);
    }
}
