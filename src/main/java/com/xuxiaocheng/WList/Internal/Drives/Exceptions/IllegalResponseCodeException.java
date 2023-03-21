package com.xuxiaocheng.WList.Internal.Drives.Exceptions;

import java.io.IOException;
import java.io.Serial;

public class IllegalResponseCodeException extends IOException {
    @Serial
    private static final long serialVersionUID = -5945144520835868429L;

    public IllegalResponseCodeException(final int code) {
        super(Integer.toString(code));
    }
}
