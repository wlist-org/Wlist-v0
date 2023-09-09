package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Commons.Options;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @see com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName
 */
public interface UserGroupSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) long getAdminId();
    @Contract(pure = true) long getDefaultId();


    /* --- Insert --- */

    /**
     * Insert group unless name is conflicted.
     * @return null: name is conflict. !null: inserted information.
     */
    @Nullable UserGroupInformation insertGroup(final @NotNull String name, final @Nullable String _connectionId) throws SQLException;


    /* --- Update --- */

    /**
     * Update group name.
     * @return false: name is conflict OR no such id. true: success.
     */
    boolean updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;

    /**
     * Update group permissions.
     * @return false: no such id. true: success.
     */
    boolean updateGroupPermission(final long id, final @NotNull EnumSet<Operation.@NotNull Permission> permissions, final @Nullable String _connectionId) throws SQLException;


    /* --- Select --- */

    /**
     * Select group by id.
     * @return null: not found. !null: found.
     */
    @Nullable UserGroupInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select all groups.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroups(final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select groups which has/hasn't the permissions.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull EnumMap<Operation.@NotNull Permission, @Nullable Boolean> permissions, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;


    /* --- Delete --- */

    /**
     * Delete group by id.
     * @return false: in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName} OR no such id. true: success.
     */
    boolean deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException;

    /**
     * Delete groups which has/hasn't the permissions. (Do NOT delete groups in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName})
     * @return deleted count.
     */
    long deleteGroupsByPermissions(final @NotNull EnumMap<Operation.@NotNull Permission, @Nullable Boolean> permissions, final @Nullable String _connectionId) throws SQLException;


    /* --- Search --- */

    // TODO
//    @NotNull @UnmodifiableView List<@Nullable UserGroupInformation> searchGroupsNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException;
}
