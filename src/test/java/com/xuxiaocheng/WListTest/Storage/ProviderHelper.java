package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ProviderHelper {
    private ProviderHelper() {
        super();
    }

    public static @NotNull Optional<FilesListInformation> list(final @NotNull ProviderInterface<?> provider, final long directoryId, final Options.@NotNull FilterPolicy filter, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<FilesListInformation>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders = new LinkedHashMap<>();
        orders.put(VisibleFileInformation.Order.Id, Options.OrderDirection.ASCEND);
        provider.list(directoryId, filter, orders, position, limit, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(list) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }
    public static void testList(final @NotNull FilesListInformation list, final @NotNull Collection<@NotNull FileInformation> collection, final Options.@NotNull FilterPolicy filter) {
        Assertions.assertEquals(collection.size(), list.total());
        final List<FileInformation> information = (switch (filter) {
            case Both -> collection.stream();
            case OnlyDirectories -> collection.stream().filter(FileInformation::isDirectory);
            case OnlyFiles -> collection.stream().filter(Predicate.not(FileInformation::isDirectory));
        }).sorted(Comparator.comparingLong(FileInformation::id)).collect(Collectors.toList());
        Assertions.assertEquals(information.size(), list.filtered());
        Assertions.assertEquals(information, list.informationList());
    }

    public static @NotNull Optional<FileInformation> info(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<FileInformation>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.info(id, isDirectory, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(info) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }

    public static boolean refresh(final @NotNull ProviderInterface<?> provider, final long directoryId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Boolean, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.refreshDirectory(directoryId, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(refresh) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT().booleanValue();
    }

    public static @NotNull Optional<Boolean> trash(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<Boolean>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.trash(id, isDirectory, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(trash) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }

    public static @NotNull UnionPair<FileInformation, FailureReason> create(final @NotNull ProviderInterface<?> provider, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.createDirectory(id, name, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(create) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }

    public static @NotNull Optional<UnionPair<FileInformation, Optional<FailureReason>>> copy(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory, final long parent, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.copyDirectly(id, isDirectory, parent, name, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(copy) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }

    public static @NotNull Optional<UnionPair<FileInformation, Optional<FailureReason>>> move(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory, final long parent, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.moveDirectly(id, isDirectory, parent, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(copy) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }

    public static @NotNull Optional<UnionPair<FileInformation, FailureReason>> rename(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> result = new AtomicReference<>();
        final AtomicBoolean barrier = new AtomicBoolean(true);
        provider.renameDirectly(id, isDirectory, name, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new RuntimeException("Duplicate message.(copy) " + p));
                return;
            }
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            MiscellaneousUtil.throwException(throwable);
        }
        return result.get().getT();
    }
}
