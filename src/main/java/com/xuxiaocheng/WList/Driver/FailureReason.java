package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FailureReason {
    public static final @NotNull FailureReason NIL = new FailureReason(FailureReason.Others, "NIL.", null);

    public static final @NotNull String InvalidFileName = "Invalid file name.";
    public static final @NotNull String DuplicatePolicyError = "ERROR by duplicate policy.";
    public static final @NotNull String ExceedMaxSize = "Exceed max size per file.";
    public static final @NotNull String NoSuchFile = "No such file.";
    public static final @NotNull String Others = "Others.";

    public static @NotNull FailureReason byInvalidName(final @NotNull String name, final @NotNull DrivePath fullPath) {
        return new FailureReason(FailureReason.InvalidFileName, name, fullPath);
    }

    public static @NotNull FailureReason byDuplicateError(final @NotNull String callingMethod, final @NotNull DrivePath path) {
        return new FailureReason(FailureReason.DuplicatePolicyError, callingMethod, path);
    }

    public static @NotNull FailureReason byExceedMaxSize(final long current, final long max, final @NotNull DrivePath path) {
        return new FailureReason(FailureReason.ExceedMaxSize, String.format("current (%d) > max (%d).", current, max), path);
    }

    public static @NotNull FailureReason byExceedMaxSize(final long current, final @NotNull String message, final @NotNull DrivePath path) {
        return new FailureReason(FailureReason.ExceedMaxSize, "Current: " + current + ", " + message, path);
    }

    public static @NotNull FailureReason byNoSuchFile(final @NotNull String callingMethod, final @NotNull DrivePath path) {
        return new FailureReason(FailureReason.NoSuchFile, callingMethod, path);
    }

    public static @NotNull FailureReason others(final @NotNull String message, final @Nullable Object extra) {
        return new FailureReason(FailureReason.Others, message, extra);
    }

    protected final @NotNull String kind;
    protected final @NotNull String message;
    protected final @Nullable Object extra;
    protected final @NotNull Throwable throwable;

    protected FailureReason(final @NotNull String kind, final @NotNull String message, final @Nullable Object extra) {
        super();
        this.kind = kind;
        this.message = message;
        this.extra = extra;
        this.throwable = new Throwable();
    }

    public @NotNull String kind() {
        return this.kind;
    }

    public @NotNull String message() {
        return this.message;
    }

    public @Nullable Object extra() {
        return this.extra;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof FailureReason that)) return false;
        return this.kind.equals(that.kind) && this.message.equals(that.message) && Objects.equals(this.extra, that.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.kind, this.message);
    }

    @Override
    public @NotNull String toString() {
        return "FailureReason{" +
                "kind='" + this.kind + '\'' +
                ", message='" + this.message + '\'' +
                ", extra=" + this.extra +
                '}';
    }
}