package com.xuxiaocheng.WList;

import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupSqlInformation;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException {
        GlobalConfiguration.initialize(null);
        UserGroupManager.initialize();
        final AtomicReference<String> id1 = new AtomicReference<>();
        try (final Connection connection1 = DatabaseUtil.getInstance().getNewConnection(id1::set)){
            connection1.setAutoCommit(false);
            UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter("a", Operation.emptyPermissions()), id1.get());
            final AtomicReference<String> id2 = new AtomicReference<>();
            try (final Connection connection2 = DatabaseUtil.getInstance().getNewConnection(id2::set)) {
                connection2.setAutoCommit(false);
                UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter("b", Operation.allPermissions()), id1.get());
                connection2.commit();
            }
            UserGroupManager.insertGroup(new UserGroupSqlInformation.Inserter("b", Operation.emptyPermissions()), id1.get());
            connection1.commit();
        }
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
