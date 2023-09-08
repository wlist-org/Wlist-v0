package com.xuxiaocheng.WList.Server.Storage.Selectors;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderTrashInterface;
import com.xuxiaocheng.WList.Server.Storage.FailureReason;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("OverlyBroadThrowsClause")
public final class RootSelector implements ProviderInterface<RootSelector.RootDriverConfiguration> {
    private static final RootSelector instance = new RootSelector();
    public static RootSelector getInstance() {
        return RootSelector.instance;
    }

    public static @NotNull FileInformation getDriverInformation(final @NotNull ProviderConfiguration<?, ?, ?> configuration) {
        return new FileInformation(new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), configuration.getWebSide().getRootDirectoryId()),
                0, configuration.getName(), FileSqlInterface.FileSqlType.Directory, configuration.getWebSide().getSpaceUsed(),
                configuration.getLocalSide().getCreateTime(), configuration.getLocalSide().getUpdateTime(),
                configuration.getLocalSide().getDisplayName(), null);
    }

    public static @NotNull FileInformation getDatabaseDriverInformation(final @NotNull ProviderConfiguration<?, ?, ?> configuration) {
        return new FileInformation(new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), configuration.getWebSide().getRootDirectoryId()),
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
            try {
                d.buildCache();
            } finally {
                DriverManager.dumpConfigurationIfModified(d.getConfiguration());
            }
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
            try {
                d.buildIndex();
            } finally {
                DriverManager.dumpConfigurationIfModified(d.getConfiguration());
            }
        });
        if (!exceptions.isEmpty()) {
            final Exception exception = new Exception("Failed to build index." + ParametersMap.create().add("names", exceptions.keySet()));
            exceptions.values().forEach(exception::addSuppressed);
            throw exception;
        }
    }

    public boolean buildIndex(final @NotNull String name) throws Exception {
        final ProviderInterface<?> driver = DriverManager.getDriver(name);
        if (driver != null) {
            final ProviderTrashInterface<?> trash = DriverManager.getTrash(name);
            try {
                final LocalDateTime old = driver.getConfiguration().getCacheSide().getLastFileIndexBuildTime();
                if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
                    driver.buildIndex();
            } finally {
                DriverManager.dumpConfigurationIfModified(driver.getConfiguration());
            }
            if (trash != null)
                try {
                    final LocalDateTime old = trash.getDriver().getConfiguration().getCacheSide().getLastTrashIndexBuildTime();
                    if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
                        trash.buildIndex();
                } finally {
                    DriverManager.dumpConfigurationIfModified(trash.getDriver().getConfiguration());
                }
        }
        return driver != null;
    }

    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws Exception {
        final ProviderInterface<?> driver = DriverManager.getDriver(location.driver());
        if (driver != null)
            try {
                driver.forceRefreshDirectory(location);
            } finally {
                DriverManager.dumpConfigurationIfModified(driver.getConfiguration());
            }
    }

    @Override
    public Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> list(final @NotNull FileLocation location, final Options.@NotNull DirectoriesOrFiles filter, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.driver())) {
            if (filter == Options.DirectoriesOrFiles.OnlyFiles)
                return Triad.ImmutableTriad.makeImmutableTriad((long) DriverManager.getDriverCount(), 0L, List.of());
            final Comparator<ProviderConfiguration<?, ?, ?>> comparator = switch (policy) {
                case FileName -> Comparator.comparing((ProviderConfiguration<?, ?, ?> a) -> a.getLocalSide().getDisplayName());
                case Size -> Comparator.comparing((ProviderConfiguration<?, ?, ?> a) -> a.getWebSide().getSpaceUsed(), Long::compareUnsigned);
                case CreateTime -> Comparator.comparing((ProviderConfiguration<?, ?, ?> a) -> a.getLocalSide().getCreateTime());
                case UpdateTime -> Comparator.comparing((ProviderConfiguration<?, ?, ?> a) -> a.getLocalSide().getUpdateTime());
            };
            final SortedSet<ProviderConfiguration<?, ?, ?>> all = new ConcurrentSkipListSet<>(switch (direction) {
                case ASCEND -> comparator; case DESCEND -> comparator.reversed();
            });
            DriverManager.operateAllDrivers(d -> all.add(d.getConfiguration()));
            final Iterator<ProviderConfiguration<?, ?, ?>> iterator = all.stream().skip((long) limit * page).iterator();
            final List<FileInformation> list = new ArrayList<>(limit);
            while (list.size() < limit && iterator.hasNext())
                list.add(RootSelector.getDriverInformation(iterator.next()));
            return Triad.ImmutableTriad.makeImmutableTriad((long) all.size(), (long) all.size(), list);
        }
        final ProviderInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return null;
        final Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
        try {
            list = real.list(location, filter, limit, page, policy, direction);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return list;
    }

    @Override
    public @Nullable FileInformation info(final @NotNull FileLocation location) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.driver()))
            throw new UnsupportedOperationException("Cannot get root info.");
        final ProviderInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return null;
        final FileInformation info;
        try {
            info = real.info(location);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return info;
    }

    @Override
    public @NotNull UnionPair<DownloadMethods, FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
        final ProviderInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        final UnionPair<DownloadMethods, FailureReason> methods;
        try {
            methods = real.download(location, from, to);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return methods;
    }

    @Override
    public @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(parentLocation.driver()))
            throw new UnsupportedOperationException("Cannot create root directory.");
        final ProviderInterface<?> real = DriverManager.getDriver(parentLocation.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Creating directories.", parentLocation));
        final UnionPair<FileInformation, FailureReason> directory;
        try {
            directory = real.createDirectory(parentLocation, directoryName, policy);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return directory;
    }

    @Override
    public @NotNull UnionPair<UploadMethods, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(parentLocation.driver()))
            throw new UnsupportedOperationException("Cannot create root file.");
        final ProviderInterface<?> real = DriverManager.getDriver(parentLocation.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Uploading.", parentLocation));
        if (size > real.getConfiguration().getWebSide().getMaxSizePerFile())
            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, real.getConfiguration().getWebSide().getMaxSizePerFile(), parentLocation, filename));
        final UnionPair<UploadMethods, FailureReason> methods;
        try {
            methods = real.upload(parentLocation, filename, size, md5, policy);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return methods;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(final @NotNull FileLocation location) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.driver()))
            throw new UnsupportedOperationException("Cannot delete root driver.");
        final ProviderInterface<?> real = DriverManager.getDriver(location.driver());
        if (real == null) return;
        try {
            real.delete(location);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
    }

    @Override
    public @NotNull UnionPair<FileInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.driver()))
            throw new UnsupportedOperationException("Cannot copy from root driver.");
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.driver()))
            throw new UnsupportedOperationException("Cannot copy to root driver.");
        if (sourceLocation.driver().equals(targetParentLocation.driver())) {
            final ProviderInterface<?> real = DriverManager.getDriver(sourceLocation.driver());
            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
            try {
                return real.copy(sourceLocation, targetParentLocation, targetFilename, policy);
            } finally {
                DriverManager.dumpConfigurationIfModified(real.getConfiguration());
            }
        }
        return ProviderInterface.super.copy(sourceLocation, targetParentLocation, targetFilename, policy);
    }

    @Override
    public @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.driver()))
            throw new UnsupportedOperationException("Cannot move from root driver.");
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.driver()))
            throw new UnsupportedOperationException("Cannot move to root driver.");
        if (sourceLocation.driver().equals(targetParentLocation.driver())) {
            final ProviderInterface<?> real = DriverManager.getDriver(sourceLocation.driver());
            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
            try {
                return real.move(sourceLocation, targetParentLocation, policy);
            } finally {
                DriverManager.dumpConfigurationIfModified(real.getConfiguration());
            }
        }
        return ProviderInterface.super.move(sourceLocation, targetParentLocation, policy);
    }

    @Override
    public @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.driver()))
            throw new UnsupportedOperationException("Cannot rename root driver.");
        final ProviderInterface<?> real = DriverManager.getDriver(sourceLocation.driver());
        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
        try {
            return real.rename(sourceLocation, name, policy);
        } finally {
            DriverManager.dumpConfigurationIfModified(real.getConfiguration());
        }
    }

    protected static class RootDriverConfiguration extends ProviderConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
        private RootDriverConfiguration() {
            super("RootSelector", LocalSide::new, WebSide::new, CacheSide::new);
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
        return "RootSelector{" +
                "configuration=" + this.configuration +
                '}';
    }
}
