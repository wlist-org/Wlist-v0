package com.xuxiaocheng.WList.Driver.Exceptions;

import java.io.Serial;

public class IllegalResponseCodeException extends NetworkException {
    @Serial
    private static final long serialVersionUID = -5945144520835868429L;

    public IllegalResponseCodeException(final int code) {
        super(code);
    }

    public IllegalResponseCodeException(final int code, final String message) {
        super(code, message);
    }
}
