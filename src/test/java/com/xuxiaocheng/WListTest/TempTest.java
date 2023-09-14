package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class TempTest {
    private static final boolean initializeServer = false;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {
        final DownloadRequirements.DownloadMethods methods = DownloadRequirements.tryGetDownloadFromUrl(HttpNetworkHelper.DefaultHttpClient,
                Objects.requireNonNull(HttpUrl.parse("https://developer-oss.lanzouc.com/file/?A2UFO1tqV2YIAQszU2YBbQQ7BDwFvgW4CqxQsAfaAK4H4VSJW6MFtAHHBN9T4lLcVLkPJAAlVD9WIVd3XWdTOwNvBTdbWVdqCDoLYFM2ATMEbQQ4BWsFMgo9UG4HagAnB2FUcFs7BWkBYgRkUzdSalQ4DzwAeVQmViFXbF0zU2IDMQVhWylXMwhmC3JTNQE4BHEEMAU5BWYKNFA3B2kAMgc+VDBbYgUyAWIENlMyUjFUOQ9sAGlUMFYzVzddNVMwA2YFNls2V2YIbAtpUzEBYwRuBC8FIAVuCn1QcAcuAHIHYlRxW28FNQFqBGFTNVJhVDkPMgBtVGZWd1clXWhTPwNmBTRbO1cyCGgLZFMzATcEbgQ1BWkFNwo0UHgHdQAnB2FUb1txBWwBZgRkUz5SZ1Q4DzwAa1RuVmdXZV0nUycDcwUlWztXMghoC2RTNAEyBG4EMAVvBTIKNVBwBy4AaAd3VD5bNwVgAWAEfFM2UmdUPQ8kAGtUY1Z/V2NdOQ==")),
                null, null, new Headers.Builder().add("accept-language", "zh-CN"), 0, Long.MAX_VALUE, null).supplier().get();
        final CompositeByteBuf buffer = ByteBufAllocator.DEFAULT.compositeBuffer();
        for (final DownloadRequirements.OrderedSuppliers suppliers: methods.parallelMethods()) {
            DownloadRequirements.OrderedNode node = suppliers.suppliersLink();
            while (node != null) {
                final CountDownLatch latch = new CountDownLatch(1);
                node = node.apply(p -> {
                    buffer.addComponent(true, p.getT());
                    latch.countDown();
                });
                latch.await();
            }
        }
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(TempTest.runtimeDirectory, "1.zip")));
                final InputStream inputStream = new ByteBufInputStream(buffer)) {
            inputStream.transferTo(outputStream);
        }
        return null;
    };

    private static final @NotNull File runtimeDirectory = new File("./run");
    @BeforeAll
    public static void initialize() throws IOException, SQLException {
        StaticLoader.load();
        if (TempTest.initializeServer) {
            ServerConfiguration.Location.initialize(new File(TempTest.runtimeDirectory, "server.yaml"));
            ServerConfiguration.parseFromFile();
            final File file = new File(TempTest.runtimeDirectory, "data.db");
            final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
            ConstantManager.quicklyInitialize(database, "initialize");
            UserGroupManager.quicklyInitialize(database, "initialize");
            UserManager.quicklyInitialize(database, "initialize");
            StorageManager.initialize(new File(TempTest.runtimeDirectory, "configs"),
                    new File(TempTest.runtimeDirectory, "caches"));
        }
    }

    @Test
    public void tempTest() throws Exception {
        try {
            if (TempTest.initializeServer) {
                final Map<String, Exception> failures = StorageManager.getFailedProvidersAPI();
                if (!failures.isEmpty()) {
                    HLog.DefaultLogger.log(HLogLevel.FAULT, failures);
                    return;
                }
            }
            final Object obj = TempTest._main.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            if (TempTest.initializeServer)
                for (final Map.Entry<String, ProviderInterface<?>> provider: StorageManager.getAllProviders().entrySet())
                    try {
                        StorageManager.dumpConfigurationIfModified(provider.getValue().getConfiguration());
                    } catch (final IOException exception) {
                        HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump provider configuration.", ParametersMap.create().add("name", provider.getKey()), exception);
                    }
            HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            HttpNetworkHelper.CountDownExecutors.shutdownGracefully();
        }
    }
}
