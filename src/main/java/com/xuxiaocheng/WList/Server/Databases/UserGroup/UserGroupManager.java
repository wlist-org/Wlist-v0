package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class UserGroupManager {
    private UserGroupManager() {
        super();
    }

    public static @NotNull DatabaseUtil getDatabaseUtil() {
        return UserGroupSqlHelper.getInstance().getDatabase();
    }

    public static void initialize(final @NotNull DatabaseUtil database) throws SQLException {
        UserGroupSqlHelper.initialize(database);
    }

    // TODO cache lock.
    private static final @NotNull Cache<@NotNull Long, @NotNull UserGroupSqlInformation> Cache = Caffeine.newBuilder()
            .maximumSize(GlobalConfiguration.getInstance().maxCacheSize())
            .softValues().build();

    public static @NotNull @UnmodifiableView Map<UserGroupSqlInformation.@NotNull Inserter, @NotNull Boolean> insertGroups(final @NotNull Set<UserGroupSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserGroupSqlHelper.getInstance().insertGroups(inserters, _connectionId);
    }

    public static boolean insertGroup(final UserGroupSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.insertGroups(Set.of(inserter), _connectionId).get(inserter).booleanValue();
    }

    public static void updateGroups(final @NotNull Collection<@NotNull UserGroupSqlInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.Cache.putAll(infoList.stream().collect(Collectors.toMap(UserGroupSqlInformation::id, Function.identity())));
        UserGroupSqlHelper.getInstance().updateGroups(infoList, _connectionId);
    }

    public static void updateGroup(final @NotNull UserGroupSqlInformation info, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.updateGroups(List.of(info), _connectionId);
    }

    @Deprecated
    public static void updateGroupsByName(final @NotNull Collection<UserGroupSqlInformation.@NotNull Inserter> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserGroupManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            UserGroupSqlHelper.getInstance().updateGroupsByName(infoList, connectionId.get());
            connection.commit();
            // Update cache.
            UserGroupManager.selectGroupsByName(infoList.stream().map(UserGroupSqlInformation.Inserter::name).collect(Collectors.toSet()), connectionId.get());
        }
    }

    @Deprecated
    public static void updateGroupByName(final UserGroupSqlInformation.@NotNull Inserter info, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.updateGroupsByName(List.of(info), _connectionId);
    }

    public static void deleteGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        UserGroupSqlHelper.getInstance().deleteGroups(idList, _connectionId);
        UserGroupManager.Cache.invalidateAll(idList);
    }

    public static void deleteGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.deleteGroups(List.of(id), _connectionId);
    }

    @Deprecated
    public static void deleteGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserGroupManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<UserGroupSqlInformation> list = UserGroupSqlHelper.getInstance().selectGroupsByName(nameList, connectionId.get());
            UserGroupSqlHelper.getInstance().deleteGroupsByName(nameList, connectionId.get());
            connection.commit();
            UserGroupManager.Cache.invalidateAll(list.stream().map(UserGroupSqlInformation::id).collect(Collectors.toSet()));
        }
    }

    @Deprecated
    public static void deleteGroupByName(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        UserGroupManager.deleteGroupsByName(List.of(name), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @Nullable UserGroupSqlInformation> selectGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        final Map<Long, UserGroupSqlInformation> cached = UserGroupManager.Cache.getAllPresent(idList);
        final Collection<Long> rest = new HashSet<>(idList);
        rest.removeAll(cached.keySet());
        final Map<Long, UserGroupSqlInformation> required = UserGroupSqlHelper.getInstance().selectGroups(rest, _connectionId);
        UserGroupManager.Cache.putAll(required);
        cached.putAll(required);
        return cached;
    }

    public static @Nullable UserGroupSqlInformation selectGroup(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.selectGroups(List.of(id), _connectionId).get(id);
    }

    @Deprecated
    public static @NotNull @UnmodifiableView List<@Nullable UserGroupSqlInformation> selectGroupsByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        final List<UserGroupSqlInformation> list = UserGroupSqlHelper.getInstance().selectGroupsByName(nameList, _connectionId);
        UserGroupManager.Cache.putAll(list.stream().collect(Collectors.toMap(UserGroupSqlInformation::id, Function.identity())));
        return list;
    }

    @Deprecated
    public static @Nullable UserGroupSqlInformation selectGroupByName(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return UserGroupManager.selectGroupsByName(List.of(name), _connectionId).get(0);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserGroupSqlInformation>> selectAllUserGroupsInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        final Pair.ImmutablePair<Long, List<UserGroupSqlInformation>> list = UserGroupSqlHelper.getInstance().selectAllUserGroupsInPage(limit, offset, direction, _connectionId);
        UserGroupManager.Cache.putAll(list.getSecond().stream().collect(Collectors.toMap(UserGroupSqlInformation::id, Function.identity())));
        return list;
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserGroupSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        final List<UserGroupSqlInformation> list = UserGroupSqlHelper.getInstance().searchUsersByNameLimited(rule, caseSensitive, limit, _connectionId);
        UserGroupManager.Cache.putAll(list.stream().collect(Collectors.toMap(UserGroupSqlInformation::id, Function.identity())));
        return list;
    }
}
