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
import java.sql.SQLException;
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
    public void login(final @Nullable RootDriverConfiguration configuration) throws SQLException {
        DriverManager.init();
    }

    @Override
    public void deleteDriver() {
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> list(@NotNull final DrivePath path, final int limit, final int page, @Nullable final OrderDirection direction, @Nullable final OrderPolicy policy) throws Exception {
        final DriverInterface<?> real = DriverManager.get(path.getRoot());
        if (real == null)
            return null;
        return real.list(path.getRemovedRoot(), limit, page, direction, policy);
    }

    @Override
    public @Nullable FileInformation info(@NotNull final DrivePath path) throws Exception {
        final DriverInterface<?> real = DriverManager.get(path.getRoot());
        if (real == null)
            return null;
        return real.info(path.getRemovedRoot());
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(@NotNull final DrivePath path, final long from, final long to) throws Exception {
        final DriverInterface<?> real = DriverManager.get(path.getRoot());
        if (real == null)
            return null;
        return real.download(path.getRemovedRoot(), from, to);
    }

    @Override
    public @Nullable FileInformation mkdirs(@NotNull final DrivePath path) throws Exception {
        final DriverInterface<?> real = DriverManager.get(path.getRoot());
        if (real == null)
            return null;
        return real.mkdirs(path.getRemovedRoot());
    }

    @Override
    public @Nullable FileInformation upload(@NotNull final DrivePath path, @NotNull final InputStream stream, @NotNull final String tag, @NotNull final List<@NotNull String> partTags) throws Exception {
        return null;
    }

    @Override
    public void delete(@NotNull final DrivePath path) throws Exception {
        final DriverInterface<?> real = DriverManager.get(path.getRoot());
        if (real == null)
            return;
        if (path.getDepth() > 1)
            real.delete(path.getRemovedRoot());
    }

    @Override
    public @Nullable FileInformation copy(@NotNull final DrivePath source, @NotNull final DrivePath target) throws Exception {
        if (source.getRoot().equals(target.getRoot())) {
            final DriverInterface<?> real = DriverManager.get(source.getRoot());
            if (real == null)
                return null;
            return real.copy(source.getRemovedRoot(), target.getRemovedRoot());
        }
        return DriverInterface.super.copy(source, target);
    }

    @Override
    public @Nullable FileInformation move(@NotNull final DrivePath sourceFile, @NotNull final DrivePath targetDirectory) throws Exception {
        return DriverInterface.super.move(sourceFile, targetDirectory);
    }

    @Override
    public @Nullable FileInformation rename(@NotNull final DrivePath source, @NotNull final String name) throws Exception {
        return DriverInterface.super.rename(source, name);
    }

    @Override
    public void buildCache() throws Exception {
        DriverInterface.super.buildCache();
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
