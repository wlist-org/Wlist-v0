package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.IdsHelper;
import com.xuxiaocheng.WList.Server.Operations.Helpers.ProgressBar;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

public final class OperateProgressHandler {
    private OperateProgressHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.GetProgress, OperateProgressHandler.doGetProgress);
    }

    private static final @NotNull ServerHandler doGetProgress = (channel, buffer) -> {
        final String id = ByteBufIOUtil.readUTF(buffer);
        final ProgressBar progress = IdsHelper.getProgressBar(id);
        if (progress == null) {
            WListServer.ServerChannelHandler.write(channel, MessageProto.WrongParameters);
            return null;
        }
        return () -> WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(progress::dump));
    };
}
