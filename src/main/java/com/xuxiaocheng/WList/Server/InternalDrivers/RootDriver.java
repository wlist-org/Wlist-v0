package com.xuxiaocheng.WList.Server.InternalDrivers;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Driver.SpecialDriverName;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

@SuppressWarnings("OverlyBroadThrowsClause")
public final class RootDriver implements DriverInterface<RootDriver.RootDriverConfiguration> {
    private static final RootDriver instance = new RootDriver();
    public static RootDriver getInstance() {
        return RootDriver.instance;
    }

    public static @NotNull FileSqlInformation getDriverInformation(final @NotNull DriverConfiguration<?, ?, ?> configuration) {
        return new FileSqlInformation(new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), configuration.getWebSide().getRootDirectoryId()),
                0, configuration.getName(), FileSqlInterface.FileSqlType.Directory, configuration.getWebSide().getSpaceUsed(),
                configuration.getLocalSide().getCreateTime(), configuration.getLocalSide().getUpdateTime(),
                configuration.getLocalSide().getDisplayName(), null);
    }

    public static @NotNull FileSqlInformation getDatabaseDriverInformation(final @NotNull DriverConfiguration<?, ?, ?> configuration) {
        return new FileSqlInformation(new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), configuration.getWebSide().getRootDirectoryId()),
                configuration.getWebSide().getRootDirectoryId(), configuration.getName(),
                FileSqlInterface.FileSqlType.Directory, configuration.getWebSide().getSpaceUsed(),
                configuration.getLocalSide().getCreateTime(), configuration.getLocalSide().getUpdateTime(), "", null);
    }

    private @NotNull RootDriverConfiguration configuration = new RootDriverConfiguration();

    @Override
    public @NotNull RootDriverConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull RootDriverConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() {
        throw new UnsupportedOperationException("Root Driver is the core driver of WList. Cannot be uninitialized.");
    }

    @Deprecated
    @Override
    public void buildCache() throws Exception {
        final Map<String, Exception> exceptions = DriverManager.operateAllDrivers(d -> {
            d.buildCache();
            DriverManager.dumpConfigurationIfModified(d.getConfiguration());
        });
        if (!exceptions.isEmpty()) {
            final Exception exception = new Exception("Failed to build cache." + ParametersMap.create().add("names", exceptions.keySet()));
            exceptions.values().forEach(exception::addSuppressed);
            throw exception;
        }
    }

    @Deprecated
    @Override
    public void buildIndex() throws Exception {
        final Map<String, Exception> exceptions = DriverManager.operateAllDrivers(d -> {
            d.buildIndex();
            DriverManager.dumpConfigurationIfModified(d.getConfiguration());
        });
        if (!exceptions.isEmpty()) {
            final Exception exception = new Exception("Failed to build index." + ParametersMap.create().add("names", exceptions.keySet()));
            exceptions.values().forEach(exception::addSuppressed);
            throw exception;
        }
    }

    public boolean buildIndex(final @NotNull String name) throws Exception {
        final DriverInterface<?> driver = DriverManager.getDriver(name);
        if (driver != null) {
            // TODO Background task.
            // TODO build time interval and force control.
            driver.buildIndex();
            final DriverTrashInterface<?> trash = DriverManager.getTrash(name);
            if (trash != null)
                trash.buildIndex();
        }
        return driver != null;
    }

    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws Exception {
        final DriverInterface<?> driver = DriverManager.getDriver(location.driver());
        if (driver != null) {
            driver.forceRefreshDirectory(location);
            DriverManager.dumpConfigurationIfModified(driver.getConfiguration());
        }
    }

    @Override
    public Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final Options.@NotNull DirectoriesOrFiles filter, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        if (SpecialDriverName.RootDriver.getIdentifier().equals(location.driver())) {
            if (filter == Options.DirectoriesOrFiles.OnlyFiles)
                return Triad.ImmutableTriad.makeImmutableTriad((long) DriverManager.getDriverCount(), 0L, List.of());
            final Comparator<DriverConfiguration<?, ?, ?>> comparator = switch (policy) {
                case FileName -> Comparator.comparing((DriverConfiguration<?, ?, ?> a) -> a.getLocalSide().getDisplayName());
                case Size -> Comparator.comparing((DriverConfiguration<?, ?, ?> a) -> a.getWebSide().getSpaceUsed(), Long::compareUnsigned);
                case CreateTime -> Comparator.comparing((DriverConfiguration<?, ?, ?> a) -> a.getLocalSide().getCreateTime());
                case UpdateTime -> Comparator.comparing((DriverConfiguration<?, ?, ?> a) -> a.getLocalSide().getUpdateTime());
            };
            final SortedSet<DriverConfiguration<?, ?, ?>> all = new ConcurrentSkipListSet<>(switch (direction) {
                case ASCEND -> comparator; case DESCEND -> comparator.reversed();
            });
            DriverManager.operateAllDrivers(d -> all.add(d.getConfiguration()));
            final Iterator<DriverConfiguration<?, ?, ?>> iterator = all.stream().skip((long) limit * page).iterator();
            final List<FileSqlInformation> list = new ArrayList<>(limit);
            while (list.size() < limit && iterator.hasNext())
                list.add(RootDriver.getDriverInformation(iterator.next()));
            return Triad.ImmutableTriad.makeImmutableTriad((long) all.size(), (long) all.size(), list);
        }
        final DriverInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return null;
        final Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list = real.list(location, filter, limit, page, policy, direction);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        return list;
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws Exception {
        if (SpecialDriverName.RootDriver.getIdentifier().equals(location.driver()))
            throw new UnsupportedOperationException("Cannot get root info.");
        final DriverInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return null;
        final FileSqlInformation info = real.info(location);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        return info;
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
        final DriverInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        final UnionPair<DownloadMethods, FailureReason> methods = real.download(location, from, to);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        return methods;
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (SpecialDriverName.RootDriver.getIdentifier().equals(parentLocation.driver()))
            throw new UnsupportedOperationException("Cannot create root directory.");
        final DriverInterface<?> real = DriverManager.getDriver(parentLocation.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Creating directories.", parentLocation));
        final UnionPair<FileSqlInformation, FailureReason> directory = real.createDirectory(parentLocation, directoryName, policy);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        return directory;
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (SpecialDriverName.RootDriver.getIdentifier().equals(parentLocation.driver()))
            throw new UnsupportedOperationException("Cannot create root file.");
        final DriverInterface<?> real = DriverManager.getDriver(parentLocation.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Uploading file.", parentLocation));
        if (size > real.getConfiguration().getWebSide().getMaxSizePerFile())
            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading file.", size, real.getConfiguration().getWebSide().getMaxSizePerFile(), parentLocation, filename));
        final UnionPair<UploadMethods, FailureReason> methods = real.upload(parentLocation, filename, size, md5, policy);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        return methods;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(final @NotNull FileLocation location) throws Exception {
        if (SpecialDriverName.RootDriver.getIdentifier().equals(location.driver()))
            throw new UnsupportedOperationException("Cannot delete root driver.");
        final DriverInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return;
        real.delete(location);
        DriverManager.dumpConfigurationIfModified(real.getConfiguration());
    }

    protected static class RootDriverConfiguration extends DriverConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
        private RootDriverConfiguration() {
            super("RootDriver", LocalSide::new, WebSide::new, CacheSide::new);
        }
        private static class LocalSide extends LocalSideDriverConfiguration {
            protected LocalSide() {
                super();
            }
        }
        private static class WebSide extends WebSideDriverConfiguration {
            protected WebSide() {
                super();
            }
        }
        private static class CacheSide extends CacheSideDriverConfiguration {
            protected CacheSide() {
                super();
            }
        }
    }

    @Override
    public @NotNull String toString() {
        return "RootDriver{" +
                "configuration=" + this.configuration +
                '}';
    }
}
