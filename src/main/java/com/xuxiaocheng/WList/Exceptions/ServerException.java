package com.xuxiaocheng.WList.Exceptions;

import java.io.Serial;

public class ServerException extends Exception {
    @Serial
    private static final long serialVersionUID = -3159589575455894339L;

    public ServerException() {
        super();
    }

    public ServerException(final String message) {
        super(message);
    }

    public ServerException(final Throwable cause) {
        super(cause);
    }

    public ServerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
