package com.xuxiaocheng.WListTest.Storage.Real;

import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import com.xuxiaocheng.WListTest.Storage.ProviderHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LanzouTest {
    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.set(ServerConfiguration.parse(new ByteArrayInputStream("providers:\n  test: lanzou\n".getBytes(StandardCharsets.UTF_8))));
        StorageManager.initialize(new File("run/configs"), new File("run/caches"));
    }

    @AfterAll
    public static void uninitialize() throws IOException {
        StorageManager.removeProvider("test", false);
    }

    public @NotNull ProviderInterface<?> provider() {
        return Objects.requireNonNull(StorageManager.getProvider("test"));
    }

    public long root() {
        return this.provider().getConfiguration().getRootDirectoryId();
    }

    @Test
    @Order(0)
    public void list() throws Exception {
        Assumptions.assumeTrue(ProviderHelper.list(this.provider(), this.root(), Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 10).getT().total() == 0);

//        latch.await();
//        Assumptions.assumeTrue(result.get() != null);
//        Assumptions.assumeTrue(result.get().isSuccess());
//        final FilesListInformation information = result.get().getT();
//        HLog.DefaultLogger.log("", information);
    }
}
