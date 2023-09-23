package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Assertions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ProviderHelper {
    private ProviderHelper() {
        super();
    }

    public static @NotNull UnionPair<FilesListInformation, Boolean> list(final @NotNull ProviderInterface<?> provider, final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> result = new AtomicReference<>();
        provider.list(directoryId, filter, orders, position, limit, p -> {
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            if (throwable instanceof Exception exception)
                throw exception;
            throw (Error) throwable;
        }
        return result.get().getT();
    }
    public static void testList(final @NotNull FilesListInformation list, final @NotNull Collection<@NotNull FileInformation> collection, final Options.@NotNull FilterPolicy filter) {
        Assertions.assertEquals(collection.size(), list.total());
        final Collection<FileInformation> information = switch (filter) {
            case Both -> collection;
            case OnlyDirectories -> collection.stream().filter(FileInformation::isDirectory).collect(Collectors.toList());
            case OnlyFiles -> collection.stream().filter(Predicate.not(FileInformation::isDirectory)).collect(Collectors.toList());
        };
        Assertions.assertEquals(information.size(), list.filtered());
        Assertions.assertEquals(information, list.informationList());
    }

    public static @NotNull UnionPair<Pair.ImmutablePair<@NotNull FileInformation, @NotNull Boolean>, Boolean> info(final @NotNull ProviderInterface<?> provider, final long id, final boolean isDirectory) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<UnionPair<Pair.ImmutablePair<FileInformation, Boolean>, Boolean>, Throwable>> result = new AtomicReference<>();
        provider.info(id, isDirectory, p -> {
            result.set(p);
            latch.countDown();
        });
        latch.await();
        if (result.get().isFailure()) {
            final Throwable throwable = result.get().getE();
            if (throwable instanceof Exception exception)
                throw exception;
            throw (Error) throwable;
        }
        return result.get().getT();
    }
    public static void testInfo(final Pair.@NotNull ImmutablePair<@NotNull FileInformation, @NotNull Boolean> pair, final @NotNull FileInformation information, final boolean isUpdated) {
        Assertions.assertEquals(information, pair.getFirst());
        Assertions.assertEquals(isUpdated, pair.getSecond());
    }
}
