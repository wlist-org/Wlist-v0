package com.xuxiaocheng.WList.Exceptions;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

@SuppressWarnings("ClassHasNoToStringMethod")
public class IllegalResponseCodeException extends WrongResponseException {
    @Serial
    private static final long serialVersionUID = 6296885928221076105L;

    protected final int code;
    protected final @NotNull String meaning;

    public IllegalResponseCodeException(final int code, final @Nullable String message, final @NotNull ParametersMap parameters) {
        super(code, message, parameters);
        this.code = code;
        this.meaning = Objects.requireNonNullElse(message, "");
    }

    public int getCode() {
        return this.code;
    }

    public @NotNull String getMeaning() {
        return this.meaning;
    }
}
