package com.xuxiaocheng.WList.Exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;

public class DriverTokenExpiredException extends IOException {
    @Serial
    private static final long serialVersionUID = 6342302125979689210L;

    @Deprecated
    public DriverTokenExpiredException() {
        super();
    }

    public DriverTokenExpiredException(final @NotNull String message) {
        super(message);
    }

    @Deprecated
    public DriverTokenExpiredException(final @NotNull Throwable cause) {
        super(cause);
    }

    public DriverTokenExpiredException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }
}
