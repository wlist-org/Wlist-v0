package com.xuxiaocheng.WList.Databases.Constant;

import com.xuxiaocheng.WList.Utils.DatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ConstantSqlHelper implements ConstantSqlInterface {
    private final @NotNull DatabaseInterface database;

    public ConstantSqlHelper(final @NotNull DatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.database = database;
        this.createTable(_connectionId);
    }

    @Override
    public @NotNull Connection getConnection(@Nullable final String _connectionId, @Nullable final AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection("initialize", null)) {
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

    @Override
    public @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public @NotNull String toString() {
        return "ConstantSqlHelper{" +
                "database=" + this.database +
                '}';
    }
}
