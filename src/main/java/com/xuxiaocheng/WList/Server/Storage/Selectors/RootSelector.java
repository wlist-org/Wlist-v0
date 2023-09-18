package com.xuxiaocheng.WList.Server.Storage.Selectors;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AStreams;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

@SuppressWarnings("OverlyBroadThrowsClause")
public final class RootSelector {
    private RootSelector() {
        super();
    }

    public static @NotNull FileInformation getProviderInformation(final @NotNull ProviderConfiguration configuration) {
        return new FileInformation(configuration.getRootDirectoryId(), 0, configuration.getName(), true, configuration.getSpaceUsed(),
                configuration.getCreateTime(), configuration.getUpdateTime(), configuration.getDisplayName());
    }

    private static <T> @NotNull Consumer<T> dumper(final ProviderConfiguration configuration) {
        return t -> {
            try {
                StorageManager.dumpConfigurationIfModified(configuration);
            } catch (final IOException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
        };
    }

    public static void list(final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit, final @NotNull Consumer<@Nullable UnionPair<FilesListInformation, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(directory.storage())) {
                if (filter == Options.FilterPolicy.OnlyFiles) {
                    consumer.accept(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), 0L, List.of())));
                    return;
                }
                final Set<ProviderConfiguration> all = new ConcurrentSkipListSet<>((a, b) -> {
                    for (final Map.Entry<VisibleFileInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
                        final Comparator<ProviderConfiguration> comparator = switch (order.getKey()) {
                            case Id, Name -> Comparator.comparing(ProviderConfiguration::getDisplayName);
                            case Directory -> (m, n) -> 0;
                            case Size -> Comparator.comparing(ProviderConfiguration::getSpaceUsed, Long::compareUnsigned);
                            case CreateTime -> Comparator.comparing(ProviderConfiguration::getCreateTime);
                            case UpdateTime -> Comparator.comparing(ProviderConfiguration::getUpdateTime);
                        };
                        final int res = (switch (order.getValue()) {
                            case ASCEND -> comparator;
                            case DESCEND -> comparator.reversed();
                        }).compare(a, b);
                        if (res != 0)
                            return res;
                    }
                    return 0;
                });
                StorageManager.getAllProviders().values().forEach(p -> all.add(p.getConfiguration()));
                final List<FileInformation> list = AStreams.streamToList(all.stream().skip(position).limit(limit).map(RootSelector::getProviderInformation));
                consumer.accept(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), StorageManager.getProvidersCount(), list)));
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(FilesListInformation.Empty));
                return;
            }
            real.list(directory.id(), filter, orders, position, limit, consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void refreshDirectory(final @NotNull FileLocation directory, final Consumer<? super @Nullable UnionPair<Boolean, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(directory.storage())) {
                consumer.accept(UnionPair.ok(Boolean.TRUE));
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(Boolean.FALSE));
                return;
            }
            real.refreshDirectory(directory.id(), consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static @Nullable FileInformation info(final @NotNull FileLocation location, final boolean isDirectory) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage()))
            return null; // TODO: Root information.
        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
        if (real == null)
            return null;
        final FileInformation info;
        try {
            info = real.info(location.id(), isDirectory);
        } finally {
            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return info;
    }

    public static boolean delete(final @NotNull FileLocation location, final boolean isDirectory) throws Exception {
        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage()))
            return false;
        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
        if (real == null)
            return false;
        final boolean res;
        try {
            res = real.delete(location.id(), isDirectory);
        } finally {
            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return res;
    }

    public static @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final ProviderInterface<?> real = StorageManager.getProvider(parentLocation.storage());
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile(parentLocation));
        final UnionPair<FileInformation, FailureReason> directory;
        try {
            directory = real.createDirectory(parentLocation.id(), directoryName, policy, parentLocation);
        } finally {
            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return directory;
    }

    public static @NotNull UnionPair<DownloadRequirements, FailureReason> download(final @NotNull FileLocation file, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
        final ProviderInterface<?> real = StorageManager.getProvider(file.storage());
        if (real == null)
            return UnionPair.fail(FailureReason.byNoSuchFile(file));
        final UnionPair<DownloadRequirements, FailureReason> methods;
        try {
            methods = real.download(file.id(), from, to, file);
        } finally {
            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
        }
        return methods;
    }

//    @Override
//    public @NotNull UnionPair<UploadMethods, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(parentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot create root file.");
//        final ProviderInterface<?> real = StorageManager.getProvider(parentLocation.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Uploading.", parentLocation));
//        if (size > real.getConfiguration().getMaxSizePerFile())
//            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, real.getConfiguration().getMaxSizePerFile(), parentLocation, filename));
//        final UnionPair<UploadMethods, FailureReason> methods;
//        try {
//            methods = real.upload(parentLocation, filename, size, md5, policy);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return methods;
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot copy from root driver.");
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot copy to root driver.");
//        if (sourceLocation.storage().equals(targetParentLocation.storage())) {
//            final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
//            try {
//                return real.copy(sourceLocation, targetParentLocation, targetFilename, policy);
//            } finally {
//                StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//            }
//        }
//        return ProviderInterface.super.copy(sourceLocation, targetParentLocation, targetFilename, policy);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot move from root driver.");
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot move to root driver.");
//        if (sourceLocation.storage().equals(targetParentLocation.storage())) {
//            final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
//            try {
//                return real.move(sourceLocation, targetParentLocation, policy);
//            } finally {
//                StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//            }
//        }
//        return ProviderInterface.super.move(sourceLocation, targetParentLocation, policy);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot rename root driver.");
//        final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
//        try {
//            return real.rename(sourceLocation, name, policy);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//    }
//
//    protected static class RootDriverConfiguration extends ProviderConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
//        private RootDriverConfiguration() {
//            super("RootSelector", LocalSide::new, WebSide::new, CacheSide::new);
//        }
//        private static class LocalSide extends LocalSideDriverConfiguration {
//            protected LocalSide() {
//                super();
//            }
//        }
//        private static class WebSide extends WebSideDriverConfiguration {
//            protected WebSide() {
//                super();
//            }
//        }
//        private static class CacheSide extends CacheSideDriverConfiguration {
//            protected CacheSide() {
//                super();
//            }
//        }
//    }
//
//    @Override
//    public @NotNull String toString() {
//        return "RootSelector{" +
//                "configuration=" + this.configuration +
//                '}';
//    }
}
