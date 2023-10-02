package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateProgressHandler
 */
public final class OperateProgressHelper {
    private OperateProgressHelper() {
        super();
    }

    public static @Nullable InstantaneousProgressState getProgress(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetProgress, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive) == null ? InstantaneousProgressState.parse(receive) : null;
        } finally {
            receive.release();
        }
    }
}
