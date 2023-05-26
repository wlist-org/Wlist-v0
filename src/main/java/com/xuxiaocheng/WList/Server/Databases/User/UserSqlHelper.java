package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;

// TODO user group.
public final class UserSqlHelper {
    private UserSqlHelper() {
        super();
    }

    public static final @NotNull DatabaseUtil DefaultDatabaseUtil = HExceptionWrapper.wrapSupplier(DatabaseUtil::getInstance).get();

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private static @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermissions = Collections.emptySortedSet();

    public static @NotNull @UnmodifiableView Collection<Operation.@NotNull Permission> getDefaultPermissions() {
        return UserSqlHelper.DefaultPermissions;
    }

    // TODO cache.

    // Util

    private static @NotNull String generateRandomPassword() {
        //noinspection SpellCheckingInspection
        return HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 8, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_");
    }

    private static @NotNull String getModifyTime() {
        return LocalDateTime.now().format(UserSqlHelper.DefaultFormatter);
    }

    private static @Nullable UserSqlInformation createNextUserInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new UserSqlInformation(result.getLong("id"),
                result.getString("username"), result.getString("password"),
                Objects.requireNonNullElse(Operation.parsePermissions(result.getString("permissions")), UserSqlHelper.DefaultPermissions),
                LocalDateTime.parse(result.getString("modify_time"), UserSqlHelper.DefaultFormatter));
    }

    private static @NotNull @UnmodifiableView List<@NotNull UserSqlInformation> createUsersInfo(final @NotNull ResultSet result) throws SQLException {
        final List<UserSqlInformation> list = new LinkedList<>();
        while (true) {
            final UserSqlInformation info = UserSqlHelper.createNextUserInfo(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    // Initialize

    public static void initialize(final @NotNull SortedSet<Operation.@NotNull Permission> defaultPermissions, final @NotNull Collection<Operation.@NotNull Permission> adminPermissions, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
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
                            permissions TEXT       NOT NULL
                                                   DEFAULT '%s',
                            modify_time TEXT       NOT NULL
                        );
                        """, Operation.dumpPermissions(defaultPermissions)));
                try (final ResultSet admins = statement.executeQuery(String.format("""
                        SELECT 1 FROM users WHERE permissions == '%s' LIMIT 1;
                        """, Operation.dumpPermissions(adminPermissions)))) {
                    needCreate = !admins.next();
                }
            }
            UserSqlHelper.DefaultPermissions = Collections.unmodifiableSortedSet(defaultPermissions);
            if (needCreate) {
                final String password = UserSqlHelper.generateRandomPassword();
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, permissions, modify_time)
                            VALUES (?, ?, ?, ?)
                        ON CONFLICT (username) DO UPDATE SET
                            id = excluded.id, password = excluded.password,
                            permissions = excluded.permissions, modify_time = excluded.modify_time;
                        """)) {
                    statement.setString(1, "admin");
                    statement.setString(2, PasswordGuard.encryptPassword(password));
                    statement.setString(3, Operation.dumpPermissions(adminPermissions));
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    statement.executeUpdate();
                }
                HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user. password: ", password);
                // Force log. Ensure password is recorded in the log file.
                HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "admin password: ", password);
            }
            connection.commit();
        }
    }

    // Insert

    public static @NotNull List<@NotNull Boolean> insertUsers(final @NotNull Collection<@NotNull UserCommonInformation> infoList, final @Nullable String connectionId) throws SQLException {
        if (infoList.isEmpty())
            return List.of();
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            final List<Boolean> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR IGNORE INTO users (username, password, permissions, modify_time)
                            VALUES (?, ?, ?, ?);
                        """)) {
                for (final UserCommonInformation info: infoList) {
                    statement.setString(1, info.username());
                    statement.setString(2, PasswordGuard.encryptPassword(info.password()));
                    statement.setString(3, Operation.dumpPermissions(Objects.requireNonNullElse(info.permissions(), UserSqlHelper.DefaultPermissions)));
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    list.add(statement.executeUpdate() > 0);
                }
            }
            connection.commit();
            return list;
        }
    }

    public static boolean insertUser(final @NotNull UserCommonInformation info, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.insertUsers(List.of(info), connectionId).get(0).booleanValue();
    }

    // Update

    public static void updateUsers(final @NotNull Collection<@NotNull UserSqlInformation> infoList, final @Nullable String connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                        UPDATE users SET username = ?, password = ?, permissions = ?, modify_time = ? WHERE id == ?;
                        """)) {
                for (final UserSqlInformation info: infoList) {
                    statement.setString(1, info.username());
                    statement.setString(2, PasswordGuard.encryptPassword(info.password()));
                    statement.setString(3, Operation.dumpPermissions(info.permissions()));
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    statement.setLong(5, info.id());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void updateUser(final @NotNull UserSqlInformation info, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.updateUsers(List.of(info), connectionId);
    }

    public static void updateUsersByName(final @NotNull Collection<@NotNull UserCommonInformation> infoList, final @Nullable String connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                        UPDATE users SET password = ?, permissions = ?, modify_time = ? WHERE username == ?;
                        """)) {
                for (final UserCommonInformation info: infoList) {
                    statement.setString(1, PasswordGuard.encryptPassword(info.password()));
                    statement.setString(2, Operation.dumpPermissions(Objects.requireNonNullElse(info.permissions(), UserSqlHelper.DefaultPermissions)));
                    statement.setString(3, UserSqlHelper.getModifyTime());
                    statement.setString(4, info.username());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void updateUserByName(final @NotNull UserCommonInformation info, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.updateUsersByName(List.of(info), connectionId);
    }

    // Delete

    public static void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id == ?;")) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteUser(final long id, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.deleteUsers(List.of(id), connectionId);
    }

    public static void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return;
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE username == ?;")) {
                for (final String username: usernameList) {
                    statement.setString(1, username);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteUserByName(final @NotNull String username, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.deleteUsersByName(List.of(username), connectionId);
    }

    // Select

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        if (idList.isEmpty())
            return List.of();
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<UserSqlInformation> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT * FROM users WHERE id == ? LIMIT 1;
                        """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(UserSqlHelper.createNextUserInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @Nullable UserSqlInformation selectUser(final long id, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.selectUsers(List.of(id), connectionId).get(0);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return List.of();
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<UserSqlInformation> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT password, permissions, modify_time FROM users WHERE username == ? LIMIT 1;
                        """)) {
                for (final String username: usernameList) {
                    statement.setString(1, username);
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(UserSqlHelper.createNextUserInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @Nullable UserSqlInformation selectUserByName(final @NotNull String username, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.selectUsersByName(List.of(username), connectionId).get(0);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final @NotNull OrderDirection direction, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = UserSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final long count;
            try (final Statement statement = connection.createStatement()) {
                try (final ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users;")) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<UserSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "SELECT * FROM users ORDER BY id %s LIMIT ? OFFSET ?;",
                    switch (direction) {case ASCEND -> "ASC";case DESCEND -> "DESC";}))) {
                statement.setLong(1, limit);
                statement.setLong(2, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserSqlHelper.createUsersInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }
}
