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
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public record UserManager(@NotNull UserSqlInterface innerSqlInstance) implements UserSqlInterface {
    private static final @NotNull HInitializer<UserManager> ManagerInstance = new HInitializer<>("UserManager");

    @Deprecated
    public static @NotNull HInitializer<UserManager> getManagerInstance() {
        return UserManager.ManagerInstance;
    }

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull UserSqlInterface>> SqlMapper = new HInitializer<>("UserGroupSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing UserManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new UserSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            UserManager.ManagerInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final UserSqlInterface instance = UserManager.SqlMapper.getInstance().apply(database);
                instance.createTable(_connectionId);
                return new UserManager(instance);
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitializeReserveTable() {
        return UserManager.ManagerInstance.uninitializeNullable() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final UserSqlInterface sqlInstance = UserManager.ManagerInstance.uninitializeNullable();
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }


    public static @NotNull UserManager getInstance() {
        return UserManager.ManagerInstance.getInstance();
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
    public @Nullable String getAndDeleteDefaultAdminPassword() {
        return this.innerSqlInstance.getAndDeleteDefaultAdminPassword();
    }

    /* --- Insert --- */

    @Override
    public @Nullable UserInformation insertUser(final @NotNull String username, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.insertUser(username, encryptedPassword, _connectionId);
    }

    /* --- Update --- */

    @Override
    public @Nullable ZonedDateTime updateUserName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.updateUserName(id, name, _connectionId);
    }

    @Override
    public @Nullable ZonedDateTime updateUserPassword(final long id, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.updateUserPassword(id, encryptedPassword, _connectionId);
    }

    @Override
    public @Nullable ZonedDateTime updateUserGroup(final long id, final long groupId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.updateUserGroup(id, groupId, _connectionId);
    }

    /* --- Select --- */

    @Override
    public @Nullable UserInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectUser(id, _connectionId);
    }

    @Override
    public @Nullable UserInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectUserByName(username, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsers(final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectUsers(orders, position, limit, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsersByGroups(final @NotNull Set<@NotNull Long> chooser, final boolean blacklist, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectUsersByGroups(chooser, blacklist, orders, position, limit, _connectionId);
    }

    /* --- Delete --- */

    @Override
    public boolean deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteUser(id, _connectionId);
    }

    @Override
    public long deleteUsersByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteUsersByGroup(groupId, _connectionId);
    }

    /* --- Search --- */

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.searchUsersByRegex(regex, orders, position, limit, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.searchUsersByNames(names, position, limit, _connectionId);
    }
}
