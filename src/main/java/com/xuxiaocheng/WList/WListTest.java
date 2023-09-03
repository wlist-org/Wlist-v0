package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class WListTest {
    private WListTest() {
        super();
    }

    private static final boolean initializeServer = false;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {
        final Pair.ImmutablePair<NetworkTransmission.ClientRsaPrivateKey, ByteBuf> request = NetworkTransmission.clientStart();
        final Pair.ImmutablePair<ByteBuf, NetworkTransmission.AesKeyPair> response = NetworkTransmission.serverStart(request.getSecond(), "WList");
        final UnionPair<NetworkTransmission.AesKeyPair, UnionPair<String, String>> check = NetworkTransmission.clientCheck(request.getFirst(), response.getFirst(), "WList");
        final NetworkTransmission.AesKeyPair client = Objects.requireNonNull(check).getT(), server = Objects.requireNonNull(response.getSecond());
        for (int i = 0; i < 10000; ++i) {
            final ByteBuf c1 = ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from client): " + i).getBytes(StandardCharsets.UTF_8));
            final ByteBuf c2 = Objects.requireNonNull(NetworkTransmission.clientEncrypt(client, c1));
            final ByteBuf c3 = Objects.requireNonNull(NetworkTransmission.serverDecrypt(server, c2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(c1), ByteBufIOUtil.allToByteArray(c3))) throw new AssertionError();
            c1.release(); c2.release(); c3.release();

            final ByteBuf s1 = ByteBufAllocator.DEFAULT.buffer().writeBytes(("Wlist test. (from server): " + i).getBytes(StandardCharsets.UTF_8));
            final ByteBuf s2 = Objects.requireNonNull(NetworkTransmission.serverEncrypt(server, s1));
            final ByteBuf s3 = Objects.requireNonNull(NetworkTransmission.clientDecrypt(client, s2));
            if (!Arrays.equals(ByteBufIOUtil.allToByteArray(s1), ByteBufIOUtil.allToByteArray(s3))) throw new AssertionError();
            s1.release(); s2.release(); s3.release();
        }
        return null;
    };

    static {
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static @Nullable Object wrapServerInitialize() throws Exception {
        GlobalConfiguration.initialize(new File("server.yaml"));
//        GlobalConfiguration.initialize(null);
        PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
        ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        DriverManager.initialize(new File("configs"));
        try {
            final Map<String, Exception> failures = DriverManager.getFailedDriversAPI();
            if (!failures.isEmpty()) {
                HLog.DefaultLogger.log(HLogLevel.FAULT, failures);
                return null;
            }
            return WListTest._main.get();
        } finally {
            for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
        }
    }

    public static void main(final String @NotNull [] args) {
        HLog.DefaultLogger.log(HLogLevel.INFO, "Running on pid: ", ProcessHandle.current().pid());
        try {
            final Object obj = WListTest.initializeServer ? WListTest.wrapServerInitialize() : WListTest._main.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        } finally {
            HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            DriverNetworkHelper.CountDownExecutors.shutdownGracefully();
        }
    }
}
