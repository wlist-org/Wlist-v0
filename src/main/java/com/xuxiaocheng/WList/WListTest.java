package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.ServerHandlers.AesCipher;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDisk;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskConfiguration;
import com.xuxiaocheng.WList.WebDrivers.LocalDisk.LocalDiskManager;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws SQLException, IOException, IllegalParametersException {
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(buf, AesCipher.doAes);
        ByteBufIOUtil.writeByteArray(buf, "Hello aes and gzip!".getBytes(StandardCharsets.UTF_8));
        HLog.DefaultLogger.log("LESS", ByteBufIOUtil.allToByteArray(buf));
        final AesCipher cipher = new AesCipher(WList.key, WList.vector, WListServer.MaxSizePerPacket);
        final List<Object> list = new ArrayList<>();
        cipher.encode(null, buf, list);
        final ByteBuf msg = (ByteBuf) list.get(0);
        HLog.DefaultLogger.log("DEBUG", ByteBufIOUtil.allToByteArray(msg));
        cipher.decode(null, msg, list);
        HLog.DefaultLogger.log("FINE", ByteBufIOUtil.allToByteArray((ByteBuf) list.get(1)));


        if (true)return;

        GlobalConfiguration.init(null);
        DriverManager.add("Local Disk", WebDriversType.LocalDiskDriver);
        final LocalDiskConfiguration configuration = new LocalDiskConfiguration();
        final LocalDisk disk = new LocalDisk();
        disk.initiate(configuration);
        disk.buildCache();
        LocalDiskManager.recursiveRefreshDirectory(configuration, new DrivePath(""), null);
    }

}
