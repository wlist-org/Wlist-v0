package com.xuxiaocheng.WList.Server.Storage.Selectors;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

/**
 * @see ProviderInterface
 */
@SuppressWarnings("OverlyBroadCatchBlock")
public final class RootSelector {
    private RootSelector() {
        super();
    }

    private static @NotNull FileInformation getProviderInformation(final @NotNull StorageConfiguration configuration) {
        return new FileInformation(configuration.getRootDirectoryId(), 0, configuration.getName(), true, configuration.getSpaceUsed(),
                configuration.getCreateTime(), configuration.getUpdateTime(), configuration.getDisplayName());
    }

    private static <T> @NotNull Consumer<T> dumper(final StorageConfiguration configuration) {
        return t -> {
            try {
                StorageManager.dumpConfigurationIfModified(configuration);
            } catch (final IOException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
        };
    }

    public static void list(final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit, final @NotNull Consumer<@NotNull UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(directory.storage())) {
                if (filter == Options.FilterPolicy.OnlyFiles) {
                    consumer.accept(UnionPair.ok(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), 0L, List.of()))));
                    return;
                }
                final Set<StorageConfiguration> all = new ConcurrentSkipListSet<>((a, b) -> {
                    for (final Map.Entry<VisibleFileInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
                        final Comparator<StorageConfiguration> comparator = switch (order.getKey()) {
                            case Id, Name -> Comparator.comparing(StorageConfiguration::getDisplayName);
                            case Directory -> (m, n) -> 0;
                            case Size -> Comparator.comparing(StorageConfiguration::getSpaceUsed, Long::compareUnsigned);
                            case CreateTime -> Comparator.comparing(StorageConfiguration::getCreateTime);
                            case UpdateTime -> Comparator.comparing(StorageConfiguration::getUpdateTime);
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
                final List<FileInformation> list = AndroidSupporter.streamToList(all.stream().skip(position).limit(limit).map(RootSelector::getProviderInformation));
                consumer.accept(UnionPair.ok(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), StorageManager.getProvidersCount(), list))));
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.ok(FilesListInformation.Empty)));
                return;
            }
            real.list(directory.id(), filter, orders, position, limit, consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void info(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<Pair.ImmutablePair<@NotNull FileInformation, @NotNull Boolean>, Boolean>, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage()))
                throw new UnsupportedOperationException("Cannot get root information.");
            final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(Boolean.FALSE)));
                return;
            }
            real.info(location.id(), isDirectory, consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void refresh(final @NotNull FileLocation directory, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<Pair.ImmutablePair<@NotNull Set<Long>, @NotNull Set<Long>>, Boolean>, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(directory.storage())) {
                consumer.accept(ProviderInterface.RefreshNotAvailable);
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.RefreshNotExisted);
                return;
            }
            real.refreshDirectory(directory.id(), consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void trash(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) {
        try {
            if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage())) {
                consumer.accept(ProviderInterface.TrashNotAvailable);
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.TrashNotAvailable);
                return;
            }
            real.trash(location.id(), isDirectory, consumer.andThen(RootSelector.dumper(real.getConfiguration())));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void download(final @NotNull FileLocation file, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(file.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(file, false))));
                return;
            }
            real.downloadFile(file.id(), from, to, consumer.andThen(RootSelector.dumper(real.getConfiguration())), file);
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void create(final @NotNull FileLocation parent, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(parent.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parent, true))));
                return;
            }
            real.createDirectory(parent.id(), directoryName, policy, consumer.andThen(RootSelector.dumper(real.getConfiguration())), parent);
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    public static void uploadFile(final @NotNull FileLocation parent, final @NotNull String filename, final @LongRange(minimum = 0) long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(parent.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parent, true))));
                return;
            }
            real.uploadFile(parent.id(), filename, size, policy, consumer.andThen(RootSelector.dumper(real.getConfiguration())), parent);
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
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
}
