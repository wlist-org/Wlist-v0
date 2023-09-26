package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class OperateFilesTest extends ProvidersWrapper {
    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    @BeforeAll
    public static void initialize() throws Exception {
        ProvidersWrapper.initialize();
        StorageManager.addStorage("test", StorageTypes.Lanzou, null);
    }

    public static Stream<Arguments> download() throws IOException, InterruptedException, WrongStateException {
        final WListClientInterface client = ServerWrapper.client().toList().get(0);
        final String token = ServerWrapper.AdminToken.getInstance();
        final FileLocation root = new FileLocation("test", Objects.requireNonNull(StorageManager.getProvider("test")).getConfiguration().getRootDirectoryId());

//        Assumptions.assumeTrue(OperateFilesHelper.refreshDirectory(client, token, root));
        final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token, root, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 2);
        Assumptions.assumeTrue(list != null);
        Assumptions.assumeTrue(list.informationList().size() == 1);
        final VisibleFileInformation information = list.informationList().get(0);
        Assumptions.assumeFalse(information.isDirectory());
        Assumptions.assumeTrue("WList-V0.2.0.jar".equals(information.name()));
        // assumption "0efa9c569a7f37f0c92a352042a01df7".equals(MD5(information.content()));

        final DownloadConfirm confirm = OperateFilesHelper.requestDownloadFile(client, token, new FileLocation("test", information.id()), 0, Long.MAX_VALUE);
        Assumptions.assumeTrue(confirm != null);
//        Assertions.assertFalse(confirm.acceptedRange());
        Assertions.assertEquals(information.size(), confirm.downloadingSize());
        return Stream.of(Arguments.arguments(client, confirm.id(), information));
    }
    @SuppressWarnings("UnqualifiedMethodAccess")
    @Nested
    public class DownloadTest {
        @Disabled
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.OperateFilesTest#download")
        public void cancel(final WListClientInterface client, final String id) throws WrongStateException, IOException, InterruptedException {
            Assertions.assertTrue(OperateFilesHelper.cancelDownloadFile(client, token(), id));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.OperateFilesTest#download")
        public void download(final WListClientInterface client, final String id, final VisibleFileInformation information) throws WrongStateException, IOException, InterruptedException {
            final DownloadConfirm.DownloadInformation confirm = OperateFilesHelper.confirmDownloadFile(client, token(), id);
            Assertions.assertNotNull(confirm);
//            Assumptions.assumeTrue(confirm.expire() == null);
            final CountDownLatch latch = new CountDownLatch(confirm.parallel().size());
            final Collection<ByteBuf> buffers = new ArrayList<>(confirm.parallel().size());
            long size = 0;
            int i = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: confirm.parallel()) {
                Assertions.assertEquals(size, pair.getFirst());
                size = pair.getSecond().longValue();
                final CompositeByteBuf buffer = ByteBufAllocator.DEFAULT.compositeBuffer();
                buffers.add(buffer);
                final int k = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(client.getAddress())) {
                        ByteBuf buf;
                        while (true) {
                            buf = OperateFilesHelper.downloadFile(c, token(), id, k);
                            if (buf == null)
                                break;
                            buffer.addComponent(true, buf);
                        }
                    }
                }, latch::countDown), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            Assertions.assertEquals(size, information.size());
            latch.await();
            OperateFilesHelper.finishDownloadFile(client, token(), id);
            final CompositeByteBuf file = ByteBufAllocator.DEFAULT.compositeBuffer();
            for (final ByteBuf buf: buffers)
                file.addComponent(true, buf);
            Assertions.assertEquals(information.size(), file.readableBytes());
            final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
            HMessageDigestHelper.updateMessageDigest(digest, new ByteBufInputStream(file));
            Assertions.assertEquals("0efa9c569a7f37f0c92a352042a01df7", HMessageDigestHelper.MD5.digest(digest));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
            Assertions.assertNull(OperateFilesHelper.requestDownloadFile(client, token(), new FileLocation("test", 1), 0, Long.MAX_VALUE));
            Assertions.assertFalse(OperateFilesHelper.cancelDownloadFile(client, token(), ""));
        }
    }
}
