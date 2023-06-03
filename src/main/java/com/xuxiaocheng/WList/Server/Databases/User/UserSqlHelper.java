package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqlInformation;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ClassHasNoToStringMethod")
final class UserSqlHelper {
    private static @Nullable UserSqlHelper instance;

    public static synchronized void initialize(final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        if (UserSqlHelper.instance != null)
            throw new IllegalStateException("User sql helper is initialized. instance: " + UserSqlHelper.instance);
        UserSqlHelper.instance = new UserSqlHelper(database ,_connectionId);
    }

    public static synchronized @NotNull UserSqlHelper getInstance() {
        if (UserSqlHelper.instance == null)
            throw new IllegalStateException("User sql helper is not initialized.");
        return UserSqlHelper.instance;
    }

    private final @NotNull DatabaseUtil database;
    private final long adminId;
    private final long defaultId;

    private UserSqlHelper(final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.database = database;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, UserGroupSqlInformation> map = UserGroupManager.selectGroupsByName(List.of("admin", "default"), connectionId.get());
            if (!map.containsKey("admin") || !map.containsKey("default"))
                throw new SQLException("Missing 'admin' or 'default' user group.");
            this.adminId = map.get("admin").id();
            this.defaultId = map.get("default").id();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS users (
                        id          INTEGER    PRIMARY KEY AUTOINCREMENT
                                               UNIQUE
                                               NOT NULL,
                        username    TEXT       UNIQUE
                                               NOT NULL,
                        password    TEXT       NOT NULL,
                        group_id    INTEGER    NOT NULL
                                               DEFAULT ?,
                        modify_time TEXT       NOT NULL
                    );
                """)) {
                statement.setLong(1, this.defaultId);
                statement.executeUpdate();
            }
            final boolean noAdmin;
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT 1 FROM users WHERE group_id == ? LIMIT 1;
                """)) {
                statement.setLong(1, this.adminId);
                try (final ResultSet admins = statement.executeQuery()) {
                    noAdmin = !admins.next();
                }
            }
            if (noAdmin) {
                final String password = UserSqlHelper.generateRandomPassword();
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (username, password, group_id, modify_time)
                            VALUES (?, ?, ?, ?)
                        ON CONFLICT (username) DO UPDATE SET
                            id = excluded.id, password = excluded.password,
                            group_id = excluded.group_id, modify_time = excluded.modify_time;
                        """)) {
                    statement.setString(1, "admin");
                    statement.setString(2, PasswordGuard.encryptPassword(password));
                    statement.setLong(3, this.adminId);
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    statement.executeUpdate();
                }
                HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user. password: ", password);
                // Force log. Ensure password is recorded in the log file.
                HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Reset admin password: ", password);
            }
            connection.commit();
        }
    }

    // Util

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static @NotNull String generateRandomPassword() {
        //noinspection SpellCheckingInspection
        return HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 8, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_");
    }

    private static @NotNull String getModifyTime() {
        return LocalDateTime.now().format(UserSqlHelper.DefaultFormatter);
    }

    private static @NotNull UserGroupSqlInformation getUserGroupInformation(final @NotNull ResultSet result) throws SQLException {
        return new UserGroupSqlInformation(result.getLong("group_id"),
                result.getString("name"),
                Objects.requireNonNullElseGet(Operation.parsePermissions(result.getString("permissions")),
                        Operation::emptyPermissions));
    }

    private static @Nullable UserSqlInformation createNextUserInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new UserSqlInformation(result.getLong("id"),
                result.getString("username"), result.getString("password"),
                UserSqlHelper.getUserGroupInformation(result),
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

    long getAdminId() {
        return this.adminId;
    }

    long getDefaultId() {
        return this.defaultId;
    }


    public @NotNull @UnmodifiableView Map<UserSqlInformation.@NotNull Inserter, @NotNull Boolean> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<UserSqlInformation.Inserter, Boolean> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR IGNORE INTO users (username, password, group_id, modify_time)
                            VALUES (?, ?, ?, ?);
                        """)) {
                for (final UserSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, inserter.username());
                    statement.setString(2, PasswordGuard.encryptPassword(inserter.password()));
                    statement.setLong(3, inserter.groupId());
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    map.put(inserter, statement.executeUpdate() > 0);
                }
            }
            connection.commit();
            return Collections.unmodifiableMap(map);
        }
    }

    public void updateUsers(final @NotNull Collection<@NotNull UserSqlInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE users SET username = ?, password = ?, group_id = ?, modify_time = ? WHERE id == ?;
                """)) {
                for (final UserSqlInformation info: infoList) {
                    statement.setString(1, info.username());
                    statement.setString(2, PasswordGuard.encryptPassword(info.password()));
                    statement.setLong(3, info.group().id());
                    statement.setString(4, UserSqlHelper.getModifyTime());
                    statement.setLong(5, info.id());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE users SET password = ?, group_id = ?, modify_time = ? WHERE username == ?;
                """)) {
                for (final UserSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, PasswordGuard.encryptPassword(inserter.password()));
                    statement.setLong(2, inserter.groupId());
                    statement.setString(3, UserSqlHelper.getModifyTime());
                    statement.setString(4, inserter.username());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM users WHERE id == ?;
                """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM users WHERE username == ?;
                """)) {
                for (final String username: usernameList) {
                    statement.setString(1, username);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, UserSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM users WHERE id == ? LIMIT 1;
                """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        final UserSqlInformation information = UserSqlHelper.createNextUserInfo(result);
                        if (information != null)
                            map.put(id, information);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, UserSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM users WHERE username == ? LIMIT 1;
                """)) {
                for (final String username: usernameList) {
                    statement.setString(1, username);
                    try (final ResultSet result = statement.executeQuery()) {
                        final UserSqlInformation information = UserSqlHelper.createNextUserInfo(result);
                        if (information != null)
                            map.put(username, information);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
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
                statement.setInt(1, limit);
                statement.setLong(2, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserSqlHelper.createUsersInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<UserSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM users WHERE username %s ? ORDER BY id ASC LIMIT ?;
                """, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserSqlHelper.createUsersInfo(result);
                }
            }
            return list;
        }
    }
}
