package com.xuxiaocheng.WList.Server;

import com.alibaba.fastjson2.JSON;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Operation {
    private Operation() {
        super();
    }

    public enum Type {
        Undefined,
        Login,
        Registry,
        AddPermission,
        ReducePermission,
//        List,
    }

    public enum Permission {
        Undefined,
        UsersList,
        UsersAdd,
        UsersDelete,
        UsersChangePermissions,
        FilesList,
    }

    public enum State {
        Undefined,
        Success,
        ServerError,
        Unsupported,
        NoPermission,
        DataError,
    }

    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Type> TypeMap = Stream.of(Type.values())
            .collect(Collectors.toMap(Enum::name, t -> t));
    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Permission> PermissionMap = Stream.of(Permission.values())
            .collect(Collectors.toMap(Enum::name, p -> p));
    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull State> StateMap = Stream.of(State.values())
            .collect(Collectors.toMap(Enum::name, s -> s));

    public static @NotNull String dumpPermissions(final @NotNull SortedSet<@NotNull Permission> permissions) {
        return JSON.toJSONString(permissions);
    }

    public static @NotNull SortedSet<@NotNull Permission> parsePermissions(final @NotNull String permissions) {
        return new TreeSet<>(JSON.parseArray(permissions).stream()
                .map(s -> Operation.PermissionMap.get(s.toString())).filter(Objects::nonNull).toList());
    }
}
