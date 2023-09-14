package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public record UserGroupManager(@NotNull UserGroupSqlInterface innerSqlInstance) implements UserGroupSqlInterface {
    private static final @NotNull HInitializer<UserGroupManager> ManagerInstance = new HInitializer<>("UserGroupManager");

    @Deprecated
    public static @NotNull HInitializer<UserGroupManager> getManagerInstance() {
        return UserGroupManager.ManagerInstance;
    }

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull UserGroupSqlInterface>> SqlMapper = new HInitializer<>("UserGroupSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing UserGroupManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new UserGroupSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            UserGroupManager.ManagerInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final UserGroupSqlInterface instance = UserGroupManager.SqlMapper.getInstance().apply(database);
                instance.createTable(_connectionId);
                return new UserGroupManager(instance);
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitializeReserveTable() {
        return UserGroupManager.ManagerInstance.uninitializeNullable() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final UserGroupSqlInterface sqlInstance = UserGroupManager.ManagerInstance.uninitializeNullable();
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }


    public static @NotNull UserGroupManager getInstance() {
        return UserGroupManager.ManagerInstance.getInstance();
    }

    @Deprecated
    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.createTable(_connectionId);
    }

    @Deprecated
    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.deleteTable(_connectionId);
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.innerSqlInstance.getConnection(_connectionId, connectionId);
    }

    @Override
    public long getAdminId() {
        return this.innerSqlInstance.getAdminId();
    }

    @Override
    public long getDefaultId() {
        return this.innerSqlInstance.getDefaultId();
    }


    /* --- Insert --- */

    @Override
    public @Nullable UserGroupInformation insertGroup(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.insertGroup(name, _connectionId);
    }

    /* --- Update --- */

    @Override
    public @Nullable LocalDateTime updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.updateGroupName(id, name, _connectionId);
    }

    @Override
    public @Nullable LocalDateTime updateGroupPermission(final long id, final @NotNull Set<@NotNull UserPermission> permissions, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.updateGroupPermission(id, permissions, _connectionId);
    }

    /* --- Select --- */

    @Override
    public @Nullable UserGroupInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectGroup(id, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroups(final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectGroups(orders, position, limit, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull Map<@NotNull UserPermission, @Nullable Boolean> permissions, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectGroupsByPermissions(permissions, orders, position, limit, _connectionId);
    }

    /* --- Delete --- */

    @Override
    public boolean deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteGroup(id, _connectionId);
    }

    /* --- Search --- */

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.searchGroupsByRegex(regex, orders, position, limit, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.searchGroupsByNames(names, position, limit, _connectionId);
    }
}
