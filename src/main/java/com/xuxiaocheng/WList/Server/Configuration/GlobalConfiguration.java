package com.xuxiaocheng.WList.Server.Configuration;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class GlobalConfiguration {
    protected static @Nullable GlobalConfiguration instance;

    public static void init(final @Nullable InputStream input) throws IOException {
        if (GlobalConfiguration.instance == null) {
            if (input == null) {
                GlobalConfiguration.instance = new GlobalConfiguration();
                return;
            }
            try {
                GlobalConfiguration.instance = new Yaml().loadAs(input, GlobalConfiguration.class);
                input.close();
            } catch (final RuntimeException exception) {
                throw new IOException(exception);
            }
            if (GlobalConfiguration.instance == null) // Empty input stream.
                GlobalConfiguration.instance = new GlobalConfiguration();
        }
    }

    public static @NotNull GlobalConfiguration getInstance() {
        return Objects.requireNonNullElseGet(GlobalConfiguration.instance, GlobalConfiguration::new);
    }

    protected GlobalConfiguration() {
        super();
    }

    protected @IntRange(minimum = 0, maximum = 65535) int port = 5212;
    protected @IntRange(minimum = 1) int max_connection = 128;
    protected @NotNull String data_db = "data/data.db";
    protected @NotNull String index_db = "data/index.db";
    protected @IntRange(minimum = 1) int thread_count = 10;
    protected @IntRange(minimum = 1) int token_expire_time = 259200;
    protected @IntRange(minimum = 1) int download_id_expire_time = 1800;
    protected @IntRange(minimum = 1) int max_limit = 100;

    public int getPort() {
        return this.port;
    }

    public int getMax_connection() {
        return this.max_connection;
    }

    public @NotNull String getData_db() {
        return this.data_db;
    }

    public @NotNull String getIndex_db() {
        return this.index_db;
    }

    public int getThread_count() {
        return this.thread_count;
    }

    public int getToken_expire_time() {
        return this.token_expire_time;
    }

    public int getDownload_id_expire_time() {
        return this.download_id_expire_time;
    }

    public int getMax_limit() {
        return this.max_limit;
    }

    @Deprecated
    public void setPort(final int port) {
        this.port = port;
    }

    @Deprecated
    public void setMax_connection(final int max_connection) {
        this.max_connection = max_connection;
    }

    @Deprecated
    public void setData_db(final @NotNull String data_db) {
        this.data_db = data_db;
    }

    @Deprecated
    public void setIndex_db(final @NotNull String index_db) {
        this.index_db = index_db;
    }

    @Deprecated
    public void setThread_count(final int thread_count) {
        this.thread_count = thread_count;
    }

    @Deprecated
    public void setToken_expire_time(final int token_expire_time) {
        this.token_expire_time = token_expire_time;
    }

    @Deprecated
    public void setDownload_id_expire_time(final int download_id_expire_time) {
        this.download_id_expire_time = download_id_expire_time;
    }

    @Deprecated
    public void setMax_limit(final int max_limit) {
        this.max_limit = max_limit;
    }

    // TODO equal

    @Override
    public @NotNull String toString() {
        return "GlobalConfiguration{" +
                "port=" + this.port +
                ", data_db='" + this.data_db + '\'' +
                ", index_db='" + this.index_db + '\'' +
                ", thread_count=" + this.thread_count +
                ", token_expire_time=" + this.token_expire_time +
                '}';
    }
}
