package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// TODO enhance.
public class FailureReason {
    public static final @NotNull String InvalidFilename = "Invalid filename.";
    public static final @NotNull String DuplicatePolicyError = "ERROR by duplicate policy.";
    public static final @NotNull String ExceedMaxSize = "Exceed max size per file.";
    public static final @NotNull String NoSuchFile = "No such file.";
    public static final @NotNull String Others = "Others.";

    public static @NotNull FailureReason byInvalidName(final @NotNull String callingMethodMessage, final @NotNull FileLocation location, final @NotNull String name) {
        return new FailureReason(FailureReason.InvalidFilename, name, location);
    }

    public static @NotNull FailureReason byDuplicateError(final @NotNull String callingMethodMessage, final @NotNull FileLocation location, final @NotNull String name) {
        return new FailureReason(FailureReason.DuplicatePolicyError, callingMethodMessage, location);
    }

    public static @NotNull FailureReason byExceedMaxSize(final @NotNull String callingMethodMessage, final long current, final long max, final @NotNull FileLocation location, final @NotNull String name) {
        return new FailureReason(FailureReason.ExceedMaxSize, String.format("current (%d) > max (%d).", current, max), location);
    }

    public static @NotNull FailureReason byExceedMaxSize(final @NotNull String callingMethodMessage, final long current, final @NotNull String message, final @NotNull FileLocation location, final @NotNull String name) {
        return new FailureReason(FailureReason.ExceedMaxSize, "Current: " + current + ", " + message, location);
    }

    public static @NotNull FailureReason byNoSuchFile(final @NotNull String callingMethodMessage, final @NotNull FileLocation location) {
        return new FailureReason(FailureReason.NoSuchFile, callingMethodMessage, location);
    }

    @Deprecated
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

    public @NotNull Throwable throwable() {
        return this.throwable;
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
