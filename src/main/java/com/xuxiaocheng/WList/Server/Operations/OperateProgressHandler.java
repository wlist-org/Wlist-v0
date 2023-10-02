package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.IdsHelper;
import com.xuxiaocheng.WList.Server.Operations.Helpers.ProgressBar;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class OperateProgressHandler {
    private OperateProgressHandler() {
        super();
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.GetProgress, OperateProgressHandler.doGetProgress);
    }

    private static final @NotNull ServerHandler doGetProgress = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token);
        final String id = ByteBufIOUtil.readUTF(buffer);
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else {
            final Set<UserPermission> permissions = user.getT().group().permissions();
            if (!permissions.contains(UserPermission.FileDownload) && !permissions.contains(UserPermission.FileUpload))
                message = OperateSelfHandler.NoPermission(List.of(UserPermission.Undefined));
        }
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        return () -> {
            final ProgressBar progress = IdsHelper.getProgressBar(id);
            WListServer.ServerChannelHandler.write(channel, progress == null ? MessageProto.WrongParameters : MessageProto.successMessage(progress::dump));
        };
    };
}
