package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public record GlobalConfiguration(String host, int port, int limit, int threadCount) {
    private static final @NotNull HInitializer<Pair<@NotNull GlobalConfiguration, @Nullable File>> instance = new HInitializer<>("ClientGlobalConfiguration");

    public static synchronized void init(final @Nullable File path) throws IOException {
        final Map<String, Object> config;
        if (path != null) {
            if (!HFileHelper.ensureFileExist(path))
                throw new IOException("Failed to create configuration file. path: " + path.getAbsolutePath());
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
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
        GlobalConfiguration.instance.initialize(Pair.ImmutablePair.makeImmutablePair(configuration, path));
        GlobalConfiguration.dumpToFile();
    }

    public static synchronized @NotNull GlobalConfiguration getInstance() {
        return GlobalConfiguration.instance.getInstance().getFirst();
    }

    private static synchronized void dumpToFile() throws IOException {
        final GlobalConfiguration configuration = GlobalConfiguration.instance.getInstance().getFirst();
        final File path = GlobalConfiguration.instance.getInstance().getSecond();
        if (path == null)
            return;
        if (!HFileHelper.ensureFileExist(path))
            throw new IOException("Failed to create configuration file. path: " + path.getAbsolutePath());
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("host", configuration.host);
        config.put("port", configuration.port);
        config.put("limit", configuration.limit);
        config.put("thread_count", configuration.threadCount);
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
            YamlHelper.dumpYaml(config, outputStream);
        }
    }

    public static synchronized void reInitialize(final @NotNull GlobalConfiguration configuration) throws IOException {
        GlobalConfiguration.instance.getInstance().setFirst(configuration);
        GlobalConfiguration.dumpToFile();
    }

    public static synchronized void setPath(final @Nullable File path) throws IOException {
        GlobalConfiguration.instance.getInstance().setSecond(path);
        GlobalConfiguration.dumpToFile();
    }
}
