package com.xuxiaocheng.WList.Driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DrivePath {
    protected final @NotNull String path;

    public DrivePath(final @Nullable String from) {
        super();
        String path = Objects.requireNonNullElse(from, "/").trim();
        while (path.contains("\\"))
            path = path.replace('\\', '/');
        while (path.contains("//"))
            path = path.replace("//", "/");
        this.path = path.isEmpty() ? "/" : path;
    }

    public DrivePath(final @NotNull DrivePath from, final @NotNull String child) {
        this(from.path + '/' + child);
    }

    public @NotNull String getPath() {
        return this.path;
    }

    public @NotNull DrivePath getParent() {
        return new DrivePath(this.path.substring(0, this.path.lastIndexOf('/')));
    }

    public @NotNull String getName() {
        return this.path.substring(this.path.lastIndexOf('/') + 1);
    }

    @Override
    public @NotNull String toString() {
        return "DrivePath{" +
                "path='" + this.path + '\'' +
                '}';
    }
}
