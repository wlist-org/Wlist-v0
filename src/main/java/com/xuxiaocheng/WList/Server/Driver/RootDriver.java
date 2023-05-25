package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(@NotNull final DrivePath path, final int limit, final int page, @Nullable final OrderDirection direction, @Nullable final OrderPolicy policy) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            path.removedRoot();
            return real.list(path, limit, page, direction, policy);
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
    public @Nullable FileSqlInformation mkdirs(@NotNull final DrivePath path) throws Exception {
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
    public @Nullable UploadMethods upload(final @NotNull DrivePath path, final long size, final @NotNull String tag) throws Exception {
        final String root = path.getRoot();
        final DriverInterface<?> real = DriverManager.get(root);
        if (real == null)
            return null;
        try {
            return real.upload(path.removedRoot(), size, tag);
        } finally {
            path.addRoot(root);
        }
    }

    public void completeUpload(final @NotNull FileSqlInformation information) {
        HLog.DefaultLogger.log("", information);
        // TODO
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

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public @Nullable FileSqlInformation copy(@NotNull final DrivePath source, @NotNull final DrivePath target) throws Exception {
        if (source.getRoot().equals(target.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(source.getRoot());
            if (real == null)
                return null;
            return real.copy(source.getRemovedRoot(), target.getRemovedRoot());
        }
        final Pair.ImmutablePair<InputStream, Long> url = this.download(source, 0, Long.MAX_VALUE);
        final FileSqlInformation info = this.info(source);
        if (url == null || info == null)
            return null;
        assert info.size() == url.getSecond().longValue();
        final UploadMethods methods = this.upload(target, info.size(), info.md5());
        if (methods == null)
            return null;
        try {
            for (final UploadMethods.UploadPartMethod partMethod: methods.methods()) {
                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(partMethod.size(), partMethod.size());
                try {
                    buffer.writeBytes(url.getFirst(), partMethod.size());
                    partMethod.consumer().accept(buffer);
                } finally {
                    buffer.release();
                }
            }
            return methods.supplier().get();
        } finally {
            methods.finisher().run();
            url.getFirst().close();
        }
    }

    @Override
    public @Nullable FileSqlInformation move(@NotNull final DrivePath sourceFile, @NotNull final DrivePath targetDirectory) throws Exception {
        if (sourceFile.getRoot().equals(targetDirectory.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(sourceFile.getRoot());
            if (real == null)
                return null;
            return real.move(sourceFile.getRemovedRoot(), targetDirectory.getRemovedRoot());
        }
        return DriverInterface.super.move(sourceFile, targetDirectory);
    }

    @Override
    public @Nullable FileSqlInformation rename(@NotNull final DrivePath source, @NotNull final String name) throws Exception {
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
