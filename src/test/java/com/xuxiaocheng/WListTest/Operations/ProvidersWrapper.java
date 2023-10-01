package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WListTest.Storage.AbstractProviderTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.net.SocketAddress;
import java.util.Objects;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class ProvidersWrapper extends ServerWrapper {
    @BeforeAll
    public static void initialize() throws Exception {
        StaticLoader.load();
        ServerConfiguration.parseFromFile();
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(new File("run/data.db"));
        ConstantManager.quicklyInitialize(database, "initialize");
        UserGroupManager.quicklyInitialize(database, "initialize");
        UserManager.quicklyInitialize(database, "initialize");
        StorageManager.initialize(new File("run/configs"), new File("run/caches"));
        WListServer.getInstance().start(ServerConfiguration.get().port());
        final SocketAddress address = WListServer.getInstance().getAddress().getInstance();
//        ServerWrapper.AdminPassword.initialize(Objects.requireNonNull(UserManager.getInstance().getAndDeleteDefaultAdminPassword()));
        ServerWrapper.AdminPassword.initialize("Slz12ApN"); // random, has been generated.

        ClientConfiguration.parseFromFile();
        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
        ServerWrapper.address.initialize(address);
        final String token;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            token = OperateSelfHelper.login(client, ServerWrapper.AdminUsername.getInstance(), ServerWrapper.AdminPassword.getInstance());
        }
        Assumptions.assumeTrue(token != null);
        ServerWrapper.AdminToken.initialize(token);
    }

    @AfterAll
    public static void uninitialize() throws Exception {
        AbstractProviderTest.check();
        ServerWrapper.uninitialize();
    }

    @Override
    public @NotNull String token() {
        return super.adminToken();
    }

    public long root() {
        return Objects.requireNonNull(StorageManager.getProvider("test")).getConfiguration().getRootDirectoryId();
    }

    public @NotNull FileLocation location(final long id) {
        return new FileLocation("test", id);
    }
}
