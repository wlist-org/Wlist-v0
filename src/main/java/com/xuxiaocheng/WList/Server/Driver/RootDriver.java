package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileLocation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Map;

public final class RootDriver implements DriverInterface<RootDriver.RootDriverConfiguration> {
    private static final RootDriver instance = new RootDriver();
    public static RootDriver getInstance() {
        return RootDriver.instance;
    }

    private static @NotNull FileSqlInformation getDriverInformation(final @NotNull DriverConfiguration<?, ?, ?> configuration) {
        // TODO create and modified time.
        return new FileSqlInformation(new FileLocation(FileLocation.SpecialDriverName.RootDriver.getIdentify(), configuration.getName().hashCode()),
                0, configuration.getName(), true, 0, null, null, "", null);
    }

    private @NotNull RootDriverConfiguration configuration = new RootDriverConfiguration();

    @Override
    public @NotNull RootDriverConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull RootDriverConfiguration configuration) {
        // TODO get root user config.
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() {
        throw new UnsupportedOperationException("Root Driver is the core driver of WList. Cannot be uninitialized.");
    }

    @Deprecated
    @Override
    public void buildCache() throws Exception {
        for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
            driver.getSecond().buildCache();
    }

    @Deprecated
    @Override
    public void buildIndex() throws Exception {
        for (final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver: DriverManager.getAll().values())
            driver.getSecond().buildIndex();
    }

    public boolean buildIndex(final @NotNull String name) throws Exception {
        final DriverInterface<?> driver = DriverManager.get(name);
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
        final DriverInterface<?> driver = DriverManager.get(location.driver());
        if (driver != null)
            driver.forceRefreshDirectory(location);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        if (FileLocation.SpecialDriverName.RootDriver.getIdentify().equals(location.driver())) {
            // TODO list root drivers in page.
            final Map<String, Pair.ImmutablePair<WebDriversType, DriverInterface<?>>> map = DriverManager.getAll();
            return Pair.ImmutablePair.makeImmutablePair((long) map.size(), map.values().stream()
                    .map(k -> RootDriver.getDriverInformation(k.getSecond().getConfiguration())).toList());
        }
        final DriverInterface<?> real = DriverManager.get(location.driver());
        if (real == null)
            return null;
        return real.list(location, limit, page, policy, direction);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws Exception {
        if (FileLocation.SpecialDriverName.RootDriver.getIdentify().equals(location.driver()))
            return RootDriver.getDriverInformation(DriverManager.getById(location.id()).getSecond().getConfiguration());
        final DriverInterface<?> real = DriverManager.get(location.driver());
        if (real == null)
            return null;
        return real.info(location);
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
        if (FileLocation.SpecialDriverName.RootDriver.getIdentify().equals(location.driver()))
            return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        final DriverInterface<?> real = DriverManager.get(location.driver());
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        return real.download(location, from, to);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> mkdir(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final DriverInterface<?> real = DriverManager.get(parentLocation.driver());
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Creating directories.", parentLocation));
        return real.mkdir(parentLocation, directoryName, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final DriverInterface<?> real = DriverManager.get(parentLocation.driver());
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Uploading.", parentLocation));
//      TODO  HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Uploaded a file. information: ", f);
        return real.upload(parentLocation, filename, size, md5, policy);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(final @NotNull FileLocation location) throws Exception {
        if (FileLocation.SpecialDriverName.RootDriver.getIdentify().equals(location.driver())) {
            DriverManager.del(location.driver());
            return;
        }
        final DriverInterface<?> real = DriverManager.get(location.driver());
        if (real == null)
            return;
        real.delete(location);
    }

    public static class RootDriverConfiguration extends DriverConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
        public RootDriverConfiguration() {
            super("RootDriver", LocalSide::new, WebSide::new, CacheSide::new);
        }
        public static class LocalSide extends LocalSideDriverConfiguration {}
        public static class WebSide extends WebSideDriverConfiguration {}
        public static class CacheSide extends CacheSideDriverConfiguration {}
    }

    @Override
    public @NotNull  String toString() {
        return "RootDriver{" +
                "configuration=" + this.configuration +
                '}';
    }
}
