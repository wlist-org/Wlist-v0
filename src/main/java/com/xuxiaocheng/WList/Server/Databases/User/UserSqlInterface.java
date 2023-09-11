package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * @see PasswordGuard
 */
public interface UserSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) long getAdminId();
    @Nullable String getAndDeleteDefaultAdminPassword(); // {@see com.xuxiaocheng.HeadLibs.Initializers.HInitializer#uninitializeNullable()}


    /* --- Insert --- */

    /**
     * Insert user unless name is conflicted.
     * @return null: name is conflict. !null: inserted information.
     */
    @Nullable UserInformation insertUser(final @NotNull String username, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException;


    /* --- Update --- */

    /**
     * Update username. (Do NOT update name in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserName})
     * @return null: name is conflict OR no such id. !null: operate time.
     */
    @Nullable LocalDateTime updateUserName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;

    /**
     * Update user password.
     * @return null: no such id. !null: operate time.
     */
    @Nullable LocalDateTime updateUserPassword(final long id, final @NotNull String encryptedPassword, final @Nullable String _connectionId) throws SQLException;

    /**
     * Update user group.
     * @return null: no such id. !null: operate time.
     */
    @Nullable LocalDateTime updateUserGroup(final long id, final long groupId, final @Nullable String _connectionId) throws SQLException;


    /* --- Select --- */

    /**
     * Select user by id.
     * @return null: not found. !null: found.
     */
    @Nullable UserInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select all users.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsers(final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select groups which in/not in the groups.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> selectUsersByGroups(final @NotNull Set<@NotNull Long> chooser, final boolean blacklist, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;


    /* --- Delete --- */

    /**
     * Delete user by id.
     * @return false: in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserName} OR no such id. true: success.
     */
    boolean deleteUser(final long id, final @Nullable String _connectionId) throws SQLException;

    /**
     * Delete users in group. (Do NOT delete user in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserName})
     * @return false: deleted count.
     */
    long deleteUsersByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException;


    /* --- Search --- */

    /**
     * Search users whose name matches regex.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /**
     * Search users which contains name.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserInformation>> searchUsersByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;
}
