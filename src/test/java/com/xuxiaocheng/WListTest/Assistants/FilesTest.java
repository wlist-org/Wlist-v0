package com.xuxiaocheng.WListTest.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class FilesTest extends ProvidersWrapper {
    @BeforeAll
    public static void initialize() throws Exception {
        ProvidersWrapper.initialize();
        StorageManager.addStorage("test", StorageTypes.Lanzou, null);
    }

    @AfterAll
    public static void uninitialize() throws IOException {
        StorageManager.removeStorage("test", false);
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    public void upload(final @NotNull WListClientInterface client, final @NotNull WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        TokenAssistant.login(this.address(), this.adminUsername(), this.adminPassword(), WListServer.IOExecutors);

        Assertions.assertNull(FilesAssistant.upload(this.address(), this.adminUsername(), new File("run/test_file.zip"),
                Options.DuplicatePolicy.ERROR, this.location(this.root())));
        final Pair.ImmutablePair<OperationType, ByteBuf> buffer = OperateServerHelper.waitBroadcast(broadcast).getT();
        Assertions.assertEquals(OperationType.UploadFile, buffer.getFirst());
        Assertions.assertEquals("test", ByteBufIOUtil.readUTF(buffer.getSecond()));
        final VisibleFileInformation information = VisibleFileInformation.parse(buffer.getSecond());
        buffer.getSecond().release();
        HLog.DefaultLogger.log("", information);
        OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.adminUsername()), this.location(information.id()), false);
    }
}
