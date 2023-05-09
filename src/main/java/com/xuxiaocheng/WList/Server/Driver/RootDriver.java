package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;

public class RootDriver implements DriverInterface<RootDriver.RootDriverConfiguration> {
    protected static final RootDriver instance = new RootDriver();
    public static RootDriver getInstance() {
        return RootDriver.instance;
    }

    @Override
    public @NotNull Class<RootDriverConfiguration> getDefaultConfigurationClass() {
        return RootDriverConfiguration.class;
    }

    @Override
    public void login(final @Nullable RootDriverConfiguration configuration) {
        DriverManager.init();
        // TODO get root user config.
    }

    @Override
    public void deleteDriver() throws IllegalAccessException {
        throw new IllegalAccessException("Root Driver is the core driver of WList. Shouldn't delete.");
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> list(@NotNull final DrivePath path, final int limit, final int page, @Nullable final OrderDirection direction, @Nullable final OrderPolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.list(path.removedRoot(), limit, page, direction, policy);
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable FileInformation info(@NotNull final DrivePath path) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.info(path.removedRoot());
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(@NotNull final DrivePath path, final long from, final long to) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.download(path.removedRoot(), from, to);
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable FileInformation mkdirs(@NotNull final DrivePath path) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.mkdirs(path.removedRoot());
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable FileInformation upload(@NotNull final DrivePath path, @NotNull final InputStream stream, @NotNull final String tag, @NotNull final List<@NotNull String> partTags) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.upload(path.removedRoot(), stream, tag, partTags);
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public void delete(@NotNull final DrivePath path) throws Exception {
        if (path.getDepth() < 2) {
            DriverManager.del(path.getRoot());
            return;
        }
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return;
        try {
            real.delete(path.removedRoot());
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable FileInformation copy(@NotNull final DrivePath source, @NotNull final DrivePath target) throws Exception {
        if (source.getRoot().equals(target.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(source.getRoot());
            if (real == null)
                return null;
            return real.copy(source.getRemovedRoot(), target.getRemovedRoot());
        }
        throw new UnsupportedOperationException("Copy");
//        return DriverInterface.super.copy(source, target);
    }

    @Override
    public @Nullable FileInformation move(@NotNull final DrivePath sourceFile, @NotNull final DrivePath targetDirectory) throws Exception {
        if (sourceFile.getRoot().equals(targetDirectory.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(sourceFile.getRoot());
            if (real == null)
                return null;
            return real.copy(sourceFile.getRemovedRoot(), targetDirectory.getRemovedRoot());
        }
        throw new UnsupportedOperationException("Move");
//        return DriverInterface.super.move(sourceFile, targetDirectory);
    }

    @Override
    public @Nullable FileInformation rename(@NotNull final DrivePath source, @NotNull final String name) throws Exception {
        final String root = source.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.rename(source.removedRoot(), name);
        } finally {
            source.addRoot(root);
        }
    }

    @Override
    public void buildCache() throws Exception {
//        DriverInterface.super.buildCache();
    }

    public static class RootDriverConfiguration extends DriverConfiguration<RootDriverConfiguration.L, RootDriverConfiguration.W, RootDriverConfiguration.C> {
        protected RootDriverConfiguration() {
            super(L::new, W::new, C::new);
        }
        public static class L extends LocalSideDriverConfiguration {}
        public static class W extends WebSideDriverConfiguration {}
        public static class C extends CacheSideDriverConfiguration {}
    }
}
