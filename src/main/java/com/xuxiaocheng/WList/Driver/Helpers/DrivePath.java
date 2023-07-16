package com.xuxiaocheng.WList.Driver.Helpers;

import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class DrivePath implements Iterable<String> {
    protected static final Pattern Separator = Pattern.compile("[\\\\/]");
    protected static @NotNull @UnmodifiableView List<@NotNull String> split(final @Nullable String path) {
        if (path == null)
            return List.of();
        final Stream<String> stream = Stream.of(DrivePath.Separator.split(path.replace('\n', '\t')))
                .filter(Predicate.not(String::isEmpty));
        return PlatformDependent.isAndroid() ? stream.collect(Collectors.toList()) : stream.toList();
    }

    protected final @NotNull List<String> path;
    protected @Nullable String pathCache;
    protected @Nullable String parentPathCache;

    public DrivePath(final @Nullable String path) {
        this(DrivePath.split(path));
    }

    public DrivePath(final @NotNull DrivePath path) {
        this(path.path);
        this.pathCache = path.pathCache;
        this.parentPathCache = path.parentPathCache;
    }

    public DrivePath(final @Nullable String path, final @NotNull DrivePath child) {
        this(DrivePath.split(path));
        this.path.addAll(child.path);
    }

    public DrivePath(final @NotNull DrivePath root, final @Nullable String path) {
        this(DrivePath.split(path));
        this.path.addAll(0, root.path);
    }

    protected DrivePath(final @NotNull Collection<String> path) {
        super();
        this.path = new LinkedList<>(path);
    }

    public @NotNull DrivePath parent() {
        if (!this.path.isEmpty())
            this.path.remove(this.path.size() - 1);
        this.pathCache = this.parentPathCache;
        this.parentPathCache = null;
        return this;
    }

    public @NotNull DrivePath getParent() {
        final DrivePath parent = new DrivePath(this.path).parent();
        parent.pathCache = this.parentPathCache;
        return parent;
    }

    public @NotNull DrivePath child(final @NotNull String child) {
        final List<String> children = DrivePath.split(child);
        this.path.addAll(children);
        if (children.size() == 1) {
            this.parentPathCache = this.pathCache;
            if (this.pathCache != null)
                this.pathCache += '/' + children.get(0);
        } else if (!children.isEmpty()) {
            this.pathCache = null;
            this.parentPathCache = null;
        }
        return this;
    }

    public @NotNull DrivePath getChild(final @NotNull String child) {
        return new DrivePath(this.path).child(child);
    }

    public @NotNull DrivePath removedRoot() {
        if (this.path.isEmpty())
            return this;
        this.path.remove(0);
        this.pathCache = null;
        this.parentPathCache = null;
        return this;
    }

    public @NotNull DrivePath getRemovedRoot() {
        return new DrivePath(this.path).removedRoot();
    }

    public @NotNull DrivePath addedRoot(final @NotNull String root) {
        this.path.addAll(0, DrivePath.split(root));
        this.pathCache = null;
        this.parentPathCache = null;
        return this;
    }

    public @NotNull DrivePath getAddedRoot(final @NotNull String root) {
        return new DrivePath(this.path).addedRoot(root);
    }

    public @NotNull DrivePath addedRoot(final @NotNull DrivePath root) {
        this.path.addAll(0, root.path);
        this.pathCache = null;
        this.parentPathCache = null;
        return this;
    }

    public @NotNull DrivePath getAddedRoot(final @NotNull DrivePath root) {
        return new DrivePath(this.path).addedRoot(root);
    }

    public @NotNull DrivePath replace(final int index, final @NotNull String part) {
        this.path.remove(index);
        this.path.addAll(index, DrivePath.split(part));
        this.pathCache = null;
        this.parentPathCache = null;
        return this;
    }

    public @NotNull DrivePath getReplace(final int index, final @NotNull String part) {
        return new DrivePath(this.path).replace(index, part);
    }

    public @NotNull String getPath() {
        if (this.pathCache != null)
            return this.pathCache;
        if (this.path.isEmpty()) {
            this.pathCache = "";
            this.parentPathCache = "";
            return this.pathCache;
        }
        final StringBuilder builder = new StringBuilder();
        for (final String p: this.path)
            builder.append('/').append(p);
        this.pathCache = builder.toString();
        return builder.toString();
    }

    public @NotNull String getParentPath() {
        if (this.parentPathCache != null)
            return this.parentPathCache;
        if (this.path.size() < 2) {
            this.parentPathCache = "";
            return this.parentPathCache;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.path.size() - 1; ++i)
            builder.append('/').append(this.path.get(i));
        this.parentPathCache = builder.toString();
        if (this.pathCache == null)
            this.pathCache = builder.append('/').append(this.path.get(this.path.size() - 1)).toString();
        return this.parentPathCache;
    }

    public @NotNull String getChildPath(final @NotNull String child) {
        final StringBuilder builder = new StringBuilder(this.getParentPath());
        for (final String p: DrivePath.split(child))
            builder.append('/').append(p);
        return builder.toString();
    }

    public @NotNull String getRoot() {
        if (this.path.isEmpty())
            return "";
        return this.path.get(0);
    }

    public @NotNull String getName() {
        if (this.path.isEmpty())
            return "";
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
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DrivePath that)) return false;
        return this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.path);
    }

    @Override
    public @NotNull String toString() {
        return "DrivePath(" + this.getPath() + ')';
    }
}
