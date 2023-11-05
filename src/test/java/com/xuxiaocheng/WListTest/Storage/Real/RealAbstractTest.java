package com.xuxiaocheng.WListTest.Storage.Real;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WListTest.Operations.ProvidersWrapper;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @see com.xuxiaocheng.WListTest.Storage.AbstractProviderTest
 */
@Execution(ExecutionMode.SAME_THREAD)
public abstract class RealAbstractTest<C extends StorageConfiguration> extends ProvidersWrapper {
    @SuppressWarnings({"CommentedOutCode", "SpellCheckingInspection", "RedundantThrows"})
    public static @NotNull File smallFile() throws IOException {
//        final File small = Files.createTempFile("uploading", ".txt").toFile();
////        small.deleteOnExit();
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(small))) {
//            stream.write("WList test upload: small file.\nrandom: ".getBytes(StandardCharsets.UTF_8));
//            stream.write(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 64, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
//        }
//        return small;
        return new File("C:\\Users\\27622\\AppData\\Local\\Temp\\uploading3526592196602715442.txt");
    }

    @SuppressWarnings({"CommentedOutCode", "SpellCheckingInspection", "RedundantThrows"})
    public static @NotNull File bigFile() throws IOException {
//        final File big = Files.createTempFile("uploading", ".txt").toFile();
////        big.deleteOnExit();
//        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(big))) {
//            stream.write("WList test upload: big file.\nrandom: ".getBytes(StandardCharsets.UTF_8));
//            stream.write(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, NetworkTransmission.FileTransferBufferSize << 1, HRandomHelper.AnyWords).getBytes(StandardCharsets.UTF_8));
//        }
//        return big;
        return new File("C:\\Users\\27622\\AppData\\Local\\Temp\\uploading8255215792986666245.txt");
    }

    @SuppressWarnings("unchecked")
    public @NotNull ProviderInterface<C> provider() {
        return (ProviderInterface<C>) Objects.requireNonNull(StorageManager.getProvider("test"));
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractInfoTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void file(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            final VisibleFileInformation information = list.informationList().get(0);
            Assertions.assertEquals(information, OperateFilesHelper.getFileOrDirectory(client, token(), location(information.id()), false));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void directory(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            final VisibleFileInformation information = list.informationList().get(0);
            Assertions.assertEquals(information, OperateFilesHelper.getFileOrDirectory(client, token(), location(information.id()), true));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertNull(OperateFilesHelper.getFileOrDirectory(client, token(), location(-2), false));
            Assertions.assertNull(OperateFilesHelper.getFileOrDirectory(client, token(), location(-2), true));
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractCreateTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void create(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<VisibleFileInformation> information = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpload.getCallbacks().put("create", p -> {
                Assertions.assertEquals(location(0).storage(), p.getFirst());
                information.set(p.getSecond());
                latch.countDown();
            });
            final UnionPair<VisibleFileInformation, VisibleFailureReason> p = OperateFilesHelper.createDirectory(client, token(), location(root()), "directory", DuplicatePolicy.ERROR);
            Assertions.assertTrue(p.isSuccess(), p.toString());
            latch.await();
            BroadcastAssistant.get(address()).FileUpload.getCallbacks().remove("create");
            HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().map(Method::getName).orElse("unknown"), ": ", information.get());
            Assertions.assertEquals(information.get(), p.getT());
            OperateFilesHelper.trashFileOrDirectory(client, token(), location(information.get().id()), true);
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = OperateFilesHelper.createDirectory(client, token(), location(-2), "unreachable", DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isFailure(), res.toString());
            Assertions.assertEquals(FailureKind.NoSuchFile, res.getE().kind());
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractRefreshTest {
        @Test
        public void refresh() throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 50, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeTrue(list.informationList().size() < 50);
            final Collection<Pair.ImmutablePair<FileLocation, Boolean>> l = new HashSet<>();
            for (final VisibleFileInformation info : list.informationList())
                l.add(Pair.ImmutablePair.makeImmutablePair(location(info.id()), info.isDirectory()));

            final CountDownLatch latch = new CountDownLatch(list.informationList().size());
            final Set<Pair.ImmutablePair<FileLocation, Boolean>> set = ConcurrentHashMap.newKeySet();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().put("refresh", p -> {
                set.add(p);
                latch.countDown();
            });
            Assertions.assertTrue(FilesAssistant.refresh(address(), adminUsername(), location(root()), WListServer.IOExecutors, null));
            latch.await();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().remove("refresh");

            Assertions.assertEquals(l, set);
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = OperateFilesHelper.createDirectory(client, token(), location(-2), "unreachable", DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isFailure(), res.toString());
            Assertions.assertEquals(FailureKind.NoSuchFile, res.getE().kind());
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractDownloadTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void cancel(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            // Prepare.
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 2, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);
            // Request.
            final UnionPair<DownloadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestDownloadFile(client, token(), location(information.id()), 0, Long.MAX_VALUE);
            Assertions.assertTrue(confirm.isSuccess(), confirm.toString());
            Assertions.assertEquals(information.size(), confirm.getT().downloadingSize());
            // Test.
            Assertions.assertTrue(OperateFilesHelper.cancelDownloadFile(client, token(), confirm.getT().id()));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.requestDownloadFile(client, token(), location(1), 0, Long.MAX_VALUE).getE().kind());
            Assertions.assertFalse(OperateFilesHelper.cancelDownloadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.confirmDownloadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.downloadFile(client, token(), "", 0));
            Assertions.assertDoesNotThrow(() -> OperateFilesHelper.finishDownloadFile(client, token(), ""));
        }

        @Test
        public void download() throws IOException, InterruptedException, WrongStateException {
            final LinkedHashMap<VisibleFileInformation.Order, OrderDirection> order = new LinkedHashMap<>();
            order.put(VisibleFileInformation.Order.Size, OrderDirection.ASCEND);
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, order, 0, 3, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeTrue(list.informationList().size() == 2);
            final VisibleFileInformation small = list.informationList().get(0); /* <= NetworkTransmission.FileTransferBufferSize */
            final VisibleFileInformation big = list.informationList().get(1); /* > NetworkTransmission.FileTransferBufferSize */
            Assumptions.assumeTrue("WListClientConsole-v0.1.1.exe".equals(small.name()));
            Assumptions.assumeTrue(small.size() == 1803776); // assumption "127d400ae420533548891ef54390f495".equals(MD5(small.content()));
            Assumptions.assumeTrue("WList-V0.2.0.jar".equals(big.name()));
            Assumptions.assumeTrue(big.size() == 24915053); // assumption "0efa9c569a7f37f0c92a352042a01df7".equals(MD5(big.content()));
            // Test.
            this.testDownload(location(small.id()), "127d400ae420533548891ef54390f495");
            this.testDownload(location(big.id()), "0efa9c569a7f37f0c92a352042a01df7");
        }

        public void testDownload(final @NotNull FileLocation location, final @NotNull String md5) throws InterruptedException, WrongStateException, IOException {
            final File file = Files.createTempFile("downloading", ".bin").toFile();
            file.deleteOnExit();
            FilesAssistant.download(address(), adminUsername(), location, file, PredicateE.truePredicate(), null);
            final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
            try (final InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                HMessageDigestHelper.updateMessageDigest(digest, stream);
            }
            Assertions.assertEquals(md5, HMessageDigestHelper.MD5.digest(digest));
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractUploadTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void cancel(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            // Request.
            final UnionPair<UploadConfirm, VisibleFailureReason> confirm = OperateFilesHelper.requestUploadFile(client, token(), location(root()), "1.txt", 0, DuplicatePolicy.ERROR);
            Assertions.assertTrue(confirm.isSuccess(), confirm.toString());
            // Test.
            Assertions.assertTrue(OperateFilesHelper.cancelUploadFile(client, token(), confirm.getT().id()));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.requestUploadFile(client, token(), location(1), "1.txt", 0, DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertFalse(OperateFilesHelper.cancelUploadFile(client, token(), ""));
            Assertions.assertNull(OperateFilesHelper.confirmUploadFile(client, token(), "", List.of()));
            Assertions.assertFalse(OperateFilesHelper.uploadFile(client, token(), "", 0, Unpooled.EMPTY_BUFFER));
            Assertions.assertNull(OperateFilesHelper.finishUploadFile(client, token(), ""));
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void upload(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            // Test 1.
            final File small = RealAbstractTest.smallFile();
            final VisibleFileInformation smallInformation = this.testUpload(location(root()), small);
            HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().map(Method::getName).orElse("unknown"), ": small: ", smallInformation);
            OperateFilesHelper.trashFileOrDirectory(client, token(), location(smallInformation.id()), false);
            Assertions.assertEquals(small.length(), smallInformation.size());

            final File big = RealAbstractTest.bigFile();
            final VisibleFileInformation bigInformation = this.testUpload(location(root()), big);
            HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().map(Method::getName).orElse("unknown"), ": big: ", bigInformation);
            OperateFilesHelper.trashFileOrDirectory(client, token(), location(bigInformation.id()), false);
            Assertions.assertEquals(big.length(), bigInformation.size());
        }

        public @NotNull VisibleFileInformation testUpload(final @NotNull FileLocation parent, final @NotNull File file) throws InterruptedException, WrongStateException, IOException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<VisibleFileInformation> information = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpload.getCallbacks().put("testUpload", p -> {
                Assertions.assertEquals(location(0).storage(), p.getFirst());
                information.set(p.getSecond());
                latch.countDown();
            });
            FilesAssistant.upload(address(), adminUsername(), file, parent, null, PredicateE.truePredicate(), null);
            latch.await();
            BroadcastAssistant.get(address()).FileUpload.getCallbacks().remove("testUpload");
            return information.get();
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractMergeTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void uploadAndDownload(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final File file = RealAbstractTest.bigFile();
            final MessageDigest digest = HMessageDigestHelper.MD5.getDigester();
            try (final InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                HMessageDigestHelper.updateMessageDigest(digest, stream);
            }
            final String md5 = HMessageDigestHelper.MD5.digest(digest);
            final VisibleFileInformation information = new AbstractUploadTest(){}.testUpload(location(root()), file);
            HLog.DefaultLogger.log(HLogLevel.LESS, info.getTestMethod().map(Method::getName).orElse("unknown"), ": file: ", information);
            try {
                new AbstractDownloadTest(){}.testDownload(location(information.id()), md5);
            } finally {
                OperateFilesHelper.trashFileOrDirectory(client, token(), location(information.id()), false);
            }
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractCopyTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void file(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation l = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(l != null);
            Assumptions.assumeFalse(l.informationList().isEmpty());
            final VisibleFileInformation parent = l.informationList().get(0);
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Pair.ImmutablePair<FileLocation, Boolean>> r = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().put("file", p -> {
                r.set(p);
                latch.countDown();
            });
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> res = OperateFilesHelper.copyDirectly(client, token(), location(information.id()), false, location(parent.id()), "temp-" + information.name(), DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isSuccess(), res.toString());
            if (res.getT().isEmpty()) {
                BroadcastAssistant.get(address()).FileUpdate.getCallbacks().remove("file");
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Unsupported operation: ", info.getTestClass().orElseThrow().getName(), "#", info.getTestMethod().orElseThrow().getName());
                return;
            }
            latch.await();
            Assertions.assertEquals(location(information.id()), r.get().getFirst());
            Assertions.assertFalse(r.get().getSecond().booleanValue());
            OperateFilesHelper.trashFileOrDirectory(client, token(), location(information.id()), false);
        }

        // TODO: directory

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.copyDirectly(client, token(),
                    location(-2), false, location(-3), "1.txt", DuplicatePolicy.ERROR).getE().kind());
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractMoveTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void file(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation l = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(l != null);
            Assumptions.assumeFalse(l.informationList().isEmpty());
            final VisibleFileInformation parent = l.informationList().get(0);
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Pair.ImmutablePair<FileLocation, Boolean>> r = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().put("file", p -> {
                r.set(p);
                latch.countDown();
            });
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> res = OperateFilesHelper.moveDirectly(client, token(), location(information.id()), false, location(parent.id()), DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isSuccess(), res.toString());
            if (res.getT().isEmpty()) {
                BroadcastAssistant.get(address()).FileUpdate.getCallbacks().remove("file");
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Unsupported operation: ", info.getTestClass().orElseThrow().getName(), "#", info.getTestMethod().orElseThrow().getName());
                return;
            }
            latch.await();
            Assertions.assertEquals(location(information.id()), r.get().getFirst());
            Assertions.assertFalse(r.get().getSecond().booleanValue());
            OperateFilesHelper.moveDirectly(client, token(), location(information.id()), false, location(root()), DuplicatePolicy.ERROR);
        }

        // TODO: directory

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.moveDirectly(client, token(),
                    location(-2), false, location(-3), DuplicatePolicy.ERROR).getE().kind());
        }
    }

    @SuppressWarnings("UnqualifiedMethodAccess")
    public abstract class AbstractRenameTest {
        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void file(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Pair.ImmutablePair<FileLocation, Boolean>> r = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().put("file", p -> {
                r.set(p);
                latch.countDown();
            });
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> res = OperateFilesHelper.renameDirectly(client, token(), location(information.id()), false, "renamed-" + information.name(), DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isSuccess(), res.toString());
            if (res.getT().isEmpty()) {
                BroadcastAssistant.get(address()).FileUpdate.getCallbacks().remove("file");
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Unsupported operation: ", info.getTestClass().orElseThrow().getName(), "#", info.getTestMethod().orElseThrow().getName());
                return;
            }
            latch.await();
            Assertions.assertEquals(location(information.id()), r.get().getFirst());
            Assertions.assertFalse(r.get().getSecond().booleanValue());
            OperateFilesHelper.renameDirectly(client, token(), location(information.id()), false, information.name(), DuplicatePolicy.ERROR);
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void directory(final WListClientInterface client, final @NotNull TestInfo info) throws IOException, InterruptedException, WrongStateException {
            final VisibleFilesListInformation list = FilesAssistant.list(address(), adminUsername(), location(root()), FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, 1, WListServer.IOExecutors, PredicateE.truePredicate(), null);
            Assumptions.assumeTrue(list != null);
            Assumptions.assumeFalse(list.informationList().isEmpty());
            final VisibleFileInformation information = list.informationList().get(0);

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Pair.ImmutablePair<FileLocation, Boolean>> r = new AtomicReference<>();
            BroadcastAssistant.get(address()).FileUpdate.getCallbacks().put("directory", p -> {
                r.set(p);
                latch.countDown();
            });
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> res = OperateFilesHelper.renameDirectly(client, token(), location(information.id()), true, "renamed-" + information.name(), DuplicatePolicy.ERROR);
            Assertions.assertTrue(res.isSuccess(), res.toString());
            if (res.getT().isEmpty()) {
                BroadcastAssistant.get(address()).FileUpdate.getCallbacks().remove("directory");
                HLog.DefaultLogger.log(HLogLevel.ERROR, "Unsupported operation: ", info.getTestClass().orElseThrow().getName(), "#", info.getTestMethod().orElseThrow().getName());
                return;
            }
            latch.await();
            Assertions.assertEquals(location(information.id()), r.get().getFirst());
            Assertions.assertTrue(r.get().getSecond().booleanValue());
            OperateFilesHelper.renameDirectly(client, token(), location(information.id()), true, information.name(), DuplicatePolicy.ERROR);
        }

        @ParameterizedTest(name = "running")
        @MethodSource("com.xuxiaocheng.WListTest.Operations.ServerWrapper#client")
        public void notAvailable(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.renameDirectly(client, token(),
                    location(-2), false, "1.txt", DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(FailureKind.NoSuchFile, OperateFilesHelper.renameDirectly(client, token(),
                    location(-2), true, "a", DuplicatePolicy.ERROR).getE().kind());
        }
    }
}
