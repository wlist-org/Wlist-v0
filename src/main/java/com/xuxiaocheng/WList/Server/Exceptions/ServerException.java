package com.xuxiaocheng.WList.Server.Exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class ServerException extends Exception {
    @Serial
    private static final long serialVersionUID = -3159589575455894339L;

    @Deprecated
    public ServerException() {
        super();
    }

    public ServerException(final @NotNull String message) {
        super(message);
    }

    public ServerException(final @NotNull Throwable cause) {
        super(cause);
    }

    public ServerException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }
}
