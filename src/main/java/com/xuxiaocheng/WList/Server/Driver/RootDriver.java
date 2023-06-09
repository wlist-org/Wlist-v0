package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
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
import java.util.stream.Collectors;

public class RootDriver implements DriverInterface<RootDriver.RootDriverConfiguration> {
    protected static final RootDriver instance = new RootDriver();
    public static RootDriver getInstance() {
        return RootDriver.instance;
    }

    @Override
    public void initialize(final @Nullable RootDriverConfiguration configuration) {
//        DriverManager.init();
        // TODO get root user config.
    }

    @Override
    public void uninitialize() {
        throw new UnsupportedOperationException("Root Driver is the core driver of WList. Cannot be deleted.");
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(@NotNull final DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        if (path.getDepth() == 0) {
            // TODO list root drivers.
            final Map<String, Pair.ImmutablePair<WebDriversType, DriverInterface<?>>> map = DriverManager.getAll();
            return Pair.ImmutablePair.makeImmutablePair((long) map.size(), map.keySet().stream()
                    .map(k -> new FileSqlInformation(k.hashCode(), new DrivePath(k), true, 0, null, null, "", null))
                    .collect(Collectors.toList()));
        }
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            path.removedRoot();
            final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = real.list(path, limit, page, policy, direction);
            if (list == null)
                return null;
            list.getSecond().forEach(f -> f.path().addedRoot(root));
            return list;
        } finally {
            path.addedRoot(root);
        }
    }

    @Override
    public @Nullable FileSqlInformation info(@NotNull final DrivePath path) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            final FileSqlInformation information = real.info(path.removedRoot());
            if (information != null)
                information.path().addedRoot(root);
            return information;
        } finally {
            path.addedRoot(root);
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
            path.addedRoot(root);
        }
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> mkdirs(@NotNull final DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Creating directories.", path));
        try {
            final UnionPair<FileSqlInformation, FailureReason> dir = real.mkdirs(path.removedRoot(), policy);
            if (dir.isSuccess())
                dir.getT().path().addedRoot(root);
            return dir;
        } finally {
            path.addedRoot(root);
        }
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Uploading.", path));
        try {
            final UnionPair<UploadMethods, FailureReason> methods = real.upload(path.removedRoot(), size, md5, policy);
            if (methods.isFailure())
                return methods;
            return UnionPair.ok(new UploadMethods(methods.getT().methods(), methods.getT().supplier().transfer(f -> {
                if (f != null)
                    f.path().addedRoot(root);
                return f;
            }), methods.getT().finisher()));
        } finally {
            path.addedRoot(root);
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(@NotNull final DrivePath path) throws Exception {
        if (path.getDepth() < 1) {
            this.uninitialize();
            return;
        }
        final String root = path.getRoot();
        if (path.getDepth() == 1) {
            DriverManager.del(root);
            return;
        }
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return;
        try {
            real.delete(path.removedRoot());
        } finally {
            path.addedRoot(root);
        }
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(@NotNull final DrivePath source, @NotNull final DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (source.equals(target)) {
            final FileSqlInformation information = this.info(source);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
            return UnionPair.ok(information);
        }
        if (source.getRoot().equals(target.getRoot())) {
            final String root = source.getRoot();
            final DriverInterface<?> real = DriverManager.get(root);
            if (real == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
            try {
                final UnionPair<FileSqlInformation, FailureReason> information = real.copy(source.removedRoot(), target.removedRoot(), policy);
                if (information.isSuccess())
                    information.getT().path().addedRoot(root);
                return information;
            } finally {
                source.addedRoot(root);
                target.addedRoot(root);
            }
        }
        return DriverInterface.super.copy(source, target, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(@NotNull final DrivePath sourceFile, @NotNull final DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        try {
            if (sourceFile.equals(targetDirectory.child(sourceFile.getName()))) {
                final FileSqlInformation information = this.info(sourceFile);
                if (information == null)
                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceFile));
                return UnionPair.ok(information);
            }
        } finally {
            targetDirectory.parent();
        }
        if (sourceFile.getRoot().equals(targetDirectory.getRoot())) {
            final String root = sourceFile.getRoot();
            final DriverInterface<?> real = DriverManager.get(root);
            if (real == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceFile));
            try {
                final UnionPair<FileSqlInformation, FailureReason> information = real.move(sourceFile.removedRoot(), targetDirectory.removedRoot(), policy);
                if (information.isSuccess())
                    information.getT().path().addedRoot(root);
                return information;
            } finally {
                sourceFile.addedRoot(root);
                targetDirectory.addedRoot(root);
            }
        }
        return DriverInterface.super.move(sourceFile, targetDirectory, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(@NotNull final DrivePath source, @NotNull final String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (name.equals(source.getName())) {
            final FileSqlInformation information = this.info(source);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", source));
            return UnionPair.ok(information);
        }
        final String root = source.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", source));
        try {
            final UnionPair<FileSqlInformation, FailureReason> information = real.rename(source.removedRoot(), name, policy);
            if (information.isSuccess())
                information.getT().path().addedRoot(root);
            return information;
        } finally {
            source.addedRoot(root);
        }
    }

    @Override
    public void buildCache() throws Exception {
        for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
            driver.getSecond().buildCache();
    }

    @Override
    public void buildIndex() throws Exception {
        for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
            driver.getSecond().buildIndex();
    }

    public static class RootDriverConfiguration extends DriverConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
        protected RootDriverConfiguration() {
            super(LocalSide::new, WebSide::new, CacheSide::new);
        }
        public static class LocalSide extends LocalSideDriverConfiguration {}
        public static class WebSide extends WebSideDriverConfiguration {}
        public static class CacheSide extends CacheSideDriverConfiguration {}
    }
}
