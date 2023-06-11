package com.xuxiaocheng.WListClient.Server;

import org.jetbrains.annotations.NotNull;

public enum FailureReason {
    InvalidFilename,
    DuplicatePolicyError,
    ExceedMaxSize,
    NoSuchFile,
    Others,
    ;

    public static @NotNull String handleFailureReason(final @NotNull FailureReason reason) {
        return switch (reason) {
            case InvalidFilename -> "Invalid filename.";
            case DuplicatePolicyError -> "ERROR by duplicate policy.";
            case ExceedMaxSize -> "Exceed max size per file.";
            case NoSuchFile -> "No such file.";
            case Others -> "Failure, unknown reason.";
        };
    }
}
