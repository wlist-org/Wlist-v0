package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class FailureReason {
    public static @NotNull FailureReason byInvalidName(final @NotNull FileLocation location, final @NotNull String name, final @NotNull String rules) {
        return new FailureReason(FailureKind.InvalidName, location, ParametersMap.create().add("name", name).add("rules", rules).toString());
    }

    public static @NotNull FailureReason byDuplicateError(final @NotNull FileLocation location, final @NotNull String name) {
        return new FailureReason(FailureKind.DuplicateError, location, ParametersMap.create().add("name", name).toString());
    }

    public static @NotNull FailureReason byExceedMaxSize(final @NotNull FileLocation location, final long current, final long max) {
        return new FailureReason(FailureKind.ExceedMaxSize, location, ParametersMap.create().add("current", current).add("max", max).toString());
    }

    public static @NotNull FailureReason byNoSuchFile(final @NotNull FileLocation location, final boolean isDirectory) {
        return new FailureReason(FailureKind.NoSuchFile, location, isDirectory ? "No such directory." : "No such file.");
    }

    @Deprecated
    public static @NotNull FailureReason others(final @NotNull FileLocation location, final @Nullable String message) {
        return new FailureReason(FailureKind.Others, location, message);
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, this.kind.name());
        this.location.dump(buffer);
        ByteBufIOUtil.writeUTF(buffer, this.message);
        return buffer;
    }

    protected final @NotNull FailureKind kind;
    protected final @NotNull FileLocation location;
    protected final @NotNull String message;
    protected final @NotNull Exception exception;

    protected FailureReason(final @NotNull FailureKind kind, final @NotNull FileLocation location, final @Nullable String message) {
        super();
        this.kind = kind;
        this.location = location;
        this.message = Objects.requireNonNullElse(message, "");
        this.exception = new Exception(kind.description() + this.message);
    }

    public @NotNull FailureKind kind() {
        return this.kind;
    }

    public @NotNull FileLocation location() {
        return this.location;
    }

    public @NotNull String message() {
        return this.message;
    }

    public @NotNull Exception exception() {
        return this.exception;
    }

    @Override
    public @NotNull String toString() {
        return "FailureReason{" +
                "kind=" + this.kind +
                ", location=" + this.location +
                ", exception=" + this.exception +
                '}';
    }
}
