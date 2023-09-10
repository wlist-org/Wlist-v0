package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Options.Options;
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

public final class UserManager {
    private UserManager() {
        super();
    }

    public static final @NotNull HInitializer<UserSqlInterface> sqlInstance = new HInitializer<>("UserSqlInstance");

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull UserSqlInterface>> SqlMapper = new HInitializer<>("UserGroupSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing UserManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new UserSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            UserManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final UserSqlInterface instance = UserManager.SqlMapper.getInstance().apply(database);
                instance.createTable(_connectionId);
                return instance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable() {
        return UserManager.sqlInstance.uninitializeNullable() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final UserSqlInterface sqlInstance = UserManager.sqlInstance.uninitializeNullable();
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }

    public static @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().getConnection(_connectionId, connectionId);
    }

    public static long getAdminId() {
        return UserManager.sqlInstance.getInstance().getAdminId();
    }

    public static @Nullable String getAndDeleteDefaultAdminPasswordAPI() {
        return UserManager.sqlInstance.getInstance().getDefaultAdminPassword().uninitializeNullable();
    }

    public static @NotNull @UnmodifiableView Map<UserInformation.@NotNull Inserter, @Nullable Long> insertUsers(final @NotNull Collection<UserInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().insertUsers(inserters, _connectionId);
    }

    public static @Nullable Long insertUser(final UserInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserManager.insertUsers(List.of(inserter), _connectionId).get(inserter);
    }

    public static void updateUsers(final @NotNull Collection<UserInformation.@NotNull Updater> updaters, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().updateUsers(updaters, _connectionId);
    }

    public static void updateUser(final UserInformation.@NotNull Updater updater, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsers(List.of(updater), _connectionId);
    }

    public static void updateUsersByName(final @NotNull Collection<UserInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().updateUsersByName(inserters, _connectionId);
    }

    public static void updateUserByName(final UserInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsersByName(List.of(inserter), _connectionId);
    }

    public static void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().deleteUsers(idList, _connectionId);
    }

    public static void deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsers(List.of(id), _connectionId);
    }

    public static void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().deleteUsersByName(usernameList, _connectionId);
    }

    public static void deleteUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsersByName(List.of(username), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsers(idList, _connectionId);
    }

    public static @Nullable UserInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsers(List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsersByName(usernameList, _connectionId);
    }

    public static @Nullable UserInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersByName(List.of(username), _connectionId).get(username);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectUsersCountByGroup(final @NotNull Collection<@NotNull Long> groupIdList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsersCountByGroup(groupIdList, _connectionId);
    }

    public static long selectUserCountByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersCountByGroup(List.of(groupId), _connectionId).get(groupId).longValue();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectAllUsersInPage(limit, offset, direction, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().searchUsersByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
