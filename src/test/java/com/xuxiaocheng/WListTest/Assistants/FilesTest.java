package com.xuxiaocheng.WListTest.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
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

@Disabled("Manually test")
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
        StorageManager.removeStorage("test", false);
        ProvidersWrapper.uninitialize();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void calculate() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        final Method method = FilesAssistant.class.getDeclaredMethod("calculateChecksums", File.class, Collection.class);
        method.setAccessible(true);
        final File file = Files.createTempFile("calculate_checksums", ".txt").toFile();
        file.deleteOnExit();
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
            stream.write("123123".getBytes(StandardCharsets.UTF_8));
        }
        final List<UploadChecksum> requirements = List.of(
                new UploadChecksum(0, 3, UploadChecksum.MD5),
                new UploadChecksum(3, 6, UploadChecksum.MD5),
                new UploadChecksum(0, 6, UploadChecksum.MD5)
        );
        final List<String> result = (List<String>) method.invoke(null, file, requirements);
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123"), result.get(0));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123"), result.get(1));
        Assertions.assertEquals(HMessageDigestHelper.MD5.get("123123"), result.get(2));
    }

    @Disabled("Prepare 'run/test_file.zip' and then run this test.")
    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    public void upload(final @NotNull WListClientInterface client, final @NotNull WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        TokenAssistant.login(this.address(), this.adminUsername(), this.adminPassword(), WListServer.IOExecutors);

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
        final VisibleFileInformation smallInformation = this.testUpload(client, broadcast, smallFile);
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
        final VisibleFileInformation bigInformation = this.testUpload(client, broadcast, bigFile);
        Assertions.assertEquals(bigFile.length(), bigInformation.size());
    }

    public @NotNull VisibleFileInformation testUpload(final @NotNull WListClientInterface client, final @NotNull WListClientInterface broadcast, final @NotNull File file) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertNull(FilesAssistant.upload(this.address(), this.adminUsername(), file,
                Options.DuplicatePolicy.ERROR, this.location(this.root()), c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        final Pair.ImmutablePair<OperationType, ByteBuf> buffer = OperateServerHelper.waitBroadcast(broadcast).getT();
        Assertions.assertEquals(OperationType.UploadFile, buffer.getFirst());
        Assertions.assertEquals("test", ByteBufIOUtil.readUTF(buffer.getSecond()));
        final VisibleFileInformation information = VisibleFileInformation.parse(buffer.getSecond());
        buffer.getSecond().release();
        HLog.DefaultLogger.log("", information);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(information.id()), false);
        OperateServerHelper.waitBroadcast(broadcast).getT().getSecond().release();
        return information;
    }

    @Test
    public void download() throws IOException, InterruptedException, WrongStateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TokenAssistant.login(this.address(), this.adminUsername(), this.adminPassword(), WListServer.IOExecutors);
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

        final File file = Files.createTempFile("test-download", ".jar").toFile();
        file.deleteOnExit();
        Assertions.assertNull(FilesAssistant.download(this.address(), this.adminUsername(), this.location(id),
                file, c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        Assertions.assertEquals(List.of(md5), method.invoke(null, file,
                List.of(new UploadChecksum(0, file.length(), UploadChecksum.MD5))));
    }
}
