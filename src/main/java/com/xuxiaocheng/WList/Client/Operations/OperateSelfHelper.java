package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateSelfHandler
 */
public final class OperateSelfHelper {
    private OperateSelfHelper() {
        super();
    }

    public static boolean logon(final @NotNull WListClientInterface client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(OperationType.Logon);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Logon, null, p -> p.add("username", username).add("password", password));
        return OperateHelper.booleanOperation(client, send, OperationType.Logon);
    }

    public static Pair.@Nullable ImmutablePair<@NotNull String, @NotNull ZonedDateTime> login(final @NotNull WListClientInterface client, final @NotNull String username, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operate(OperationType.Login);
        ByteBufIOUtil.writeUTF(send, username);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Login, null, p -> p.add("username", username).add("password", password));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final String token = ByteBufIOUtil.readUTF(receive);
                final ZonedDateTime expires = ZonedDateTime.parse(ByteBufIOUtil.readUTF(receive), DateTimeFormatter.ISO_DATE_TIME);
                OperateHelper.logOperated(OperationType.Login, null, p -> p.add("token", token).add("tokenHash", token.hashCode()).add("expires", expires));
                return Pair.ImmutablePair.makeImmutablePair(token, expires);
            }
            OperateHelper.logOperated(OperationType.Login, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean logoff(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String password) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.Logoff, token);
        ByteBufIOUtil.writeUTF(send, password);
        OperateHelper.logOperating(OperationType.Logoff, token, p -> p.add("password", password));
        return OperateHelper.booleanOperation(client, send, OperationType.Logoff);
    }

    public static boolean changeUsername(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String newUsername) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeUsername, token);
        ByteBufIOUtil.writeUTF(send, newUsername);
        OperateHelper.logOperating(OperationType.ChangeUsername, token, p -> p.add("newUsername", newUsername));
        return OperateHelper.booleanOperation(client, send, OperationType.ChangeUsername);
    }

    public static boolean changePassword(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String oldPassword, final @NotNull String newPassword) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangePassword, token);
        ByteBufIOUtil.writeUTF(send, oldPassword);
        ByteBufIOUtil.writeUTF(send, newPassword);
        OperateHelper.logOperating(OperationType.ChangePassword, token, p -> p.add("oldPassword", oldPassword).add("newPassword", newPassword));
        return OperateHelper.booleanOperation(client, send, OperationType.ChangePassword);
    }

    public static @Nullable VisibleUserGroupInformation getSelfGroup(final @NotNull WListClientInterface client, final @NotNull String token) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetSelfGroup, token);
        OperateHelper.logOperating(OperationType.GetSelfGroup, token, null);
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final VisibleUserGroupInformation group = VisibleUserGroupInformation.parse(receive);
                OperateHelper.logOperated(OperationType.GetSelfGroup, null, p -> p.add("group", group));
                return group;
            }
            OperateHelper.logOperated(OperationType.GetSelfGroup, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }
}
