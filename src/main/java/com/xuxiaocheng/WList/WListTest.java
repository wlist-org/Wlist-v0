package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Databases.Constant.ConstantSqlHelper;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseHelper;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Databases.User.UserSqlHelper;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Databases.UserGroup.UserGroupSqlHelper;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class WListTest {
    private WListTest() {
        super();
    }

    private static void test(final @NotNull ByteBuf buf) {
        try {
            buf.writeBytes("tester".getBytes(StandardCharsets.UTF_8));
            final byte[] bytes = new byte[6];
            buf.nioBuffer().get(bytes);
            HLog.DefaultLogger.log(HLogLevel.INFO, "Success (", new String(bytes, StandardCharsets.UTF_8), "): ", buf.getClass().getName());
        } catch (final RuntimeException exception) {
            HLog.DefaultLogger.log(HLogLevel.ERROR, "Failure: ", buf.getClass().getName(), exception.getMessage());
        } finally {
            buf.release();
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String @NotNull [] args) throws Exception {
        WListTest.test(PooledByteBufAllocator.DEFAULT.heapBuffer());
        WListTest.test(PooledByteBufAllocator.DEFAULT.directBuffer());
        WListTest.test(UnpooledByteBufAllocator.DEFAULT.heapBuffer());
        WListTest.test(UnpooledByteBufAllocator.DEFAULT.directBuffer());
        WListTest.test(ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true,
                ByteBufAllocator.DEFAULT.buffer().writeBytes("aa".getBytes(StandardCharsets.UTF_8)),
                ByteBufAllocator.DEFAULT.buffer().writeBytes("bb".getBytes(StandardCharsets.UTF_8))));
        if (true) return;
        WListTest.wrapServerInitialize(() -> {
//            DriverManager_123pan.getFileInformation((DriverConfiguration_123Pan) driver.getConfiguration(), 2345490, null, null)
//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
        });
    }

    static {
        HUncaughtExceptionHelper.putIfAbsentUncaughtExceptionListener("listener", (t, e) -> HLog.DefaultLogger.log(HLogLevel.FAULT, "Uncaught exception listened by WListTester. thread: ", t.getName(), e));
        System.setProperty("io.netty.leakDetectionLevel", "ADVANCED");
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private static void wrapServerInitialize(final @NotNull SupplierE<@Nullable Object> runnable) throws Exception {
        GlobalConfiguration.initialize(new File("server.yaml"));
        PooledDatabase.quicklyInitialize(PooledDatabaseHelper.getDefault(new File("data.db")));
        ConstantManager.quicklyInitialize(new ConstantSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserGroupManager.quicklyInitialize(new UserGroupSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        UserManager.quicklyInitialize(new UserSqlHelper(PooledDatabase.instance.getInstance()), "initialize");
        DriverManager.initialize(new File("configs"));
        try {
            final Map<String, Exception> failures = DriverManager.getFailedDriversAPI();
            if (!failures.isEmpty()) {
                HLog.DefaultLogger.log(HLogLevel.ERROR, failures);
                return;
            }
            final Object obj = runnable.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            for (final Map.Entry<String, Exception> exception: DriverManager.operateAllDrivers(d -> DriverManager.dumpConfigurationIfModified(d.getConfiguration())).entrySet())
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump driver configuration.", ParametersMap.create().add("name", exception.getKey()), exception.getValue());
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
        }
    }


    private static void wrapServerInitialize(final @NotNull RunnableE runnable) throws Exception {
        WListTest.wrapServerInitialize(() -> {
            runnable.run();
            return null;
        });
    }
}
