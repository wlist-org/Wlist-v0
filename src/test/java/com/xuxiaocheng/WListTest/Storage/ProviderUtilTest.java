package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import org.junit.jupiter.api.Assertions;
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
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void wrapperPage(final boolean exception) throws Exception {
        final FileInformation information = AbstractProvider.build(0, 0, false).get();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<Optional<Iterator<FileInformation>>, Throwable>> result = new AtomicReference<>();
        ProviderUtil.wrapSuppliersInPages(consumer -> consumer.accept(ProviderUtil.WrapAvailable), WListServer.IOExecutors, (page, consumer) -> {
            HLog.DefaultLogger.log(HLogLevel.INFO, "At page:" + page);
            TimeUnit.MILLISECONDS.sleep(300);
            if (exception && page.intValue() == 3)
                throw new RuntimeException();
            consumer.accept(UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(List.of(information), page.intValue() >= 5)));
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
