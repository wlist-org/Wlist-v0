package com.xuxiaocheng.WListClient;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.xuxiaocheng.WListClient.Server.DrivePath;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Utils.AesCipher;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

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

    public static void register(final @NotNull WListClient client, final @NotNull String passport, final @NotNull String password) throws IOException, InterruptedException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, AesCipher.defaultCipher);
        ByteBufIOUtil.writeUTF(send, Operation.Type.Register.name());
        ByteBufIOUtil.writeUTF(send, passport);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    static @NotNull ByteBuf operateWithToken(final Operation.@NotNull Type type, final @NotNull String token) throws IOException {
        final ByteBuf send = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(send, AesCipher.defaultCipher);
        ByteBufIOUtil.writeUTF(send, type.name());
        ByteBufIOUtil.writeUTF(send, token);
        return send;
    }

    public static void changePassword(final @NotNull WListClient client, final @NotNull String token, final @NotNull String oldPassword, final @NotNull String newPassword) throws IOException, InterruptedException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ChangePassword, token);
        ByteBufIOUtil.writeUTF(send, oldPassword);
        ByteBufIOUtil.writeUTF(send, newPassword);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static void logoff(final @NotNull WListClient client, final @NotNull String token, final @NotNull String password) throws IOException, InterruptedException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.Logoff, token);
        ByteBufIOUtil.writeUTF(send, password);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @NotNull JSONArray listUsers(final @NotNull WListClient client, final @NotNull String token) throws IOException, InterruptedException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListUsers, token);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
//            final Map<String, List<Operation.Permission>> list = new LinkedHashMap<>();
            return JSON.parseArray(ByteBufIOUtil.readUTF(receive));
        } finally {
            receive.release();
        }
    }

    public static void deleteUser(final @NotNull WListClient client, final @NotNull String token, final @NotNull String username) throws IOException, InterruptedException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteUser, token);
        ByteBufIOUtil.writeUTF(send, username);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static void changePermission(final @NotNull WListClient client, final @NotNull String token, final boolean add, final @NotNull String username, final String list) throws IOException, InterruptedException {
        final ByteBuf send;
        if (add)
            send = OperateHelper.operateWithToken(Operation.Type.AddPermission, token);
        else
            send = OperateHelper.operateWithToken(Operation.Type.ReducePermission, token);
        ByteBufIOUtil.writeUTF(send, username);
        // TODO
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @NotNull Optional<String> requestUploadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final long size, final @NotNull String md5) throws IOException, InterruptedException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestUploadFile, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeVariableLenLong(send, size);
        ByteBufIOUtil.writeUTF(send, md5);
        final ByteBuf receive = client.send(send);
        try {
            OperateHelper.handleState(receive);
            if (ByteBufIOUtil.readBoolean(receive))
                return Optional.empty();
            return Optional.of(ByteBufIOUtil.readUTF(receive));
        } finally {
            receive.release();
        }
    }

    public static void uploadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull String id, final int chunk, final @NotNull ByteBuf buffer) throws IOException, InterruptedException {
        final ByteBuf prefix = OperateHelper.operateWithToken(Operation.Type.UploadFile, token);
        ByteBufIOUtil.writeUTF(prefix, id);
        ByteBufIOUtil.writeVariableLenInt(prefix, chunk);
        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
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
