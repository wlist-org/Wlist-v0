package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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


    public static @NotNull @UnmodifiableView Map<UserSqlInformation.@NotNull Inserter, @NotNull Boolean> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().insertUsers(inserters, _connectionId);
    }

    public static boolean insertUser(final UserSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        return UserManager.insertUsers(List.of(inserter), _connectionId).get(inserter).booleanValue();
    }

    public static void updateUsers(final @NotNull Collection<UserSqlInformation.@NotNull Updater> updaters, final @Nullable String _connectionId) throws SQLException {
        UserSqlHelper.getInstance().updateUsers(updaters, _connectionId);
    }

    public static void updateUser(final @NotNull UserSqlInformation.Updater updater, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsers(List.of(updater), _connectionId);
    }

    public static void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        UserSqlHelper.getInstance().updateUsersByName(inserters, _connectionId);

    }

    public static void updateUserByName(final UserSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        UserManager.updateUsersByName(List.of(inserter), _connectionId);
    }

    public static void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        UserSqlHelper.getInstance().deleteUsers(idList, _connectionId);
    }

    public static void deleteUser(final long id, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsers(List.of(id), _connectionId);
    }

    public static void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        UserSqlHelper.getInstance().deleteUsersByName(usernameList, _connectionId);
    }

    public static void deleteUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        UserManager.deleteUsersByName(List.of(username), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().selectUsers(idList, _connectionId);
    }

    public static @Nullable UserSqlInformation selectUser(final long id, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsers(List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().selectUsersByName(usernameList, _connectionId);
    }

    public static @Nullable UserSqlInformation selectUserByName(final @NotNull String username, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersByName(List.of(username), _connectionId).get(username);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectUsersCountByGroup(final @NotNull Collection<@NotNull Long> groupIdList, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().selectUsersCountByGroup(groupIdList, _connectionId);
    }

    public static long selectUserCountByGroup(final long groupId, final @Nullable String _connectionId) throws SQLException {
        return UserManager.selectUsersCountByGroup(List.of(groupId), _connectionId).get(groupId).longValue();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().selectAllUsersInPage(limit, offset, direction, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return UserSqlHelper.getInstance().searchUsersByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
