package com.xuxiaocheng.WList.Driver.Exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class IllegalConfigurationException extends Exception {
    @Serial
    private static final long serialVersionUID = -695714574144702379L;

    public IllegalConfigurationException(final @NotNull String message) {
        super(message);
    }

}
