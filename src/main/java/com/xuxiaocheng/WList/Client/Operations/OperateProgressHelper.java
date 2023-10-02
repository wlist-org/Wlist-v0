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

public final class OperateProgressHelper {
    private OperateProgressHelper() {
        super();
    }

    public static @Nullable InstantaneousProgressState getProgress(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetProgress, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.GetProgress, token, p -> p.add("id", id));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final InstantaneousProgressState state = InstantaneousProgressState.parse(receive);
                OperateHelper.logOperated(OperationType.GetProgress, null, p -> p.add("state", state));
                return state;
            }
            OperateHelper.logOperated(OperationType.GetProgress, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }
}
