package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
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
import java.util.stream.Collectors;

public record GlobalConfiguration(int port, int maxConnection,
                                  long tokenExpireTime, long idIdleExpireTime,
                                  int maxLimitPerPage, int forwardDownloadCacheCount,
                                  boolean deleteDriver, long maxCacheSize,
                                  @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers) {
    private static final @NotNull HInitializer<Pair<@NotNull GlobalConfiguration, @Nullable File>> instance = new HInitializer<>("GlobalConfiguration");

    public static synchronized void initialize(final @Nullable File path) throws IOException {
        final Map<String, Object> config;
        if (path != null) {
            if (!HFileHelper.ensureFileExist(path))
                throw new IOException("Failed to create configuration file. path: " + path.getAbsolutePath());
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
                config = YamlHelper.loadYaml(inputStream); // config.putAll
            }
        } else
            config = Map.of();
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final GlobalConfiguration configuration = new GlobalConfiguration(
            YamlHelper.getConfig(config, "port", "5212",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ZERO, BigInteger.valueOf(65535))).intValue(),
            YamlHelper.getConfig(config, "max_connection", "128",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_connection", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE) /*100*/)).intValue(),
            YamlHelper.getConfig(config, "token_expire_time", "259200",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "token_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
            YamlHelper.getConfig(config, "id_idle_expire_time", "1800",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "id_idle_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
            YamlHelper.getConfig(config, "max_limit_per_page", "500",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_limit_per_page", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE))).intValue(),
            YamlHelper.getConfig(config, "forward_download_cache_count", "3",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "forward_download_cache_count", BigInteger.ZERO, BigInteger.valueOf(Integer.MAX_VALUE))).intValue(),
            YamlHelper.getConfig(config, "delete_driver", "false",
                o -> YamlHelper.transferBooleanFromStr(o, errors, "delete_driver")).booleanValue(),
            YamlHelper.getConfig(config, "max_cache_size", "1000",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_cache_size", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
            YamlHelper.getConfig(config, "drivers", new LinkedHashMap<>(),
                o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "drivers");
                    if (map == null) return new LinkedHashMap<>();
                    final Map<String, WebDriversType> drivers = new LinkedHashMap<>(map.size());
                    for (final Map.Entry<String, Object> e: map.entrySet()) {
                        final String identifier = YamlHelper.transferString(e.getValue(), errors, "driver(" + e.getKey() + ')');
                        if (identifier == null) continue;
                        final WebDriversType type = WebDriversType.get(identifier);
                        if (type == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Unsupported driver type.", ParametersMap.create().add("name", e.getKey()).add("identifier", identifier));
                            continue;
                        }
                        drivers.put(e.getKey(), type);
                    }
                    return drivers;
                })
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
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("port", configuration.port);
        config.put("max_connection", configuration.maxConnection);
        config.put("token_expire_time", configuration.tokenExpireTime);
        config.put("id_idle_expire_time", configuration.idIdleExpireTime);
        config.put("max_limit_per_page", configuration.maxLimitPerPage);
        config.put("forward_download_cache_count", configuration.forwardDownloadCacheCount);
        config.put("delete_driver", configuration.deleteDriver);
        config.put("max_cache_size", configuration.maxCacheSize);
        config.put("drivers", configuration.drivers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().getIdentifier())));
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
            YamlHelper.dumpYaml(config, outputStream);
        }
    }

    public static synchronized void reInitialize(final @NotNull GlobalConfiguration configuration) throws IOException {
        GlobalConfiguration.instance.getInstance().setFirst(configuration);
        GlobalConfiguration.dumpToFile();
    }

    /**
     * @see DriverManager#addDriver(String, WebDriversType)
     */
    public static synchronized void addUninitializedDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException {
        GlobalConfiguration.getInstance().drivers.put(name, type);
        GlobalConfiguration.dumpToFile();
    }

    /**
     * @see DriverManager#removeDriver(String)
     */
    public static synchronized void removeUninitializedDriver(final @NotNull String name) throws IOException {
        if (GlobalConfiguration.getInstance().drivers.remove(name) != null)
            GlobalConfiguration.dumpToFile();
    }
}
