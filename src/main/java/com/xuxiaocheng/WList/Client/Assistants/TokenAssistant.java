package com.xuxiaocheng.WList.Client.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TokenAssistant {
    private TokenAssistant() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull String, @NotNull ScheduledFuture<?>>> map = new ConcurrentHashMap<>();

    @Contract(pure = true)
    public static @NotNull String mergeKey(final @NotNull SocketAddress address, final @NotNull String username) {
        return address + "@" + username;
    }

    public static void login(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull String password, final @NotNull ScheduledExecutorService executor) throws IOException, InterruptedException, WrongStateException {
        final Pair.ImmutablePair<String, ZonedDateTime> token;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            token = OperateSelfHelper.login(client, username, password);
        }
        if (token == null) {
            TokenAssistant.map.remove(TokenAssistant.mergeKey(address, username));
            return;
        }
        final long duration = Duration.between(MiscellaneousUtil.now(), token.getSecond().minusSeconds(30)).toMillis();
        if (duration <= 0) {
            TokenAssistant.map.remove(TokenAssistant.mergeKey(address, username));
            return;
        }
        TokenAssistant.map.compute(TokenAssistant.mergeKey(address, username), (k, o) -> {
            final Pair<String, ScheduledFuture<?>> t;
            if (o != null) {
                o.getSecond().cancel(false);
                o.setFirst(token.getFirst());
                t = o;
            } else t = Pair.makePair(token.getFirst(), null);
            t.setSecond(executor.schedule(HExceptionWrapper.wrapRunnable(() ->
                            TokenAssistant.login(address, username, password, executor), MiscellaneousUtil.exceptionCallback, true),
                    duration, TimeUnit.MILLISECONDS));
            return t;
        });
    }

    public static @NotNull String getToken(final @NotNull SocketAddress address, final @NotNull String username) {
        final Pair<String, ScheduledFuture<?>> token = TokenAssistant.map.get(TokenAssistant.mergeKey(address, username));
        if (token == null)
            throw new IllegalStateException("No token set." + ParametersMap.create().add("address", address).add("username", username));
        return token.getFirst();
    }

    public static void removeToken(final @NotNull SocketAddress address, final @NotNull String username) {
        final Pair<String, ScheduledFuture<?>> token = TokenAssistant.map.remove(TokenAssistant.mergeKey(address, username));
        if (token != null)
            token.getSecond().cancel(true);
    }
}
