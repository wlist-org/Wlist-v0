package com.xuxiaocheng.WList.Client.OperationHelpers;

import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class OperateSelfHelper {
    private OperateSelfHelper() {
        super();
    }

    public static boolean logon(final @NotNull WListClientInterface client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(OperationType.Logon);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Logon, null, p -> p.add("username", username).add("password", password));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.Logon, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static @Nullable String login(final @NotNull WListClientInterface client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(OperationType.Login);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Login, null, p -> p.add("username", username).add("password", password));
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive)) {
                final String token = ByteBufIOUtil.readUTF(receive);
                OperateHelper.logOperated(OperationType.Login, p -> p.add("success", true).add("token", token).add("tokenHash", token.hashCode()));
                return token;
            }
            OperateHelper.logOperated(OperationType.Login, p -> p.add("success", false));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean logoff(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.Logoff, token);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Logoff, token, p -> p.add("password", password));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.Logoff, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static boolean changeUsername(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String newUsername) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeUsername, token);
        ByteBufIOUtil.writeUTF(send, newUsername);
        OperateHelper.logOperating(OperationType.ChangeUsername, token, p -> p.add("newUsername", newUsername));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.ChangeUsername, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static boolean changePassword(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String oldPassword, final @NotNull String newPassword) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangePassword, token);
        ByteBufIOUtil.writeUTF(send, oldPassword);
        ByteBufIOUtil.writeUTF(send, newPassword);
        OperateHelper.logOperating(OperationType.ChangePassword, token, p -> p.add("oldPassword", oldPassword).add("newPassword", newPassword));
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            OperateHelper.logOperated(OperationType.ChangePassword, p -> p.add("success", success));
            return success;
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleUserGroupInformation getSelfGroup(final @NotNull WListClientInterface client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetSelfGroup, token);
        OperateHelper.logOperating(OperationType.GetSelfGroup, token, null);
        final ByteBuf receive = client.send(send);
        try {
            final boolean success = OperateHelper.handleState(receive);
            final VisibleUserGroupInformation group = success ? VisibleUserGroupInformation.parse(receive) : null;
            OperateHelper.logOperated(OperationType.GetSelfGroup, p -> p.add("success", success).optionallyAdd(success, "groupName", group));
            return group;
        } finally {
            receive.release();
        }
    }
}
