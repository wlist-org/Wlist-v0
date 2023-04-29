package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class UserManager {
    private UserManager() {
        super();
    }

    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> AdminPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.stream(Operation.Permission.values()).skip(1)/*Permission.Undefined*/.toList()));
    public static final @NotNull @UnmodifiableView SortedSet<Operation.@NotNull Permission> DefaultPermission =
            Collections.unmodifiableSortedSet(new TreeSet<>(List.of(Operation.Permission.FilesList)));

    public static @Nullable String doLogin(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
        if (user == null || !user.getA().equals(password))
            return null;
        return UserTokenHelper.encodeToken(username, user.getC());
    }

    public static boolean doRegister(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        return UserSqlHelper.insertUser(username, password, UserManager.DefaultPermission);
    }


}
