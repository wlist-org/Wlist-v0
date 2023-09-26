package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WListTest.Operations.ProvidersWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class RealAbstractTest<C extends StorageConfiguration> extends ProvidersWrapper {
    @SuppressWarnings("unchecked")
    public @NotNull ProviderInterface<C> provider() {
        return (ProviderInterface<C>) Objects.requireNonNull(StorageManager.getProvider("test"));
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public class AbstractDownloadTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void cancel(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
            // Prepare.
            final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token(), location(root()), Options.FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 2);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);
            // Request.
            final DownloadConfirm confirm = OperateFilesHelper.requestDownloadFile(client, token(), location(information.id()), 0, Long.MAX_VALUE);
            Assumptions.assumeTrue(confirm != null);
            Assertions.assertEquals(information.size(), confirm.downloadingSize());
            // Test.
            Assertions.assertTrue(OperateFilesHelper.cancelDownloadFile(client, token(), confirm.id()));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
            Assertions.assertNull(OperateFilesHelper.requestDownloadFile(client, token(), location(1), 0, Long.MAX_VALUE));
            Assertions.assertFalse(OperateFilesHelper.cancelDownloadFile(client, token(), ""));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void download(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
            // Prepare.
            final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> order = new LinkedHashMap<>();
            order.put(VisibleFileInformation.Order.Size, Options.OrderDirection.ASCEND);
            final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token(), location(root()), Options.FilterPolicy.OnlyFiles, order, 0, 3);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeTrue(list.informationList().size() == 2);
            final VisibleFileInformation small = list.informationList().get(0); /* <= NetworkTransmission.FileTransferBufferSize */
            final VisibleFileInformation big = list.informationList().get(1); /* > NetworkTransmission.FileTransferBufferSize */
            Assumptions.assumeTrue("WListClientConsole-v0.1.1.exe".equals(small.name()));
            Assumptions.assumeTrue(small.size() == 1803776); // assumption "127d400ae420533548891ef54390f495".equals(MD5(small.content()));
            Assumptions.assumeTrue("WList-V0.2.0.jar".equals(big.name()));
            Assumptions.assumeTrue(big.size() == 24915053); // assumption "0efa9c569a7f37f0c92a352042a01df7".equals(MD5(big.content()));
            // Test.
            this.testDownload(client, small, "127d400ae420533548891ef54390f495");
            this.testDownload(client, big, "0efa9c569a7f37f0c92a352042a01df7");
        }

        private void testDownload(final WListClientInterface client, final @NotNull VisibleFileInformation file, final @NotNull String md5) throws InterruptedException, WrongStateException, IOException {
            // Request.
            final DownloadConfirm confirm = OperateFilesHelper.requestDownloadFile(client, token(), location(file.id()), 0, Long.MAX_VALUE);
            Assertions.assertNotNull(confirm);
            Assertions.assertEquals(file.size(), confirm.downloadingSize());
            final DownloadConfirm.DownloadInformation information = OperateFilesHelper.confirmDownloadFile(client, token(), confirm.id());
            Assertions.assertNotNull(information);
            // Download.
            final CountDownLatch latch = new CountDownLatch(information.parallel().size());
            final Collection<ByteBuf> buffers = new ArrayList<>(information.parallel().size());
            long length = 0;
            int i = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                Assertions.assertEquals(length, pair.getFirst());
                length = pair.getSecond().longValue();
                final CompositeByteBuf buffer = ByteBufAllocator.DEFAULT.compositeBuffer();
                buffers.add(buffer);
                final int k = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(client.getAddress())) {
                        ByteBuf buf;
                        while (true) {
                            buf = OperateFilesHelper.downloadFile(c, token(), confirm.id(), k);
                            if (buf == null)
                                break;
                            buffer.addComponent(true, buf);
                        }
                    }
                }, latch::countDown), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            Assertions.assertEquals(length, length);
            latch.await();
            OperateFilesHelper.finishDownloadFile(client, token(), confirm.id());
            final CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer();
            for (final ByteBuf b: buffers)
                buf.addComponent(true, b);
            // Test.
            Assertions.assertEquals(file.size(), buf.readableBytes());
            final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
            HMessageDigestHelper.updateMessageDigest(digest, new ByteBufInputStream(buf));
            Assertions.assertEquals(md5, HMessageDigestHelper.MD5.digest(digest));
        }
    }
}
