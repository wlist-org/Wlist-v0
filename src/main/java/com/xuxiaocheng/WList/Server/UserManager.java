package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Server.Helper.TokenHelper;
import com.xuxiaocheng.WList.Server.Helper.UserHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public final class UserManager {
    private UserManager() {
        super();
    }

    private static @NotNull String getNewToken(final @NotNull String username) throws SQLException {
        final long time = System.currentTimeMillis();
        final String token = UserHelper.generateRandomToken(username, time);
        TokenHelper.addToken(token, username, time);
        return token;
    }

    public static void doRegister(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = UserHelper.encryptPassword(ByteBufIOUtil.readUTF(buf));
        final Pair<String, Set<Operation.Permission>> user = UserHelper.getUser(username);
        //noinspection VariableNotUsedInsideIf
        if (user != null)
            throw new IllegalNetworkDataException("The same username has existed.");
        UserHelper.addUser(username, password);
    }

    public static @NotNull String doLoginIn(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = UserHelper.encryptPassword(ByteBufIOUtil.readUTF(buf));
        final Pair<String, Set<Operation.Permission>> user = UserHelper.getUser(username);
        if (user == null || !password.equals(user.getFirst()))
            throw new IllegalNetworkDataException("The username or password is wrong.");
        return UserManager.getNewToken(username);
    }

    public static @NotNull String doLoginOut(final @NotNull ByteBuf ignoredBuf) {
        return "";
    }

    public static @NotNull Set<Operation.@NotNull Permission> getPermissions(final @NotNull String token) throws SQLException {
        final String username = TokenHelper.getUsername(token);
        if (username == null)
            return Set.of();
        final Pair<String, Set<Operation.Permission>> user = UserHelper.getUser(username);
        if (user == null)
            return Set.of();
        return user.getSecond();
    }
}
