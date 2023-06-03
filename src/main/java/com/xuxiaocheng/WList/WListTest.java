package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;

import java.io.IOException;
import java.sql.SQLException;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException {
        GlobalConfiguration.initialize(null);
        ConstantManager.initialize();
        UserGroupManager.initialize();
        UserManager.initialize();
        UserManager.insertUser(new UserSqlInformation.Inserter("123", "123456", 3), null);
        final UserSqlInformation i = UserManager.selectUserByName("123", null);
        HLog.DefaultLogger.log("", i);

//        WListServer.CodecExecutors.shutdownGracefully().syncUninterruptibly();
//        WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
//        WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
//        if(true) return;
//        GlobalConfiguration.initialize(null);
//        DriverManager.add("Local Disk", WebDriversType.LocalDiskDriver);
//        final LocalDiskConfiguration configuration = new LocalDiskConfiguration();
//        final LocalDisk disk = new LocalDisk();
//        disk.initialize(configuration);
//        disk.buildCache();
//        LocalDiskManager.recursiveRefreshDirectory(configuration, new DrivePath(""), null);
    }

}
