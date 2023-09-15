package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Execution(ExecutionMode.SAME_THREAD)
public final class UserGroupsTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() throws SQLException {
        StaticLoader.load();
        final File file = new File(UserGroupsTest.directory, "data.db");
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
        UserGroupManager.quicklyInitialize(database, null);
    }

    @AfterAll
    public static void uninitialize() throws SQLException {
        UserGroupManager.quicklyUninitialize(null);
        final File file = new File(UserGroupsTest.directory, "data.db");
        SqlDatabaseManager.quicklyClose(file);
    }

    public static Stream<Arguments> curd() throws SQLException {
        Assertions.assertFalse(UserGroupManager.getInstance().deleteGroup(UserGroupManager.getInstance().getAdminId(), null));
        Assertions.assertFalse(UserGroupManager.getInstance().deleteGroup(UserGroupManager.getInstance().getDefaultId(), null));
        return Stream.of( null,
                Arguments.of(IdentifierNames.UserGroupName.Admin.getIdentifier(), false, null, false, null),
                Arguments.of(IdentifierNames.UserGroupName.Default.getIdentifier(), false, null, false, null),
                Arguments.of("test-admin", true, IdentifierNames.UserGroupName.Admin.getIdentifier(), false, null),
                Arguments.of("test-default", true, IdentifierNames.UserGroupName.Default.getIdentifier(), false, null),
                Arguments.of("test-test", true, "test", true, UserPermission.All),
                Arguments.of("test-2", true, null, true, UserPermission.Default),
                Arguments.of("test-3", true, null, true, UserPermission.Empty)
        ).skip(1);
    }

    @ParameterizedTest
    @MethodSource
    public void curd(final String name, final boolean nameS,
                     final String newName, final boolean newNameS,
                     final Set<UserPermission> newPermissions)
            throws SQLException {

        final UserGroupInformation information = UserGroupManager.getInstance().insertGroup(name, null);
        if (!nameS) {
            Assertions.assertNull(information);
            return;
        }
        Assertions.assertNotNull(information);
        Assertions.assertEquals(information, UserGroupManager.getInstance().selectGroup(information.id(), null));

        if (newName != null) {
            final ZonedDateTime time = UserGroupManager.getInstance().updateGroupName(information.id(), newName, null);
            if (!newNameS) {
                Assertions.assertNull(time);
                return;
            }
            Assertions.assertNotNull(time);
            Assertions.assertEquals(new UserGroupInformation(information.id(), newName, information.permissions(), information.createTime(), time),
                    UserGroupManager.getInstance().selectGroup(information.id(), null));
        }

        if (newPermissions != null) {
            final ZonedDateTime time = UserGroupManager.getInstance().updateGroupPermission(information.id(), newPermissions, null);
            Assertions.assertNotNull(time);
            Assertions.assertEquals(new UserGroupInformation(information.id(), Objects.requireNonNullElse(newName, name), newPermissions, information.createTime(), time),
                    UserGroupManager.getInstance().selectGroup(information.id(), null));
        }

        Assertions.assertTrue(UserGroupManager.getInstance().deleteGroup(information.id(), null));
    }

    @Test
    public void curd_() throws SQLException {
        Assertions.assertNull(UserGroupManager.getInstance().updateGroupPermission(UserGroupManager.getInstance().getAdminId(), UserPermission.Empty, null));
        Assertions.assertNotNull(UserGroupManager.getInstance().updateGroupPermission(UserGroupManager.getInstance().getDefaultId(), UserPermission.Empty, null));
    }

    @RepeatedTest(value = 5, failureThreshold = 1)
    public void selectList() throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection ignore = UserGroupManager.getInstance().getConnection(null, connectionId)) {
            final int count = 100 - 2;
            final Collection<UserGroupInformation> informationList = new ArrayList<>(count + 2);
            informationList.add(UserGroupManager.getInstance().selectGroup(UserGroupManager.getInstance().getAdminId(), connectionId.get()));
            informationList.add(UserGroupManager.getInstance().selectGroup(UserGroupManager.getInstance().getDefaultId(), connectionId.get()));
            for (int i = 0; i < count; i++) {
                final UserGroupInformation information = UserGroupManager.getInstance().insertGroup("test " + i, connectionId.get());
                Assertions.assertNotNull(information);
                final Set<UserPermission> permissions = EnumSet.noneOf(UserPermission.class);
                for (final UserPermission permission: UserPermission.All)
                    if (HRandomHelper.DefaultSecureRandom.nextBoolean())
                        permissions.add(permission);
                UserGroupManager.getInstance().updateGroupPermission(information.id(), permissions, connectionId.get());
                informationList.add(UserGroupManager.getInstance().selectGroup(information.id(), connectionId.get()));
            }
            final LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection> orders = new LinkedHashMap<>();
            orders.put(VisibleUserGroupInformation.Order.Id, Options.OrderDirection.ASCEND);
            Assumptions.assumeTrue(UserGroupManager.getInstance().selectGroups(orders, 0, 0, connectionId.get()).getFirst().longValue() == count + 2);
            Assertions.assertEquals(informationList, UserGroupManager.getInstance().selectGroups(orders, 0, count + 2, connectionId.get()).getSecond());

            // Test limit and position.
            for (int i = 0; i < 5; ++i) {
                final int limit = HRandomHelper.DefaultSecureRandom.nextInt(1, count + 2);
                final int position = HRandomHelper.DefaultSecureRandom.nextInt(0, count + 2);
                Assertions.assertEquals(informationList.stream().limit(limit).collect(Collectors.toList()),
                        UserGroupManager.getInstance().selectGroups(orders, 0, limit, connectionId.get()).getSecond());
                Assertions.assertEquals(informationList.stream().skip(position).limit(limit).collect(Collectors.toList()),
                        UserGroupManager.getInstance().selectGroups(orders, position, limit, connectionId.get()).getSecond());
            }

            // Test order.
            orders.put(VisibleUserGroupInformation.Order.Name, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserGroupInformation.Order.CreateTime, Options.OrderDirection.ASCEND);
            orders.put(VisibleUserGroupInformation.Order.UpdateTime, Options.OrderDirection.ASCEND);
            Assertions.assertEquals(informationList, UserGroupManager.getInstance().selectGroups(orders, 0, count + 2, connectionId.get()).getSecond());
            for (int i = 0; i < 5; ++i) {
                orders.clear();
                final long seed = HRandomHelper.DefaultSecureRandom.nextLong();
                final RandomGenerator random = new Random(seed);
                for (final UserPermission permission: UserPermission.All)
                    if (random.nextBoolean())
                        orders.put(VisibleUserGroupInformation.Order.valueOf("Permissions_" + permission.name()),
                                random.nextBoolean() ? Options.OrderDirection.ASCEND : Options.OrderDirection.DESCEND);
                orders.put(VisibleUserGroupInformation.Order.Id, random.nextBoolean() ? Options.OrderDirection.ASCEND : Options.OrderDirection.DESCEND);
                final Comparator<UserGroupInformation> id = Comparator.comparingLong(UserGroupInformation::id);
                Assertions.assertEquals(informationList.stream().sorted((a, b) -> {
                    final RandomGenerator r = new Random(seed);
                    for (final UserPermission permission: UserPermission.All)
                        if (r.nextBoolean()) {
                            final boolean a1 = a.permissions().contains(permission);
                            final boolean b1 = b.permissions().contains(permission);
                            final boolean asc = r.nextBoolean();
                            if (a1 != b1)
                                return (asc ? a1 : b1) ? 1 : -1;
                        }
                    return (r.nextBoolean() ? id : id.reversed()).compare(a, b);
                }).collect(Collectors.toList()),
                        UserGroupManager.getInstance().selectGroups(orders, 0, count + 2, connectionId.get()).getSecond());
            }

            // By permissions
            orders.clear();
            orders.put(VisibleUserGroupInformation.Order.Id, Options.OrderDirection.ASCEND);
            for (int i = 0; i < 5; ++i) {
                final Map<UserPermission, Boolean> chooser = new EnumMap<>(UserPermission.class);
                final long seed = HRandomHelper.DefaultSecureRandom.nextLong();
                final RandomGenerator random = new Random(seed);
                for (final UserPermission permission: UserPermission.All)
                    if (random.nextBoolean())
                        chooser.put(permission, random.nextBoolean());
                Assertions.assertEquals(informationList.stream().filter(p -> {
                    final RandomGenerator r = new Random(seed);
                    for (final UserPermission permission: UserPermission.All)
                        if (r.nextBoolean())
                            if (r.nextBoolean() != p.permissions().contains(permission))
                                return false;
                    return true;
                }).collect(Collectors.toList()),
                        UserGroupManager.getInstance().selectGroupsByPermissions(chooser, orders, 0, count + 2, connectionId.get()).getSecond());
            }
        }
    }

    @RepeatedTest(value = 5, failureThreshold = 1)
    public void searchList() throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection ignore = UserGroupManager.getInstance().getConnection(null, connectionId)) {
            final int count = 100 - 2;
            final Collection<UserGroupInformation> informationList = new ArrayList<>(count + 2);
            informationList.add(UserGroupManager.getInstance().selectGroup(UserGroupManager.getInstance().getAdminId(), connectionId.get()));
            informationList.add(UserGroupManager.getInstance().selectGroup(UserGroupManager.getInstance().getDefaultId(), connectionId.get()));
            for (int i = 0; i < count; i++) {
                final UserGroupInformation information = UserGroupManager.getInstance().insertGroup("test " + i, connectionId.get());
                Assertions.assertNotNull(information);
                final Set<UserPermission> permissions = EnumSet.noneOf(UserPermission.class);
                for (final UserPermission permission: UserPermission.All)
                    if (HRandomHelper.DefaultSecureRandom.nextBoolean())
                        permissions.add(permission);
                Assertions.assertNotNull(UserGroupManager.getInstance().updateGroupPermission(information.id(), permissions, connectionId.get()));
                informationList.add(UserGroupManager.getInstance().selectGroup(information.id(), connectionId.get()));
            }
            final LinkedHashMap<VisibleUserGroupInformation.Order, Options.OrderDirection> orders = new LinkedHashMap<>();
            orders.put(VisibleUserGroupInformation.Order.Id, Options.OrderDirection.ASCEND);
            Assumptions.assumeTrue(UserGroupManager.getInstance().selectGroups(orders, 0, 0, connectionId.get()).getFirst().longValue() == count + 2);

            Assertions.assertEquals(informationList.stream().skip(2).collect(Collectors.toList()),
                    UserGroupManager.getInstance().searchGroupsByRegex("test [0-9]*", orders, 0, count + 2, connectionId.get()).getSecond());

            for (int i = 0; i < 10; ++i) {
                final String n = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, HRandomHelper.DefaultSecureRandom.nextInt(0, 3), "0123456789");
                Assertions.assertEquals(informationList.stream().filter(p -> p.name().contains(n)).collect(Collectors.toSet()),
                        new HashSet<>(UserGroupManager.getInstance().searchGroupsByNames(Set.of(n), 0, count + 2, connectionId.get()).getSecond()));
            }
        }
    }
}
