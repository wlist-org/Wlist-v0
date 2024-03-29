package com.xuxiaocheng.WList.Server.Exceptions;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class IllegalParametersException extends Exception {
    @Serial
    private static final long serialVersionUID = -7075894286625354029L;

    @Deprecated
    public IllegalParametersException() {
        super();
    }

    @Deprecated
    public IllegalParametersException(final @NotNull Throwable cause) {
        super(cause);
    }

    @Deprecated
    public IllegalParametersException(final @NotNull String message) {
        super(message);
    }

    @Deprecated
    public IllegalParametersException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }

    @Deprecated
    public IllegalParametersException(final @NotNull String message, final @Nullable Object parameter) {
        super(message + " Parameter: " + parameter);
    }

    @Deprecated
    public IllegalParametersException(final @NotNull String message, final @Nullable Object parameter, final @NotNull Throwable cause) {
        super(message + " Parameter: " + parameter, cause);
    }

    public IllegalParametersException(final @NotNull String message, final @Nullable ParametersMap parameters) {
        super(message + " Parameters:" + parameters);
    }

    public IllegalParametersException(final @NotNull String message, final @Nullable ParametersMap parameters, final @NotNull Throwable cause) {
        super(message + " Parameters:" + parameters, cause);
    }
}
