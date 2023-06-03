package com.xuxiaocheng.WList.Server.Databases.Constant;

import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings("ClassHasNoToStringMethod")
final class ConstantSqlHelper {
    private static @Nullable ConstantSqlHelper instance;

    public static synchronized void initialize(final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        if (ConstantSqlHelper.instance != null)
            throw new IllegalStateException("Constant sql helper is initialized. instance: " + ConstantSqlHelper.instance);
        ConstantSqlHelper.instance = new ConstantSqlHelper(database ,_connectionId);
    }

    public static synchronized @NotNull ConstantSqlHelper getInstance() {
        if (ConstantSqlHelper.instance == null)
            throw new IllegalStateException("Constant sql helper is not initialized.");
        return ConstantSqlHelper.instance;
    }

    private final @NotNull DatabaseUtil database;

    private ConstantSqlHelper(final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.database = database;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS constants (
                        key         TEXT       PRIMARY KEY
                                               UNIQUE
                                               NOT NULL,
                        value       TEXT
                    );
                """);
            }
            connection.commit();
        }
    }

    public @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT value FROM constants WHERE key == ? LIMIT 1;
                """)) {
                statement.setString(1, key);
                try (final ResultSet constant = statement.executeQuery()) {
                    if (constant.next())
                        return constant.getString(1);
                }
            }
            final String value = defaultValue.get();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO constants (key, value) VALUES (?, ?);
                """)) {
                statement.setString(1, key);
                statement.setString(2, value);
                statement.executeUpdate();
            }
            connection.commit();
            return value;
        }
    }
}
