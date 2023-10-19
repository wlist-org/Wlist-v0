package com.xuxiaocheng.WList.Client.Exceptions;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

public class NoPermissionException extends WrongStateException {
    @Serial
    private static final long serialVersionUID = -3604601823190779388L;

    protected final @NotNull UserPermission @NotNull [] permissions;

    public NoPermissionException(final @NotNull UserPermission @NotNull ... permissions) {
        super(ResponseState.NoPermission, Arrays.toString(permissions));
        this.permissions = permissions;
    }

    public @NotNull @Unmodifiable List<@NotNull UserPermission> getPermissions() {
        return List.of(this.permissions);
    }

    @Override
    public @NotNull String toString() {
        return "NoPermissionException{" +
                "permissions=" + Arrays.toString(this.permissions) +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final NoPermissionException that = (NoPermissionException) o;
        return Arrays.equals(this.permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(this.permissions);
        return result;
    }
}
