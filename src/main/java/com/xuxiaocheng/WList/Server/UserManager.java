package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Exceptions.IllegalNetworkDataException;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    public static boolean doRegister(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
        //noinspection VariableNotUsedInsideIf
        if (user != null)
            throw new IllegalNetworkDataException("The same username has existed.");
        return UserSqlHelper.insertUser(username, password, UserManager.DefaultPermission);
    }

    public static @NotNull String doLoginIn(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final String username = ByteBufIOUtil.readUTF(buf);
        final String password = ByteBufIOUtil.readUTF(buf);
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
        if (user == null || !password.equals(user.getA()))
            throw new IllegalNetworkDataException("The username or password is wrong.");
        return UserTokenHelper.encodeToken(username, user.getC());
    }

    public static @NotNull String doLoginOut(final @NotNull ByteBuf ignoredBuf) {
        return "";
    }

    public static @NotNull Set<Operation.@NotNull Permission> getPermissions(final @NotNull String token) throws SQLException {
        final Pair<String, LocalDateTime> pair = UserTokenHelper.decodeToken(token);
        if (pair == null)
            return Set.of();
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(pair.getFirst());
        if (user == null)
            return Set.of();
        return user.getB();
    }
}
