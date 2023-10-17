package com.xuxiaocheng.WListTest.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WListTest.Operations.ProvidersWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

//@Disabled("Manually test")
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class FilesTest extends ProvidersWrapper {
    @BeforeAll
    public static void initialize() throws Exception {
        ProvidersWrapper.initialize();
        StorageManager.addStorage("test", StorageTypes.Lanzou, null);
    }

    @AfterAll
    public static void uninitialize() throws Exception {
        TimeUnit.MILLISECONDS.sleep(300); // Wait for broadcast.
        StorageManager.removeStorage("test", false);
        ProvidersWrapper.uninitialize();
    }

    @Disabled
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    public void _del(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        OperateFilesHelper.trashFileOrDirectory(client, this.token(), this.location(285522805 >> 1), false);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void calculate() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        final Method method = FilesAssistant.class.getDeclaredMethod("calculateChecksums", File.class, Collection.class);
        method.setAccessible(true);
        final File file = Files.createTempFile("calculate_checksums", ".txt").toFile();
        file.deleteOnExit();
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
            stream.write("123456789".getBytes(StandardCharsets.UTF_8));
        }
        final List<UploadChecksum> requirements = List.of(
                new UploadChecksum(0, 3, UploadChecksum.MD5),
                new UploadChecksum(3, 6, UploadChecksum.MD5),
                new UploadChecksum(0, 6, UploadChecksum.MD5),
                new UploadChecksum(8, 9, UploadChecksum.MD5)
        );
        final List<String> result = (List<String>) method.invoke(null, file, requirements);
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123"), result.get(0));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("456"), result.get(1));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123456"), result.get(2));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("9"), result.get(3));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    public void upload(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final File smallFile = Files.createTempFile("test-upload", ".txt").toFile();
        smallFile.deleteOnExit();
        final ByteBuf small = ByteBufAllocator.DEFAULT.buffer().writeBytes("WList test upload: small file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 64, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
        try (final RandomAccessFile access = new RandomAccessFile(smallFile, "rw");
             final FileChannel channel = access.getChannel()) {
            small.readBytes(channel, small.readableBytes());
        } finally {
            small.release();
        }
        final VisibleFileInformation smallInformation = this.testUpload(client, smallFile);
        Assertions.assertEquals(smallFile.length(), smallInformation.size());

        final File bigFile = Files.createTempFile("test-upload", ".txt").toFile();
        bigFile.deleteOnExit();
        final ByteBuf big = ByteBufAllocator.DEFAULT.buffer().writeBytes("WList test upload: big file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, NetworkTransmission.FileTransferBufferSize << 1, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
        try (final RandomAccessFile access = new RandomAccessFile(bigFile, "rw");
             final FileChannel channel = access.getChannel()) {
            big.readBytes(channel, big.readableBytes());
        } finally {
            big.release();
        }
        final VisibleFileInformation bigInformation = this.testUpload(client, bigFile);
        Assertions.assertEquals(bigFile.length(), bigInformation.size());
    }

    public @NotNull VisibleFileInformation testUpload(final @NotNull WListClientInterface client, final @NotNull File file) throws WrongStateException, IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<VisibleFileInformation> information = new AtomicReference<>();
        final Consumer<Pair.ImmutablePair<String, VisibleFileInformation>> callback = p -> {
            Assertions.assertEquals("test", p.getFirst());
            information.set(p.getSecond());
            latch.countDown();
        };
        BroadcastAssistant.get(this.address()).FileUpload.register(callback);
        Assertions.assertNull(FilesAssistant.upload(this.address(), this.adminUsername(), file,
                this.location(this.root()), c -> {HLog.DefaultLogger.log(HLogLevel.INFO, c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        latch.await();
        BroadcastAssistant.get(this.address()).FileUpload.unregister(callback);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(information.get().id()), false);
        return information.get();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void calculateStream() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = FilesAssistant.class.getDeclaredMethod("calculateChecksumsStream", InputStream.class, Collection.class);
        method.setAccessible(true);
        final List<UploadChecksum> requirements = List.of(
                new UploadChecksum(0, 3, UploadChecksum.MD5),
                new UploadChecksum(3, 6, UploadChecksum.MD5),
                new UploadChecksum(0, 6, UploadChecksum.SHA512),
                new UploadChecksum(8, 9, UploadChecksum.CRC32)
        );
        final char[] array = "123456789".toCharArray();
        final List<String> result = (List<String>) method.invoke(null, new InputStream() {
            private final @NotNull AtomicInteger pos = new AtomicInteger(0);
            @Override
            public int read() {
                final int i = this.pos.getAndIncrement();
                if (i >= array.length)
                    return -1;
                return array[i];
            }
        }, requirements);
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123"), result.get(0));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("456"), result.get(1));
        Assertions.assertEquals(HMessageDigestHelper.SHA512.get("123456"), result.get(2));
        Assertions.assertEquals(HMessageDigestHelper.CRC32.get("9"), result.get(3));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    public void uploadStream(final @NotNull WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final ByteBuf small = ByteBufAllocator.DEFAULT.heapBuffer().writeBytes("WList test upload: small file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 64, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
        HLog.DefaultLogger.log(HLogLevel.INFO, HMessageDigestHelper.MD5.get(ByteBufIOUtil.allToByteArray(small)));
        final VisibleFileInformation smallInformation = this.testUploadStream(small, "1.txt");
        HLog.DefaultLogger.log(HLogLevel.INFO, smallInformation);

        final ByteBuf big = ByteBufAllocator.DEFAULT.heapBuffer().writeBytes("WList test upload: big file.\nrandom: ".getBytes(StandardCharsets.UTF_8))
                .writeBytes(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, NetworkTransmission.FileTransferBufferSize << 1, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
        HLog.DefaultLogger.log(HLogLevel.INFO, HMessageDigestHelper.MD5.get(ByteBufIOUtil.allToByteArray(big)));
        final VisibleFileInformation bigInformation = this.testUploadStream(big, "2.txt");
        HLog.DefaultLogger.log(HLogLevel.INFO, bigInformation);

        HLog.DefaultLogger.log(HLogLevel.BUG, "Please check the uploaded file and then continue the test.");
        TimeUnit.SECONDS.sleep(10);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(smallInformation.id()), false);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(bigInformation.id()), false);
    }

    public @NotNull VisibleFileInformation testUploadStream(final @NotNull ByteBuf buffer, final @NotNull String filename) throws WrongStateException, IOException, InterruptedException {
        final long size = buffer.readableBytes();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<VisibleFileInformation> information = new AtomicReference<>();
        final Consumer<Pair.ImmutablePair<String, VisibleFileInformation>> callback = p -> {
            Assertions.assertEquals("test", p.getFirst());
            information.set(p.getSecond());
            latch.countDown();
        };
        BroadcastAssistant.get(this.address()).FileUpload.register(callback);
        Assertions.assertNull(FilesAssistant.uploadStream(this.address(), this.adminUsername(), consumer -> consumer.accept(new InputStream() {
            private final @NotNull AtomicInteger pos = new AtomicInteger(0);
            @Override
            public int read() {
                final int i = this.pos.getAndIncrement();
                if (i >= size)
                    return -1;
                return buffer.getByte(i);
            }
        }), size, filename, this.location(this.root()), c -> {HLog.DefaultLogger.log(HLogLevel.INFO, c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        latch.await();
        BroadcastAssistant.get(this.address()).FileUpload.unregister(callback);
        return information.get();
    }

    @Test
    public void download() throws IOException, InterruptedException, WrongStateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> order = new LinkedHashMap<>();
        order.put(VisibleFileInformation.Order.Size, Options.OrderDirection.ASCEND);
        final VisibleFilesListInformation list = FilesAssistant.list(this.address(), this.adminUsername(), this.location(this.root()), Options.FilterPolicy.OnlyFiles, order, 0, 3, WListServer.IOExecutors, ConsumerE.emptyConsumer());
        Assumptions.assumeTrue(list != null);
        Assumptions.assumeTrue(list.informationList().size() == 2);

        final VisibleFileInformation small = list.informationList().get(0);
        Assumptions.assumeTrue("WListClientConsole-v0.1.1.exe".equals(small.name()));
        Assumptions.assumeTrue(small.size() == 1803776);
        this.testDownload(small.id(), "127d400ae420533548891ef54390f495");

        final VisibleFileInformation big = list.informationList().get(1);
        Assumptions.assumeTrue("WList-V0.2.0.jar".equals(big.name()));
        Assumptions.assumeTrue(big.size() == 24915053);
        this.testDownload(big.id(), "0efa9c569a7f37f0c92a352042a01df7");
    }

    public void testDownload(final long id, final @NotNull String md5) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, WrongStateException, InterruptedException {
        final Method method = FilesAssistant.class.getDeclaredMethod("calculateChecksums", File.class, Collection.class);
        method.setAccessible(true);

        final File file = Files.createTempFile("test-upload", ".bin").toFile();
        file.deleteOnExit();
        Assertions.assertNull(FilesAssistant.download(this.address(), this.adminUsername(), this.location(id),
                file, c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        Assertions.assertEquals(List.of(md5), method.invoke(null, file,
                List.of(new UploadChecksum(0, file.length(), UploadChecksum.MD5))));
    }


}
