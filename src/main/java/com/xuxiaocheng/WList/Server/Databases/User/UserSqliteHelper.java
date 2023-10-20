package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlHelper;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqliteHelper;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class UserSqliteHelper implements UserSqlInterface {
    protected final @NotNull DatabaseInterface database;
    protected final @NotNull HInitializer<Long> adminId = new HInitializer<>("UserAdminId");
    protected final @NotNull HInitializer<String> defaultAdminPassword = new HInitializer<>("DefaultAdminUserPassword");

    public UserSqliteHelper(final @NotNull DatabaseInterface database) {
        super();
        this.database = database;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.database.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
    CREATE TABLE IF NOT EXISTS users (
        id          INTEGER     PRIMARY KEY AUTOINCREMENT
                                UNIQUE
                                NOT NULL,
        username    TEXT        UNIQUE
                                NOT NULL,
        name_order  BLOB        NOT NULL,
        password    TEXT        NOT NULL,
        group_id    INTEGER     NOT NULL
                                DEFAULT %d
                                REFERENCES groups (group_id),
        create_time TIMESTAMP   NOT NULL
                                DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP   NOT NULL
                                DEFAULT CURRENT_TIMESTAMP,
        modify_time TIMESTAMP   NOT NULL
                                DEFAULT CURRENT_TIMESTAMP
    );
                """, UserGroupManager.getInstance().getDefaultId()));
                statement.executeUpdate("""
    CREATE INDEX IF NOT EXISTS users_name ON users (username);
                """);
            }
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT id FROM users WHERE username == ? LIMIT 1;
                """)) {
                statement.setString(1, IdentifierNames.UserName.Admin.getIdentifier());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next())
                        this.adminId.initialize(result.getLong(1));
                }
                if (this.adminId.isNotInitialized()) {
                    final String password = PasswordGuard.generateRandomPassword();
                    try (final PreparedStatement insertStatement = connection.prepareStatement("""
    INSERT INTO users (username, name_order, password, group_id)
        VALUES (?, ?, ?, ?);
                        """)) {
                        insertStatement.setString(1, IdentifierNames.UserName.Admin.getIdentifier());
                        insertStatement.setBytes(2, SqlHelper.toOrdered(IdentifierNames.UserName.Admin.getIdentifier()));
                        insertStatement.setString(3, PasswordGuard.encryptPassword(password));
                        insertStatement.setLong(4, UserGroupManager.getInstance().getAdminId());
                        insertStatement.executeUpdate();
                    }
                    statement.setString(1, IdentifierNames.UserName.Admin.getIdentifier());
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        this.adminId.initialize(result.getLong(1));
                    }
                    HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Reset admin user. password: ", password);
                    this.defaultAdminPassword.initialize(password);
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteTable(@Nullable final String _connectionId) throws SQLException {
        try (final Connection connection = this.database.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
    DROP TABLE IF EXISTS users;
                """);
            }
            this.adminId.uninitializeNullable();
            connection.commit();
        }
    }

    @Override
    public long getAdminId() {
        return this.adminId.getInstance().longValue();
    }

    @Override
    public @Nullable String getAndDeleteDefaultAdminPassword() {
        return this.defaultAdminPassword.uninitializeNullable();
    }


    public static final @NotNull String UserInfoExtra = "id, username, password, group_id, create_time, update_time, modify_time";
    // @see #selectUser(long, String) WITH temp AS(...) ...
    public static final @NotNull String UserAndGroupInfoExtra = "id, username, password, temp.create_time AS ct, temp.update_time AS ut, modify_time, " +
            "groups.group_id AS group_id, name, " + UserGroupSqliteHelper.PermissionsHeader + ", groups.create_time AS create_time, groups.update_time AS update_time";

    public static @Nullable UserInformation nextUser(final @NotNull ResultSet result) throws SQLException {
        final UserGroupInformation group = UserGroupSqliteHelper.nextGroup(result);
        if (group == null)
            return null;
        return new UserInformation(result.getLong("id"), result.getString("username"),
                result.getString("password"), group,
                SqlHelper.toZonedDataTime(result.getTimestamp("ct")),
                SqlHelper.toZonedDataTime(result.getTimestamp("ut")),
                SqlHelper.toZonedDataTime(result.getTimestamp("modify_time")));
    }

    public static @NotNull @UnmodifiableView List<@NotNull UserInformation> allUsers(final @NotNull ResultSet result) throws SQLException {
        final List<UserInformation> list = new LinkedList<>();
        while (true) {
            final UserInformation info = UserSqliteHelper.nextUser(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }


    /* --- Insert --- */

    @Override
    public @Nullable UserInformation insertUser(final @NotNull String username, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        if (IdentifierNames.UserName.contains(username))
            return null;
        UserInformation information = null;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final boolean success;
            try (final PreparedStatement statement = connection.prepareStatement("""
    INSERT OR IGNORE INTO users (username, name_order, password)
        VALUES (?, ?, ?);
                """)) {
                statement.setString(1, username);
                statement.setBytes(2, SqlHelper.toOrdered(username));
                statement.setString(3, encryptedPassword);
                success = statement.executeUpdate() == 1;
            }
            if (success)
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s FROM users WHERE username == ? LIMIT 1) SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id LIMIT 1;
                    """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra))) {
                    statement.setString(1, username);
                    try (final ResultSet result = statement.executeQuery()) {
                        information = UserSqliteHelper.nextUser(result);
                    }
                }
            connection.commit();
        }
        return information;
    }


    /* --- Update --- */

    @Override
    public @Nullable ZonedDateTime updateUserName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId() || IdentifierNames.UserName.contains(name))
            return null;
        ZonedDateTime time;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    UPDATE OR IGNORE users SET username = ?, name_order = ?, update_time = ? WHERE id == ?;
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
    public @Nullable ZonedDateTime updateUserPassword(final long id, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        ZonedDateTime time;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    UPDATE OR IGNORE users SET password = ?, update_time = ?, modify_time = ? WHERE id == ?;
                """)) {
                statement.setString(1, encryptedPassword);
                time = SqlHelper.now();
                statement.setTimestamp(2, AndroidSupporter.toTimestamp(time.toLocalDateTime()));
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
    public @Nullable ZonedDateTime updateUserGroup(final long id, final long groupId, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId())
            return null;
        ZonedDateTime time;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    UPDATE OR IGNORE users SET group_id = ?, update_time = ?, modify_time = ? WHERE id == ?;
                """)) {
                statement.setLong(1, groupId);
                time = SqlHelper.now();
                statement.setTimestamp(2, AndroidSupporter.toTimestamp(time.toLocalDateTime()));
                statement.setTimestamp(3, AndroidSupporter.toTimestamp(time.toLocalDateTime()));
                statement.setLong(4, id);
                if (statement.executeUpdate() == 0)
                    time = null;
            }
            connection.commit();
        }
        return time;
    }


    /* --- Select --- */

    @Contract(pure = true)
    protected static @NotNull String orderBy(@SuppressWarnings("TypeMayBeWeakened") final @NotNull @Unmodifiable LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders) {
        if (orders.isEmpty())
            return "ORDER BY temp.name_order ASC, id ASC";
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (final Map.Entry<VisibleUserInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
            builder.append(switch (order.getKey()) {
                case Id -> "temp.id";
                case Name -> "temp.name_order";
                case CreateTime -> "temp.create_time";
                case UpdateTime -> "temp.update_time";
                case GroupId -> "temp.group_id";
                case GroupName -> "groups.name_order";
            }).append(switch (order.getValue()) {
                case ASCEND -> " ASC";
                case DESCEND -> " DESC";
            }).append(',');
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    @Contract(pure = true)
    protected static @NotNull String whereGroup(@SuppressWarnings("TypeMayBeWeakened") final @NotNull @Unmodifiable Set<@NotNull Long> chooser, final boolean blacklist) {
        if (chooser.isEmpty())
            return blacklist ? "WHERE true" : "WHERE false";
        final StringBuilder builder = new StringBuilder("WHERE (");
        if (blacklist) {
            for (final Long id: chooser)
                builder.append("group_id != ").append(id.longValue()).append(" AND ");
            return builder.replace(builder.length() - 5, builder.length(), ")").toString();
        }
        for (final Long id: chooser)
            builder.append("group_id == ").append(id.longValue()).append(" OR ");
        return builder.replace(builder.length() - 4, builder.length(), ")").toString();
    }

    @Override
    public @Nullable UserInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        final UserInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s FROM users WHERE id == ? LIMIT 1) SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id LIMIT 1;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra))) {
                statement.setLong(1, id);
                try (final ResultSet result = statement.executeQuery()) {
                    information = UserSqliteHelper.nextUser(result);
                }
            }
        }
        return information;
    }

    @Override
    public @Nullable UserInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        final UserInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s FROM users WHERE username == ? LIMIT 1) SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id LIMIT 1;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra))) {
                statement.setString(1, username);
                try (final ResultSet result = statement.executeQuery()) {
                    information = UserSqliteHelper.nextUser(result);
                }
            }
        }
        return information;
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsers(final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserInformation> users;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM users;
                """)) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                users = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s, name_order FROM users)
        SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id %s LIMIT ? OFFSET ?;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra, UserSqliteHelper.orderBy(orders)))) {
                    statement.setLong(1, limit);
                    statement.setLong(2, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        users = UserSqliteHelper.allUsers(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, users);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsersByGroups(final @NotNull Set<@NotNull Long> chooser, final boolean blacklist, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (chooser.isEmpty())
            return blacklist ? this.selectUsers(orders, position, limit, _connectionId) : Pair.ImmutablePair.makeImmutablePair(0L, List.of());
        final long count;
        final List<UserInformation> users;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM users %s;
                """, UserSqliteHelper.whereGroup(chooser, blacklist)))) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                users = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s, name_order FROM users %s)
        SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id %s LIMIT ? OFFSET ?;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.whereGroup(chooser, blacklist), UserSqliteHelper.UserAndGroupInfoExtra, UserSqliteHelper.orderBy(orders)))) {
                    statement.setLong(1, limit);
                    statement.setLong(2, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        users = UserSqliteHelper.allUsers(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, users);
    }


    /* --- Delete --- */

    @Override
    public boolean deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        if (id == this.getAdminId())
            return false;
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    DELETE FROM users WHERE id == ?;
                """)) {
                statement.setLong(1, id);
                success = statement.executeUpdate() == 1;
            }
            connection.commit();
        }
        return success;
    }

    @Override
    public long deleteUsersByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        final long count;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    DELETE FROM users WHERE group_id == ? AND id != ?;
                """)) {
                statement.setLong(1, groupId);
                statement.setLong(2, this.getAdminId());
                count = AndroidSupporter.isAndroid ? statement.executeUpdate() : statement.executeLargeUpdate();
            }
            connection.commit();
        }
        return count;
    }

    /* --- Search --- */

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count;
        final List<UserInformation> users;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM users WHERE username REGEXP ?;
                """)) {
                statement.setString(1, regex);
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                users = List.of();
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s, name_order FROM users WHERE username REGEXP ?)
        SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id %s LIMIT ? OFFSET ?;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra, UserSqliteHelper.orderBy(orders)))) {
                    statement.setString(1, regex);
                    statement.setLong(2, limit);
                    statement.setLong(3, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        users = UserSqliteHelper.allUsers(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, users);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (names.size() != 1)
            throw new UnsupportedOperationException("Cannot search users by multiple names."); // TODO multiple names search support.
        final String name = names.stream().findFirst().get();
        final long count;
        final List<UserInformation> users;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement("""
    SELECT COUNT(*) FROM users WHERE username LIKE ? ESCAPE '\\';
                """)) {
                statement.setString(1, SqlHelper.likeName(name));
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (position < 0 || count <= position || limit <= 0)
                users = List.of();
            else
                //noinspection SpellCheckingInspection
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    WITH temp AS (SELECT %s FROM users WHERE username LIKE ? ESCAPE '\\')
        SELECT %s FROM temp INNER JOIN groups ON temp.group_id = groups.group_id ORDER BY length(name), charindex(?, name) LIMIT ? OFFSET ?;
                """, UserSqliteHelper.UserInfoExtra, UserSqliteHelper.UserAndGroupInfoExtra))) {
                    statement.setString(1, SqlHelper.likeName(name));
                    statement.setString(2, name);
                    statement.setLong(3, limit);
                    statement.setLong(4, position);
                    try (final ResultSet result = statement.executeQuery()) {
                        users = UserSqliteHelper.allUsers(result);
                    }
                }
            connection.commit();
        }
        return Pair.ImmutablePair.makeImmutablePair(count, users);
    }

    @Override
    public @NotNull String toString() {
        return "UserSqliteHelper{" +
                "database=" + this.database +
                ", adminId=" + this.adminId +
                ", defaultAdminPassword=" + this.defaultAdminPassword +
                '}';
    }
}
