package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
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
import java.util.Objects;
import java.util.stream.Collectors;

public record GlobalConfiguration(boolean dumpConfiguration, int port, int maxConnection,
                                  @NotNull String databasePath,
                                  long tokenExpireTime, long idIdleExpireTime,
                                  int maxLimitPerPage,
                                  @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers,
                                  boolean deleteDriver) {
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
                YamlHelper.getConfig(config, "port", "5212",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ONE, BigInteger.valueOf(65535))).intValue(),
                YamlHelper.getConfig(config, "max_connection", "128",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "max_connection", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE) /*100*/)).intValue(),
                YamlHelper.getConfig(config, "database_path", "data.db",
                        o -> YamlHelper.transferString(o, errors, "database_path")),
                YamlHelper.getConfig(config, "token_expire_time", "259200",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "token_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
                YamlHelper.getConfig(config, "id_idle_expire_time", "1800",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "id_idle_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
                YamlHelper.getConfig(config, "max_limit_per_page", "500",
                        o -> YamlHelper.transferIntegerFromStr(o, errors, "max_limit_per_page", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE))).intValue(),
                YamlHelper.getConfig(config, "drivers", new LinkedHashMap<>(),
                        o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "drivers");
                            if (map == null) return Map.of();
                            return map.entrySet().stream().map(e -> {
                                final WebDriversType type = WebDriversType.get(
                                        YamlHelper.transferString(e.getValue(), errors, "driver(" + e.getKey() + ')'));
                                if (type == null) //noinspection ReturnOfNull
                                    return null;
                                return Pair.ImmutablePair.makeImmutablePair(e.getKey(), type);
                            }).filter(Objects::nonNull)
                                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                        }),
                YamlHelper.getConfig(config, "delete_driver", "false",
                        o -> YamlHelper.transferBooleanFromStr(o, errors, "delete_driver")).booleanValue()
            );
        } catch (final RuntimeException exception) {
            throw new IOException(exception);
        }
        YamlHelper.throwErrors(errors);
        if (GlobalConfiguration.instance.dumpConfiguration && path != null) {
            config.put("dumpConfiguration", true);
            config.put("port", GlobalConfiguration.instance.port);
            config.put("max_connection", GlobalConfiguration.instance.maxConnection);
            config.put("database_path", GlobalConfiguration.instance.databasePath);
            config.put("token_expire_time", GlobalConfiguration.instance.tokenExpireTime);
            config.put("id_idle_expire_time", GlobalConfiguration.instance.idIdleExpireTime);
            config.put("max_limit_per_page", GlobalConfiguration.instance.maxLimitPerPage);
            config.put("drivers", GlobalConfiguration.instance.drivers.entrySet().stream()
                    .map(e -> Pair.ImmutablePair.makeImmutablePair(e.getKey(), e.getValue().name()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
            config.put("delete_driver", GlobalConfiguration.instance.deleteDriver);
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
