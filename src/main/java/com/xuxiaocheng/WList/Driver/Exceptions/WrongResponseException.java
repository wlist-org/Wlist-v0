package com.xuxiaocheng.WList.Driver.Exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class WrongResponseException extends NetworkException {
    @Serial
    private static final long serialVersionUID = 7263014283737534125L;

    public WrongResponseException(final @NotNull String message) {
        super(message);
    }

    public WrongResponseException(final int code, final @Nullable String message) {
        super("Code: " + code + " Message: " + message);
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response) {
        super(message + " Response: " + response);
    }


    public WrongResponseException(final @NotNull Throwable cause) {
        super(cause);
    }


    public WrongResponseException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response, final @NotNull Throwable cause) {
        super(message + " Response: " + response, cause);
    }
}
