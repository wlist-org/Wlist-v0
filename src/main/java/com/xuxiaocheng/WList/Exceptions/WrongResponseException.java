package com.xuxiaocheng.WList.Exceptions;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;

public class WrongResponseException extends NetworkException {
    @Serial
    private static final long serialVersionUID = 7263014283737534125L;

    private final @NotNull String parameters; // String instead of ParametersMap: Freeze data.
    private final @NotNull LocalDateTime time = LocalDateTime.now();

    public WrongResponseException(final int code, final @Nullable String message, final @Nullable ParametersMap parameters) {
        super("Code: " + code + " Message: " + message);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response, final @Nullable ParametersMap parameters) {
        super(message + " Response: " + response);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable ParametersMap parameters, final @NotNull Throwable cause) {
        super(message, cause);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response, final @Nullable ParametersMap parameters, final @NotNull Throwable cause) {
        super(message + " Response: " + response, cause);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public @NotNull String getParameters() {
        return this.parameters;
    }

    public @NotNull LocalDateTime getTime() {
        return this.time;
    }
}
