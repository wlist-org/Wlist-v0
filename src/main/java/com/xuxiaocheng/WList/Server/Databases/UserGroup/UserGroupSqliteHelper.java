package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


    public static final @NotNull String PermissionsHeader; static {
        final StringBuilder builder = new StringBuilder();
        for (final UserPermission permission: UserPermission.All)
            builder.append(", permissions_").append(permission.name());
        PermissionsHeader = builder.delete(0, 2).toString();
    }
    protected static @NotNull String permissionsInsertValue(final @NotNull Collection<@NotNull UserPermission> permissions) {
        final StringBuilder builder = new StringBuilder();
        for (final UserPermission permission: UserPermission.All)
            builder.append(", ").append(permissions.contains(permission) ? 1 : 0);
        return builder.delete(0, 2).toString();
    }
    protected static @NotNull String permissionsUpdateValue(final @NotNull Collection<@NotNull UserPermission> permissions) {
        final StringBuilder builder = new StringBuilder();
        for (final UserPermission permission: UserPermission.All)
            builder.append(", permissions_").append(permission.name()).append(" = ").append(permissions.contains(permission) ? 1 : 0);
        return builder.delete(0, 2).toString();
    }

    @Override
    @SuppressWarnings("ExtractMethodRecommender")
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
                for (final UserPermission permission: UserPermission.All) {
                    final String p = "permissions_" + permission.name();
                    builder.append(String.format("""
        %s          INTEGER     NOT NULL
                                DEFAULT (0)
                                CHECK (%s == 0 OR %s == 1),
                        """, p, p, p));
                }
                builder.append("""
        create_time TIMESTAMP   NOT NULL
                                DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP   NOT NULL
                                DEFAULT CURRENT_TIMESTAMP
    );
                    """);
                statement.executeUpdate(builder.toString());
                statement.executeUpdate("""
    CREATE INDEX IF NOT EXISTS groups_name ON groups (name);
                """);
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
    INSERT INTO groups (name, name_order, %s)
        VALUES (?, ?, %s);
                    """, UserGroupSqliteHelper.PermissionsHeader, UserGroupSqliteHelper.permissionsInsertValue(UserPermission.All)))) {
                        insertStatement.setString(1, IdentifierNames.UserGroupName.Admin.getIdentifier());
                        insertStatement.setBytes(2, SqlHelper.toOrdered(IdentifierNames.UserGroupName.Admin.getIdentifier()));
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
    INSERT INTO groups (name, name_order, %s)
        VALUES (?, ?, %s);
                    """, UserGroupSqliteHelper.PermissionsHeader, UserGroupSqliteHelper.permissionsInsertValue(UserPermission.Default)))) {
                        insertStatement.setString(1, IdentifierNames.UserGroupName.Default.getIdentifier());
                        insertStatement.setBytes(2, SqlHelper.toOrdered(IdentifierNames.UserGroupName.Default.getIdentifier()));
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
            this.adminId.uninitializeNullable();
            this.defaultId.uninitializeNullable();
            connection.commit();
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


    public static final @NotNull String UserGroupInfoExtra = "group_id, name, " + UserGroupSqliteHelper.PermissionsHeader + ", create_time, update_time";

    public static @Nullable UserGroupInformation nextGroup(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        final Set<UserPermission> permissions = EnumSet.noneOf(UserPermission.class);
        for (final UserPermission permission: UserPermission.All)
            if (result.getBoolean("permissions_" + permission.name()))
                permissions.add(permission);
        return new UserGroupInformation(result.getLong("group_id"), result.getString("name"), permissions,
                SqlHelper.toZonedDataTime(result.getTimestamp("create_time")),
                SqlHelper.toZonedDataTime(result.getTimestamp("update_time")));
    }

    public static @NotNull @UnmodifiableView List<@NotNull UserGroupInformation> allGroups(final @NotNull ResultSet result) throws SQLException {
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
    INSERT OR IGNORE INTO groups (name, name_order)
        VALUES (?, ?);
                """)) {
                statement.setString(1, name);
                statement.setBytes(2, SqlHelper.toOrdered(name));
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
    public @Nullable ZonedDateTime updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId() || id == this.getDefaultId() || IdentifierNames.UserGroupName.contains(name))
            return null;
        ZonedDateTime time;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    UPDATE OR IGNORE groups SET name = ?, name_order = ?, update_time = ? WHERE group_id == ?;
                """)) {
                statement.setString(1, name);
                statement.setBytes(2, SqlHelper.toOrdered(name));
                time = SqlHelper.now();
                statement.setTimestamp(3, AndroidSupporter.toTimestamp(time.toLocalDateTime()));
                statement.setLong(4, id);
                if (statement.executeUpdate() == 0)
                    time = null;
            }
            connection.commit();
        }
        return time;
    }

    @Override
    public @Nullable ZonedDateTime updateGroupPermission(final long id, final @NotNull Set<@NotNull UserPermission> permissions, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId())
            return null;
        ZonedDateTime time;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE OR IGNORE groups SET %s, update_time = ? WHERE group_id == ?;
                """, UserGroupSqliteHelper.permissionsUpdateValue(permissions)))) {
                time = SqlHelper.now();
                statement.setTimestamp(1, AndroidSupporter.toTimestamp(time.toLocalDateTime()));
                statement.setLong(2, id);
                if (statement.executeUpdate() == 0)
                    time = null;
            }
            connection.commit();
        }
        return time;
    }


    /* --- Select --- */

    @Contract(pure = true)
    protected static @NotNull String orderBy(@SuppressWarnings("TypeMayBeWeakened") final @NotNull @Unmodifiable LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders) {
        if (orders.isEmpty())
            return "ORDER BY name_order ASC, group_id ASC";
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (final Map.Entry<VisibleUserGroupInformation.Order, OrderDirection> order: orders.entrySet()) {
            builder.append(switch (order.getKey()) {
                case Id -> "group_id";
                case Name -> "name_order";
                case CreateTime -> "create_time";
                case UpdateTime -> "update_time";
                default -> "permissions" + order.getKey().name().substring("Permission_".length());
            }).append(switch (order.getValue()) {
                case ASCEND -> " ASC";
                case DESCEND -> " DESC";
            }).append(',');
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    @Contract(pure = true)
    protected static @NotNull String wherePermissions(final @NotNull @Unmodifiable Map<@NotNull UserPermission, @Nullable Boolean> chooser) {
        if (chooser.isEmpty())
            return "";
        final StringBuilder builder = new StringBuilder("WHERE ");
        for (final UserPermission permission: UserPermission.All) {
            final Boolean has = chooser.get(permission);
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
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroups(final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
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
            if (position < 0 || count <= position || limit <= 0)
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
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull Map<@NotNull UserPermission, @Nullable Boolean> chooser, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserGroupInformation> groups;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM groups %s;
                """, UserGroupSqliteHelper.wherePermissions(chooser)))) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                groups = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups %s %s LIMIT ? OFFSET ?;
                """, UserGroupSqliteHelper.UserGroupInfoExtra, UserGroupSqliteHelper.wherePermissions(chooser), UserGroupSqliteHelper.orderBy(orders)))) {
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


    /* --- Search --- */

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserGroupInformation> groups;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM groups WHERE name REGEXP ?;
                """)) {
                statement.setString(1, regex);
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                groups = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups WHERE name REGEXP ? %s LIMIT ? OFFSET ?;
                """, UserGroupSqliteHelper.UserGroupInfoExtra, UserGroupSqliteHelper.orderBy(orders)))) {
                    statement.setString(1, regex);
                    statement.setLong(2, limit);
                    statement.setLong(3, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        groups = UserGroupSqliteHelper.allGroups(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, groups);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (names.size() != 1)
            throw new UnsupportedOperationException("Cannot search groups by multiple names."); // TODO multiple names search support.
        final String name = names.stream().findFirst().get();
        final long count;
        final List<UserGroupInformation> groups;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM groups WHERE name LIKE ? ESCAPE '\\';
                """)) {
                statement.setString(1, SqlHelper.likeName(name));
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                groups = List.of();
            else
                //noinspection SpellCheckingInspection
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM groups WHERE name LIKE ? ESCAPE '\\' ORDER BY length(name), charindex(?, name) LIMIT ? OFFSET ?;
                """, UserGroupSqliteHelper.UserGroupInfoExtra))) {
                    statement.setString(1, SqlHelper.likeName(name));
                    statement.setString(2, name);
                    statement.setLong(3, limit);
                    statement.setLong(4, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        groups = UserGroupSqliteHelper.allGroups(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, groups);
    }

    @Override
    public @NotNull String toString() {
        return "UserGroupSqliteHelper{" +
                "database=" + this.database +
                ", adminId=" + this.adminId +
                ", defaultId=" + this.defaultId +
                '}';
    }
}
