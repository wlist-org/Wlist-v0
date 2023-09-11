package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
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
import java.util.Set;
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
        return UserManager.sqlInstance.getInstance().getAndDeleteDefaultAdminPassword();
    }


    /* --- Insert --- */

    public static @Nullable UserInformation insertUser(final @NotNull String username, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().insertUser(username, encryptedPassword, _connectionId);
    }

    /* --- Update --- */

    public static @Nullable LocalDateTime updateUserName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().updateUserName(id, name, _connectionId);
    }

    public static @Nullable LocalDateTime updateUserPassword(final long id, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().updateUserPassword(id, encryptedPassword, _connectionId);
    }

    public static @Nullable LocalDateTime updateUserGroup(final long id, final long groupId, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().updateUserGroup(id, groupId, _connectionId);
    }

    /* --- Select --- */

    public static @Nullable UserInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUser(id, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsers(final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsers(orders, position, limit, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsersByGroup(final @NotNull Set<@NotNull Long> chooser, final boolean blacklist, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().selectUsersByGroup(chooser, blacklist, orders, position, limit, _connectionId);
    }

    /* --- Delete --- */

    public static boolean deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().deleteUser(id, _connectionId);
    }

    public static long deleteUsersByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().deleteUsersByGroup(groupId, _connectionId);
    }

    /* --- Search --- */

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().searchUsersByRegex(regex, orders, position, limit, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserManager.sqlInstance.getInstance().searchUsersByNames(names, position, limit, _connectionId);
    }
}
