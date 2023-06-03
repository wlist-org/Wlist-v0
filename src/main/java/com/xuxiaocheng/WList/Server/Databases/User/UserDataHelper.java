package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

// TODO user group.
public final class UserDataHelper {
    private UserDataHelper() {
        super();
    }

    public static void initialize(final @NotNull SortedSet<Operation.@NotNull Permission> defaultPermissions, final @NotNull Iterable<Operation.@NotNull Permission> adminPermissions) throws SQLException {
        UserSqlHelper.initialize(defaultPermissions, adminPermissions, "initialize");
    }

    // TODO cache.

    public static @NotNull List<@NotNull Boolean> insertUsers(final @NotNull Collection<UserSqlInformation.@NotNull Inserter> infoList, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.insertUsers(infoList, connectionId);
    }

    public static boolean insertUser(final @NotNull UserSqlInformation.Inserter info, final @Nullable String connectionId) throws SQLException {
        return UserDataHelper.insertUsers(List.of(info), connectionId).get(0).booleanValue();
    }

    public static void updateUsers(final @NotNull Collection<@NotNull UserSqlInformation> infoList, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.updateUsers(infoList, connectionId);
    }

    public static void updateUser(final @NotNull UserSqlInformation info, final @Nullable String connectionId) throws SQLException {
        UserDataHelper.updateUsers(List.of(info), connectionId);
    }

    public static void updateUsersByName(final @NotNull Collection<UserSqlInformation.@NotNull Updater> infoList, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.updateUsersByName(infoList, connectionId);
    }

    public static void updateUserByName(final @NotNull UserSqlInformation.Updater info, final @Nullable String connectionId) throws SQLException {
        UserDataHelper.updateUsersByName(List.of(info), connectionId);
    }

    public static void deleteUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.deleteUsers(idList, connectionId);
    }

    public static void deleteUser(final long id, final @Nullable String connectionId) throws SQLException {
        UserDataHelper.deleteUsers(List.of(id), connectionId);
    }

    public static void deleteUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String connectionId) throws SQLException {
        UserSqlHelper.deleteUsersByName(usernameList, connectionId);
    }

    public static void deleteUserByName(final @NotNull String username, final @Nullable String connectionId) throws SQLException {
        UserDataHelper.deleteUsersByName(List.of(username), connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> selectUsers(final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.selectUsers(idList, connectionId);
    }

    public static @Nullable UserSqlInformation selectUser(final long id, final @Nullable String connectionId) throws SQLException {
        return UserDataHelper.selectUsers(List.of(id), connectionId).get(0);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> selectUsersByName(final @NotNull Collection<@NotNull String> usernameList, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.selectUsersByName(usernameList, connectionId);
    }

    public static @Nullable UserSqlInformation selectUserByName(final @NotNull String username, final @Nullable String connectionId) throws SQLException {
        return UserDataHelper.selectUsersByName(List.of(username), connectionId).get(0);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserSqlInformation>> selectAllUsersInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.selectAllUsersInPage(limit, offset, direction, connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable UserSqlInformation> searchUsersByNameInPage(final @NotNull String rule, final boolean caseSensitive, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final @Nullable String connectionId) throws SQLException {
        return UserSqlHelper.searchUsersByNameInPage(rule, caseSensitive, limit, offset, direction, connectionId);
    }
}
