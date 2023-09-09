package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class UserGroupManager {
    private UserGroupManager() {
        super();
    }

    public static final @NotNull HInitializer<UserGroupSqlInterface> sqlInstance = new HInitializer<>("UserGroupSqlInstance");

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull UserGroupSqlInterface>> Mapper = new HInitializer<>("UserGroupSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing UserGroupManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new UserGroupSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            UserGroupManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final UserGroupSqlInterface instance = UserGroupManager.Mapper.getInstance().apply(database);
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

    public static @NotNull @UnmodifiableView Map<UserGroupInformation.@NotNull Inserter, @Nullable Long> insertGroups(final @NotNull Collection<UserGroupInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().insertGroups(inserters, _connectionId);
    }

    public static @Nullable Long insertGroup(final UserGroupInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.insertGroups(List.of(inserter), _connectionId).get(inserter);
    }

    public static void updateGroups(final @NotNull Collection<@NotNull UserGroupInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.sqlInstance.getInstance().updateGroups(infoList, _connectionId);
    }

    public static void updateGroup(final @NotNull UserGroupInformation info, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.updateGroups(List.of(info), _connectionId);
    }

    public static void updateGroupsByName(final @NotNull Collection<UserGroupInformation.@NotNull Inserter> infoList, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.sqlInstance.getInstance().updateGroupsByName(infoList, _connectionId);
    }

    public static void updateGroupByName(final UserGroupInformation.@NotNull Inserter info, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.updateGroupsByName(List.of(info), _connectionId);
    }

    public static void deleteGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.sqlInstance.getInstance().deleteGroups(idList, _connectionId);
    }

    public static void deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.deleteGroups(List.of(id), _connectionId);
    }

    public static void deleteGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.sqlInstance.getInstance().deleteGroupsByName(nameList, _connectionId);
    }

    public static void deleteGroupByName(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.deleteGroupsByName(List.of(name), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserGroupInformation> selectGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectGroups(idList, _connectionId);
    }

    public static @Nullable UserGroupInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.selectGroups(List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserGroupInformation> selectGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectGroupsByName(nameList, _connectionId);
    }

    public static @Nullable UserGroupInformation selectGroupByName(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.selectGroupsByName(List.of(name), _connectionId).get(name);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserGroupInformation>> selectAllUserGroupsInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().selectAllUserGroupsInPage(limit, offset, direction, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserGroupInformation> searchUserGroupsByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.sqlInstance.getInstance().searchUserGroupsByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
