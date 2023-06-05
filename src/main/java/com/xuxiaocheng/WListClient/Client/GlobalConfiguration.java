package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
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

public record GlobalConfiguration(boolean dumpConfiguration, String host, int port, int limit, boolean showPermissions) {
    private static @Nullable GlobalConfiguration instance;

    public static synchronized void init(final @Nullable File path) throws IOException {
        if (GlobalConfiguration.instance != null)
            throw new IllegalStateException("Global configuration is initialized. instance: " + GlobalConfiguration.instance + " path: " + (path == null ? "null" : path.getAbsolutePath()));
        final Map<String, Object> config = new LinkedHashMap<>();
        if (path != null) {
            if (!HFileHelper.ensureFileExist(path))
                throw new IOException("Failed to create configuration file. path: " + path.getAbsolutePath());
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
                config.putAll(YamlHelper.loadYaml(inputStream));
            }
        }
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        try {
            GlobalConfiguration.instance = new GlobalConfiguration(
                    YamlHelper.getConfig(config, "dump_configuration", "true",
                            o -> YamlHelper.transferBooleanFromStr(o, errors, "dump_configuration")).booleanValue(),
                YamlHelper.getConfig(config, "host", "localhost",
                        o -> YamlHelper.transferString(o, errors, "host")),
                YamlHelper.getConfig(config, "port", "5212",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ONE, BigInteger.valueOf(65535))).intValue(),
                YamlHelper.getConfig(config, "limit", "20",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "limit", BigInteger.ONE, BigInteger.valueOf(200))).intValue(),
                YamlHelper.getConfig(config, "show_permissions", "false",
                        o -> YamlHelper.transferBooleanFromStr(o, errors, "show_permissions")).booleanValue()
            );
        } catch (final RuntimeException exception) {
            throw new IOException(exception);
        }
        YamlHelper.throwErrors(errors);
        if (GlobalConfiguration.instance.dumpConfiguration && path != null) {
            config.put("dumpConfiguration", true);
            config.put("host", GlobalConfiguration.instance.host);
            config.put("port", GlobalConfiguration.instance.port);
            config.put("limit", GlobalConfiguration.instance.limit);
            config.put("show_permissions", GlobalConfiguration.instance.showPermissions);
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
                YamlHelper.dumpYaml(config, outputStream);
            }
        }
    }

    public static synchronized @NotNull GlobalConfiguration getInstance() {
        if (GlobalConfiguration.instance == null)
            throw new IllegalStateException("Global configuration is not initialized.");
        return GlobalConfiguration.instance;
    }
}
