package com.xuxiaocheng.WListTest.Storage.Real;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
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
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @see com.xuxiaocheng.WListTest.Storage.AbstractProviderTest
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
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
        public void cancel(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            // Prepare.
            final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token(), location(root()), Options.FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 2);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);
            // Request.
            final UnionPair<DownloadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestDownloadFile(client, token(), location(information.id()), 0, Long.MAX_VALUE);
            Assumptions.assumeTrue(confirm != null);
            Assertions.assertEquals(information.size(), confirm.getT().downloadingSize());
            // Test.
            Assertions.assertTrue(OperateFilesHelper.cancelDownloadFile(client, token(), confirm.getT().id()));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertNull(OperateFilesHelper.requestDownloadFile(client, token(), location(1), 0, Long.MAX_VALUE));
            Assertions.assertFalse(OperateFilesHelper.cancelDownloadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.confirmDownloadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.downloadFile(client, token(), "", 0));
            Assertions.assertDoesNotThrow(() -> OperateFilesHelper.finishDownloadFile(client, token(), ""));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void download(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
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

        public void testDownload(final WListClientInterface client, final @NotNull VisibleFileInformation file, final @NotNull String md5) throws InterruptedException, WrongStateException, IOException {
            // Request.
            final UnionPair<DownloadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestDownloadFile(client, token(), location(file.id()), 0, Long.MAX_VALUE);
            Assertions.assertNotNull(confirm);
            Assertions.assertEquals(file.size(), confirm.getT().downloadingSize());
            final DownloadConfirm.DownloadInformation information = OperateFilesHelper.confirmDownloadFile(client, token(), confirm.getT().id());
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
                            buf = OperateFilesHelper.downloadFile(c, token(), confirm.getT().id(), k);
                            if (buf == null)
                                break;
                            buffer.addComponent(true, buf);
                            if (buffer.readableBytes() >= pair.getSecond().longValue() - pair.getFirst().longValue())
                                break;
                        }
                    }
                }, latch::countDown), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            Assertions.assertEquals(length, length);
            latch.await();
            OperateFilesHelper.finishDownloadFile(client, token(), confirm.getT().id());
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

    @SuppressWarnings("UnqualifiedMethodAccess")
    public class AbstractUploadTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void cancel(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            // Request.
            final UnionPair<UploadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestUploadFile(client, token(), location(root()), "1.txt", 0, Options.DuplicatePolicy.ERROR);
            Assumptions.assumeTrue(confirm != null && confirm.isSuccess());
            // Test.
            Assertions.assertTrue(OperateFilesHelper.cancelUploadFile(client, token(), confirm.getT().id()));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, Objects.requireNonNull(OperateFilesHelper.requestUploadFile(client, token(), location(1), "1.txt", 0, Options.DuplicatePolicy.ERROR)).getE().kind());
            Assertions.assertFalse(OperateFilesHelper.cancelUploadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.confirmUploadFile(client, token(), "", List.of()));
            Assertions.assertFalse(OperateFilesHelper.uploadFile(client, token(), "", 0, Unpooled.EMPTY_BUFFER));
            Assertions.assertFalse(OperateFilesHelper.finishUploadFile(client, token(), ""));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#broadcast")
        public void upload(final WListClientInterface client, final WListClientInterface broadcast, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            // Prepare. (Ensure filename is not duplicated.)
            final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> order = new LinkedHashMap<>();
            order.put(VisibleFileInformation.Order.Size, Options.OrderDirection.ASCEND);
            final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token(), location(root()), Options.FilterPolicy.Both, order, 0, 3);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeTrue(list.informationList().size() == 2);
            Assumptions.assumeTrue("WListClientConsole-v0.1.1.exe".equals(list.informationList().get(0).name()));
            Assumptions.assumeTrue("WList-V0.2.0.jar".equals(list.informationList().get(1).name()));
            Assumptions.assumeTrue(list.informationList().get(0).size() == 1803776);
            Assumptions.assumeTrue(list.informationList().get(1).size() == 24915053);
            long total = 1803776 + 24915053;

            // Test.
            final ByteBuf small = ByteBufAllocator.DEFAULT.buffer().writeBytes("WList test upload: small file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                    .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 64, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
            try {
                this.testUpload(client, root(), "1.txt", small);
            } finally {
                small.release();
            }
            final Pair.ImmutablePair<OperationType, ByteBuf> smallBroadcast = OperateServerHelper.waitBroadcast(broadcast).getT();
            Assertions.assertEquals(OperationType.UploadFile, smallBroadcast.getFirst());
            Assertions.assertEquals("test", ByteBufIOUtil.readUTF(smallBroadcast.getSecond()));
            final VisibleFileInformation smallInformation = VisibleFileInformation.parse(smallBroadcast.getSecond());
            Assertions.assertFalse(ByteBufIOUtil.readBoolean(smallBroadcast.getSecond()));
            HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().get().getName(), ": small: ", smallInformation);
            total += smallInformation.size();
            try {
                final ByteBuf big = ByteBufAllocator.DEFAULT.buffer().writeBytes("WList test upload: big file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                        .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, NetworkTransmission.FileTransferBufferSize << 1, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
                try {
                    this.testUpload(client, root(), "2.txt", big);
                } finally {
                    big.release();
                }
                final Pair.ImmutablePair<OperationType, ByteBuf> bigBroadcast = OperateServerHelper.waitBroadcast(broadcast).getT();
                Assertions.assertEquals(OperationType.UploadFile, bigBroadcast.getFirst());
                Assertions.assertEquals("test", ByteBufIOUtil.readUTF(bigBroadcast.getSecond()));
                final VisibleFileInformation bigInformation = VisibleFileInformation.parse(bigBroadcast.getSecond());
                Assertions.assertFalse(ByteBufIOUtil.readBoolean(bigBroadcast.getSecond()));
                HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().get().getName(), ": big: ", bigInformation);
                total += bigInformation.size();
                try {
                    Assertions.assertEquals(total, Objects.requireNonNull(OperateFilesHelper.getFileOrDirectory(client, token(), location(root()), true)).size());
                    // Clean.
                } finally {
                    OperateFilesHelper.trashFileOrDirectory(client, token(), location(bigInformation.id()), false);
                    Assertions.assertEquals(OperationType.TrashFileOrDirectory, OperateServerHelper.waitBroadcast(broadcast).getT().getFirst());
                }
            } finally {
                OperateFilesHelper.trashFileOrDirectory(client, token(), location(smallInformation.id()), false);
                Assertions.assertEquals(OperationType.TrashFileOrDirectory, OperateServerHelper.waitBroadcast(broadcast).getT().getFirst());
            }
        }

        public void testUpload(final WListClientInterface client, final long parent, final @NotNull String filename, final @NotNull ByteBuf file) throws InterruptedException, WrongStateException, IOException {
            // Request.
            final UnionPair<UploadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestUploadFile(client, token(), location(parent), filename, file.readableBytes(), Options.DuplicatePolicy.ERROR);
            Assertions.assertNotNull(confirm);
            final List<String> checksums = new ArrayList<>(confirm.getT().checksums().size());
            for (final UploadChecksum checksum: confirm.getT().checksums()) {
                Assumptions.assumeTrue(UploadChecksum.MD5.equals(checksum.algorithm())); // TODO
                final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
                HMessageDigestHelper.updateMessageDigest(digest, new ByteBufInputStream(file.slice(Math.toIntExact(checksum.start()), Math.toIntExact(checksum.end() - checksum.start()))));
                checksums.add(HMessageDigestHelper.MD5.digest(digest));
            }
            final UploadConfirm.UploadInformation information = OperateFilesHelper.confirmUploadFile(client, token(), confirm.getT().id(), checksums);
            Assertions.assertNotNull(information);
            // Upload.
            final CountDownLatch latch = new CountDownLatch(information.parallel().size());
            int i = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                final int index = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    final ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(client.getAddress());
                         final InputStream stream = new ByteBufInputStream(file.slice(pair.getFirst().intValue(), pair.getSecond().intValue() - pair.getFirst().intValue()))) {
                        while (stream.available() > 0) {
                            buf.writeBytes(stream, NetworkTransmission.FileTransferBufferSize);
                            Assertions.assertTrue(OperateFilesHelper.uploadFile(c, token(), confirm.getT().id(), index, buf.retain()));
                            buf.clear();
                        }
                    } finally {
                        buf.release();
                    }
                }, latch::countDown), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            latch.await();
            OperateFilesHelper.finishUploadFile(client, token(), confirm.getT().id());
        }
    }

    @Disabled
    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    public void uploadAndDownload(final WListClientInterface client, final WListClientInterface broadcast, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf file = ByteBufAllocator.DEFAULT.buffer().writeBytes("WList test upload and download.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, NetworkTransmission.FileTransferBufferSize  + 512, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
        final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
        HMessageDigestHelper.updateMessageDigest(digest, new ByteBufInputStream(file));
        final String md5 = HMessageDigestHelper.MD5.digest(digest);
        try {
            file.readerIndex(0);
            new AbstractUploadTest().testUpload(client, this.root(), "3.txt", file);
        } finally {
            file.release();
        }
        final Pair.ImmutablePair<OperationType, ByteBuf> bigBroadcast = OperateServerHelper.waitBroadcast(broadcast).getT();
        Assertions.assertEquals(OperationType.UploadFile, bigBroadcast.getFirst());
        Assertions.assertEquals("test", ByteBufIOUtil.readUTF(bigBroadcast.getSecond()));
        final VisibleFileInformation information = VisibleFileInformation.parse(bigBroadcast.getSecond());
        Assertions.assertFalse(ByteBufIOUtil.readBoolean(bigBroadcast.getSecond()));
        HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().get().getName(), ": file: ", information);
        try {
            new AbstractDownloadTest().testDownload(client, information, md5);
        } finally {
            OperateFilesHelper.trashFileOrDirectory(client, this.token(), this.location(information.id()), false);
            Assertions.assertEquals(OperationType.TrashFileOrDirectory, OperateServerHelper.waitBroadcast(broadcast).getT().getFirst());
        }
    }
}
