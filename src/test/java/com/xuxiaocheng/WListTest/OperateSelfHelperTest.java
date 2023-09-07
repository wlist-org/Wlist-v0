package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.GlobalConfiguration;
import com.xuxiaocheng.WList.Client.OperationHelpers.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperateSelfHelperTest {
    private static final @NotNull HInitializer<SocketAddress> address = new HInitializer<>("address");

    @BeforeAll
    public static void initialize() throws IOException {
        GlobalConfiguration.initialize(null);
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 5212);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        OperateSelfHelperTest.address.initialize(address);
    }

    @AfterAll
    public static void uninitialize() {
        WListClientManager.quicklyUninitialize(OperateSelfHelperTest.address.getInstance());
    }

    private final @NotNull HInitializer<WListClientInterface> client = new HInitializer<>("Client");

    @BeforeEach
    public void borrow_() throws IOException, InterruptedException {
        this.client.initialize(WListClientManager.quicklyGetClient(OperateSelfHelperTest.address.getInstance()));
    }

    @AfterEach
    public void return_() {
        Objects.requireNonNull(this.client.uninitialize()).close();
        //noinspection UseOfSystemOutOrSystemErr
        System.out.print('\n');
    }

    private static final @NotNull HInitializer<String> token = new HInitializer<>("Token");
    private static final @NotNull HInitializer<String> username = new HInitializer<>("Username", "tester-self");
    private static final @NotNull HInitializer<String> password = new HInitializer<>("Password", "123456");

    @Test
    @Order(1)
    public void logon() throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateSelfHelper.logon(this.client.getInstance(), OperateSelfHelperTest.username.getInstance(), OperateSelfHelperTest.password.getInstance()));
    }

    @Test
    @Order(2)
    public void login() throws WrongStateException, IOException, InterruptedException {
        final String token = OperateSelfHelper.login(this.client.getInstance(), OperateSelfHelperTest.username.getInstance(), OperateSelfHelperTest.password.getInstance());
        Assertions.assertNotNull(token);
        OperateSelfHelperTest.token.reinitialize(token);
    }

    @Test
    @Order(5)
    public void logoff() throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateSelfHelper.logoff(this.client.getInstance(), OperateSelfHelperTest.token.getInstance(), OperateSelfHelperTest.password.getInstance()));
    }

    @Test
    @Order(4)
    public void changeUsername() throws WrongStateException, IOException, InterruptedException {
        final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changeUsername(this.client.getInstance(), OperateSelfHelperTest.token.getInstance(), username));
        OperateSelfHelperTest.username.reinitialize(username);
    }

    @Test
    @Order(3)
    public void changePassword() throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changePassword(this.client.getInstance(), OperateSelfHelperTest.token.getInstance(), OperateSelfHelperTest.password.getInstance(), password));
        OperateSelfHelperTest.password.reinitialize(password);
        // refresh token
        this.login();
    }

    @Test
    @Order(4)
    public void getPermissions() {
    }
}
