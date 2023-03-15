package com.xuxiaocheng.WList.Internal.Server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class OperationTypes {
    private OperationTypes() {
        super();
    }

    private static final @NotNull Map<Byte, Type> map = new HashMap<>();

    public static @NotNull OperationTypes.Type getType(final byte b) {
        return Objects.requireNonNullElse(OperationTypes.map.get(b), Type.Undefined);
    }

    public static byte getId(final @Nullable OperationTypes.Type type) {
        if (type == null)
            return 0;
        return type.getId();
    }

    public enum Type {
        Undefined((byte) 0),
        Registry((byte) 1),
        LoginIn((byte) 2),
        LoginOut((byte) 3),
        List((byte) 4);

        private final byte id;

        Type(final byte id) {
            this.id = id;
            OperationTypes.map.put(id, this);
        }

        public byte getId() {
            return this.id;
        }

        @Override
        public @NotNull String toString() {
            return "Type(" + this.id + ')';
        }
    }
}
