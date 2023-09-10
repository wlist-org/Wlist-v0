package com.xuxiaocheng.WList.Commons.Operations;

import org.jetbrains.annotations.NotNull;

// assert State.name().length() > 1 (Differ from broadcast which start with boolean.)
public enum ResponseState {
    Undefined,
    Success,
    ServerError,
    Unsupported,
    NoPermission,
    DataError,
    FormatError,
    ;

    public static @NotNull ResponseState of(final @NotNull String state) {
        try {
            return ResponseState.valueOf(state);
        } catch (final IllegalArgumentException exception) {
            return ResponseState.Undefined;
        }
    }
}
