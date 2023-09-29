package com.xuxiaocheng.WList.Commons.Operations;

import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import org.jetbrains.annotations.NotNull;

public enum FailureKind {
    InvalidName(I18NUtil.get("server.failure.invalid_name")),
    DuplicateError(I18NUtil.get("server.failure.duplicate_error")),
    ExceedMaxSize(I18NUtil.get("server.failure.exceed_max_size")),
    NoSuchFile(I18NUtil.get("server.failure.no_such_file")),
    Others(I18NUtil.get("server.failure.others")),
    ;

    public static @NotNull FailureKind of(final @NotNull String type) {
        try {
            return FailureKind.valueOf(type);
        } catch (final IllegalArgumentException exception) {
            return FailureKind.Others;
        }
    }

    private final @NotNull String description;

    FailureKind(final @NotNull String description) {
        this.description = description;
    }

    public @NotNull String description() {
        return this.description;
    }

    @Override
    public @NotNull String toString() {
        return "FailureKind{" +
                "name='" + this.name() + '\'' +
                ", description='" + this.description + '\'' +
                '}';
    }
}
