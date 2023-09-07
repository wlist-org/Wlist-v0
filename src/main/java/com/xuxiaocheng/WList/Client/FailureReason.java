package com.xuxiaocheng.WList.Client;

import org.jetbrains.annotations.NotNull;

@Deprecated
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

    public static @NotNull FailureReason parse(final @NotNull String reason) {
        return switch (reason) {
            case "Filename" -> FailureReason.InvalidFilename;
            case "Duplicate" -> FailureReason.DuplicatePolicyError;
            case "Size" -> FailureReason.ExceedMaxSize;
            case "File" -> FailureReason.NoSuchFile;
            default -> FailureReason.Others;
        };
    }
}
