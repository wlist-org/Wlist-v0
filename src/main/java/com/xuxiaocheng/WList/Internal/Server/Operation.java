package com.xuxiaocheng.WList.Internal.Server;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Operation {
    private Operation() {
        super();
    }

    private static final @NotNull Map<Byte, Type> TypeMap = new HashMap<>();
    private static final @NotNull Map<Byte, Permission> PermissionMap = new HashMap<>();
    private static final @NotNull Map<Byte, State> StateMap = new HashMap<>();

    @SuppressWarnings({"unused", "UnusedAssignment"})
    public static void init() {
        Object e;
        e = Type.Undefined;
        e = Permission.Undefined;
        e = State.Undefined;
    }

    public static byte getId(final @NotNull Type type) {
        return type.id;
    }
    public static @NotNull Type getType(final byte b) {
        return Objects.requireNonNullElse(Operation.TypeMap.get(b), Type.Undefined);
    }

    public static byte getId(final @NotNull Permission permission) {
        return permission.id;
    }
    public static @NotNull Permission getPermission(final byte b) {
        return Objects.requireNonNullElse(Operation.PermissionMap.get(b), Permission.Undefined);
    }

    public static byte getId(final @NotNull State state) {
        return state.id;
    }
    public static @NotNull State getState(final byte b) {
        return Objects.requireNonNullElse(Operation.StateMap.get(b), State.Undefined);
    }

    public enum Type {
        Undefined((byte) 0),
        LoginIn((byte) 1),
        LoginOut((byte) 2),
        Registry((byte) 3),
//        List,
        ;
        private final byte id;

        Type(final byte id) {
            this.id = id;
            Operation.TypeMap.put(id, this);
        }

        public byte getId() {
            return this.id;
        }

        @Override
        public @NotNull String toString() {
            return "Type(" + this.name() + ')';
        }
    }

    public enum Permission {
        Undefined((byte) 0),
        UsersList((byte) 1),
        UsersAdd((byte) 2),
        UsersDelete((byte) 3),
        UsersChangePermissions((byte) 4),
//        FilesList((byte) 11),
        ;
        private final byte id;

        Permission(final byte id) {
            this.id = id;
            Operation.PermissionMap.put(id, this);
        }

        public byte getId() {
            return this.id;
        }

        @Override
        public @NotNull String toString() {
            return "Permission(" + this.name() + ')';
        }
    }

    public enum State {
        Undefined((byte) 0),
        Success((byte) 1),
        Unsupported((byte) 2),
        NoPermission((byte) 3),
        DataError((byte) 4),
        ;
        private final byte id;

        State(final byte id) {
            this.id = id;
            Operation.StateMap.put(id, this);
        }

        public byte getId() {
            return this.id;
        }

        @Override
        public @NotNull String toString() {
            return "State(" + this.name() + ')';
        }
    }
}
