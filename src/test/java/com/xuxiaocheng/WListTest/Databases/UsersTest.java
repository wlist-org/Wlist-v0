package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.WList.Commons.IdentifierNames;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
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
        ConstantManager.quicklyUninitializeReserveTable();
        final File file = new File(UsersTest.directory, "data.db");
        SqlDatabaseManager.quicklyClose(file);
    }

    public static Stream<Arguments> curd() throws SQLException {
        Assertions.assertFalse(UserManager.deleteUser(UserManager.getAdminId(), null));
        Assertions.assertEquals(PasswordGuard.encryptPassword(Objects.requireNonNull(UserManager.getAndDeleteDefaultAdminPasswordAPI())),
                Objects.requireNonNull(UserManager.selectUser(UserManager.getAdminId(), null)).encryptedPassword());
        return Stream.of( null,
                Arguments.of(IdentifierNames.UserName.Admin.getIdentifier(), false, 0, false, null, false),
                Arguments.of("test-admin", true, UserGroupManager.getAdminId(), true, "test", true),
                Arguments.of("test", true, UserGroupManager.getDefaultId(), true, "test2", true)
        ).skip(1);
    }

    @ParameterizedTest
    @MethodSource
    public void curd(final String name, final boolean nameS,
                     final long newGroup, final boolean newGroupS,
                     final String newName, final boolean newNameS)
            throws SQLException {

        final UserInformation information = UserManager.insertUser(name, "", null);
        if (!nameS) {
            Assertions.assertNull(information);
            return;
        }
        Assertions.assertNotNull(information);
        Assertions.assertEquals(information, UserManager.selectUser(information.id(), null));

        final LocalDateTime t = UserManager.updateUserPassword(information.id(), PasswordGuard.encryptPassword(""), null);
        Assertions.assertNotNull(t);
        Assertions.assertEquals(new UserInformation(information.id(), information.username(), PasswordGuard.encryptPassword(""), information.group(),
                        information.createTime(), t, t), UserManager.selectUser(information.id(), null));

        final UserGroupInformation group = UserGroupManager.selectGroup(newGroup, null);
        Assumptions.assumeTrue(group != null);
        final LocalDateTime t2 = UserManager.updateUserGroup(information.id(), newGroup, null);
        if (!newGroupS) {
            Assertions.assertNull(t2);
            return;
        }
        Assertions.assertNotNull(t2);
        Assertions.assertEquals(new UserInformation(information.id(), information.username(), PasswordGuard.encryptPassword(""),
                        group, information.createTime(), t2, t2),
                UserManager.selectUser(information.id(), null));

        if (newName != null) {
            final LocalDateTime time = UserManager.updateUserName(information.id(), newName, null);
            if (!newNameS) {
                Assertions.assertNull(time);
                return;
            }
            Assertions.assertNotNull(time);
            Assertions.assertEquals(new UserInformation(information.id(), newName, PasswordGuard.encryptPassword(""), group,
                            information.createTime(), time, t2), UserManager.selectUser(information.id(), null));
        }

        Assertions.assertTrue(UserManager.deleteUser(information.id(), null));
    }

    @Test
    public void curd_() throws SQLException {
        Assertions.assertNull(UserManager.updateUserGroup(UserManager.getAdminId(), UserGroupManager.getAdminId(), null));
        Assertions.assertNotNull(UserManager.updateUserGroup(Objects.requireNonNull(UserManager.insertUser("t", "", null)).id(),
                UserGroupManager.getAdminId(), null));
    }

}
