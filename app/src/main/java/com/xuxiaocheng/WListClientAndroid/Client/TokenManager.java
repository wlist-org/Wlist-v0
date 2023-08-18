package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;

import java.io.IOException;
import java.net.SocketAddress;

public final class TokenManager {
    private TokenManager() {
        super();
    }

    @Nullable private static String token;

    public static synchronized void setToken(@NonNull final SocketAddress address, @NonNull final String username, @NonNull final String password) throws InterruptedException, IOException, WrongStateException {
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            TokenManager.token = OperateUserHelper.login(client, username, password);
        }
        // TODO refresh token.
    }

    @NonNull public static synchronized String getToken() {
        if (TokenManager.token == null)
            throw new IllegalStateException("No token set.");
        return TokenManager.token;
    }

    public static synchronized boolean noToken() {
        return TokenManager.token == null;
    }
}
