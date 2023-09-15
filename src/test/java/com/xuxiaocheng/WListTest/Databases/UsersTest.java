package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Execution(ExecutionMode.SAME_THREAD)
public final class UsersTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() throws SQLException {
        StaticLoader.load();
        final File file = new File(UsersTest.directory, "data.db");
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
        ConstantManager.quicklyInitialize(database, null); // Salt
        UserGroupManager.quicklyInitialize(database, null); // Group
        UserManager.quicklyInitialize(database, "initialize");
    }

    @AfterAll
    public static void uninitialize() throws SQLException {
        UserManager.quicklyUninitialize(null);
        UserGroupManager.quicklyUninitialize(null);
        Assertions.assertTrue(ConstantManager.quicklyUninitializeReserveTable());
        final File file = new File(UsersTest.directory, "data.db");
        SqlDatabaseManager.quicklyClose(file);
    }

    public static Stream<Arguments> curd() throws SQLException {
        Assertions.assertFalse(UserManager.getInstance().deleteUser(UserManager.getInstance().getAdminId(), null));
        Assertions.assertEquals(PasswordGuard.encryptPassword(Objects.requireNonNull(UserManager.getInstance().getAndDeleteDefaultAdminPassword())),
                Objects.requireNonNull(UserManager.getInstance().selectUser(UserManager.getInstance().getAdminId(), null)).encryptedPassword());
        return Stream.of( null,
                Arguments.of(IdentifierNames.UserName.Admin.getIdentifier(), false, 0, false, null, false),
                Arguments.of("test-admin", true, UserGroupManager.getInstance().getAdminId(), true, "test1", true),
                Arguments.of("test", true, UserGroupManager.getInstance().getDefaultId(), true, "test2", true)
        ).skip(1);
    }

    @ParameterizedTest
    @MethodSource
    public void curd(final String name, final boolean nameS,
                     final long newGroup, final boolean newGroupS,
                     final String newName, final boolean newNameS)
            throws SQLException {

        final UserInformation information = UserManager.getInstance().insertUser(name, "", null);
        if (!nameS) {
            Assertions.assertNull(information);
            return;
        }
        Assertions.assertNotNull(information);
        Assertions.assertEquals(information, UserManager.getInstance().selectUser(information.id(), null));

        final ZonedDateTime t = UserManager.getInstance().updateUserPassword(information.id(), PasswordGuard.encryptPassword(""), null);
        Assertions.assertNotNull(t);
        Assertions.assertEquals(new UserInformation(information.id(), information.username(), PasswordGuard.encryptPassword(""), information.group(),
                        information.createTime(), t, t), UserManager.getInstance().selectUser(information.id(), null));

        final UserGroupInformation group = UserGroupManager.getInstance().selectGroup(newGroup, null);
        Assumptions.assumeTrue(group != null);
        final ZonedDateTime t2 = UserManager.getInstance().updateUserGroup(information.id(), newGroup, null);
        if (!newGroupS) {
            Assertions.assertNull(t2);
            return;
        }
        Assertions.assertNotNull(t2);
        Assertions.assertEquals(new UserInformation(information.id(), information.username(), PasswordGuard.encryptPassword(""),
                        group, information.createTime(), t2, t2),
                UserManager.getInstance().selectUser(information.id(), null));

        if (newName != null) {
            final ZonedDateTime time = UserManager.getInstance().updateUserName(information.id(), newName, null);
            if (!newNameS) {
                Assertions.assertNull(time);
                return;
            }
            Assertions.assertNotNull(time);
            Assertions.assertEquals(new UserInformation(information.id(), newName, PasswordGuard.encryptPassword(""), group,
                            information.createTime(), time, t2), UserManager.getInstance().selectUser(information.id(), null));
        }

        Assertions.assertTrue(UserManager.getInstance().deleteUser(information.id(), null));
    }

    @Test
    public void curd_() throws SQLException {
        Assertions.assertNull(UserManager.getInstance().updateUserGroup(UserManager.getInstance().getAdminId(), UserGroupManager.getInstance().getAdminId(), null));
        Assertions.assertNotNull(UserManager.getInstance().updateUserGroup(Objects.requireNonNull(UserManager.getInstance().insertUser("t", "", null)).id(),
                UserGroupManager.getInstance().getAdminId(), null));
        Assertions.assertEquals(UserManager.getInstance().selectUser(UserManager.getInstance().getAdminId(), null), UserManager.getInstance().selectUserByName(IdentifierNames.UserName.Admin.getIdentifier(), null));
    }

    @RepeatedTest(value = 5, failureThreshold = 1)
    public void selectList() throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection ignore = UserManager.getInstance().getConnection(null, connectionId)) {
            final UserGroupInformation group = UserGroupManager.getInstance().insertGroup("test", connectionId.get());
            Assumptions.assumeTrue(group != null);
            Assertions.assertEquals(1, UserGroupManager.getInstance().getAdminId());
            Assertions.assertEquals(2, UserGroupManager.getInstance().getDefaultId());
            Assertions.assertEquals(3, group.id());
            final int count = 100 - 1;
            final Collection<UserInformation> informationList = new ArrayList<>(count + 1);
            informationList.add(UserManager.getInstance().selectUser(UserManager.getInstance().getAdminId(), connectionId.get()));
            for (int i = 0; i < count; i++) {
                final UserInformation information = UserManager.getInstance().insertUser("test " + i, "", connectionId.get());
                Assertions.assertNotNull(information);
                Assertions.assertNotNull(UserManager.getInstance().updateUserGroup(information.id(), HRandomHelper.DefaultSecureRandom.nextInt(1, 4), connectionId.get()));
                informationList.add(UserManager.getInstance().selectUser(information.id(), connectionId.get()));
            }
            final LinkedHashMap<VisibleUserInformation.Order, Options.OrderDirection> orders = new LinkedHashMap<>();
            orders.put(VisibleUserInformation.Order.Id, Options.OrderDirection.ASCEND);
            Assumptions.assumeTrue(UserManager.getInstance().selectUsers(orders, 0, 0, connectionId.get()).getFirst().longValue() == count + 1);
            Assertions.assertEquals(informationList, UserManager.getInstance().selectUsers(orders, 0, count + 1, connectionId.get()).getSecond());

            // Test limit and position.
            for (int i = 0; i < 5; ++i) {
                final int limit = HRandomHelper.DefaultSecureRandom.nextInt(1, count + 1);
                final int position = HRandomHelper.DefaultSecureRandom.nextInt(0, count + 1);
                Assertions.assertEquals(informationList.stream().limit(limit).collect(Collectors.toList()),
                        UserManager.getInstance().selectUsers(orders, 0, limit, connectionId.get()).getSecond());
                Assertions.assertEquals(informationList.stream().skip(position).limit(limit).collect(Collectors.toList()),
                        UserManager.getInstance().selectUsers(orders, position, limit, connectionId.get()).getSecond());
            }

            // Test order.
            orders.put(VisibleUserInformation.Order.Name, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserInformation.Order.CreateTime, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserInformation.Order.UpdateTime, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserInformation.Order.GroupId, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserInformation.Order.GroupName, Options.OrderDirection.ASCEND);
            Assertions.assertEquals(informationList, UserManager.getInstance().selectUsers(orders, 0, count + 1, connectionId.get()).getSecond());
            orders.clear();
            orders.put(VisibleUserInformation.Order.GroupId, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserInformation.Order.Id, Options.OrderDirection.DESCEND);
            Assertions.assertEquals(informationList.stream().sorted((a, b) -> {
                final int c1 = Comparator.comparingLong((UserInformation p) -> p.group().id()).compare(a, b);
                if (c1 != 0)
                    return c1;
                return Comparator.comparingLong(UserInformation::id).reversed().compare(a, b);
            }).collect(Collectors.toList()),
                    UserManager.getInstance().selectUsers(orders, 0, count + 1, connectionId.get()).getSecond());

            // By groups
            orders.clear();
            orders.put(VisibleUserInformation.Order.Id, Options.OrderDirection.ASCEND);
            for (int i = 0; i < 5; ++i) {
                final Set<Long> chooser = new HashSet<>();
                for (int k = 1; k < 4; ++k)
                    if (HRandomHelper.DefaultSecureRandom.nextBoolean())
                        chooser.add((long) k);
                final boolean blacklist = HRandomHelper.DefaultSecureRandom.nextBoolean();

                Assertions.assertEquals(informationList.stream().filter(p -> blacklist != chooser.contains(p.group().id())).collect(Collectors.toList()),
                        UserManager.getInstance().selectUsersByGroups(chooser, blacklist, orders, 0, count + 1, connectionId.get()).getSecond());
            }

            // Delete by group
            UserManager.getInstance().deleteUsersByGroup(3, connectionId.get());
            Assertions.assertEquals(informationList.stream().filter(p -> p.group().id() != 3).collect(Collectors.toList()),
                    UserManager.getInstance().selectUsers(orders, 0, count + 1, connectionId.get()).getSecond());

            UserManager.getInstance().deleteUsersByGroup(1, connectionId.get());
            Assertions.assertEquals(informationList.stream().filter(p -> p.group().id() == 2 || p.id() == UserManager.getInstance().getAdminId()).collect(Collectors.toList()),
                    UserManager.getInstance().selectUsers(orders, 0, count + 1, connectionId.get()).getSecond());
        }
    }

    @RepeatedTest(value = 5, failureThreshold = 1)
    public void searchList() throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection ignore = UserManager.getInstance().getConnection(null, connectionId)) {
            final int count = 100 - 1;
            final Collection<UserInformation> informationList = new ArrayList<>(count + 1);
            informationList.add(UserManager.getInstance().selectUser(UserManager.getInstance().getAdminId(), connectionId.get()));
            for (int i = 0; i < count; i++) {
                final UserInformation information = UserManager.getInstance().insertUser("test " + i, "", connectionId.get());
                Assertions.assertNotNull(information);
                informationList.add(information);
            }
            final LinkedHashMap<VisibleUserInformation.Order, Options.OrderDirection> orders = new LinkedHashMap<>();
            orders.put(VisibleUserInformation.Order.Id, Options.OrderDirection.ASCEND);
            Assumptions.assumeTrue(UserManager.getInstance().selectUsers(orders, 0, 0, connectionId.get()).getFirst().longValue() == count + 1);

            Assertions.assertEquals(informationList.stream().skip(1).collect(Collectors.toList()),
                    UserManager.getInstance().searchUsersByRegex("test [0-9]*", orders, 0, count + 1, connectionId.get()).getSecond());

            for (int i = 0; i < 10; ++i) {
                final String n = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, HRandomHelper.DefaultSecureRandom.nextInt(0, 3), "0123456789");
                Assertions.assertEquals(informationList.stream().filter(p -> p.username().contains(n)).collect(Collectors.toSet()),
                        new HashSet<>(UserManager.getInstance().searchUsersByNames(Set.of(n), 0, count + 1, connectionId.get()).getSecond()));
            }
        }
    }
}
