package com.xuxiaocheng.WList.DataAccessObjects;

import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

public final class ConstantSqlHelper {
    private ConstantSqlHelper() {
        super();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String DefaultRandomChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-_=+[]{}\\|;:,.<>/? ";

    public static void init() throws SQLException {
        try (final Connection connection = DatabaseUtil.getInstance().getConnection()) {
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

    public static @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue) throws SQLException {
        try (final Connection connection = DatabaseUtil.getInstance().getConnection()) {
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
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO constants (key, value)
                            VALUES (?, ?);
                        """)) {
                statement.setString(1, key);
                statement.setString(2, value);
                statement.executeUpdate();
            }
            connection.commit();
            return value;
        }
    }

    public static @NotNull String getSafely(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @NotNull String constDefaultValue) {
        try {
            return ConstantSqlHelper.get(key, defaultValue);
        } catch (final SQLException exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
            return constDefaultValue;
        }
    }
}
