package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.WList.Configuration.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SQLiteUtil {
    protected static @Nullable SQLiteUtil DataDB;
    protected static @Nullable SQLiteUtil IndexDB;

    public static @NotNull SQLiteUtil getDataInstance() throws SQLException {
        if (SQLiteUtil.DataDB == null)
            SQLiteUtil.DataDB = new SQLiteUtil(new File(GlobalConfiguration.getInstance().getData_db()));
        return SQLiteUtil.DataDB;
    }

    public static @NotNull SQLiteUtil getIndexInstance() throws SQLException {
        if (SQLiteUtil.IndexDB == null)
            SQLiteUtil.IndexDB = new SQLiteUtil(new File(GlobalConfiguration.getInstance().getIndex_db()));
        return SQLiteUtil.IndexDB;
    }

    protected final @NotNull Map<@NotNull String, @NotNull ReadWriteLock> lock = new ConcurrentHashMap<>();
    protected final @NotNull File path;
    protected final @NotNull SQLiteDataSource sqliteDataSource;
    protected final @NotNull Map<@NotNull String, @NotNull Connection> sqliteConnections = new ConcurrentHashMap<>();

    protected final @NotNull Connection getNewConnection() throws SQLException {
        final Connection connection = this.sqliteDataSource.getConnection();
        if (connection == null)
            throw new SQLException("Failed to get connection with sqlite database.");
        return connection;
    }

    protected SQLiteUtil(final @NotNull File path) throws SQLException {
        super();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException("Failed to load sqlite JDBC.", exception);
        }
        this.path = path.getAbsoluteFile();
        if (!this.path.exists() && !this.path.getParentFile().mkdirs() && !this.path.getParentFile().exists())
            throw new SQLException("Cannot create database directory.");
        this.sqliteDataSource = new SQLiteDataSource();
        this.sqliteDataSource.setUrl("jdbc:sqlite:" + this.path.getPath());
        this.sqliteConnections.put("default", this.getNewConnection());
    }

    public @NotNull File getPath() {
        return this.path;
    }

    public @NotNull ReadWriteLock getLock(final @NotNull String name) {
        return this.lock.computeIfAbsent(name, k -> new ReentrantReadWriteLock());
    }

    public @NotNull Connection getConnection(final @Nullable String name) throws SQLException {
        try {
            return this.sqliteConnections.computeIfAbsent(Objects.requireNonNullElse(name, "default"), k -> {
                try {
                    return this.getNewConnection();
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof SQLException sqlException)
                throw sqlException;
            throw exception;
        }
    }

    public void deleteConnection(final @Nullable String name) throws SQLException {
        final Connection connection = this.sqliteConnections.remove(Objects.requireNonNullElse(name, "default"));
        if (connection != null)
            connection.close();
    }

    @Override
    public @NotNull String toString() {
        return "SQLiteInstance{" +
                "path=" + this.path +
                '}';
    }
}
