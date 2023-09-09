package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Options;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqliteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class UserGroupSqliteHelper implements UserGroupSqlInterface {
    protected final @NotNull DatabaseInterface database;
    protected final @NotNull HInitializer<Long> adminId = new HInitializer<>("UserGroupAdminId");
    protected final @NotNull HInitializer<Long> defaultId = new HInitializer<>("UserGroupDefaultId");

    public UserGroupSqliteHelper(final @NotNull DatabaseInterface database) {
        super();
        this.database = database;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }


    protected static final @NotNull String PermissionsHeader; static {
        final StringBuilder builder = new StringBuilder();
        for (final Operation.Permission permission: Operation.AllPermissions)
            builder.append(", permissions_").append(permission.name());
        PermissionsHeader = builder.delete(0, 2).toString();
    }
    protected static @NotNull String permissionsInsertValue(final @NotNull Collection<Operation.@NotNull Permission> permissions) {
        final StringBuilder builder = new StringBuilder();
        for (final Operation.Permission permission: Operation.AllPermissions)
            builder.append(", ").append(permissions.contains(permission) ? 1 : 0);
        return builder.delete(0, 2).toString();
    }
    protected static @NotNull String permissionsUpdateValue(final @NotNull Collection<Operation.@NotNull Permission> permissions) {
        final StringBuilder builder = new StringBuilder();
        for (final Operation.Permission permission: Operation.AllPermissions)
            builder.append(", permission_").append(permission.name()).append(" = ").append(permissions.contains(permission) ? 1 : 0);
        return builder.delete(0, 2).toString();
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                final StringBuilder builder = new StringBuilder("""
    CREATE TABLE IF NOT EXISTS groups (
        group_id    INTEGER     PRIMARY KEY AUTOINCREMENT
                                UNIQUE
                                NOT NULL,
        name        TEXT        UNIQUE
                                NOT NULL,
        name_order  BLOB        NOT NULL,
                    """);
                for (final Operation.Permission permission: Operation.AllPermissions) {
                    final String p = "permissions_" + permission.name();
                    builder.append(String.format("""
        %s          INTEGER     NOT NULL
                                DEFAULT (0)
                                CHECK (%s == 0 OR %s == 1),
                        """, p, p, p));
                }
                builder.append("""
        create_time TEXT        NOT NULL,
        update_time TEXT        NOT NULL
    );
                    """);
                statement.executeUpdate(builder.toString());
            }
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT group_id FROM groups WHERE name == ? LIMIT 1;
                """)) {
                statement.setString(1, IdentifierNames.UserGroupName.Admin.getIdentifier());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next())
                        this.adminId.initialize(result.getLong(1));
                }
                if (this.adminId.isNotInitialized()) {
                    try (final PreparedStatement insertStatement = connection.prepareStatement(String.format("""
    INSERT INTO groups (name, name_order, create_time, update_time, %s)
        VALUES (?, ?, ?, ?, %s);
                    """, UserGroupSqliteHelper.PermissionsHeader, UserGroupSqliteHelper.permissionsInsertValue(Operation.AllPermissions)))) {
                        insertStatement.setString(1, IdentifierNames.UserGroupName.Admin.getIdentifier());
                        insertStatement.setBytes(2, SqliteHelper.toOrdered(IdentifierNames.UserGroupName.Admin.getIdentifier()));
                        final String now = SqliteHelper.now();
                        insertStatement.setString(3, now);
                        insertStatement.setString(4, now);
                        insertStatement.executeUpdate();
                    }
                    statement.setString(1, IdentifierNames.UserGroupName.Admin.getIdentifier());
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        this.adminId.initialize(result.getLong(1));
                    }
                }
                statement.setString(1, IdentifierNames.UserGroupName.Default.getIdentifier());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next())
                        this.defaultId.initialize(result.getLong(1));
                }
                if (this.defaultId.isNotInitialized()) {
                    try (final PreparedStatement insertStatement = connection.prepareStatement(String.format("""
    INSERT INTO groups (name, name_order, create_time, update_time, %s)
        VALUES (?, ?, ?, ?, %s);
                    """, UserGroupSqliteHelper.PermissionsHeader, UserGroupSqliteHelper.permissionsInsertValue(Operation.DefaultPermissions)))) {
                        insertStatement.setString(1, IdentifierNames.UserGroupName.Default.getIdentifier());
                        insertStatement.setBytes(2, SqliteHelper.toOrdered(IdentifierNames.UserGroupName.Default.getIdentifier()));
                        final String now = SqliteHelper.now();
                        insertStatement.setString(3, now);
                        insertStatement.setString(4, now);
                        insertStatement.executeUpdate();
                    }
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        this.defaultId.initialize(result.getLong(1));
                    }
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
    DROP TABLE IF EXISTS groups;
                    """);
            }
            connection.commit();
        }
        this.adminId.uninitialize();
        this.defaultId.uninitialize();
    }

    @Override
    public long getAdminId() {
        return this.adminId.getInstance().longValue();
    }

    @Override
    public long getDefaultId() {
        return this.defaultId.getInstance().longValue();
    }


    protected static final @NotNull String UserGroupInfoExtra = "group_id, name, " + UserGroupSqliteHelper.PermissionsHeader + ", create_time, update_time";
    protected static @Nullable UserGroupInformation nextGroup(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        final EnumSet<Operation.Permission> permissions = EnumSet.noneOf(Operation.Permission.class);
        for (final Operation.Permission permission: Operation.AllPermissions)
            if (result.getBoolean("permissions_" + permission.name()))
                permissions.add(permission);
        return new UserGroupInformation(result.getLong("group_id"), result.getString("name"), permissions,
                SqliteHelper.getTime(result.getString("create_time")), SqliteHelper.getTime(result.getString("update_time")));
    }

    protected static @NotNull @UnmodifiableView List<@NotNull UserGroupInformation> allGroups(final @NotNull ResultSet result) throws SQLException {
        final List<UserGroupInformation> list = new LinkedList<>();
        while (true) {
            final UserGroupInformation info = UserGroupSqliteHelper.nextGroup(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }


    /* --- Insert --- */

    @Override
    public @Nullable UserGroupInformation insertGroup(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        if (IdentifierNames.UserGroupName.contains(name))
            return null;
        UserGroupInformation information = null;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final boolean success;
            try (final PreparedStatement statement = connection.prepareStatement("""
    INSERT OR IGNORE INTO groups (name, name_order, create_time, update_time)
        VALUES (?, ?, ?, ?);
                """)) {
                statement.setString(1, name);
                statement.setBytes(2, SqliteHelper.toOrdered(name));
                final String now = SqliteHelper.now();
                statement.setString(3, now);
                statement.setString(4, now);
                success = statement.executeUpdate() == 1;
            }
            if (success)
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups WHERE name == ? LIMIT 1;
                    """, UserGroupSqliteHelper.UserGroupInfoExtra))) {
                    statement.setString(1, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        information = UserGroupSqliteHelper.nextGroup(result);
                    }
                }
            connection.commit();
        }
        return information;
    }


    /* --- Update --- */

    @Override
    public boolean updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    UPDATE OR IGNORE groups SET name = ?, update_time = ? WHERE group_id == ?;
                """)) {
                statement.setString(1, name);
                statement.setString(2, SqliteHelper.now());
                statement.setLong(3, id);
                success = statement.executeUpdate() == 1;
            }
            connection.commit();
        }
        return success;
    }

    @Override
    public boolean updateGroupPermission(final long id, final @NotNull EnumSet<Operation.@NotNull Permission> permissions, final @Nullable String _connectionId) throws SQLException {
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE OR IGNORE groups SET %s, update_time = ? WHERE group_id == ?;
                """, UserGroupSqliteHelper.permissionsUpdateValue(permissions)))) {
                statement.setString(1, SqliteHelper.now());
                statement.setLong(2, id);
                success = statement.executeUpdate() == 1;
            }
            connection.commit();
        }
        return success;
    }


    /* --- Select --- */

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation.Order
     */
    protected static @NotNull String orderBy(@SuppressWarnings("TypeMayBeWeakened") final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders) {
        if (orders.isEmpty())
            return "";
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (final Map.Entry<VisibleUserGroupInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
            builder.append(switch (order.getKey()) {
                case Id -> "group_id";
                case Name -> "name_order";
                case CreateTime -> "create_time";
                case UpdateTime -> "update_time";
                default -> "permissions_" + order.getKey().name().substring("Permission_".length());
            }).append(' ').append(switch (order.getValue()) {
                case ASCEND -> "ASC";
                case DESCEND -> "DESC";
            }).append(',');
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    protected static @NotNull String wherePermissions(final @NotNull EnumMap<Operation.@NotNull Permission, @Nullable Boolean> permissions) {
        if (permissions.isEmpty())
            return "";
        final StringBuilder builder = new StringBuilder("WHERE ");
        for (final Operation.Permission permission: Operation.AllPermissions) {
            final Boolean has = permissions.get(permission);
            if (has == null)
                continue;
            builder.append("permissions_").append(permission.name()).append(" = ").append(has.booleanValue() ? 1 : 0).append(" AND ");
        }
        return builder.delete(builder.length() - 5, builder.length()).toString();
    }

    @Override
    public @Nullable UserGroupInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        final UserGroupInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups WHERE group_id == ? LIMIT 1;
                """, UserGroupSqliteHelper.UserGroupInfoExtra))) {
                statement.setLong(1, id);
                try (final ResultSet result = statement.executeQuery()) {
                    information = UserGroupSqliteHelper.nextGroup(result);
                }
            }
        }
        return information;
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroups(final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserGroupInformation> groups;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM groups;
                """)) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position)
                groups = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups %s LIMIT ? OFFSET ?;
                """, UserGroupSqliteHelper.UserGroupInfoExtra, UserGroupSqliteHelper.orderBy(orders)))) {
                    statement.setLong(1, limit);
                    statement.setLong(2, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        groups = UserGroupSqliteHelper.allGroups(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, groups);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull EnumMap<Operation.@NotNull Permission, @Nullable Boolean> permissions, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserGroupInformation> groups;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM groups %s;
                """, UserGroupSqliteHelper.wherePermissions(permissions)))) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position)
                groups = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups %s %s LIMIT ? OFFSET ?;
                """, UserGroupSqliteHelper.UserGroupInfoExtra, UserGroupSqliteHelper.wherePermissions(permissions), UserGroupSqliteHelper.orderBy(orders)))) {
                    statement.setLong(1, limit);
                    statement.setLong(2, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        groups = UserGroupSqliteHelper.allGroups(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, groups);
    }


    /* --- Delete --- */

    @Override
    public boolean deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId() || id == this.getDefaultId())
            return false;
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    DELETE FROM groups WHERE group_id == ?;
                """)) {
                statement.setLong(1, id);
                success = statement.executeUpdate() == 1;
            }
            connection.commit();
        }
        return success;
    }

    @Override
    public long deleteGroupsByPermissions(@NotNull final EnumMap<Operation.@NotNull Permission, @Nullable Boolean> permissions, @Nullable final String _connectionId) throws SQLException {
        long count;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM groups %s AND !(group_id == ? AND group_id == ?);
                """, UserGroupSqliteHelper.wherePermissions(permissions)))) {
                statement.setLong(1, this.getAdminId());
                statement.setLong(2, this.getDefaultId());
                try {
                    count = statement.executeLargeUpdate();
                } catch (final UnsupportedOperationException ignore) {
                    count = statement.executeUpdate();
                }
            }
            connection.commit();
        }
        return count;
    }

//    @Override
//    public @NotNull @UnmodifiableView List<@Nullable UserGroupInformation> searchUserGroupsByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
//        if (limit <= 0)
//            return List.of();
//        try (final Connection connection = this.getConnection(_connectionId, null)) {
//            final List<UserGroupInformation> list;
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    SELECT * FROM groups WHERE name %s ?
//                    ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
//                """, caseSensitive ? "GLOB" : "LIKE"))) {
//                statement.setString(1, rule);
//                statement.setInt(2, rule.length());
//                statement.setInt(3, limit);
//                try (final ResultSet result = statement.executeQuery()) {
//                    list = UserGroupSqliteHelper.createUserGroupsInfo(result);
//                }
//            }
//            return list;
//        }
//    }

    @Override
    public @NotNull String toString() {
        return "UserGroupSqliteHelper{" +
                "database=" + this.database +
                ", adminId=" + this.adminId +
                ", defaultId=" + this.defaultId +
                '}';
    }
}
