package com.xuxiaocheng.WList.Server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Operation {
    private Operation() {
        super();
    }

    public enum Type {
        Undefined,
        LoginIn,
        LoginOut,
        Registry,
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
}
