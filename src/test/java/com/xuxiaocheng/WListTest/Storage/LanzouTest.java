package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Selectors.RootSelector;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class LanzouTest {
    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.set(ServerConfiguration.parse(new ByteArrayInputStream("providers:\n  test: lanzou\n".getBytes(StandardCharsets.UTF_8))));
        StorageManager.initialize(new File("run/configs"), new File("run/caches"));
    }

    @Test
    public void list() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
        RootSelector.list(new FileLocation("test", -1), Options.FilterPolicy.OnlyDirectories, new LinkedHashMap<>(), 0, 10, e -> {
            result.set(e);
            latch.countDown();
        });
        latch.await();
        Assumptions.assumeTrue(result.get() != null);
        Assumptions.assumeTrue(result.get().isSuccess());
        final FilesListInformation information = result.get().getT();
        HLog.DefaultLogger.log("", information);
    }
}
