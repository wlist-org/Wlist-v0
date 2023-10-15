package com.xuxiaocheng.WListAndroid.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public final class PasswordHelper {
    private PasswordHelper() {
        super();
    }

    public static @Nullable String updateInternalPassword(final @NotNull Context context, final @NotNull String username, final @Nullable String password) {
        final SharedPreferences database = context.getSharedPreferences("client.passwords", Context.MODE_PRIVATE);
        final String identifier = "internal@" + username;
        if (password == null)
            return database.getString(identifier, null);
        database.edit().putString(identifier, password).apply();
        return password;
    } // Whatever the address is, identifier should be the same.

    public static @Nullable String updatePassword(final @NotNull Context context, final @NotNull InetSocketAddress address, final @NotNull String username, final @Nullable String password) {
        final SharedPreferences database = context.getSharedPreferences("client.passwords", Context.MODE_PRIVATE);
        final String identifier = address + "@" + username;
        if (password == null)
            return database.getString(identifier, null);
        database.edit().putString(identifier, password).apply();
        return password;
    }
}
