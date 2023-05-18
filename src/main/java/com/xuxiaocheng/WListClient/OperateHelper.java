package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Utils.AesCipher;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class OperateHelper {
    private OperateHelper() {
        super();
    }

    static void handleState(final @NotNull ByteBuf receive) throws IOException {
        final byte cipher = ByteBufIOUtil.readByte(receive);
        final String state = ByteBufIOUtil.readUTF(receive);
        if (Operation.State.Success.name().equals(state))
            return;
        if (Operation.State.DataError.name().equals(state)) {
            final String info;
            try {
                info = ByteBufIOUtil.readUTF(receive);
            } catch (final IOException exception) {
                throw new IOException(state);
            }
            throw new IOException(state + ": " + info);
        }
        if (Operation.State.ServerError.name().equals(state))
            throw new IOException(state + ": " + ByteBufIOUtil.readUTF(receive));
        throw new IOException(state);
    }

    public static @NotNull String login(final @NotNull WListClient client, final @NotNull String passport, final @NotNull String password) throws IOException, InterruptedException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, AesCipher.defaultCipher);
        ByteBufIOUtil.writeUTF(send, Operation.Type.Login.name());
        ByteBufIOUtil.writeUTF(send, passport);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
            return ByteBufIOUtil.readUTF(receive);
        } finally {
            receive.release();
        }
    }

    public static void changePassword(final @NotNull WListClient client, final @NotNull String token, final @NotNull String oldPassword, final @NotNull String newPassword) throws IOException, InterruptedException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, AesCipher.defaultCipher);
        ByteBufIOUtil.writeUTF(send, Operation.Type.ChangePassword.name());
        ByteBufIOUtil.writeUTF(send, token);
        ByteBufIOUtil.writeUTF(send, oldPassword);
        ByteBufIOUtil.writeUTF(send, newPassword);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static void closeServer(final @NotNull WListClient client, final @NotNull String token) throws IOException, InterruptedException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, AesCipher.defaultCipher);
        ByteBufIOUtil.writeUTF(send, Operation.Type.CloseServer.name());
        ByteBufIOUtil.writeUTF(send, token);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }
}
