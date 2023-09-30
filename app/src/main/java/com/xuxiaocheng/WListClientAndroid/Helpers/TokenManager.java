package com.xuxiaocheng.WListClientAndroid.Helpers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public final class TokenManager {
    private TokenManager() {
        super();
    }

    public static final @NotNull HMultiInitializers<InetSocketAddress, String> tokens = new HMultiInitializers<>("TokenManager");

    public static boolean setToken(final @NotNull InetSocketAddress address, final @NotNull String passport, final @NotNull String password) throws InterruptedException, IOException, WrongStateException {
        final String token;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            token = OperateSelfHelper.login(client, passport, password);
        }
        TokenManager.tokens.reinitializeNullable(address, token);
        if (token == null) return false;
        final String payload = token.substring(0, token.indexOf('.'));
        final long exp = JSON.parseObject(Base64.getDecoder().decode(payload.getBytes(StandardCharsets.UTF_8))).getLongValue("exp");
        final Duration duration = Duration.between(ZonedDateTime.now(), ZonedDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneOffset.UTC).minusMinutes(3));
        if (duration.isNegative()) return true;
        Main.runOnBackgroundThread(null, HExceptionWrapper.wrapRunnable(() -> {
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.FINE, "Automatically refreshing token.", ParametersMap.create().add("address", address).add("passport", passport));
                    TokenManager.setToken(address, passport, password);
                }), duration.toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    public static @NotNull String getToken(final @NotNull InetSocketAddress address) {
        return TokenManager.tokens.getInstance(address);
    }
}
