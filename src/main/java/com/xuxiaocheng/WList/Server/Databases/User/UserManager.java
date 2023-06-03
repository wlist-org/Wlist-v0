package com.xuxiaocheng.WList.Server.Databases.User;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class UserManager {
    private UserManager() {
        super();
    }

    public static @NotNull DatabaseUtil getDatabaseUtil() throws SQLException {
        return DatabaseUtil.getInstance();
    }

    public static void initialize() throws SQLException {
        UserSqlHelper.initialize(UserManager.getDatabaseUtil(), "initialize");
    }

    public static long getAdminGroupId() {
        return UserSqlHelper.getInstance().getAdminId();
    }

    public static long getDefaultGroupId() {
        return UserSqlHelper.getInstance().getDefaultId();
    }

    // TODO cache lock.
    private static final @NotNull com.github.benmanes.caffeine.cache.Cache<@NotNull Long, @NotNull UserSqlInformation> Cache = Caffeine.newBuilder()
            .maximumSize(GlobalConfiguration.getInstance().maxCacheSize())
            .softValues().build();


    public static @NotNull @UnmodifiableView Map<UserSqlInformation.@NotNull Inserter, @NotNull Boolean> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().insertUsers(inserters, _connectionId);
    }

    public static boolean insertUser(final UserSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserManager.insertUsers(List.of(inserter), _connectionId).get(inserter).booleanValue();
    }

    public static void updateUsers(final @NotNull Collection<@NotNull UserSqlInformation> infoList, final @Nullable String _connectionId) throws SQLException {
        UserManager.Cache.putAll(infoList.stream().collect(Collectors.toMap(UserSqlInformation::id, Function.identity())));
        UserSqlHelper.getInstance().updateUsers(infoList, _connectionId);
    }

    public static void updateUser(final @NotNull UserSqlInformation info, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsers(List.of(info), _connectionId);
    }

    @Deprecated
    public static void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> infoList, final @Nullable String _connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            UserSqlHelper.getInstance().updateUsersByName(infoList, connectionId.get());
            connection.commit();
            // Update cache.
            UserManager.selectUsersByName(infoList.stream().map(UserSqlInformation.Inserter::username).collect(Collectors.toSet()), connectionId.get());
        }
    }

    @Deprecated
    public static void updateUserByName(final UserSqlInformation.@NotNull Inserter info, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsersByName(List.of(info), _connectionId);
    }

    public static void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        UserSqlHelper.getInstance().deleteUsers(idList, _connectionId);
        UserManager.Cache.invalidateAll(idList);
    }

    public static void deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsers(List.of(id), _connectionId);
    }

    @Deprecated
    public static void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        if (usernameList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = UserManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, UserSqlInformation> map = UserSqlHelper.getInstance().selectUsersByName(usernameList, connectionId.get());
            UserSqlHelper.getInstance().deleteUsersByName(usernameList, connectionId.get());
            connection.commit();
            UserManager.Cache.invalidateAll(map.values().stream().map(UserSqlInformation::id).collect(Collectors.toSet()));
        }
    }

    @Deprecated
    public static void deleteUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsersByName(List.of(username), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        final Map<Long, UserSqlInformation> cached = UserManager.Cache.getAllPresent(idList);
        final Collection<Long> rest = new HashSet<>(idList);
        rest.removeAll(cached.keySet());
        final Map<Long, UserSqlInformation> required = UserSqlHelper.getInstance().selectUsers(rest, _connectionId);
        UserManager.Cache.putAll(required);
        cached.putAll(required);
        return cached;
    }

    public static @Nullable UserSqlInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsers(List.of(id), _connectionId).get(id);
    }

//    @Deprecated
    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        final Map<String, UserSqlInformation> map = UserSqlHelper.getInstance().selectUsersByName(usernameList, _connectionId);
        UserManager.Cache.putAll(map.values().stream().collect(Collectors.toMap(UserSqlInformation::id, Function.identity())));
        return map;
    }

//    @Deprecated
    public static @Nullable UserSqlInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersByName(List.of(username), _connectionId).get(username);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        final Pair.ImmutablePair<Long, List<UserSqlInformation>> list = UserSqlHelper.getInstance().selectAllUsersInPage(limit, offset, direction, _connectionId);
        UserManager.Cache.putAll(list.getSecond().stream().collect(Collectors.toMap(UserSqlInformation::id, Function.identity())));
        return list;
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        final List<UserSqlInformation> list = UserSqlHelper.getInstance().searchUsersByNameLimited(rule, caseSensitive, limit, _connectionId);
        UserManager.Cache.putAll(list.stream().collect(Collectors.toMap(UserSqlInformation::id, Function.identity())));
        return list;
    }
}
