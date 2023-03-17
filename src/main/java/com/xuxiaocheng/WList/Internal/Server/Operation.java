package com.xuxiaocheng.WList.Internal.Server;

import org.jetbrains.annotations.NotNull;

public final class Operation {
    private Operation() {
        super();
    }

    public enum Type {
        Undefined,
        Registry,
        LoginIn,
        LoginOut,
        List
    }

    public static @NotNull Operation.Type getType(final byte b) {
        return switch (b) {
            case 1 -> Type.Registry;
            case 2 -> Type.LoginIn;
            case 3 -> Type.LoginOut;
            case 4 -> Type.List;
            default -> Type.Undefined;
        };
    }

    public enum Permission {
        UsersList,
        UsersAdd,
        UsersDelete,
        UsersChangePermissions,
        FilesList
    }
}
