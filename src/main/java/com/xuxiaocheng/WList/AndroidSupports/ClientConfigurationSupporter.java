package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;

public final class ClientConfigurationSupporter {
    private ClientConfigurationSupporter() {
        super();
    }

    public static int threadCount(final @NotNull ClientConfiguration configuration) {
        return configuration.threadCount();
    }

    public static int progressInterval(final @NotNull ClientConfiguration configuration) {
        return configuration.progressInterval();
    }

    public static int limitPerPage(final @NotNull ClientConfiguration configuration) {
        return configuration.limitPerPage();
    }

    public static @NotNull FilterPolicy filterPolicy(final @NotNull ClientConfiguration configuration) {
        return configuration.filterPolicy();
    }

    public static @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> fileOrders(final @NotNull ClientConfiguration configuration) {
        return configuration.fileOrders();
    }

    public static @NotNull DuplicatePolicy duplicatePolicy(final @NotNull ClientConfiguration configuration) {
        return configuration.duplicatePolicy();
    }

    public static @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, @NotNull OrderDirection> userOrders(final @NotNull ClientConfiguration configuration) {
        return configuration.userOrders();
    }

    public static @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> userGroupOrders(final @NotNull ClientConfiguration configuration) {
        return configuration.userGroupOrders();
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
