package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClient;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.PasswordGuard;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerWrapper {
    @TempDir
    public static File runtimeDirectory;

    public static final @NotNull HInitializer<SocketAddress> address = new HInitializer<>("address");
    @BeforeAll
    public static void initialize() throws IOException, SQLException, InterruptedException {
        StaticLoader.load();
        ServerConfiguration.parseFromFile();
        final File file = new File(ServerWrapper.runtimeDirectory, "data.db");
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
        ConstantManager.quicklyInitialize(database, "initialize");
        UserGroupManager.quicklyInitialize(database, "initialize");
        UserManager.quicklyInitialize(database, "initialize");
        StorageManager.initialize(new File(ServerWrapper.runtimeDirectory, "configs"),
                new File(ServerWrapper.runtimeDirectory, "caches"));
        WListServer.getInstance().start(ServerConfiguration.get().port());
        final SocketAddress address = WListServer.getInstance().getAddress().getInstance();
        ServerWrapper.AdminPassword.initialize(Objects.requireNonNull(UserManager.getInstance().getAndDeleteDefaultAdminPassword()));
        ServerWrapper.Password.initialize(PasswordGuard.generateRandomPassword());

        ClientConfiguration.initialize(null);
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        ServerWrapper.address.initialize(address);
    }
    @AfterAll
    public static void uninitialize() throws SQLException {
        WListClientManager.quicklyUninitialize(ServerWrapper.address.uninitialize());

        for (final File file: new HashSet<>(SqlDatabaseManager.getOpenedDatabases()))
            SqlDatabaseManager.quicklyClose(file);
        WListServer.CodecExecutors.shutdownGracefully();
        WListServer.ServerExecutors.shutdownGracefully();
        WListServer.IOExecutors.shutdownGracefully();
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        HttpNetworkHelper.CountDownExecutors.shutdownGracefully();
    }

    public static @NotNull Stream<WListClientInterface> client() throws IOException, InterruptedException {
        return Stream.of(WListClientManager.quicklyGetClient(ServerWrapper.address.getInstance()));
    }
    public static @NotNull Stream<Arguments> broadcast() throws IOException, InterruptedException, WrongStateException {
        final WListClient broadcast = new WListClient(ServerWrapper.address.getInstance());
        broadcast.open();
        OperateServerHelper.setBroadcastMode(broadcast, true);
        return Stream.of(
                Arguments.of(WListClientManager.quicklyGetClient(ServerWrapper.address.getInstance()), broadcast)
        );
    }

    protected static final @NotNull HInitializer<String> Username = new HInitializer<>("Username", "operate-tester");
    protected static final @NotNull HInitializer<String> Password = new HInitializer<>("Password");
    protected static final @NotNull HInitializer<String> Token = new HInitializer<>("Token");
    public @NotNull String username() {
        return ServerWrapper.Username.getInstance();
    }
    public void username(final @NotNull String username) {
        ServerWrapper.Username.reinitialize(username);
    }
    public @NotNull String password() {
        return ServerWrapper.Password.getInstance();
    }
    public void password(final @NotNull String password) {
        ServerWrapper.Password.reinitialize(password);
    }
    public @NotNull String token() {
        return ServerWrapper.Token.getInstance();
    }
    public void token(final @NotNull String token) {
        ServerWrapper.Token.reinitialize(token);
    }

    protected static final @NotNull HInitializer<String> AdminUsername = new HInitializer<>("AdminUsername", IdentifierNames.UserName.Admin.getIdentifier());
    protected static final @NotNull HInitializer<String> AdminPassword = new HInitializer<>("AdminPassword");
    protected static final @NotNull HInitializer<String> AdminToken = new HInitializer<>("AdminToken");
    public @NotNull String adminUsername() {
        return ServerWrapper.AdminUsername.getInstance();
    }
    public void adminUsername(final @NotNull String adminUsername) {
        ServerWrapper.AdminUsername.reinitialize(adminUsername);
    }
    public @NotNull String adminPassword() {
        return ServerWrapper.AdminPassword.getInstance();
    }
    public void adminPassword(final @NotNull String adminPassword) {
        ServerWrapper.AdminPassword.reinitialize(adminPassword);
    }
    public @NotNull String adminToken() {
        return ServerWrapper.AdminToken.getInstance();
    }
    public void adminToken(final @NotNull String token) {
        ServerWrapper.AdminToken.reinitialize(token);
    }
}
