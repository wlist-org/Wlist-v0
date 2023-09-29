package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Client.ClientConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@Deprecated
public final class GlobalConfigurationSupporter {
    private GlobalConfigurationSupporter() {
        super();
    }

    public static @NotNull String host(final @NotNull ClientConfiguration configuration) {
        return configuration.host();
    }

    public static int port(final @NotNull ClientConfiguration configuration) {
        return configuration.port();
    }

    public static int limit(final @NotNull ClientConfiguration configuration) {
        return configuration.limit();
    }

    public static int threadCount(final @NotNull ClientConfiguration configuration) {
        return configuration.threadCount();
    }

    public static @NotNull ClientConfiguration create(final @NotNull String host, final int port, final int limit, final int threadCount) {
        return new ClientConfiguration(host, port, limit, threadCount);
    }

    public static void initialize(final @Nullable File path) throws IOException {
        ClientConfiguration.initialize(path);
    }

    public static @NotNull ClientConfiguration getInstance() {
        return ClientConfiguration.getInstance();
    }

    public static void reInitialize(final @NotNull ClientConfiguration configuration) throws IOException {
        ClientConfiguration.reInitialize(configuration);
    }

    public static void setPath(final @Nullable File path) throws IOException {
        ClientConfiguration.setPath(path);
    }
}
