package com.xuxiaocheng.WList.Commons.Operations;

import org.jetbrains.annotations.NotNull;

public enum FailureKind {
    InvalidName("Invalid name."),
    DuplicateError("Duplicate name."),
    ExceedMaxSize("Exceed max size per file."),
    NoSuchFile("No such file or directory."),
    Others("Unknown reason."),
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
