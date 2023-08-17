package com.xuxiaocheng.WList.Databases.User;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlInformation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Operation;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class UserSqlHelper implements UserSqlInterface {
    private final @NotNull DatabaseInterface database;
    private final @NotNull HInitializer<Long> adminId = new HInitializer<>("UserAdminId");
    private final @NotNull HInitializer<String> defaultAdminPassword = new HInitializer<>("DefaultAdminUserPassword");

    public UserSqlHelper(final @NotNull DatabaseInterface database) {
        super();
        this.database = database;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS users (
                        id          INTEGER    PRIMARY KEY AUTOINCREMENT
                                               UNIQUE
                                               NOT NULL,
                        username    TEXT       UNIQUE
                                               NOT NULL,
                        password    TEXT       NOT NULL,
                        group_id    INTEGER    NOT NULL
                                               DEFAULT %d
                                               REFERENCES groups (group_id),
                        modify_time TEXT       NOT NULL
                    );
                """, UserGroupManager.getDefaultId()));
            }
            Long adminId;
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT id FROM users WHERE group_id == ? LIMIT 1;
                """)) {
                statement.setLong(1, UserGroupManager.getAdminId());
                try (final ResultSet admins = statement.executeQuery()) {
                    adminId = admins.next() ? admins.getLong("id") : null;
                }
                if (adminId == null) {
                    final String password = UserSqlHelper.generateRandomPassword();
                    try (final PreparedStatement insertStatement = connection.prepareStatement("""
                        INSERT INTO users (username, password, group_id, modify_time)
                            VALUES (?, ?, ?, ?)
                        ON CONFLICT (username) DO UPDATE SET
                            id = excluded.id, password = excluded.password,
                            group_id = excluded.group_id, modify_time = excluded.modify_time;
                        """)) {
                        insertStatement.setString(1, UserManager.ADMIN);
                        insertStatement.setString(2, PasswordGuard.encryptPassword(password));
                        insertStatement.setLong(3, UserGroupManager.getAdminId());
                        insertStatement.setString(4, LocalDateTime.now().format(UserSqlHelper.DefaultFormatter));
                        insertStatement.executeUpdate();
                        statement.setLong(1, UserGroupManager.getAdminId());
                        try (final ResultSet admins = statement.executeQuery()) {
                            admins.next();
                            adminId = admins.getLong("id");
                        }
                    }
                    HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Reset admin user. password: ", password);
                    this.defaultAdminPassword.initialize(password);
                }
            }
            connection.commit();
            this.adminId.initialize(adminId);
        }
    }

    @Override
    public void deleteTable(@Nullable final String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    DROP TABLE IF EXISTS users;
                """);
            }
            connection.commit();
            this.adminId.uninitialize();
        }
    }

    @Override
    public long getAdminId() {
        return this.adminId.getInstance().longValue();
    }

    @Override
    public @NotNull HInitializer<String> getDefaultAdminPassword() {
        return this.defaultAdminPassword;
    }

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static @NotNull String generateRandomPassword() {
        //noinspection SpellCheckingInspection
        return ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 8, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_");
    }

    private static @NotNull UserGroupSqlInformation getUserGroupInformation(final @NotNull ResultSet result) throws SQLException {
        return new UserGroupSqlInformation(result.getLong("group_id"), result.getString("name"),
                Operation.parsePermissionsNotNull(result.getString("permissions")));
    }

    private static @Nullable UserSqlInformation createNextUserInfo(final @NotNull ResultSet result) throws SQLException {
        return result.next() ? new UserSqlInformation(result.getLong("id"), result.getString("username"),
                result.getString("password"), UserSqlHelper.getUserGroupInformation(result),
                LocalDateTime.parse(result.getString("modify_time"), UserSqlHelper.DefaultFormatter)) : null;
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

    @Override
    public @NotNull @UnmodifiableView Map<UserSqlInformation.@NotNull Inserter, @Nullable Long> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<UserSqlInformation.Inserter, Long> map = new HashMap<>();
            final Collection<UserSqlInformation.Inserter> inserted = new HashSet<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT OR IGNORE INTO users (username, password, group_id, modify_time)
                            VALUES (?, ?, ?, ?);
                        """)) {
                for (final UserSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, inserter.username());
                    statement.setString(2, PasswordGuard.encryptPassword(inserter.password()));
                    statement.setLong(3, inserter.groupId());
                    statement.setString(4, LocalDateTime.now().format(UserSqlHelper.DefaultFormatter));
                    if (statement.executeUpdate() > 0)
                        inserted.add(inserter);
                    else
                        map.put(inserter, null);
                }
            }
            if (!inserted.isEmpty()) {
                try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT id from users WHERE username == ?;
                        """)) {
                    for (final UserSqlInformation.Inserter inserter: inserted) {
                        statement.setString(1, inserter.username());
                        try (final ResultSet resultSet = statement.executeQuery()) {
                            resultSet.next();
                            map.put(inserter, resultSet.getLong("id"));
                        }
                    }
                }
            }
            connection.commit();
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public void updateUsers(final @NotNull Collection<UserSqlInformation.@NotNull Updater> updaters, final @Nullable String _connectionId) throws SQLException {
        if (updaters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE users SET username = ?, password = ?, group_id = ?, modify_time = ? WHERE id == ?;
                """)) {
                for (final UserSqlInformation.Updater updater: updaters) {
                    statement.setString(1, updater.username());
                    statement.setString(2, updater.encryptedPassword());
                    statement.setLong(3, updater.groupId());
                    statement.setString(4, Objects.requireNonNullElseGet(updater.modifyTime(), LocalDateTime::now).format(UserSqlHelper.DefaultFormatter));
                    statement.setLong(5, updater.id());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE users SET password = ?, group_id = ?, modify_time = ? WHERE username == ?;
                """)) {
                for (final UserSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, PasswordGuard.encryptPassword(inserter.password()));
                    statement.setLong(2, inserter.groupId());
                    statement.setString(3, LocalDateTime.now().format(UserSqlHelper.DefaultFormatter));
                    statement.setString(4, inserter.username());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<Long, UserSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM users NATURAL JOIN groups WHERE users.id == ? LIMIT 1;
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

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<String, UserSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM users NATURAL JOIN groups WHERE username == ? LIMIT 1;
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

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectUsersCountByGroup(final @NotNull Collection<@NotNull Long> groupIdList, final @Nullable String _connectionId) throws SQLException {
        if (groupIdList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<Long, Long> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM users WHERE group_id == ?;
                """)) {
                for (final Long groupId: groupIdList) {
                    statement.setLong(1, groupId.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        map.put(groupId, result.getLong(1));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM users NATURAL JOIN groups ORDER BY id %s LIMIT ? OFFSET ?;
                """, switch (direction) {case ASCEND -> "ASC";case DESCEND -> "DESC";}))) {
                statement.setInt(1, limit);
                statement.setLong(2, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserSqlHelper.createUsersInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    @Override
    public @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final List<UserSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM users NATURAL JOIN groups WHERE username %s ?
                    ORDER BY abs(length(username) - ?) ASC, id DESC LIMIT ?;
                """, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, rule.length());
                statement.setInt(3, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserSqlHelper.createUsersInfo(result);
                }
            }
            return list;
        }
    }

    @Override
    public @NotNull String toString() {
        return "UserSqlHelper{" +
                "database=" + this.database +
                ", adminId=" + this.adminId +
                ", defaultAdminPassword=" + this.defaultAdminPassword +
                '}';
    }
}
