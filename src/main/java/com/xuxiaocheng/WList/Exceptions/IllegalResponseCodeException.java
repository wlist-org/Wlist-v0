package com.xuxiaocheng.WList.Exceptions;

import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@SuppressWarnings("ClassHasNoToStringMethod")
public class IllegalResponseCodeException extends WrongResponseException {
    @Serial
    private static final long serialVersionUID = 6296885928221076105L;

    protected final int code;

    public IllegalResponseCodeException(final int code) {
        super(code, null);
        this.code = code;
    }

    public IllegalResponseCodeException(final int code, final @Nullable String message) {
        super(code, message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
