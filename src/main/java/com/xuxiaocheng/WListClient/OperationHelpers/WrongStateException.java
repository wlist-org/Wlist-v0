package com.xuxiaocheng.WListClient.OperationHelpers;

import com.xuxiaocheng.WListClient.Server.Operation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@SuppressWarnings("ClassHasNoToStringMethod")
public class WrongStateException extends Exception {
    @Serial
    private static final long serialVersionUID = 1694259253430975849L;

    protected final Operation.@NotNull State state;

    public WrongStateException(final Operation.@NotNull State state) {
        super(state.name());
        this.state = state;
    }

    public WrongStateException(final Operation.@NotNull State state, final @NotNull String message) {
        super(state.name() + ": " + message);
        this.state = state;
    }

    public Operation.@NotNull State getState() {
        return this.state;
    }
}
