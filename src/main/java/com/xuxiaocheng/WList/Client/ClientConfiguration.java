package com.xuxiaocheng.WList.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
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

public record ClientConfiguration(String host, int port, int limit, int threadCount) {
    private static final @NotNull HInitializer<Pair<@NotNull ClientConfiguration, @Nullable File>> instance = new HInitializer<>("ClientGlobalConfiguration");

    public static synchronized void initialize(final @Nullable File file) throws IOException {
        final Map<String, Object> config;
        if (file != null) {
            try {
                HFileHelper.ensureFileAccessible(file, true);
            } catch (final IOException exception) {
                throw new IOException(I18NUtil.get("client.configuration.failed_create", file), exception);
            }
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                config = YamlHelper.loadYaml(inputStream);
            }
        } else
            config = Map.of();
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final ClientConfiguration configuration = new ClientConfiguration(
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
        ClientConfiguration.instance.initialize(Pair.ImmutablePair.makeImmutablePair(configuration, file));
        ClientConfiguration.dumpToFile();
    }

    public static synchronized @NotNull ClientConfiguration getInstance() {
        return ClientConfiguration.instance.getInstance().getFirst();
    }

    private static synchronized void dumpToFile() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.instance.getInstance().getFirst();
        final File file = ClientConfiguration.instance.getInstance().getSecond();
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

    public static synchronized void reInitialize(final @NotNull ClientConfiguration configuration) throws IOException {
        ClientConfiguration.instance.getInstance().setFirst(configuration);
        ClientConfiguration.dumpToFile();
    }

    public static synchronized void setPath(final @Nullable File file) throws IOException {
        if (file != null)
            try {
                HFileHelper.ensureFileAccessible(file, true);
            } catch (final IOException exception) {
                throw new IOException(I18NUtil.get("client.configuration.failed_create", file), exception);
            }
        ClientConfiguration.instance.getInstance().setSecond(file);
        ClientConfiguration.dumpToFile();
    }
}
