package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
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
import com.xuxiaocheng.WList.Server.Storage.Records.RefreshRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static <T> @NotNull Consumer<T> wrapConsumer(final @NotNull Consumer<? super T> consumer, final @NotNull StorageConfiguration configuration) {
        return HExceptionWrapper.wrapConsumer(consumer::accept, () -> {
            try {
                StorageManager.dumpConfigurationIfModified(configuration);
            } catch (final IOException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
        });
    }


    /**
     * @see ProviderInterface#list(long, Options.FilterPolicy, LinkedHashMap, long, int, Consumer)
     */
    public static void list(final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FilesListInformation, RefreshRequirements>>, Throwable>> consumer) {
        try {
            if (IdentifierNames.RootSelector.equals(directory.storage())) {
                if (filter == Options.FilterPolicy.OnlyFiles) {
                    consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), 0L, List.of())))));
                    return;
                }
                Comparator<StorageConfiguration> comparators = null;
                for (final Map.Entry<VisibleFileInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
                    final Comparator<StorageConfiguration> select = switch (order.getKey()) {
                        case Id, Name -> Comparator.comparing(StorageConfiguration::getName);
                        case Directory -> (m, n) -> 0;
                        case Size -> Comparator.comparing(StorageConfiguration::getSpaceUsed, Long::compareUnsigned);
                        case CreateTime -> Comparator.comparing(StorageConfiguration::getCreateTime);
                        case UpdateTime -> Comparator.comparing(StorageConfiguration::getUpdateTime);
                    };
                    final Comparator<StorageConfiguration> real = (switch (order.getValue()) {
                        case ASCEND -> select;
                        case DESCEND -> select.reversed();
                    });
                    if (comparators == null)
                         comparators = real;
                    else
                        comparators = comparators.thenComparing(real);
                }
                final Comparator<StorageConfiguration> comparator = Objects.requireNonNullElse(comparators, Comparator.comparing(StorageConfiguration::getName));
                final Collection<StorageConfiguration> all = new ConcurrentSkipListSet<>(comparator);
                all.addAll(StorageManager.getAllConfigurations());
                final List<FileInformation> list = AndroidSupporter.streamToList(all.stream().skip(position).limit(limit)
                        .map(configuration -> new FileInformation(configuration.getRootDirectoryId(), 0, configuration.getName(), true,
                                configuration.getSpaceUsed(), configuration.getCreateTime(), configuration.getUpdateTime(), null)));
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(new FilesListInformation(StorageManager.getProvidersCount(), StorageManager.getProvidersCount(), list)))));
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.ListNotExisted);
                return;
            }
            real.list(directory.id(), filter, orders, position, limit, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    private static final @NotNull UnionPair<Optional<FileInformation>, Throwable> RootInfo = UnionPair.ok(Optional.of(new FileInformation(0, 0, IdentifierNames.RootSelector, true, -1, null, null, null)));
    /**
     * @see ProviderInterface#info(long, boolean, Consumer)
     */
    public static void info(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<FileInformation>, Throwable>> consumer) {
        try {
            if (IdentifierNames.RootSelector.equals(location.storage())) {
                consumer.accept(RootSelector.RootInfo);
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.InfoNotExisted);
                return;
            }
            real.info(location.id(), isDirectory, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#refreshDirectory(long, Consumer)
     */
    public static void refreshDirectory(final @NotNull FileLocation directory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<RefreshRequirements>, Throwable>> consumer) {
        try {
            if (IdentifierNames.RootSelector.equals(directory.storage())) {
                consumer.accept(ProviderInterface.RefreshNoRequire);
                return;
            }
            final ProviderInterface<?> real = StorageManager.getProvider(directory.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.RefreshNotExisted);
                return;
            }
            real.refreshDirectory(directory.id(), RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#trash(long, boolean, Consumer)
     */
    public static void trash(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<Boolean>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
            if (real == null) {
                consumer.accept(ProviderInterface.TrashTooComplex);
                return;
            }
            real.trash(location.id(), isDirectory, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#downloadFile(long, long, long, Consumer)
     */
    public static void downloadFile(final @NotNull FileLocation file, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(file.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(file, false))));
                return;
            }
            real.downloadFile(file.id(), from, to, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#createDirectory(long, String, Options.DuplicatePolicy, Consumer)
     */
    public static void createDirectory(final @NotNull FileLocation parent, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(parent.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parent, true))));
                return;
            }
            real.createDirectory(parent.id(), directoryName, policy, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#uploadFile(long, String, long, Options.DuplicatePolicy, Consumer)
     */
    public static void uploadFile(final @NotNull FileLocation parent, final @NotNull String filename, final @LongRange(minimum = 0) long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(parent.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(parent, true))));
                return;
            }
            real.uploadFile(parent.id(), filename, size, policy, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    private static @Nullable ProviderInterface<?> checkCMAvailable(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) {
        if (!location.storage().equals(parent.storage())) {
            consumer.accept(ProviderInterface.CMTooComplex);
            return null;
        }
        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
        if (real == null) {
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(Optional.of(FailureReason.byNoSuchFile(location, isDirectory))))));
            return null;
        }
        return real;
    }

    /**
     * @see ProviderInterface#copyDirectly(long, boolean, long, String, Options.DuplicatePolicy, Consumer)
     */
    public static void copyDirectly(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = RootSelector.checkCMAvailable(location, isDirectory, parent, consumer);
            if (real != null)
                real.copyDirectly(location.id(), isDirectory, parent.id(), name, policy, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#moveDirectly(long, boolean, long, Options.DuplicatePolicy, Consumer)
     */
    public static void moveDirectly(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = RootSelector.checkCMAvailable(location, isDirectory, parent, consumer);
            if (real != null)
                real.moveDirectly(location.id(), isDirectory, parent.id(), policy, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }

    /**
     * @see ProviderInterface#renameDirectly(long, boolean, String, Options.DuplicatePolicy, Consumer)
     */
    public static void renameDirectly(final @NotNull FileLocation location, final boolean isDirectory, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        try {
            final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
            if (real == null) {
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(location, isDirectory)))));
                return;
            }
            real.renameDirectly(location.id(), isDirectory, name, policy, RootSelector.wrapConsumer(consumer, real.getConfiguration()));
        } catch (final Throwable exception) {
            consumer.accept(UnionPair.fail(exception));
        }
    }



}
