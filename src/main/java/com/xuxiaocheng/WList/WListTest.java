package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws Exception {
//        assert WListServer.FileTransferBufferSize == 4;
        final ConsumerE<ByteBuf> source = b -> {
            HLog.DefaultLogger.log("", b.readableBytes());
            HLog.DefaultLogger.log("", ByteBufIOUtil.allToByteArray(b));
        };
        final List<ConsumerE<ByteBuf>> list = DriverUtil.splitUploadMethod(source, 10);
        assert list.size() == 3;
        final ByteBuf b1 = Unpooled.wrappedBuffer(new byte[] {1, 2, 3, 4});
        final ByteBuf b2 = Unpooled.wrappedBuffer(new byte[] {5, 6, 7, 8});
        final ByteBuf b3 = Unpooled.wrappedBuffer(new byte[] {9, 10});
        WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> list.get(1).accept(b2)));
        WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> list.get(2).accept(b3)));
        WListServer.ServerExecutors.submit(HExceptionWrapper.wrapRunnable(() -> list.get(0).accept(b1)));
        assert b1.refCnt() == 0 && b2.refCnt() == 0 && b3.refCnt() == 0;
        WListServer.CodecExecutors.shutdownGracefully().syncUninterruptibly();
        WListServer.ServerExecutors.shutdownGracefully().syncUninterruptibly();
        WListServer.IOExecutors.shutdownGracefully().syncUninterruptibly();
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
