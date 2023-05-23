package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.DataAccessObjects.UserInformation;
import com.xuxiaocheng.WList.Server.Polymers.UserSqlInfo;
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
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

public final class UserSqlHelper {
    private UserSqlHelper() {
        super();
    }

    // Util

    private static @NotNull String encryptPassword(final @NotNull String password) {
        return MiscellaneousUtil.getMd5((password + /*UserSqlHelper.ServerPasswordSlat*/"With Server:SALT***#WListServer%CreateBy@XXC#***TokenMD5&SALT").getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String generateRandomPassword() {
        final char[] word = new char[8];
        //noinspection SpellCheckingInspection
        HRandomHelper.setArray(HRandomHelper.DefaultSecureRandom, word, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_".toCharArray());
        return new String(word);
    }

    private static @NotNull String getModifyTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static boolean isWrongPassword(final @NotNull String source, final @NotNull String encrypted) {
        return !UserSqlHelper.encryptPassword(source).equals(encrypted);
    }

    // Helper

    @SuppressWarnings("TypeMayBeWeakened")
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
                        """, Operation.dumpPermissions(defaultPermission)));
                try (final ResultSet admins = statement.executeQuery(String.format("""
                        SELECT 1 FROM users WHERE permission == '%s' LIMIT 1;
                        """, Operation.dumpPermissions(adminPermission)))) {
                    needCreate = !admins.next();
                }
            }
            if (needCreate) {
                final String password = UserSqlHelper.generateRandomPassword();
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, permission, modify_time)
                            VALUES (?, ?, ?, ?)
                        ON CONFLICT (username) DO UPDATE SET
                            id = excluded.id, password = excluded.password,
                            permission = excluded.permission, modify_time = excluded.modify_time;
                        """)) {
                    statement.setString(1, "admin");
                    statement.setString(2, UserSqlHelper.encryptPassword(password));
                    statement.setString(3, Operation.dumpPermissions(adminPermission));
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    statement.executeUpdate();
                }
                HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user, password: ", password);
            }
            connection.commit();
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static boolean insertUser(final @NotNull String username, final @NotNull String password, final @Nullable SortedSet<Operation.@NotNull Permission> permissions) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            if (permissions == null)
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, modify_time)
                            VALUES (?, ?, ?);
                        """)) {
                    statement.setString(1, username);
                    statement.setString(2, UserSqlHelper.encryptPassword(password));
                    statement.setString(3, UserSqlHelper.getModifyTime());
                    statement.executeUpdate();
                    return true;
                }
            else
                try (final PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO users (username, password, permission, modify_time)
                                VALUES (?, ?, ?, ?);
                            """)) {
                    statement.setString(1, username);
                    statement.setString(2, UserSqlHelper.encryptPassword(password));
                    statement.setString(3, Operation.dumpPermissions(permissions));
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

    @SuppressWarnings("TypeMayBeWeakened")
    public static void updateUser(final @NotNull String username, final @Nullable String password, final @Nullable SortedSet<Operation.@NotNull Permission> permissions) throws SQLException {
        if (password == null && permissions == null)
            return;
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            connection.setAutoCommit(false);
            if (password != null)
                try (final PreparedStatement statement = connection.prepareStatement("""
                            UPDATE users SET password = ?, modify_time = ? WHERE username == ?;
                            """)) {
                    statement.setString(1, UserSqlHelper.encryptPassword(password));
                    statement.setString(2, UserSqlHelper.getModifyTime());
                    statement.setString(3, username);
                    statement.executeUpdate();
                }
            if (permissions != null)
                try (final PreparedStatement statement = connection.prepareStatement("""
                            UPDATE users SET permission = ?, modify_time = ? WHERE username == ?;
                            """)) {
                    statement.setString(1, Operation.dumpPermissions(permissions));
                    statement.setString(2, UserSqlHelper.getModifyTime());
                    statement.setString(3, username);
                    statement.executeUpdate();
                }
            connection.commit();
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

    public static @Nullable UserSqlInfo selectUser(final @NotNull String username) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT password, permission, modify_time FROM users WHERE username == ? LIMIT 1;
                        """)) {
                statement.setString(1, username);
                try (final ResultSet user = statement.executeQuery()) {
                    if (!user.next())
                        return null;
                    return new UserSqlInfo(user.getString(1),
                            Operation.parsePermissions(user.getString(2)),
                            LocalDateTime.parse(user.getString(3), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            }
        }
    }

    public static @NotNull List<UserInformation> selectAllUsers() throws SQLException {
        // TODO limit and page.
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final Statement statement = connection.createStatement()) {
                try (final ResultSet user = statement.executeQuery("""
                        SELECT * FROM users;
                        """)) {
                    final List<UserInformation> list = new LinkedList<>();
                    while (user.next())
                        list.add(new UserInformation(user.getLong("id"),
                                user.getString("username"),
                                user.getString("password"),
                            Operation.parsePermissions(user.getString("permission")),
                            LocalDateTime.parse(user.getString("modify_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                    return list;
                }
            }
        }
    }
}
