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
    protected @NotNull SqliteConfiguration sqlite = new SqliteConfiguration();

    public int getPort() {
        return this.port;
    }

    public @NotNull SqliteConfiguration getSqlite() {
        return this.sqlite;
    }

    @Deprecated
    public void setPort(final int port) {
        this.port = port;
    }

    public void setSqlite(final @NotNull SqliteConfiguration sqlite) {
        this.sqlite = sqlite;
    }

    @Override
    public String toString() {
        return "GlobalConfiguration{" +
                "port=" + this.port +
                '}';
    }

    public static class SqliteConfiguration {
        protected @NotNull String path = "data/users.db";
        protected @Nullable String username;
        protected @Nullable String password;

        public @NotNull String getPath() {
            return this.path;
        }

        public @Nullable String getUsername() {
            return this.username;
        }

        public @Nullable String getPassword() {
            return this.password;
        }

        @Deprecated
        public void setPath(final @NotNull String path) {
            this.path = path;
        }

        @Deprecated
        public void setUsername(final @Nullable String username) {
            this.username = username;
        }

        @Deprecated
        public void setPassword(final @Nullable String password) {
            this.password = password;
        }

        @Override
        public @NotNull String toString() {
            return "SqliteConfiguration{" +
                    "path='" + this.path + '\'' +
                    ", username='" + this.username + '\'' +
                    ", password='" + this.password + '\'' +
                    '}';
        }
    }
}
