package com.xuxiaocheng.WList.Configurations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class GlobalConfiguration {
    protected static @Nullable GlobalConfiguration instance;

    public static @NotNull GlobalConfiguration getInstance(final @Nullable InputStream input) {
        if (GlobalConfiguration.instance == null) {
            if (input == null)
                throw new IllegalArgumentException("input must be not null when initializing.");
            GlobalConfiguration.instance = new Yaml().loadAs(input, GlobalConfiguration.class);
            if (GlobalConfiguration.instance == null) // Empty input stream.
                GlobalConfiguration.instance = new GlobalConfiguration();
        }
        return GlobalConfiguration.instance;
    }

    protected GlobalConfiguration() {
        super();
    }

    protected int port = 5212;

    public int getPort() {
        return this.port;
    }

    @Deprecated
    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "GlobalConfiguration{" +
                "port=" + this.port +
                '}';
    }
}
