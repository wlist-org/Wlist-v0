package com.xuxiaocheng.WList.Internal.Utils;

import com.xuxiaocheng.WList.Configurations.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

    protected final @NotNull File path;
    protected final @NotNull Connection sqliteConnection;

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
        final SQLiteDataSource sqliteDataSource = new SQLiteDataSource();
        sqliteDataSource.setUrl("jdbc:sqlite:" + this.path.getPath());
        this.sqliteConnection = sqliteDataSource.getConnection();
        if (this.sqliteConnection == null)
            throw new SQLException("Failed to get connection with sqlite database.");
    }

    public @NotNull File getPath() {
        return this.path;
    }

    public @NotNull Statement createStatement() throws SQLException {
        return this.sqliteConnection.createStatement();
    }

    @Override
    public @NotNull String toString() {
        return "SQLiteInstance{" +
                "path=" + this.path +
                '}';
    }
}
