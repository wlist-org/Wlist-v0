package com.xuxiaocheng.WListClient.Client.Exceptions;

import com.xuxiaocheng.WListClient.Server.Operation;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class NoPermissionException extends WrongStateException {
    @Serial
    private static final long serialVersionUID = -3604601823190779388L;

    public NoPermissionException(final Operation.@NotNull Permission permission) {
        super(Operation.State.NoPermission, permission.name());
    }
}
