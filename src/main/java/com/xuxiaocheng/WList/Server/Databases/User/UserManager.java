package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.Driver.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class UserManager {
    private UserManager() {
        super();
    }

    private static final @NotNull HInitializer<UserSqlInterface> sqlInstance = new HInitializer<>("UserSqlInstance");

    public static void quicklyInitialize(final @NotNull UserSqlInterface sqlInstance, final @Nullable String _connectionId) throws SQLException {
        try {
            UserManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                sqlInstance.createTable(_connectionId);
                return sqlInstance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable() {
        return UserManager.sqlInstance.uninitialize() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final UserSqlInterface sqlInstance = UserManager.sqlInstance.uninitialize();
        if (sqlInstance != null) sqlInstance.deleteTable(_connectionId);
        return sqlInstance != null;
    }

    public static @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().getConnection(_connectionId, connectionId);
    }

    public static final @NotNull String ADMIN = "admin";

    public static long getAdminId() {
        return UserManager.sqlInstance.getInstance().getAdminId();
    }

    public static @Nullable String getAndDeleteDefaultAdminPasswordAPI() {
        final String password = UserManager.sqlInstance.getInstance().getDefaultAdminPassword().getInstanceNullable();
        UserManager.sqlInstance.getInstance().getDefaultAdminPassword().uninitialize();
        return password;
    }

    public static @NotNull @UnmodifiableView Map<UserSqlInformation.@NotNull Inserter, @Nullable Long> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().insertUsers(inserters, _connectionId);
    }

    public static @Nullable Long insertUser(final UserSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserManager.insertUsers(List.of(inserter), _connectionId).get(inserter);
    }

    public static void updateUsers(final @NotNull Collection<UserSqlInformation.@NotNull Updater> updaters, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().updateUsers(updaters, _connectionId);
    }

    public static void updateUser(final UserSqlInformation.@NotNull Updater updater, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsers(List.of(updater), _connectionId);
    }

    public static void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        UserManager.sqlInstance.getInstance().updateUsersByName(inserters, _connectionId);
    }

    public static void updateUserByName(final UserSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
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

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsers(idList, _connectionId);
    }

    public static @Nullable UserSqlInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsers(List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsersByName(usernameList, _connectionId);
    }

    public static @Nullable UserSqlInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersByName(List.of(username), _connectionId).get(username);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectUsersCountByGroup(final @NotNull Collection<@NotNull Long> groupIdList, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsersCountByGroup(groupIdList, _connectionId);
    }

    public static long selectUserCountByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersCountByGroup(List.of(groupId), _connectionId).get(groupId).longValue();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectAllUsersInPage(limit, offset, direction, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().searchUsersByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
