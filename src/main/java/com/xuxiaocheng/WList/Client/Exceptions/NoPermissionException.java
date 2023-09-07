package com.xuxiaocheng.WList.Client.Exceptions;

import com.xuxiaocheng.WList.Commons.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

public class NoPermissionException extends WrongStateException {
    @Serial
    private static final long serialVersionUID = -3604601823190779388L;

    protected final Operation.@NotNull Permission @NotNull [] permissions;

    public NoPermissionException(final Operation.@NotNull Permission @NotNull ... permissions) {
        super(Operation.State.NoPermission, Arrays.toString(permissions));
        this.permissions = permissions;
    }

    public @NotNull @Unmodifiable List<Operation.@NotNull Permission> getPermissions() {
        return List.of(this.permissions);
    }

    @Override
    public @NotNull String toString() {
        return "NoPermissionException{" +
                "permissions=" + Arrays.toString(this.permissions) +
                '}';
    }
}
