package com.xuxiaocheng.WList.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Databases.DatabaseInterface;
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
import java.util.concurrent.atomic.AtomicReference;

public final class UserGroupSqlHelper implements UserGroupSqlInterface {
    private final @NotNull DatabaseInterface database;
    private final @NotNull HInitializer<Long> adminId = new HInitializer<>("UserGroupAdminId");
    private final @NotNull HInitializer<Long> defaultId = new HInitializer<>("UserGroupDefaultId");

    public UserGroupSqlHelper(final @NotNull DatabaseInterface database) {
        super();
        this.database = database;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                        CREATE TABLE IF NOT EXISTS groups (
                            group_id    INTEGER    PRIMARY KEY AUTOINCREMENT
                                                   UNIQUE
                                                   NOT NULL,
                            name        TEXT       UNIQUE
                                                   NOT NULL,
                            permissions TEXT       NOT NULL
                                                   DEFAULT '%s'
                        );
                    """, Operation.dumpPermissions(Operation.emptyPermissions())));
            }
            final String adminPermissions = Operation.dumpPermissions(Operation.allPermissions());
            final String defaultPermissions = Operation.dumpPermissions(Operation.defaultPermissions());
            Long adminId;
            Long defaultId;
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT group_id FROM groups WHERE name == ? AND permissions == ? LIMIT 1;
                """)) {
                statement.setString(1, UserGroupManager.ADMIN);
                statement.setString(2, adminPermissions);
                try (final ResultSet admins = statement.executeQuery()) {
                    adminId = admins.next() ? admins.getLong("group_id") : null;
                }
                statement.setString(1, UserGroupManager.DEFAULT);
                statement.setString(2, defaultPermissions);
                try (final ResultSet defaults = statement.executeQuery()) {
                    defaultId = defaults.next() ? defaults.getLong("group_id") : null;
                }
                if (adminId == null || defaultId == null)
                    try (final PreparedStatement insertStatement = connection.prepareStatement("""
                        INSERT INTO groups (name, permissions)
                            VALUES (?, ?)
                        ON CONFLICT (name) DO UPDATE SET
                            group_id = excluded.group_id, permissions = excluded.permissions;
                    """)) {
                        if (adminId == null) {
                            insertStatement.setString(1, UserGroupManager.ADMIN);
                            insertStatement.setString(2, adminPermissions);
                            insertStatement.executeUpdate();
                            statement.setString(1, UserGroupManager.ADMIN);
                            statement.setString(2, adminPermissions);
                            try (final ResultSet admins = statement.executeQuery()) {
                                admins.next();
                                adminId = admins.getLong("group_id");
                            }
                        }
                        if (defaultId == null) {
                            insertStatement.setString(1, UserGroupManager.DEFAULT);
                            insertStatement.setString(2, defaultPermissions);
                            insertStatement.executeUpdate();
                            statement.setString(1, UserGroupManager.DEFAULT);
                            statement.setString(2, defaultPermissions);
                            try (final ResultSet defaults = statement.executeQuery()) {
                                defaults.next();
                                defaultId = defaults.getLong("group_id");
                            }
                        }
                    }
            }
            connection.commit();
            this.adminId.initialize(adminId);
            this.defaultId.initialize(defaultId);
        }
    }

    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        DROP TABLE IF EXISTS groups;
                    """);
            }
            connection.commit();
            this.adminId.uninitialize();
            this.defaultId.uninitialize();
        }
    }

    @Override
    public long getAdminId() {
        return this.adminId.getInstance().longValue();
    }

    @Override
    public long getDefaultId() {
        return this.defaultId.getInstance().longValue();
    }

    private static @Nullable UserGroupSqlInformation createNextUserGroupInfo(final @NotNull ResultSet result) throws SQLException {
        return result.next() ? new UserGroupSqlInformation(result.getLong("group_id"), result.getString("name"),
                Operation.parsePermissionsNotNull(result.getString("permissions"))) : null;
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

    @Override
    public @NotNull @UnmodifiableView Map<UserGroupSqlInformation.@NotNull Inserter, @NotNull Boolean> insertGroups(final @NotNull Collection<UserGroupSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public void updateGroups(final @NotNull Collection<@NotNull UserGroupSqlInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE groups SET name = ?, permissions = ? WHERE group_id == ?;
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

    @Override
    public void updateGroupsByName(final @NotNull Collection<UserGroupSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    UPDATE groups SET permissions = ? WHERE name == ?;
                """)) {
                for (final UserGroupSqlInformation.Inserter inserter: inserters) {
                    statement.setString(1, Operation.dumpPermissions(inserter.permissions()));
                    statement.setString(2, inserter.name());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM groups WHERE group_id == ?;
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
    public void deleteGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserGroupSqlInformation> selectGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<Long, UserGroupSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM groups WHERE group_id == ? LIMIT 1;
                """)) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        final UserGroupSqlInformation information = UserGroupSqlHelper.createNextUserGroupInfo(result);
                        if (information != null)
                            map.put(id, information);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserGroupSqlInformation> selectGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<String, UserGroupSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT * FROM groups WHERE name == ? LIMIT 1;
                        """)) {
                for (final String name: nameList) {
                    statement.setString(1, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        final UserGroupSqlInformation information = UserGroupSqlHelper.createNextUserGroupInfo(result);
                        if (information != null)
                            map.put(name, information);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserGroupSqlInformation>> selectAllUserGroupsInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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
                    SELECT * FROM groups ORDER BY group_id %s LIMIT ? OFFSET ?;
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

    @Override
    public @NotNull @UnmodifiableView List<@Nullable UserGroupSqlInformation> searchUserGroupsByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final List<UserGroupSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM groups WHERE name %s ?
                    ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, rule.length());
                statement.setInt(3, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = UserGroupSqlHelper.createUserGroupsInfo(result);
                }
            }
            return list;
        }
    }

    @Override
    public @NotNull String toString() {
        return "UserGroupSqlHelper{" +
                "database=" + this.database +
                ", adminId=" + this.adminId +
                ", defaultId=" + this.defaultId +
                '}';
    }
}
