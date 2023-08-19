package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClient.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListClientAndroid.Main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public final class TokenManager {
    private TokenManager() {
        super();
    }

    @NonNull public static final HMultiInitializers<InetSocketAddress, String> tokens = new HMultiInitializers<>("TokenManager");

    public static boolean setToken(@NonNull final InetSocketAddress address, @NonNull final String username, @NonNull final String password) throws InterruptedException, IOException, WrongStateException {
        final String token;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            token = OperateUserHelper.login(client, username, password);
        }
        TokenManager.tokens.reinitializeNullable(address, token);
        if (token == null) return false;
        final String payload = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'));
        final long exp = JSON.parseObject(Base64.getDecoder().decode(payload.getBytes(StandardCharsets.UTF_8))).getLongValue("exp");
        final Duration duration = Duration.between(LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC).minusMinutes(3), LocalDateTime.now());
        Main.AndroidExecutors.schedule(HExceptionWrapper.wrapRunnable(() -> TokenManager.setToken(address, username, password)),
                duration.toMillis(), TimeUnit.MILLISECONDS).addListener(MiscellaneousUtil.exceptionListener());
        return true; // Truth server.
    }

    @NonNull public static String getToken(@NonNull final InetSocketAddress address) {
        return TokenManager.tokens.getInstance(address);
    }
}
