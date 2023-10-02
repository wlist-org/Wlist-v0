package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ClientConfigurationSupporter {
    private ClientConfigurationSupporter() {
        super();
    }

    public static int limitPerPage(final @NotNull ClientConfiguration configuration) {
        return configuration.limitPerPage();
    }

    public static int threadCount(final @NotNull ClientConfiguration configuration) {
        return configuration.threadCount();
    }

    public static int progressInterval(final @NotNull ClientConfiguration configuration) {
        return configuration.progressInterval();
    }

    public static @NotNull HInitializer<File> location() {
        return ClientConfiguration.Location;
    }

    public static @NotNull ClientConfiguration get() {
        return ClientConfiguration.get();
    }

    public static void set(final @NotNull ClientConfiguration configuration) throws IOException {
        ClientConfiguration.set(configuration);
    }

    public static @NotNull ClientConfiguration parse(final @Nullable InputStream stream) throws IOException {
        return ClientConfiguration.parse(stream);
    }

    public static void dump(final @NotNull ClientConfiguration configuration, final @NotNull OutputStream stream) throws IOException {
        ClientConfiguration.dump(configuration, stream);
    }

    public static synchronized void parseFromFile() throws IOException {
        ClientConfiguration.parseFromFile();
    }

    public static synchronized void dumpToFile() throws IOException {
        ClientConfiguration.dumpToFile();
    }
}
