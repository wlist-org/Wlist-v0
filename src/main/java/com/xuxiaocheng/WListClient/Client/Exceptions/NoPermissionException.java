package com.xuxiaocheng.WListClient.Client.Exceptions;

import com.xuxiaocheng.WListClient.Server.Operation;

import java.io.Serial;

public class NoPermissionException extends WrongStateException {
    @Serial
    private static final long serialVersionUID = -3604601823190779388L;

    public NoPermissionException() {
        super(Operation.State.NoPermission);
    }
}
