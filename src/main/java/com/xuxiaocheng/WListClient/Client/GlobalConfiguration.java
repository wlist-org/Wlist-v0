package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListClient.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public record GlobalConfiguration(String host, int port, int limit, int threadCount) {
    private static final @NotNull HInitializer<Pair<@NotNull GlobalConfiguration, @Nullable File>> instance = new HInitializer<>("ClientGlobalConfiguration");

    public static synchronized void initialize(final @Nullable File file) throws IOException {
        final Map<String, Object> config;
        if (file != null) {
            try {
                HFileHelper.ensureFileExist(file.toPath(), true);
            } catch (final IOException exception) {
                throw new IOException("Failed to create global configuration file." + ParametersMap.create().add("file", file), exception);
            }
            if (!file.canRead() || !file.canWrite())
                throw new AccessDeniedException("No permissions to read or write global configuration file." + ParametersMap.create().add("file", file));
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                config = YamlHelper.loadYaml(inputStream);
            }
        } else
            config = Map.of();
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final GlobalConfiguration configuration = new GlobalConfiguration(
            YamlHelper.getConfig(config, "host", "localhost",
                    o -> YamlHelper.transferString(o, errors, "host")),
            YamlHelper.getConfig(config, "port", 5212,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ZERO, BigInteger.valueOf(65535))).intValue(),
            YamlHelper.getConfig(config, "limit", 20,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, "limit", BigInteger.ONE, BigInteger.valueOf(200))).intValue(),
            YamlHelper.getConfig(config, "thread_count", 4,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, "thread_count", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE))).intValue()
        );
        YamlHelper.throwErrors(errors);
        GlobalConfiguration.instance.initialize(Pair.ImmutablePair.makeImmutablePair(configuration, file));
        GlobalConfiguration.dumpToFile();
    }

    public static synchronized @NotNull GlobalConfiguration getInstance() {
        return GlobalConfiguration.instance.getInstance().getFirst();
    }

    private static synchronized void dumpToFile() throws IOException {
        final GlobalConfiguration configuration = GlobalConfiguration.instance.getInstance().getFirst();
        final File file = GlobalConfiguration.instance.getInstance().getSecond();
        if (file == null)
            return;
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("host", configuration.host);
        config.put("port", configuration.port);
        config.put("limit", configuration.limit);
        config.put("thread_count", configuration.threadCount);
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            YamlHelper.dumpYaml(config, outputStream);
        }
    }

    public static synchronized void reInitialize(final @NotNull GlobalConfiguration configuration) throws IOException {
        GlobalConfiguration.instance.getInstance().setFirst(configuration);
        GlobalConfiguration.dumpToFile();
    }

    public static synchronized void setPath(final @Nullable File file) throws IOException {
        if (file != null) {
            try {
                HFileHelper.ensureFileExist(file.toPath(), true);
            } catch (final IOException exception) {
                throw new IOException("Failed to create global configuration file." + ParametersMap.create().add("file", file), exception);
            }
            if (!file.canWrite())
                throw new AccessDeniedException("No permissions to write global configuration file." + ParametersMap.create().add("file", file));
        }
        GlobalConfiguration.instance.getInstance().setSecond(file);
        GlobalConfiguration.dumpToFile();
    }
}
