package com.xuxiaocheng.WList.Internal.Drives.Exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serial;

public class WrongResponseException extends IOException {
    @Serial
    private static final long serialVersionUID = 7263014283737534125L;

    public WrongResponseException(final int code, final @Nullable String message) {
        super("Code: " + code + " Message: " + message);
    }

    public WrongResponseException(final @NotNull String message) {
        super(message);
    }

    public WrongResponseException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }
}
