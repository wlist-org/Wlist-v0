package com.xuxiaocheng.WListTest.OperateTest;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.OperationHelpers.OperateSelfHelper;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperateSelfTest extends ServerWrapper {
    protected static final @NotNull HInitializer<String> username = new HInitializer<>("Username", "tester-self");
    protected static final @NotNull HInitializer<String> password = new HInitializer<>("Password", "");
    protected static final @NotNull HInitializer<String> token = new HInitializer<>("Token");

    @Test
    @Order(1)
    public void logon() throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateSelfHelper.logon(this.client.getInstance(), OperateSelfTest.username.getInstance(), OperateSelfTest.password.getInstance()));
    }

    @Test
    @Order(2)
    public void login() throws WrongStateException, IOException, InterruptedException {
        final String token = OperateSelfHelper.login(this.client.getInstance(), OperateSelfTest.username.getInstance(), OperateSelfTest.password.getInstance());
        Assumptions.assumeTrue(token != null);
        OperateSelfTest.token.reinitialize(token);
    }

    @Test
    @Order(5)
    public void logoff() throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateSelfHelper.logoff(this.client.getInstance(), OperateSelfTest.token.getInstance(), OperateSelfTest.password.getInstance()));
    }

    @Test
    @Order(4)
    public void changeUsername() throws WrongStateException, IOException, InterruptedException {
        final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changeUsername(this.client.getInstance(), OperateSelfTest.token.getInstance(), username));
        OperateSelfTest.username.reinitialize(username);
    }

    @Test
    @Order(3)
    public void changePassword() throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changePassword(this.client.getInstance(), OperateSelfTest.token.getInstance(), OperateSelfTest.password.getInstance(), password));
        OperateSelfTest.password.reinitialize(password);
        // refresh token
        this.login();
    }

    @Test
    @Order(4)
    public void getGroup() throws WrongStateException, IOException, InterruptedException {
        final VisibleUserGroupInformation information = OperateSelfHelper.getGroup(this.client.getInstance(), OperateSelfTest.token.getInstance());
        Assertions.assertNotNull(information);
        Assertions.assertEquals(Operation.defaultPermissions(), information.permissions());
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @SuppressWarnings("InnerClassFieldHidesOuterClassField")
    public class OperateSelfAdminTest {
        protected static final @NotNull HInitializer<String> password = new HInitializer<>("Password", "");
        protected static final @NotNull HInitializer<String> token = new HInitializer<>("Token");

        @Test
        @Order(1)
        public void _logon() throws WrongStateException, IOException, InterruptedException {
            final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
            Assertions.assertFalse(OperateSelfHelper.logon(OperateSelfTest.this.client.getInstance(), UserManager.ADMIN, password));
        }

        @Test
        @Order(1)
        public void login() throws WrongStateException, IOException, InterruptedException {
            final String token = OperateSelfHelper.login(OperateSelfTest.this.client.getInstance(), UserManager.ADMIN, OperateSelfAdminTest.password.getInstance());
            Assumptions.assumeTrue(token != null);
            OperateSelfAdminTest.token.reinitialize(token);
        }

        @Test
        @Order(2)
        public void _logoff() throws WrongStateException, IOException, InterruptedException {
            Assertions.assertFalse(OperateSelfHelper.logoff(OperateSelfTest.this.client.getInstance(), OperateSelfAdminTest.token.getInstance(), OperateSelfAdminTest.password.getInstance()));
        }

        @Test
        @Order(2)
        public void _changeUsername() throws WrongStateException, IOException, InterruptedException {
            final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
            Assertions.assertFalse(OperateSelfHelper.changeUsername(OperateSelfTest.this.client.getInstance(), OperateSelfAdminTest.token.getInstance(), username));
        }

        @Test
        @Order(3)
        public void changePassword() throws WrongStateException, IOException, InterruptedException {
            final String old = OperateSelfAdminTest.password.getInstance();
            final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
            Assertions.assertTrue(OperateSelfHelper.changePassword(OperateSelfTest.this.client.getInstance(), OperateSelfAdminTest.token.getInstance(), old, password));
            OperateSelfAdminTest.password.reinitialize(password);
            // refresh token
            this.login();

            Assertions.assertTrue(OperateSelfHelper.changePassword(OperateSelfTest.this.client.getInstance(), OperateSelfAdminTest.token.getInstance(), password, old));
            OperateSelfAdminTest.password.reinitialize(old);
        }

        @Test
        @Order(2)
        public void getGroup() throws WrongStateException, IOException, InterruptedException {
            final VisibleUserGroupInformation information = OperateSelfHelper.getGroup(OperateSelfTest.this.client.getInstance(), OperateSelfAdminTest.token.getInstance());
            Assertions.assertNotNull(information);
            Assertions.assertEquals(Operation.allPermissions(), information.permissions());
        }
    }
}
