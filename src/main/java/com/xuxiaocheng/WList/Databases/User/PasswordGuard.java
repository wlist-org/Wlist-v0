package com.xuxiaocheng.WList.Databases.User;

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
            () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 128, ConstantManager.DefaultRandomChars), "initialize")).get();

    public static @NotNull String encryptPassword(final @NotNull String password) {
        return MiscellaneousUtil.getMd5((password + PasswordGuard.ServerPasswordSlat).getBytes(StandardCharsets.UTF_8));
    }
}
