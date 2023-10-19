package com.xuxiaocheng.WList.Client.Exceptions;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

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

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final WrongStateException that = (WrongStateException) o;
        return this.state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state);
    }
}
