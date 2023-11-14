package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @see ServerConfiguration
 */
public final class ServerConfigurationSupporter {
    private ServerConfigurationSupporter() {
        super();
    }

    public static int port(final @NotNull ServerConfiguration configuration) {
        return configuration.port();
    }

    public static int maxServerBacklog(final @NotNull ServerConfiguration configuration) {
        return configuration.maxLimitPerPage();
    }

    public static long tokenExpireTime(final @NotNull ServerConfiguration configuration) {
        return configuration.tokenExpireTime();
    }

    public static long idIdleExpireTime(final @NotNull ServerConfiguration configuration) {
        return configuration.idIdleExpireTime();
    }

    public static int maxLimitPerPage(final @NotNull ServerConfiguration configuration) {
        return configuration.maxLimitPerPage();
    }

    public static boolean allowDropIndexAfterUninitializeProvider(final @NotNull ServerConfiguration configuration) {
        return configuration.allowDropIndexAfterUninitializeProvider();
    }

    public static @NotNull Map<@NotNull String, @NotNull StorageTypes<?>> providers(final @NotNull ServerConfiguration configuration) {
        return configuration.providers();
    }

    public static @NotNull ServerConfiguration get() {
        return ServerConfiguration.get();
    }

    public static @NotNull ServerConfiguration parse(final @Nullable InputStream stream) throws IOException {
        return ServerConfiguration.parse(stream);
    }

    public static void dump(final @NotNull ServerConfiguration configuration, final @NotNull OutputStream stream) throws IOException {
        ServerConfiguration.dump(configuration, stream);
    }

    public static synchronized void parseFromFile() throws IOException {
        ServerConfiguration.parseFromFile();
    }

    public static synchronized void dumpToFile() throws IOException {
        ServerConfiguration.dumpToFile();
    }

    public static void quicklySetLocation(final @NotNull File file) throws IOException {
        ServerConfiguration.quicklySetLocation(file);
    }
}
