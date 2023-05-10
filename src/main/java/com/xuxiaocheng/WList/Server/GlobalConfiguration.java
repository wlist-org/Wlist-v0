package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.constructor.ConstructYamlNull;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.schema.FailsafeSchema;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record GlobalConfiguration(boolean dumpConfiguration, int port, int maxConnection,
                                  @NotNull String dataDBPath, @NotNull String indexDBPath,
                                  long tokenExpireTime, long idIdleExpireTime,
                                  int maxLimitPerPage, int threadCount,
                                  @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers) {
    private static @Nullable File path;
    private static @Nullable GlobalConfiguration instance;
    public static synchronized void init(final @Nullable File configurationPath) throws IOException {
        if (GlobalConfiguration.instance != null)
            throw new IllegalStateException("Global configuration is initialized. instance: " + GlobalConfiguration.instance + " configurationPath: " + (configurationPath == null ? "null" : configurationPath.getAbsolutePath()));
        GlobalConfiguration.path = configurationPath;
        // TODO in dif file.
        final Map<String, Object> config = new LinkedHashMap<>();
        if (configurationPath != null) {
            if (!HFileHelper.ensureFileExist(configurationPath))
                throw new IOException("Failed to create configuration file. path: " + configurationPath.getAbsolutePath());
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationPath))) {
                final Object yaml = new Load(LoadSettings.builder().setParseComments(true).setSchema(new FailsafeSchema())
                        .setTagConstructors(Map.of(Tag.NULL, new ConstructYamlNull())).build())
                        .loadFromInputStream(inputStream);
                if (yaml != null) {
                    if (!(yaml instanceof LinkedHashMap<?, ?> map))
                        throw new IOException("Invalid yaml config format.");
                    config.putAll(map.entrySet().stream()
                            .map(e -> Pair.ImmutablePair.makeImmutablePair(e.getKey().toString(), e.getValue()))
                            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
                }
            } catch (final RuntimeException exception) {
                throw new IOException(exception);
            }
        }
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        try {
            GlobalConfiguration.instance = new GlobalConfiguration(
                YamlHelper.checkConfig(config, "dump_configuration", "true",
                        o -> YamlHelper.getBooleanFromStr(o, errors, "dump_configuration")).booleanValue(),
                YamlHelper.checkConfig(config, "port", "5212",
                        o -> YamlHelper.getIntegerFromStr(o, errors, "port", 1, 65535)).intValue(),
                YamlHelper.checkConfig(config, "max_connection", "128",
                        o -> YamlHelper.getIntegerFromStr(o, errors, "max_connection", 1, null)).intValue(),
                YamlHelper.checkConfig(config, "data_db_path", "data/data.db",
                        o -> YamlHelper.getString(o, errors, "data_db_path")),
                YamlHelper.checkConfig(config, "index_db_path", "data/index.db",
                        o -> YamlHelper.getString(o, errors, "index_db_path")),
                YamlHelper.checkConfig(config, "token_expire_time", "259200",
                        o -> YamlHelper.getLongFromStr(o, errors, "token_expire_time", 1L, null)).longValue(),
                YamlHelper.checkConfig(config, "id_idle_expire_time", "1800",
                        o -> YamlHelper.getLongFromStr(o, errors, "id_idle_expire_time", 1L, null)).longValue(),
                YamlHelper.checkConfig(config, "max_limit_per_page", "100",
                        o -> YamlHelper.getIntegerFromStr(o, errors, "max_limit_per_page", 1, null)).intValue(),
                YamlHelper.checkConfig(config, "thread_count", "10",
                        o -> YamlHelper.getIntegerFromStr(o, errors, "thread_count", 1, null)).intValue(),
                YamlHelper.checkConfig(config, "drivers", new LinkedHashMap<>(), o -> {
                    final Map<Object, Object> map = YamlHelper.getMap(o, errors, "drivers");
                    if (map == null) return Map.of();
                    return map.entrySet().stream().map(e -> {
                        final String key = YamlHelper.getString(e.getKey(), errors, "driver(name)");
                        if (key == null) //noinspection ReturnOfNull
                            return null;
                        final String name = YamlHelper.getString(e.getValue(), errors, "driver(type)_" + key);
                        if (name == null) //noinspection ReturnOfNull
                            return null;
                        final WebDriversType type = WebDriversType.get(name);
                        if (type == null) //noinspection ReturnOfNull
                            return null;
                        return Pair.ImmutablePair.makeImmutablePair(key, type);
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                })
            );
        } catch (final RuntimeException exception) {
            throw new IOException(exception);
        }
        if (!errors.isEmpty())
            throw new IOException(errors.toString());
        if (GlobalConfiguration.instance.dumpConfiguration && configurationPath != null)
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(configurationPath))) {
                final Dump dumper = new Dump(DumpSettings.builder().setDumpComments(true)
                        .setDefaultFlowStyle(FlowStyle.BLOCK).build());
               outputStream.write(dumper.dumpToString(config).getBytes(StandardCharsets.UTF_8));
            }
    }
    public static synchronized @NotNull GlobalConfiguration getInstance() {
        if (GlobalConfiguration.instance == null)
            throw new IllegalStateException("Global configuration is not initialized.");
        return GlobalConfiguration.instance;
    }

    public static synchronized void addDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException {
        final File dif = new File(GlobalConfiguration.path + ".dif");
        HFileHelper.ensureFileExist(dif);
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(dif, true))){
//            stream.write("+\n\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(name.getBytes(StandardCharsets.UTF_8));
//            stream.write("\n\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(type.name().getBytes(StandardCharsets.UTF_8));
//            stream.write("\n".getBytes(StandardCharsets.UTF_8));
//        }
    }

    public static synchronized void subDriver(final @NotNull String name) throws IOException {
        final File dif = new File(GlobalConfiguration.path + ".dif");
        HFileHelper.ensureFileExist(dif);
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(dif, true))){
//            stream.write("-\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(name.getBytes(StandardCharsets.UTF_8));
//            stream.write("\n".getBytes(StandardCharsets.UTF_8));
//        }
    }
}
