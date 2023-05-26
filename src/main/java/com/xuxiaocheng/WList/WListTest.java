package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDisk;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskManager;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws Exception {
        final EventExecutorGroup executor = new DefaultEventExecutorGroup(8);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {executor.shutdownGracefully();HLog.DefaultLogger.log("", t);HLog.DefaultLogger.log("", e);});
        final Triad.ImmutableTriad<Long, Iterator<FileSqlInformation>, Runnable> lister = DriverUtil.wrapAllFilesListerInPages(page -> {
                    if (page > 5)
                        TimeUnit.SECONDS.sleep(3);
//                    if (page == 17)
//                        throw new SQLException();
                    return Pair.ImmutablePair.makeImmutablePair(20L, List.of(new FileSqlInformation(page, null, true, 0, null, null, null, null)));
                }, Long::intValue, e -> HLog.DefaultLogger.log("", e), executor);
        HLog.DefaultLogger.log("", lister);
        int all = 0;
        for (;lister.getB().hasNext();) {
            final FileSqlInformation info = lister.getB().next();
            HLog.DefaultLogger.log("VERBOSE", info);
            ++all;
//            if (info.id() == 12)
//                lister.getC().run();
        }
        HLog.DefaultLogger.log("", all);
        executor.shutdownGracefully();
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
