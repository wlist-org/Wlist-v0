package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ClassHasNoToStringMethod")
final class UserGroupSqlHelper {
    private static @Nullable UserGroupSqlHelper instance;

    public static synchronized void initialize(final @NotNull DatabaseUtil database) throws SQLException {
        if (UserGroupSqlHelper.instance != null)
            throw new IllegalStateException("User group sql helper is initialized. instance: " + UserGroupSqlHelper.instance);
        UserGroupSqlHelper.instance = new UserGroupSqlHelper(database ,"initialize");
    }

    public static synchronized @NotNull UserGroupSqlHelper getInstance() {
        if (UserGroupSqlHelper.instance == null)
            throw new IllegalStateException("User group sql helper is not initialized.");
        return UserGroupSqlHelper.instance;
    }

    private final @NotNull DatabaseUtil database;

    public @NotNull DatabaseUtil getDatabase() {
        return this.database;
    }

    private UserGroupSqlHelper(final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.database = database;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final boolean needCreate;
            final String adminPermissions = Operation.dumpPermissions(Operation.allPermissions());
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                        CREATE TABLE IF NOT EXISTS groups (
                            id          INTEGER    PRIMARY KEY AUTOINCREMENT
                                                   UNIQUE
                                                   NOT NULL,
                            name        TEXT       UNIQUE
                                                   NOT NULL,
                            permissions TEXT       NOT NULL
                                                   DEFAULT '%s'
                        );
                    """, Operation.dumpPermissions(Operation.emptyPermissions())));
                try (final ResultSet admins = statement.executeQuery(String.format("""
                        SELECT 1 FROM users WHERE permissions == '%s' LIMIT 1;
                    """, adminPermissions))) {
                    needCreate = !admins.next();
                }
            }
            if (needCreate)
                try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO groups (name, permissions)
                            VALUES (?, ?)
                        ON CONFLICT (name) DO UPDATE SET
                            id = excluded.id, permissions = excluded.permissions;
                    """)) {
                    statement.setString(1, "admin");
                    statement.setString(2, adminPermissions);
                    statement.executeUpdate();
                }
            connection.commit();
        }
    }

    private static @Nullable UserGroupSqlInformation createNextUserGroupInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new UserGroupSqlInformation(result.getLong("id"),
                result.getString("name"),
                Objects.requireNonNullElseGet(Operation.parsePermissions(result.getString("permissions")),
                        Operation::emptyPermissions));
    }

    private static @NotNull @UnmodifiableView List<@NotNull UserGroupSqlInformation> createUserGroupsInfo(final @NotNull ResultSet result) throws SQLException {
        final List<UserGroupSqlInformation> list = new LinkedList<>();
        while (true) {
            final UserGroupSqlInformation info = UserGroupSqlHelper.createNextUserGroupInfo(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }


    public @NotNull @UnmodifiableView Map<UserGroupSqlInformation.@NotNull Inserter, @NotNull Boolean> insertGroups(final @NotNull Set<UserGroupSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<UserGroupSqlInformation.Inserter, Boolean> map = new HashMap<>(inserters.size());
            try (final PreparedStatement statement = connection.prepareStatement("""
                    INSERT OR IGNORE INTO groups (name, permissions) VALUES (?, ?);
                """)) {
                for (final UserGroupSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, inserter.name());
                    statement.setString(2, Operation.dumpPermissions(inserter.permissions()));
                    map.put(inserter, statement.executeUpdate() > 0);
                }
            }
            connection.commit();
            return Collections.unmodifiableMap(map);
        }
    }

    public void updateGroups(final @NotNull Collection<@NotNull UserGroupSqlInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE groups SET name = ?, permissions = ? WHERE id == ?;
                """)) {
                for (final UserGroupSqlInformation info: infoList) {
                    statement.setString(1, info.name());
                    statement.setString(2, Operation.dumpPermissions(info.permissions()));
                    statement.setLong(3, info.id());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void updateGroupsByName(final @NotNull Collection<UserGroupSqlInformation.@NotNull Inserter> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE groups SET permissions = ? WHERE name == ?;
                """)) {
                for (final UserGroupSqlInformation.Inserter info: infoList) {
                    statement.setString(1, Operation.dumpPermissions(info.permissions()));
                    statement.setString(2, info.name());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM groups WHERE id == ?;
                """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM groups WHERE name == ?;
                """)) {
                for (final String name: nameList) {
                    statement.setString(1, name);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @Nullable UserGroupSqlInformation> selectGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, UserGroupSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM groups WHERE id == ? LIMIT 1;
                """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, UserGroupSqlHelper.createNextUserGroupInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable UserGroupSqlInformation> selectGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<UserGroupSqlInformation> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT * FROM groups WHERE name == ? LIMIT 1;
                        """)) {
                for (final String name: nameList) {
                    statement.setString(1, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(UserGroupSqlHelper.createNextUserGroupInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserGroupSqlInformation>> selectAllUserGroupsInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count;
            try (final Statement statement = connection.createStatement()) {
                try (final ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM groups;")) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<UserGroupSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM groups ORDER BY id %s LIMIT ? OFFSET ?;
                """, switch (direction) {case ASCEND -> "ASC";case DESCEND -> "DESC";}))) {
                statement.setInt(1, limit);
                statement.setLong(2, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserGroupSqlHelper.createUserGroupsInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable UserGroupSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<UserGroupSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM users WHERE username %s ? ORDER BY id ASC LIMIT ?;
                """, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserGroupSqlHelper.createUserGroupsInfo(result);
                }
            }
            return list;
        }
    }
}
