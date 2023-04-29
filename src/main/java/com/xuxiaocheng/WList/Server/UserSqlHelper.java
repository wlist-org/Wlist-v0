package com.xuxiaocheng.WList.Server;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class UserSqlHelper {
    private UserSqlHelper() {
        super();
    }

    // Util

    private static @NotNull String generateRandomPassword() {
        final char[] word = new char[8];
        //noinspection SpellCheckingInspection
        HRandomHelper.setArray(HRandomHelper.RANDOM, word, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_".toCharArray());
        return new String(word);
    }

    private static @NotNull String encryptPassword(final @NotNull String password) {
        return MiscellaneousUtil.getMd5((password + /*UserSqlHelper.ServerPasswordSlat*/"With Server:SALT***#WListServer%CreateBy@XXC#***TokenMD5&SALT").getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String getModifyTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Helper

    public static void init(final @NotNull SortedSet<Operation.@NotNull Permission> defaultPermission, final @NotNull SortedSet<Operation.@NotNull Permission> adminPermission) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            connection.setAutoCommit(false);
            final boolean needCreate;
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                        CREATE TABLE IF NOT EXISTS users (
                            id          INTEGER    PRIMARY KEY AUTOINCREMENT
                                                   UNIQUE
                                                   NOT NULL,
                            username    TEXT       UNIQUE
                                                   NOT NULL,
                            password    TEXT       NOT NULL,
                            permission  TEXT       NOT NULL
                                                   DEFAULT '%s',
                            modify_time TEXT       NOT NULL
                        );
                        """, JSON.toJSONString(defaultPermission)));
                try (final ResultSet admins = statement.executeQuery(String.format("""
                        SELECT 1 FROM users WHERE permission == '%s' LIMIT 1;
                        """, JSON.toJSONString(adminPermission)))) {
                    needCreate = !admins.next();
                }
            }
            if (needCreate) {
                final String password = UserSqlHelper.generateRandomPassword();
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, permission, modify_time)
                            VALUES ('admin', ?, ?, ?)
                        ON CONFLICT (username) DO UPDATE SET
                            id = excluded.id, password = excluded.password,
                            permission = excluded.permission, modify_time = excluded.modify_time;
                        """)) {
                    statement.setString(1, UserSqlHelper.encryptPassword(password));
                    statement.setString(2, JSON.toJSONString(adminPermission));
                    statement.setString(3, UserSqlHelper.getModifyTime());
                    statement.executeUpdate();
                }
                HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user, password: ", password);
            }
            connection.commit();
        }
    }

    public static boolean insertUser(final @NotNull String username, final @NotNull String password, final @NotNull SortedSet<Operation.@NotNull Permission> permissions) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, permission, modify_time)
                            VALUES (?, ?, ?, ?);
                        """)) {
                statement.setString(1, username);
                statement.setString(2, UserSqlHelper.encryptPassword(password));
                statement.setString(3, permissions.toString());
                statement.setString(4, UserSqlHelper.getModifyTime());
                statement.executeUpdate();
                return true;
            }
        } catch (final SQLException exception) {
            if (exception.getMessage().contains("UNIQUE"))
                return false;
            throw exception;
        }
    }

    public static void deleteUser(final @NotNull String username) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM users WHERE username == ?;
                        """)) {
                statement.setString(1, username);
                statement.executeUpdate();
            }
        }
    }

    public static @Nullable Triad<@NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>, @NotNull LocalDateTime> selectUser(final @NotNull String username) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT password, permission, modify_time FROM users WHERE username == ? LIMIT 1;
                        """)) {
                statement.setString(1, username);
                try (final ResultSet user = statement.executeQuery()) {
                    if (!user.next())
                        return null;
                    final LocalDateTime time = LocalDateTime.parse(user.getString(3), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    final SortedSet<Operation.Permission> set = new TreeSet<>(JSON.parseArray(user.getString(2)).stream()
                            .map(s -> Operation.PermissionMap.get(s.toString())).filter(Objects::nonNull).toList());
                    return new Triad.ImmutableTriad<>(user.getString(1), Collections.unmodifiableSortedSet(set), time);
                }
            }
        }
    }
}
