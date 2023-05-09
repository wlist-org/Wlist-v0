package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class GlobalConfiguration {
    protected static @Nullable File path;
    protected static @Nullable GlobalConfiguration instance;
    public static synchronized void init(final @Nullable File configurationPath) throws IOException {
        if (GlobalConfiguration.instance != null)
            throw new IllegalStateException("Global configuration is initialized. instance: " + GlobalConfiguration.instance + " configurationPath: " + (configurationPath == null ? "null" : configurationPath.getAbsolutePath()));
        GlobalConfiguration.instance = new GlobalConfiguration();
        if (configurationPath == null)
            return;
        GlobalConfiguration.path = configurationPath;
        if (!HFileHelper.ensureFileExist(configurationPath))
            throw new IOException("Failed to create configuration file. path: " + configurationPath.getAbsolutePath());
        // TODO: In HeadLibs lib. XSML (XSML, the Strictest but Modifiable object Language)
        final Properties properties = new Properties();
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationPath))) {
            properties.load(inputStream);
        }
        for (final Map.Entry<Object, Object> entry: properties.entrySet())
            GlobalConfiguration.instance.drivers.put(entry.getKey().toString(), WebDriversType.valueOf(entry.getValue().toString()));
//        final LoadSettings loadSettings = LoadSettings.builder().setParseComments(true).setSchema(new FailsafeSchema()).build();
//        final Load loader = new Load(loadSettings, new CommentedYamlConstructor(loadSettings));
//        final Map<String, Object> configs;
//        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationPath))){
//            final Object config = loader.loadFromInputStream(inputStream);
//            if (config != null) {
//                if(!(config instanceof LinkedHashMap<?, ?>))
//                    throw new IOException("Invalid yaml config format.");
//                configs = (Map<String, Object>) config;
//            } else configs = new LinkedHashMap<>();
//        } catch (final RuntimeException exception) {
//            throw new IOException(exception);
//        }
//        final DumpSettings dumpSettings = DumpSettings.builder().setDumpComments(true).setDefaultFlowStyle(FlowStyle.BLOCK).build();
//        final Dump dumper = new Dump(dumpSettings);
//            final CommentedFileConfig toml = CommentedFileConfig.builder(configurationPath).preserveInsertionOrder().build();
//            toml.load();
//            GlobalConfiguration.instance.port = TomlUtil.getOrSet(toml, "port", GlobalConfiguration.instance.port, "Server port.");
//            GlobalConfiguration.instance.maxConnection = TomlUtil.getOrSet(toml, "max_connection", GlobalConfiguration.instance.maxConnection, "Server backlog.");
//            GlobalConfiguration.instance.dataDBPath = TomlUtil.getOrSet(toml, "data_db_path", GlobalConfiguration.instance.dataDBPath, "Server 'data' database path.");
//            GlobalConfiguration.instance.indexDBPath = TomlUtil.getOrSet(toml, "index_db_path", GlobalConfiguration.instance.indexDBPath, "Server 'index' database path.");
//            GlobalConfiguration.instance.threadCount = TomlUtil.getOrSet(toml, "thread_count", GlobalConfiguration.instance.threadCount, "Temp thread size (todo: delete).");
//            GlobalConfiguration.instance.tokenExpireTime = TomlUtil.getOrSet(toml, "token_expire_time", GlobalConfiguration.instance.tokenExpireTime, "Token expire time (sec).");
//            GlobalConfiguration.instance.idIdleExpireTime = TomlUtil.getOrSet(toml, "id_idle_expire_time", GlobalConfiguration.instance.idIdleExpireTime, "Id idle expire time (sec).");
//            GlobalConfiguration.instance.maxLimitPerPage = TomlUtil.getOrSet(toml, "max_limit_per_page", GlobalConfiguration.instance.maxLimitPerPage, "Client request 'FilesList' limit count per page.");
//            final Config drivers = TomlUtil.getOrSet(toml, "drivers", TomlFormat.newConfig(), "Web drivers with type.");
//            for (final Config.Entry entry: drivers.entrySet())
//                GlobalConfiguration.instance.drivers.put(entry.getKey(), WebDriversType.valueOf(entry.getValue()));
        // TODO in dif file.
        // TODO other check.
    }
    public static synchronized @NotNull GlobalConfiguration getInstance() {
        return Objects.requireNonNullElseGet(GlobalConfiguration.instance, GlobalConfiguration::new);
    }

    protected GlobalConfiguration() {
        super();
    }

    protected @IntRange(minimum = 0, maximum = 65535) int port = 5212;
    protected @IntRange(minimum = 1) int maxConnection = 128;
    protected @NotNull String dataDBPath = "data/data.db";
    protected @NotNull String indexDBPath = "data/index.db";
    protected @IntRange(minimum = 1) int threadCount = 10; // todo delete
    protected @IntRange(minimum = 1) int tokenExpireTime = 259200;
    protected @IntRange(minimum = 1) int idIdleExpireTime = 1800;
    protected @IntRange(minimum = 1) int maxLimitPerPage = 100;
    protected final @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers = new HashMap<>();

    public int getPort() {
        return this.port;
    }

    public int getMaxConnection() {
        return this.maxConnection;
    }

    public @NotNull String getDataDBPath() {
        return this.dataDBPath;
    }

    public @NotNull String getIndexDBPath() {
        return this.indexDBPath;
    }

    public int getThreadCount() {
        return this.threadCount;
    }

    public int getTokenExpireTime() {
        return this.tokenExpireTime;
    }

    public int getIdIdleExpireTime() {
        return this.idIdleExpireTime;
    }

    public int getMaxLimitPerPage() {
        return this.maxLimitPerPage;
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull WebDriversType> getDrivers() {
        return Collections.unmodifiableMap(this.drivers);
    }

    public static synchronized void addDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException {
//        final File dif = new File(GlobalConfiguration.path + ".dif");
//        HFileHelper.ensureFileExist(dif);
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(dif, true))){
//            stream.write("+\n\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(name.getBytes(StandardCharsets.UTF_8));
//            stream.write("\n\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(type.name().getBytes(StandardCharsets.UTF_8));
//            stream.write("\n".getBytes(StandardCharsets.UTF_8));
//        }
    }

    public static synchronized void subDriver(final @NotNull String name) throws IOException {
//        final File dif = new File(GlobalConfiguration.path + ".dif");
//        HFileHelper.ensureFileExist(dif);
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(dif, true))){
//            stream.write("-\t".getBytes(StandardCharsets.UTF_8));
//            stream.write(name.getBytes(StandardCharsets.UTF_8));
//            stream.write("\n".getBytes(StandardCharsets.UTF_8));
//        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalConfiguration that)) return false;
        return this.port == that.port && this.maxConnection == that.maxConnection && this.threadCount == that.threadCount && this.tokenExpireTime == that.tokenExpireTime && this.idIdleExpireTime == that.idIdleExpireTime && this.maxLimitPerPage == that.maxLimitPerPage && this.dataDBPath.equals(that.dataDBPath) && this.indexDBPath.equals(that.indexDBPath) && this.drivers.equals(that.drivers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.port, this.maxConnection, this.dataDBPath, this.indexDBPath, this.threadCount, this.tokenExpireTime, this.idIdleExpireTime, this.maxLimitPerPage, this.drivers);
    }

    @Override
    public @NotNull String toString() {
        return "GlobalConfiguration{" +
                "port=" + this.port +
                ", maxConnection=" + this.maxConnection +
                ", dataDBPath='" + this.dataDBPath + '\'' +
                ", indexDBPath='" + this.indexDBPath + '\'' +
                ", threadCount=" + this.threadCount +
                ", tokenExpireTime=" + this.tokenExpireTime +
                ", idIdleExpireTime=" + this.idIdleExpireTime +
                ", maxLimitPerPage=" + this.maxLimitPerPage +
                ", drivers=" + this.drivers +
                '}';
    }
}
