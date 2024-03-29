package com.xuxiaocheng.WList.Server.Exceptions;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.ZonedDateTime;

@SuppressWarnings("ClassHasNoToStringMethod")
public class WrongResponseException extends NetworkException {
    @Serial
    private static final long serialVersionUID = 7263014283737534125L;

    private final @NotNull String parameters; // String instead of ParametersMap: Freeze data.
    private final @NotNull ZonedDateTime time = MiscellaneousUtil.now();

    public WrongResponseException(final int code, final @Nullable String message, final @Nullable ParametersMap parameters) {
        super("[" + code + "] " + message + parameters);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response, final @Nullable ParametersMap parameters) {
        super(message + parameters + " Response: " + response);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable ParametersMap parameters, final @NotNull Throwable cause) {
        super(message + parameters, cause);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public WrongResponseException(final @NotNull String message, final @Nullable Object response, final @Nullable ParametersMap parameters, final @NotNull Throwable cause) {
        super(message + parameters + " Response: " + response, cause);
        this.parameters = parameters == null ? "" : parameters.toString();
    }

    public @NotNull String getParameters() {
        return this.parameters;
    }

    public @NotNull ZonedDateTime getTime() {
        return this.time;
    }
}
