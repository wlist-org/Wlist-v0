package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

// TODO Duplicate Policy Control
public class RootDriver implements DriverInterface<RootDriver.RootDriverConfiguration> {
    protected static final RootDriver instance = new RootDriver();
    public static RootDriver getInstance() {
        return RootDriver.instance;
    }

    @Override
    public void initialize(final @Nullable RootDriverConfiguration configuration) {
        DriverManager.init();
        // TODO get root user config.
    }

    @Override
    public void uninitialize() throws IllegalAccessException {
        throw new IllegalAccessException("Root Driver is the core driver of WList. Shouldn't delete.");
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(@NotNull final DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            path.removedRoot();
            return real.list(path, limit, page, policy, direction);
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable FileSqlInformation info(@NotNull final DrivePath path) throws Exception {
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
    public @Nullable FileSqlInformation mkdirs(@NotNull final DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.mkdirs(path.removedRoot(), policy);
        } finally {
            path.addRoot(root);
        }
    }

    @Override
    public @Nullable UploadMethods upload(final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            final UploadMethods raw = real.upload(path.removedRoot(), size, md5, policy);
            if (raw == null)
                return null;
            return new UploadMethods(raw.methods(), raw.supplier().transfer(f -> {
                if (f != null) {
                    HLog.DefaultLogger.log("", f);
                    // TODO
                }
                return f;
            }), raw.finisher());
        } finally {
            path.addRoot(root);
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
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
    public @Nullable FileSqlInformation copy(@NotNull final DrivePath source, @NotNull final DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (source.getRoot().equals(target.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(source.getRoot());
            if (real == null)
                return null;
            return real.copy(source.getRemovedRoot(), target.getRemovedRoot(), policy);
        }
        return DriverInterface.super.copy(source, target, policy);
    }

    @Override
    public @Nullable FileSqlInformation move(@NotNull final DrivePath sourceFile, @NotNull final DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (sourceFile.getRoot().equals(targetDirectory.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(sourceFile.getRoot());
            if (real == null)
                return null;
            return real.move(sourceFile.getRemovedRoot(), targetDirectory.getRemovedRoot(), policy);
        }
        return DriverInterface.super.move(sourceFile, targetDirectory, policy);
    }

    @Override
    public @Nullable FileSqlInformation rename(@NotNull final DrivePath source, @NotNull final String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final String root = source.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.rename(source.removedRoot(), name, policy);
        } finally {
            source.addRoot(root);
        }
    }

    @Override
    public void buildCache() throws Exception {
        for (final Map.Entry<String, Pair.ImmutablePair<WebDriversType, DriverInterface<?>>> driver: DriverManager.getAll().entrySet())
            driver.getValue().getSecond().buildCache();
    }

    @Override
    public void buildIndex() throws Exception {
        for (final Map.Entry<String, Pair.ImmutablePair<WebDriversType, DriverInterface<?>>> driver: DriverManager.getAll().entrySet())
            driver.getValue().getSecond().buildIndex();
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
