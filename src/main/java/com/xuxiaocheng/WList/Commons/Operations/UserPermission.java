package com.xuxiaocheng.WList.Commons.Operations;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation.Order
 */
public enum UserPermission {
    Undefined,
    ServerOperate,
    Broadcast,
    UsersList,
    GroupsOperate,
    UsersOperate,
    ProvidersOperate,
    FilesBuildIndex,
    FilesList,
    FileDownload,
    FileUpload,
    FileDelete,
    ;

    public static @NotNull UserPermission of(final @NotNull String permission) {
        try {
            return UserPermission.valueOf(permission);
        } catch (final IllegalArgumentException exception) {
            return UserPermission.Undefined;
        }
    }

    public static final @NotNull Set<@NotNull UserPermission> All = Stream.of(UserPermission.values()).filter(p -> p != UserPermission.Undefined).collect(Collectors.toSet());
    public static final @NotNull Set<@NotNull UserPermission> Empty = Set.of();//Collections.unmodifiableSet(EnumSet.noneOf(Permission.class));
    public static final @NotNull Set<@NotNull UserPermission> Default = Collections.unmodifiableSet(EnumSet.of(UserPermission.FilesList));

    public static @NotNull String dump(@SuppressWarnings("TypeMayBeWeakened") final @NotNull Set<@NotNull UserPermission> permissions) {
        long p = 0;
        for (final UserPermission permission: permissions)
            p |= 1L << permission.ordinal();
        return Long.toString(p, 36);
    }

    public static @Nullable EnumSet<@NotNull UserPermission> parse(final @NotNull String permissions) {
        try {
            final EnumSet<UserPermission> permissionsSet = EnumSet.noneOf(UserPermission.class);
            long p = Long.valueOf(permissions, 36).longValue();
            while (p != 0) {
                final long current = p & -p;
                p -= current;
                permissionsSet.add(UserPermission.values()[Long.numberOfTrailingZeros(current)]);
            }
            return permissionsSet;
        } catch (final NumberFormatException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    @Contract("_, _ -> param1")
    public static @NotNull ByteBuf dumpChooser(final @NotNull ByteBuf buf, final @NotNull Map<@NotNull UserPermission, @Nullable Boolean> chooser) throws IOException {
        final EnumSet<UserPermission> t = EnumSet.noneOf(UserPermission.class);
        final EnumSet<UserPermission> f = EnumSet.noneOf(UserPermission.class);
        for (final UserPermission permission: UserPermission.All) {
            final Boolean has = chooser.get(permission);
            if (has != null)
                (has.booleanValue() ? t : f).add(permission);
        }
        ByteBufIOUtil.writeUTF(buf, UserPermission.dump(t));
        ByteBufIOUtil.writeUTF(buf, UserPermission.dump(f));
        return buf;
    }

    public static @Nullable EnumMap<@NotNull UserPermission, @Nullable Boolean> parseChooser(final @NotNull ByteBuf buf) throws IOException {
        final EnumSet<UserPermission> t = UserPermission.parse(ByteBufIOUtil.readUTF(buf));
        final EnumSet<UserPermission> f = UserPermission.parse(ByteBufIOUtil.readUTF(buf));
        if (t == null || f == null)
            return null;
        final EnumMap<UserPermission, Boolean> permissions = new EnumMap<>(UserPermission.class);
        t.forEach(p -> permissions.put(p, Boolean.TRUE));
        f.forEach(p -> permissions.put(p, Boolean.FALSE));
        return permissions;
    }
}
