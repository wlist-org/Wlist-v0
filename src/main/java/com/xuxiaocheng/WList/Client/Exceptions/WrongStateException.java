package com.xuxiaocheng.WList.Client.Exceptions;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

@SuppressWarnings("ClassHasNoToStringMethod")
public class WrongStateException extends Exception {
    @Serial
    private static final long serialVersionUID = 1694259253430975849L;

    protected final @NotNull ResponseState state;

    public WrongStateException(final @NotNull ResponseState state) {
        super(state.name());
        this.state = state;
    }

    public WrongStateException(final @NotNull ResponseState state, final @NotNull String message) {
        super(state.name() + ": " + message);
        this.state = state;
    }

    public @NotNull ResponseState getState() {
        return this.state;
    }
}
