package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class UserGroupManager {
    private UserGroupManager() {
        super();
    }

    public static final @NotNull HInitializer<UserGroupSqlInterface> sqlInstance = new HInitializer<>("UserGroupSqlInstance");

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull UserGroupSqlInterface>> SqlMapper = new HInitializer<>("UserGroupSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing UserGroupManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new UserGroupSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            UserGroupManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final UserGroupSqlInterface instance = UserGroupManager.SqlMapper.getInstance().apply(database);
                instance.createTable(_connectionId);
                return instance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable() {
        return UserGroupManager.sqlInstance.uninitializeNullable() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final UserGroupSqlInterface sqlInstance = UserGroupManager.sqlInstance.uninitializeNullable();
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }


    public static @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().getConnection(_connectionId, connectionId);
    }

    public static long getAdminId() {
        return UserGroupManager.sqlInstance.getInstance().getAdminId();
    }

    public static long getDefaultId() {
        return UserGroupManager.sqlInstance.getInstance().getDefaultId();
    }


    /* --- Insert --- */

    public static @Nullable UserGroupInformation insertGroup(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().insertGroup(name, _connectionId);
    }

    /* --- Update --- */

    public static @Nullable LocalDateTime updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().updateGroupName(id, name, _connectionId);
    }

    public static @Nullable LocalDateTime updateGroupPermission(final long id, final @NotNull EnumSet<@NotNull UserPermission> permissions, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().updateGroupPermission(id, permissions, _connectionId);
    }

    /* --- Select --- */

    public static @Nullable UserGroupInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectGroup(id, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroups(final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectGroups(orders, position, limit, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull EnumMap<@NotNull UserPermission, @Nullable Boolean> permissions, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectGroupsByPermissions(permissions, orders, position, limit, _connectionId);
    }

    /* --- Delete --- */

    public static boolean deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().deleteGroup(id, _connectionId);
    }

    /* --- Search --- */

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().searchGroupsByRegex(regex, orders, position, limit, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().searchGroupsByNames(names, position, limit, _connectionId);
    }
}
