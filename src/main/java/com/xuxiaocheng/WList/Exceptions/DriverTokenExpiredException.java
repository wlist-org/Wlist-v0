package com.xuxiaocheng.WList.Exceptions;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
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

    @Deprecated
    public DriverTokenExpiredException(final @NotNull String message) {
        super(message);
    }

    public DriverTokenExpiredException(final @NotNull String message, final @NotNull ParametersMap parameters) {
        super(message + parameters);
    }

    @Deprecated
    public DriverTokenExpiredException(final @NotNull Throwable cause) {
        super(cause);
    }

    @Deprecated
    public DriverTokenExpiredException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }

    public DriverTokenExpiredException(final @NotNull String message, final @NotNull ParametersMap parameters, final @NotNull Throwable cause) {
        super(message + parameters, cause);
    }
}
