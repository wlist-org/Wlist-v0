package com.xuxiaocheng.WList.Client.Exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class InvalidTokenException extends NoPermissionException {
    @Serial
    private static final long serialVersionUID = -1819855938439525320L;

    @Override
    public @NotNull String toString() {
        return "InvalidTokenException";
    }
}
