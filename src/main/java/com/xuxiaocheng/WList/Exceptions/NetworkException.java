package com.xuxiaocheng.WList.Exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;

public class NetworkException extends IOException {
    @Serial
    private static final long serialVersionUID = 2189376168475962813L;

    @Deprecated
    public NetworkException() {
        super();
    }

    @Deprecated
    public NetworkException(final @NotNull Throwable cause) {
        super(cause);
    }

    public NetworkException(final @NotNull String message) {
        super(message);
    }

    public NetworkException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }

    @Deprecated
    public NetworkException(final int code) {
        super("[" + code + "]");
    }

    @Deprecated
    public NetworkException(final int code, final @NotNull Throwable cause) {
        super("[" + code + "]", cause);
    }

    public NetworkException(final int code, final @NotNull String message) {
        super("[" + code + "] " + message);
    }

    public NetworkException(final int code, final @NotNull String message, final @NotNull Throwable cause) {
        super("[" + code + "] " + message, cause);
    }
}
