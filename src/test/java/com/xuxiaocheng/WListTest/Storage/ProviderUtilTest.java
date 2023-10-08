package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ProviderUtilTest {
    @BeforeAll
    public static void load() {
        StaticLoader.load();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void wrapperPage(final boolean exception) throws Exception {
        final FileInformation information = AbstractProvider.build(0, 0, false).get();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<Iterator<FileInformation>>, Throwable>> result = new AtomicReference<>();
        ProviderUtil.wrapSuppliersInPages(consumer -> consumer.accept(ProviderUtil.WrapAvailable), WListServer.IOExecutors, (page, consumer) -> {
            HLog.DefaultLogger.log(HLogLevel.INFO, "At page:" + page);
            WListServer.IOExecutors.schedule(() -> {
                if (exception && page.intValue() == 3)
                    consumer.accept(UnionPair.fail(new RuntimeException()));
                else
                    consumer.accept(UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(List.of(information), page.intValue() >= 5)));
            }, 300, TimeUnit.MILLISECONDS);
        }, p -> {
            result.set(p);
            latch.countDown();
        });
        latch.await();
        Assertions.assertTrue(result.get().getT().isPresent());
        final Iterator<FileInformation> iterator = result.get().getT().get();
        final List<FileInformation> i = new ArrayList<>();
        if (exception) {
            Assertions.assertThrows(RuntimeException.class, () -> iterator.forEachRemaining(i::add));
            return;
        }
        iterator.forEachRemaining(i::add);
        Assertions.assertEquals(List.of(information, information, information, information, information, information), i);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void wrapperPages(final boolean exception) throws Exception {
        final FileInformation information = AbstractProvider.build(0, 0, false).get();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<Iterator<FileInformation>>, Throwable>> result = new AtomicReference<>();
        ProviderUtil.wrapSuppliersInPages(6, 3, WListServer.IOExecutors, (page, consumer) -> {
            HLog.DefaultLogger.log(HLogLevel.INFO, "At page:" + page);
            WListServer.IOExecutors.schedule(() -> {
                if (exception && page.intValue() == 3)
                    consumer.accept(UnionPair.fail(new RuntimeException()));
                else
                    consumer.accept(UnionPair.ok(List.of(information)));
            }, 300, TimeUnit.MILLISECONDS);
        }, p -> {
            result.set(p);
            latch.countDown();
        });
        latch.await();
        Assertions.assertTrue(result.get().getT().isPresent());
        final Iterator<FileInformation> iterator = result.get().getT().get();
        final List<FileInformation> i = new ArrayList<>();
        if (exception) {
            Assertions.assertThrows(RuntimeException.class, () -> iterator.forEachRemaining(i::add));
            return;
        }
        iterator.forEachRemaining(i::add);
        Assertions.assertEquals(List.of(information, information, information, information, information, information), i);
    }
}
