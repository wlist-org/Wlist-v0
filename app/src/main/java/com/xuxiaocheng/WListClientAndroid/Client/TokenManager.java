package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

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

    public static boolean setToken(@NonNull final InetSocketAddress address, @NonNull final String passport, @NonNull final String password) throws InterruptedException, IOException, WrongStateException {
        final String token;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            token = OperateUserHelper.login(client, passport, password);
        }
        TokenManager.tokens.reinitializeNullable(address, token);
        if (token == null) return false;
        final String payload = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'));
        final long exp = JSON.parseObject(Base64.getDecoder().decode(payload.getBytes(StandardCharsets.UTF_8))).getLongValue("exp");
        final Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC).minusMinutes(3));
        if (duration.isNegative()) return true;
        Main.runOnBackgroundThread(null, HExceptionWrapper.wrapRunnable(() -> {
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.FINE, "Automatically refreshing token.", ParametersMap.create().add("address", address).add("passport", passport));
                    TokenManager.setToken(address, passport, password);
                }), duration.toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    @NonNull public static String getToken(@NonNull final InetSocketAddress address) {
        return TokenManager.tokens.getInstance(address);
    }
}
