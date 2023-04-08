package com.xuxiaocheng.WList.Configuration;

import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Objects;

public class GlobalConfiguration {
    protected static @Nullable GlobalConfiguration instance;

    public static void init(final @Nullable InputStream input) {
        if (GlobalConfiguration.instance == null) {
            if (input == null) {
                GlobalConfiguration.instance = new GlobalConfiguration();
                return;
            }
            GlobalConfiguration.instance = new Yaml().loadAs(input, GlobalConfiguration.class);
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
    protected @NotNull String data_db = "data/data.db";
    protected @NotNull String index_db = "data/index.db";

    public int getPort() {
        return this.port;
    }

    public @NotNull String getData_db() {
        return this.data_db;
    }

    public @NotNull String getIndex_db() {
        return this.index_db;
    }

    @Deprecated
    public void setPort(final int port) {
        this.port = port;
    }

    @Deprecated
    public void setData_db(final @NotNull String data_db) {
        this.data_db = data_db;
    }

    @Deprecated
    public void setIndex_db(final @NotNull String index_db) {
        this.index_db = index_db;
    }

    @Override
    public String toString() {
        return "GlobalConfiguration{" +
                "port=" + this.port +
                ", data_db='" + this.data_db + '\'' +
                ", index_db='" + this.index_db + '\'' +
                '}';
    }
}
