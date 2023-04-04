package com.xuxiaocheng.WList.Driver.Exceptions;

import java.io.IOException;
import java.io.Serial;

public class NetworkException extends IOException {
    @Serial
    private static final long serialVersionUID = 2189376168475962813L;

    public NetworkException() {
        super();
    }

    public NetworkException(final int code) {
        super("[" + code + "]");
    }

    public NetworkException(final int code, final String message) {
        super("[" + code + "]" + message);
    }

    public NetworkException(final String message) {
        super(message);
    }

    public NetworkException(final int code, final String message, final Throwable cause) {
        super("[" + code + "]" + message, cause);
    }

    public NetworkException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetworkException(final Throwable cause) {
        super(cause);
    }
}
