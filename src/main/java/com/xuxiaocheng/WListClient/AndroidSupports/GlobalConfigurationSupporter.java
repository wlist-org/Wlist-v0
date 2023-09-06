package com.xuxiaocheng.WListClient.AndroidSupports;

import com.xuxiaocheng.WListClient.Client.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public final class GlobalConfigurationSupporter {
    private GlobalConfigurationSupporter() {
        super();
    }

    public static @NotNull String host(final @NotNull GlobalConfiguration configuration) {
        return configuration.host();
    }

    public static int port(final @NotNull GlobalConfiguration configuration) {
        return configuration.port();
    }

    public static int limit(final @NotNull GlobalConfiguration configuration) {
        return configuration.limit();
    }

    public static int threadCount(final @NotNull GlobalConfiguration configuration) {
        return configuration.threadCount();
    }

    public static @NotNull GlobalConfiguration create(final @NotNull String host, final int port, final int limit, final int threadCount) {
        return new GlobalConfiguration(host, port, limit, threadCount);
    }

    public static void initialize(final @Nullable File path) throws IOException {
        GlobalConfiguration.initialize(path);
    }

    public static @NotNull GlobalConfiguration getInstance() {
        return GlobalConfiguration.getInstance();
    }

    public static void reInitialize(final @NotNull GlobalConfiguration configuration) throws IOException {
        GlobalConfiguration.reInitialize(configuration);
    }

    public static void setPath(final @Nullable File path) throws IOException {
        GlobalConfiguration.setPath(path);
    }
}
