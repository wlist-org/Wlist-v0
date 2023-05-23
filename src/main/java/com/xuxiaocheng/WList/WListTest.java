package com.xuxiaocheng.WList;

import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDisk;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskManager;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;

import java.net.InetSocketAddress;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws Exception {
        GlobalConfiguration.init(null);
        WListServer.init(new InetSocketAddress(5212));
        final WListServer server = WListServer.getInstance();
        server.start();
        server.awaitStop();
        if(true)return;
        GlobalConfiguration.init(null);
        DriverManager.add("Local Disk", WebDriversType.LocalDiskDriver);
        final LocalDiskConfiguration configuration = new LocalDiskConfiguration();
        final LocalDisk disk = new LocalDisk();
        disk.initiate(configuration);
        disk.buildCache();
        LocalDiskManager.recursiveRefreshDirectory(configuration, new DrivePath(""), null);
    }

}
