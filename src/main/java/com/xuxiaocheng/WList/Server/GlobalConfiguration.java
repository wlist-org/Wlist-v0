package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
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
import java.util.Objects;
import java.util.stream.Collectors;

public record GlobalConfiguration(int port, int maxConnection,
                                  long tokenExpireTime, long idIdleExpireTime,
                                  int maxLimitPerPage, int forwardDownloadCacheCount,
                                  boolean deleteDriver, long maxCacheSize,
                                  @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers) {
    private static @Nullable GlobalConfiguration instance;

    public static synchronized void initialize(final @Nullable File path) throws IOException {
        if (GlobalConfiguration.instance != null)
            throw new IllegalStateException("Global configuration is initialized. instance: " + GlobalConfiguration.instance + (path == null ? "" : " path: " + path.getAbsolutePath()));
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
        GlobalConfiguration.instance = new GlobalConfiguration(
            YamlHelper.getConfig(config, "port", "5212",
                o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ONE, BigInteger.valueOf(65535))).intValue(),
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
                    if (map == null) return Map.of();
                    return map.entrySet().stream().map(e -> {
                        final String identifier = YamlHelper.transferString(e.getValue(), errors, "driver(" + e.getKey() + ')');
                        if (identifier == null)
                            //noinspection ReturnOfNull
                            return null;
                        final WebDriversType type = WebDriversType.get(identifier);
                        if (type == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Unsupported driver type.", ParametersMap.create().add("name", e.getKey()).add("identifier", identifier));
                            //noinspection ReturnOfNull
                            return null;
                        }
                        return Pair.ImmutablePair.makeImmutablePair(e.getKey(), type);
                    }).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                })
        );
        YamlHelper.throwErrors(errors);
        if (path != null) {
            final Map<String, Object> configuration = new LinkedHashMap<>();
            configuration.put("port", GlobalConfiguration.instance.port);
            configuration.put("max_connection", GlobalConfiguration.instance.maxConnection);
            configuration.put("token_expire_time", GlobalConfiguration.instance.tokenExpireTime);
            configuration.put("id_idle_expire_time", GlobalConfiguration.instance.idIdleExpireTime);
            configuration.put("max_limit_per_page", GlobalConfiguration.instance.maxLimitPerPage);
            configuration.put("forward_download_cache_count", GlobalConfiguration.instance.forwardDownloadCacheCount);
            configuration.put("delete_driver", GlobalConfiguration.instance.deleteDriver);
            configuration.put("max_cache_size", GlobalConfiguration.instance.maxCacheSize);
            configuration.put("drivers", GlobalConfiguration.instance.drivers.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().getIdentifier())));
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
                YamlHelper.dumpYaml(configuration, outputStream);
            }
        }
    }

    public static synchronized @NotNull GlobalConfiguration getInstance() {
        if (GlobalConfiguration.instance == null)
            throw new IllegalStateException("Global configuration is not initialized.");
        return GlobalConfiguration.instance;
    }
}
