package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
     * Update group name. (Do NOT update groups in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName})
     * @return null: name is conflict OR no such id. !null: operate time.
     */
    @Nullable LocalDateTime updateGroupName(final long id, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;

    /**
     * Update group permissions. (Do NOT update admin groupName {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName#Admin})
     * @return null: no such id. !null: operate time.
     */
    @Nullable LocalDateTime updateGroupPermission(final long id, final @NotNull EnumSet<@NotNull UserPermission> permissions, final @Nullable String _connectionId) throws SQLException;


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
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> selectGroupsByPermissions(final @NotNull EnumMap<@NotNull UserPermission, @Nullable Boolean> chooser, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;


    /* --- Delete --- */

    /**
     * Delete group by id.
     * @return false: in {@link com.xuxiaocheng.WList.Commons.IdentifierNames.UserGroupName} OR no such id. true: success.
     */
    boolean deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException;


    /* --- Search --- */

    /**
     * Search groups whose name matches regex.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByRegex(final @NotNull String regex, final @NotNull LinkedHashMap<VisibleUserGroupInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /**
     * Search groups which contains name.
     */
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @Unmodifiable List<@NotNull UserGroupInformation>> searchGroupsByNames(final @NotNull Set<@NotNull String> names, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;
}
