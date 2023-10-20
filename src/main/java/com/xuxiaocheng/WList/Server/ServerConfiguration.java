package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
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
import java.util.stream.Collectors;

public record ServerConfiguration(int port, int maxServerBacklog,
                                  long tokenExpireTime, long idIdleExpireTime,
                                  int maxLimitPerPage, boolean allowDropIndexAfterUninitializeProvider,
                                  @NotNull Map<@NotNull String, @NotNull StorageTypes<?>> providers) {
    public static final @NotNull HInitializer<File> Location = new HInitializer<>("ServerConfigurationLocation");
    private static final @NotNull HInitializer<ServerConfiguration> instance = new HInitializer<>("ServerConfiguration");

    public static @NotNull ServerConfiguration get() {
        return ServerConfiguration.instance.getInstance();
    }

    /**
     * @see StorageManager#addStorage(String, StorageTypes, Map)
     * @see StorageManager#removeStorage(String, boolean)
     */
    public static void set(final @NotNull ServerConfiguration configuration) throws IOException {
        final ServerConfiguration existed = ServerConfiguration.instance.getInstanceNullable();
        if (existed != null && !existed.providers.equals(configuration.providers))
            throw new IllegalStateException("Cannot set server configuration with different provider types.");
        ServerConfiguration.instance.reinitialize(configuration);
        ServerConfiguration.dumpToFile();
    }

    public static @NotNull ServerConfiguration parse(final @Nullable InputStream stream) throws IOException {
        final Map<String, Object> config = stream == null ? Map.of() : YamlHelper.loadYaml(stream);
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        final ServerConfiguration configuration = new ServerConfiguration(
            YamlHelper.getConfig(config, "port", 5212,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "port", BigInteger.ZERO, BigInteger.valueOf(65535))).intValue(),
            YamlHelper.getConfig(config, "max_server_backlog", 128,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_server_backlog", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE) /*100*/)).intValue(),
            YamlHelper.getConfig(config, "token_expire_time", TimeUnit.HOURS.toSeconds(6),
                o -> YamlHelper.transferIntegerFromStr(o, errors, "token_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
            YamlHelper.getConfig(config, "id_idle_expire_time", TimeUnit.MINUTES.toSeconds(30),
                o -> YamlHelper.transferIntegerFromStr(o, errors, "id_idle_expire_time", BigInteger.ONE, BigInteger.valueOf(Long.MAX_VALUE))).longValue(),
            YamlHelper.getConfig(config, "max_limit_per_page", 500,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_limit_per_page", BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE))).intValue(),
            YamlHelper.getConfig(config, "allow_drop_index_after_uninitialize_provider", true,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "allow_drop_index_after_uninitialize_provider")).booleanValue(),
            YamlHelper.getConfig(config, "providers", LinkedHashMap::new,
                o -> { final Map<String, Object> map = YamlHelper.transferMapNode(o, errors, "providers");
                    if (map == null) return null;
                    final Map<String, StorageTypes<?>> providers = new LinkedHashMap<>(map.size());
                    for (final Map.Entry<String, Object> e: map.entrySet()) {
                        final String reason = StorageManager.providerNameInvalidReason(e.getKey());
                        if (reason != null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Invalid provider name.", ParametersMap.create().add("name", e.getKey()).add("type", e.getValue().toString()).add("reason", reason));
                            continue;
                        }
                        final String identifier = YamlHelper.transferString(e.getValue(), errors, "provider(" + e.getKey() + ')');
                        if (identifier == null) continue;
                        final StorageTypes<?> type = StorageTypes.get(identifier);
                        if (type == null) {
                            HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Unsupported provider type.", ParametersMap.create().add("name", e.getKey()).add("identifier", identifier));
                            continue;
                        }
                        providers.put(e.getKey(), type);
                    }
                    return providers;
                })
        );
        YamlHelper.throwErrors(errors);
        return configuration;
    }

    public static void dump(final @NotNull ServerConfiguration configuration, final @NotNull OutputStream stream) throws IOException {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("port", configuration.port);
        config.put("max_server_backlog", configuration.maxServerBacklog);
        config.put("token_expire_time", configuration.tokenExpireTime);
        config.put("id_idle_expire_time", configuration.idIdleExpireTime);
        config.put("max_limit_per_page", configuration.maxLimitPerPage);
        config.put("allow_drop_index_after_uninitialize_provider", configuration.allowDropIndexAfterUninitializeProvider);
        config.put("providers", configuration.providers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().identifier(), (a, b) -> a, LinkedHashMap::new)));
        YamlHelper.dumpYaml(config, stream);
    }

    public static synchronized void parseFromFile() throws IOException {
        final File file = ServerConfiguration.Location.getInstanceNullable();
        if (file == null) {
            ServerConfiguration.set(ServerConfiguration.parse(null));
            return;
        }
        HFileHelper.ensureFileAccessible(file, true);
        final ServerConfiguration configuration;
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            configuration = ServerConfiguration.parse(stream);
        }
        ServerConfiguration.set(configuration);
    }

    public static synchronized void dumpToFile() throws IOException {
        final File file = ServerConfiguration.Location.getInstanceNullable();
        if (file == null) return;
        final ServerConfiguration configuration = ServerConfiguration.instance.getInstance();
        HFileHelper.writeFileAtomically(file, stream -> ServerConfiguration.dump(configuration, stream));
    }
}
