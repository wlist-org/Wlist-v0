package com.xuxiaocheng.WListTest.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

        Assertions.assertNull(FilesAssistant.upload(this.address(), this.adminUsername(), new File("run/test_file.zip"),
                Options.DuplicatePolicy.ERROR, this.location(this.root()), c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state)));
        final Pair.ImmutablePair<OperationType, ByteBuf> buffer = OperateServerHelper.waitBroadcast(broadcast).getT();
        Assertions.assertEquals(OperationType.UploadFile, buffer.getFirst());
        Assertions.assertEquals("test", ByteBufIOUtil.readUTF(buffer.getSecond()));
        final VisibleFileInformation information = VisibleFileInformation.parse(buffer.getSecond());
        buffer.getSecond().release();
        HLog.DefaultLogger.log("", information);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(information.id()), false);
    }

    @Disabled("Prepare files and then run this test.")
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    public void download(final @NotNull WListClientInterface client) throws IOException, InterruptedException, WrongStateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TokenAssistant.login(this.address(), this.adminUsername(), this.adminPassword(), WListServer.IOExecutors);
        final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> order = new LinkedHashMap<>();
        order.put(VisibleFileInformation.Order.Size, Options.OrderDirection.ASCEND);
        final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, this.token(), this.location(this.root()), Options.FilterPolicy.OnlyFiles, order, 0, 3);
        Assumptions.assumeTrue(list != null);
        Assumptions.assumeTrue(list.informationList().size() == 2);
        final Method method = FilesAssistant.class.getDeclaredMethod("calculateChecksums", File.class, Collection.class);
        method.setAccessible(true);

        final VisibleFileInformation small = list.informationList().get(0);
        Assumptions.assumeTrue("WListClientConsole-v0.1.1.exe".equals(small.name()));
        Assumptions.assumeTrue(small.size() == 1803776);
        final File smallFile = Files.createTempFile("test-WListClientConsole", ".exe").toFile();
        smallFile.deleteOnExit();
        Assertions.assertNull(FilesAssistant.download(this.address(), this.adminUsername(), this.location(small.id()),
                smallFile, c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        Assertions.assertEquals(List.of("127d400ae420533548891ef54390f495"), method.invoke(null, smallFile,
                List.of(new UploadChecksum(0, smallFile.length(), UploadChecksum.MD5))));

        final VisibleFileInformation big = list.informationList().get(1);
        Assumptions.assumeTrue("WList-V0.2.0.jar".equals(big.name()));
        Assumptions.assumeTrue(big.size() == 24915053);
        final File bigFile = Files.createTempFile("test-WList", ".jar").toFile();
        bigFile.deleteOnExit();
        Assertions.assertNull(FilesAssistant.download(this.address(), this.adminUsername(), this.location(big.id()),
                bigFile, c -> {HLog.DefaultLogger.log("", c);return true;},
                state -> HLog.DefaultLogger.log(HLogLevel.LESS, state.stages())));
        Assertions.assertEquals(List.of("0efa9c569a7f37f0c92a352042a01df7"), method.invoke(null, bigFile,
                List.of(new UploadChecksum(0, bigFile.length(), UploadChecksum.MD5))));
    }
}
