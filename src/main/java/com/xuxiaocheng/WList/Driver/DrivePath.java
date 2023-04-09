package com.xuxiaocheng.WList.Driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrivePath implements Iterable<String> {
    protected static final Pattern Separator = Pattern.compile("[\\\\/]");
    protected static @NotNull List<String> split(final @Nullable CharSequence path) {
        if (path == null)
            return new ArrayList<>();
        return Stream.of(DrivePath.Separator.split(path))
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toList());
    }

    protected final @NotNull List<String> path;

    public DrivePath(final @Nullable CharSequence path) {
        this(DrivePath.split(path));
    }

    protected DrivePath(final @NotNull Collection<String> path) {
        super();
        this.path = new ArrayList<>(path);
    }

    public @NotNull DrivePath parent() {
        this.path.remove(this.path.size() - 1);
        return this;
    }

    public @NotNull DrivePath getParent() {
        return new DrivePath(this.path).parent();
    }

    public @NotNull DrivePath child(final @NotNull CharSequence child) {
        this.path.addAll(DrivePath.split(child));
        return this;
    }

    public @NotNull DrivePath getChild(final @NotNull CharSequence child) {
        return new DrivePath(this.path).child(child);
    }

    public @NotNull String getPath() {
        if (this.path.isEmpty())
            return "/";
        final StringBuilder builder = new StringBuilder();
        this.path.forEach(p -> builder.append('/').append(p));
        return builder.toString();
    }

    public @NotNull String getName() {
        return this.path.get(this.path.size() - 1);
    }

    public int getDepth() {
        return this.path.size();
    }

    @Override
    public @NotNull Iterator<String> iterator() {
        return this.path.iterator();
    }

    @Override
    public @NotNull String toString() {
        return "DrivePath{" +
                "path='" + this.path + '\'' +
                '}';
    }
}
