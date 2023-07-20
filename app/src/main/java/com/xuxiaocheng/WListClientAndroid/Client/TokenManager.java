package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Client.WListClient;

import java.io.IOException;

public final class TokenManager {
    private TokenManager() {
        super();
    }

    @Nullable private static String token;

    public static synchronized void setToken(@NonNull final String username, @NonNull final String password) throws InterruptedException, IOException, WrongStateException {
        try (final WListClient client = WListClientManager.getInstance().getNewClient()) {
            TokenManager.token = OperateUserHelper.login(client, username, password);
        }
    }

    @NonNull public static synchronized String getToken() {
        if (TokenManager.token == null)
            throw new IllegalStateException("No token set.");
        return TokenManager.token;
    }

    public static synchronized boolean noToken() {
        return TokenManager.token == null;
    }

    public static synchronized void ensureToken(@NonNull final String username, @NonNull final String password) throws InterruptedException, IOException, WrongStateException {
        if (TokenManager.noToken())
            TokenManager.setToken(username, password);
    }
}
