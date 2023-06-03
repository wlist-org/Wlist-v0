package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException {
        GlobalConfiguration.initialize(null);
        UserGroupManager.initialize(DatabaseUtil.getInstance());
        Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull UserGroupSqlInformation>> p =
                UserGroupManager.selectAllUserGroupsInPage(20, 0, Options.OrderDirection.ASCEND, null);
        HLog.DefaultLogger.log("", p);

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
