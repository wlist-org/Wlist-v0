package com.xuxiaocheng.WList.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public record ClientConfiguration(int limitPerPage, int threadCount, int progressInterval) {
    public static final @NotNull HInitializer<File> Location = new HInitializer<>("ClientConfigurationLocation");
    private static final @NotNull HInitializer<ClientConfiguration> instance = new HInitializer<>("ClientConfiguration");

    public static @NotNull ClientConfiguration get() {
        return ClientConfiguration.instance.getInstance();
    }

    public static void set(final @NotNull ClientConfiguration configuration) throws IOException {
        ClientConfiguration.instance.reinitialize(configuration);
        ClientConfiguration.dumpToFile();
    }

    public static @NotNull ClientConfiguration parse(final @Nullable InputStream stream) throws IOException {
        final Map<String, Object> config = stream == null ? Map.of() : YamlHelper.loadYaml(stream);
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final ClientConfiguration configuration = new ClientConfiguration(
                YamlHelper.getConfig(config, "limit_per_page", 20,
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "limit_per_page", BigInteger.ONE, BigInteger.valueOf(100))).intValue(),
                YamlHelper.getConfig(config, "thread_count", 4,
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "thread_count", BigInteger.ONE, YamlHelper.IntegerMax)).intValue(),
                YamlHelper.getConfig(config, "progress_interval", TimeUnit.MILLISECONDS.toMillis(500),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "progress_interval", BigInteger.ONE, YamlHelper.IntegerMax)).intValue()
        );
        YamlHelper.throwErrors(errors);
        return configuration;
    }

    public static void dump(final @NotNull ClientConfiguration configuration, final @NotNull OutputStream stream) throws IOException {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("limit_per_page", configuration.limitPerPage);
        config.put("thread_count", configuration.threadCount);
        config.put("progress_interval", configuration.progressInterval);
        YamlHelper.dumpYaml(config, stream);
    }

    public static synchronized void parseFromFile() throws IOException {
        final File file = ClientConfiguration.Location.getInstanceNullable();
        if (file == null) {
            ClientConfiguration.set(ClientConfiguration.parse(null));
            return;
        }
        HFileHelper.ensureFileAccessible(file, true);
        final ClientConfiguration configuration;
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            configuration = ClientConfiguration.parse(stream);
        }
        ClientConfiguration.set(configuration);
    }

    public static synchronized void dumpToFile() throws IOException {
        final File file = ClientConfiguration.Location.getInstanceNullable();
        if (file == null) return;
        final ClientConfiguration configuration = ClientConfiguration.instance.getInstance();
        HFileHelper.writeFileAtomically(file, stream -> ClientConfiguration.dump(configuration, stream));
    }
}
