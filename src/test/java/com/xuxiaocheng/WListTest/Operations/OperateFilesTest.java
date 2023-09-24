package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Objects;
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
        final VisibleFilesListInformation list = OperateFilesHelper.listFiles(client, token, root, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 2);
        Assumptions.assumeTrue(list != null);
        Assumptions.assumeTrue(list.informationList().size() == 1);
        final VisibleFileInformation information = list.informationList().get(0);
        Assumptions.assumeFalse(information.isDirectory());

        final DownloadConfirm confirm = OperateFilesHelper.requestDownloadFile(client, token, new FileLocation("test", information.id()), 0, Long.MAX_VALUE);
        Assumptions.assumeTrue(confirm != null);
        Assertions.assertFalse(confirm.acceptedRange());
        Assertions.assertEquals(information.size(), confirm.downloadingSize());
        return Stream.of(Arguments.arguments(client, confirm.id()));
    }
    @SuppressWarnings("UnqualifiedMethodAccess")
    @Nested
    public class DownloadTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.OperateFilesTest#download")
        public void download(final WListClientInterface client, final String id) throws WrongStateException, IOException, InterruptedException {
            Assertions.assertTrue(OperateFilesHelper.cancelDownloadFile(client, token(), id));
        }
    }
}
