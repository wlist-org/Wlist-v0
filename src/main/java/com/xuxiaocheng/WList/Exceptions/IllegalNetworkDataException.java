package com.xuxiaocheng.WList.Exceptions;

import java.io.Serial;

public class IllegalNetworkDataException extends NetworkException {
    @Serial
    private static final long serialVersionUID = -6647704998750964255L;

    public IllegalNetworkDataException() {
        super();
    }

    public IllegalNetworkDataException(final String message) {
        super(message);
    }

    public IllegalNetworkDataException(final Throwable cause) {
        super(cause);
    }

    public IllegalNetworkDataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
