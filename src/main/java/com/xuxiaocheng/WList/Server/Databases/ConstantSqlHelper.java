package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static final @NotNull DatabaseUtil DefaultDatabaseUtil = HExceptionWrapper.wrapSupplier(DatabaseUtil::getInstance).get();

    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String DefaultRandomChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-_=+[]{}\\|;:,.<>/? ";

    public static void initialize(final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = ConstantSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
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

    public static @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = ConstantSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
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
}
