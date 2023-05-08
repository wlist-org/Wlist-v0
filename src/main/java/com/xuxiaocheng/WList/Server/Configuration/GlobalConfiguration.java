package com.xuxiaocheng.WList.Server.Configuration;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
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
import java.util.Objects;
import java.util.Properties;

public class GlobalConfiguration {
    protected static @Nullable GlobalConfiguration instance;
    public static synchronized void init(final @Nullable File configurationPath) throws IOException {
        if (GlobalConfiguration.instance != null)
            throw new IllegalStateException("Global configuration is initialized. instance: " + GlobalConfiguration.instance + " configurationPath: " + (configurationPath == null ? "null" : configurationPath.getAbsolutePath()));
        GlobalConfiguration.instance = new GlobalConfiguration();
        if (configurationPath == null)
            return;
        if (!HFileHelper.ensureFileExist(configurationPath))
            throw new IOException("Failed to create configuration file. configurationPath: " + configurationPath.getAbsolutePath());
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(configurationPath))) {
            final Properties properties = new Properties();
            properties.load(stream);
            GlobalConfiguration.instance.fromProperties(properties);
        } catch (final RuntimeException exception) {
            throw new IOException(exception);
        }
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(configurationPath))) {
            GlobalConfiguration.instance.toProperties().store(stream, null);
        } catch (final RuntimeException exception) {
            throw new IOException(exception);
        }
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

    protected @NotNull Properties toProperties() {
        final Properties properties = new Properties();
        properties.put("port", String.valueOf(this.port));
        properties.put("max_connection", String.valueOf(this.maxConnection));
        properties.put("data_db", this.dataDBPath);
        properties.put("index_db", this.indexDBPath);
        properties.put("thread_count", String.valueOf(this.threadCount));
        properties.put("token_expire_time", String.valueOf(this.tokenExpireTime));
        properties.put("id_idle_expire_time", String.valueOf(this.idIdleExpireTime));
        properties.put("max_limit_per_page", String.valueOf(this.maxLimitPerPage));
        return properties;
    }

    protected void fromProperties(final @NotNull Properties properties) throws IOException {
        try {
            this.port = ((Integer) properties.getOrDefault("port", this.port)).intValue();
            this.maxConnection = ((Integer) properties.getOrDefault("max_connection", this.maxConnection)).intValue();
            this.dataDBPath = (String) properties.getOrDefault("data_db", this.dataDBPath);
            this.indexDBPath = (String) properties.getOrDefault("index_db", this.indexDBPath);
            this.threadCount = ((Integer) properties.getOrDefault("thread_count", this.threadCount)).intValue();
            this.tokenExpireTime = ((Integer) properties.getOrDefault("token_expire_time", this.tokenExpireTime)).intValue();
            this.idIdleExpireTime = ((Integer) properties.getOrDefault("id_idle_expire_time", this.idIdleExpireTime)).intValue();
            this.maxLimitPerPage = ((Integer) properties.getOrDefault("max_limit_per_page", this.maxLimitPerPage)).intValue();
        } catch (final ClassCastException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalConfiguration that)) return false;
        return this.port == that.port && this.maxConnection == that.maxConnection && this.threadCount == that.threadCount && this.tokenExpireTime == that.tokenExpireTime && this.idIdleExpireTime == that.idIdleExpireTime && this.maxLimitPerPage == that.maxLimitPerPage && this.dataDBPath.equals(that.dataDBPath) && this.indexDBPath.equals(that.indexDBPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.port, this.maxConnection, this.dataDBPath, this.indexDBPath, this.threadCount, this.tokenExpireTime, this.idIdleExpireTime, this.maxLimitPerPage);
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
                '}';
    }
}
