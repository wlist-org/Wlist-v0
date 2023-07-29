package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
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
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Server.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class WListTest {
    private WListTest() {
        super();
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public static void main(final String @NotNull [] args) throws Exception {
//        if (true) return;
        WListTest.wrapServerInitialize(() -> {
            HLog.DefaultLogger.log("", DriverManager.getFailedDriversAPI());
            final DriverInterface<?> driver = Objects.requireNonNull(DriverManager.getDriver("123pan_136"));
            HLog.DefaultLogger.log("",
                    driver.info(new FileLocation("123pan_136", 0)),
//                    driver.list(new FileLocation("123pan_136", 0), 20, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND),
            "");

//            final ByteBuf content = ByteBufAllocator.DEFAULT.heapBuffer();
//            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream("C:\\Users\\27622\\Desktop\\WList - bugs.txt"))) {
//                try (final OutputStream outputStream = new ByteBufOutputStream(content)) {
//                    inputStream.transferTo(outputStream);
//                }
//            }
//            // size: 2493
//            final UnionPair<UploadMethods, FailureReason> upload = RootDriver.getInstance().upload(new FileLocation("123pan_136", 0), "WList - bugs.txt",
//                    content.readableBytes(), MiscellaneousUtil.getMd5(ByteBufIOUtil.allToByteArray(content)), Options.DuplicatePolicy.ERROR);
//            if (upload.isFailure())
//                return upload.getE();
//            assert upload.getT().methods().size() == 1;
//            upload.getT().methods().get(0).accept(content);
//            final FileSqlInformation information = upload.getT().supplier().get();
//            upload.getT().finisher().run();
//            return information;

            final UnionPair<DownloadMethods, FailureReason> download = RootDriver.getInstance().download(new FileLocation("123pan_136", 2414972), 0, 3000);
            if (download.isFailure())
                return download.getE();
            assert download.getT().methods().size() == 1;
            final ByteBuf content = download.getT().methods().get(0).get();
            return MiscellaneousUtil.getMd5(ByteBufIOUtil.allToByteArray(content));
            // a3ef4d507bef27b30319de7b029bc2b1 // 32e85c1c24968ca7f0f4a46e11403524

//            DriverManager_123pan.getFileInformation((DriverConfiguration_123Pan) driver.getConfiguration(), 2345490, null, null)
//            TrashedFileManager.initialize(configuration.getName());
//            return TrashManager_123pan.restoreFile(configuration, 2293734, true, null, null);
//            return null;
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
}
