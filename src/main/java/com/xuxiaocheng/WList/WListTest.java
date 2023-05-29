package com.xuxiaocheng.WList;

import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDisk;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskManager;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;

import javax.crypto.Cipher;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws Exception {
//        Security.addProvider(new BouncyCastleProvider());
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding");
//        WListServer.CodecExecutors.shutdownGracefully().syncUninterruptibly();
//        WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
//        WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
        if(true)return;
        GlobalConfiguration.init(null);
        DriverManager.add("Local Disk", WebDriversType.LocalDiskDriver);
        final LocalDiskConfiguration configuration = new LocalDiskConfiguration();
        final LocalDisk disk = new LocalDisk();
        disk.initialize(configuration);
        disk.buildCache();
        LocalDiskManager.recursiveRefreshDirectory(configuration, new DrivePath(""), null);
    }

}
