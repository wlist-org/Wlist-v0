package com.xuxiaocheng.WList.Internal;

import com.xuxiaocheng.WList.Configurations.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteUtil {
    private SQLiteUtil() {
        super();
    }

    private static @Nullable Connection sqliteConnection;

    public static void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException("Failed to load JDBC.", exception);
        }
        final File path = new File(GlobalConfiguration.getInstance(null).getSqlite().getPath());
        if (!path.exists() && !path.getParentFile().mkdirs() && !path.getParentFile().exists())
            throw new SQLException("Cannot create database directory.");
        final SQLiteDataSource sqliteDataSource = new SQLiteDataSource();
        sqliteDataSource.setUrl("jdbc:sqlite:" + path.getAbsolutePath());
        SQLiteUtil.sqliteConnection = sqliteDataSource.getConnection(
                GlobalConfiguration.getInstance(null).getSqlite().getUsername(),
                GlobalConfiguration.getInstance(null).getSqlite().getPassword());
    }

    public static @NotNull Statement createStatement() throws SQLException {
        if (SQLiteUtil.sqliteConnection == null)
            SQLiteUtil.init();
        return SQLiteUtil.sqliteConnection.createStatement();
    }
}
