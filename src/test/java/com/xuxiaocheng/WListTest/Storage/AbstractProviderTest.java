package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
@TestMethodOrder(MethodOrderer.Random.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AbstractProviderTest {
    @TempDir
    private static File directory;

    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.parseFromFile();
        StorageManager.initialize(new File(AbstractProviderTest.directory, "configs"), new File(AbstractProviderTest.directory, "caches"));
    }

    @AfterAll
    public static void check() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        BackgroundTaskManager.BackgroundExecutors.shutdownGracefully().sync();

        final Field removable = BackgroundTaskManager.class.getDeclaredField("removable");
        removable.setAccessible(true);
        Assertions.assertEquals(Set.of(), removable.get(null));

        final Field tasks = BackgroundTaskManager.class.getDeclaredField("tasks");
        tasks.setAccessible(true);
        Assertions.assertEquals(Map.of(), tasks.get(null));
    }

    protected final AtomicReference<AbstractProvider> provider = new AtomicReference<>();
    public AbstractProvider provider() {
        return this.provider.get();
    }

    @BeforeEach
    public void reset(final @NotNull TestInfo info) throws Exception {
        final AbstractProvider provider = new AbstractProvider();
        this.provider.set(provider);
        final AbstractProvider.AbstractConfiguration configuration = new AbstractProvider.AbstractConfiguration();
        synchronized (AbstractProviderTest.class) {
            configuration.setName(String.valueOf(System.currentTimeMillis()));
            TimeUnit.MILLISECONDS.sleep(10);
        }
        provider.initialize(configuration);
    }

    @AfterEach
    public void unset() throws Exception {
        TimeUnit.MILLISECONDS.sleep(500); // Wait for broadcast finish.
        this.provider.getAndSet(null).uninitialize(true);
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class ListTest {
        public static Stream<Consumer<AbstractProvider.@NotNull AbstractProviderFile>> list() {
            return Stream.of(root -> {
                root.add(AbstractProvider.build(1, 0, true));
                root.add(AbstractProvider.build(2, 0, false));
            }, root -> {
                root.add(AbstractProvider.build(1, 0, true));
                root.add(AbstractProvider.build(1, 0, false));
            }, root -> {
                root.add(AbstractProvider.build(1, 0, true));
                root.add(AbstractProvider.build(2, 0, false));
                root.add(AbstractProvider.build(3, 0, false));
            }, root -> {
                root.add(AbstractProvider.build(1, 0, true));
                root.add(AbstractProvider.build(2, 0, true));
                root.add(AbstractProvider.build(3, 0, false));
            }, root -> {
                for (int i = 1; i < 10; ++i)
                    root.add(AbstractProvider.build(i, 0, HRandomHelper.DefaultSecureRandom.nextBoolean()));
            }, root -> {
                for (int i = 1; i < 100000; ++i)
                    root.add(AbstractProvider.build(i, 0, (i & 1) == 1));
            });
        }

        @ParameterizedTest
        @MethodSource
        public void list(final @NotNull Consumer<? super AbstractProvider.@NotNull AbstractProviderFile> prepare) throws Exception {
            prepare.accept(provider().root());
            final List<FileInformation> list = provider().root().children().stream().map(AbstractProvider.AbstractProviderFile::get).toList();

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, list.size());
            ProviderHelper.testList(result.orElseThrow(), list, FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> resultD = ProviderHelper.list(provider(), 0, FilterPolicy.OnlyDirectories, 0, list.size());
            ProviderHelper.testList(resultD.orElseThrow(), list, FilterPolicy.OnlyDirectories);
            Assertions.assertEquals(List.of(), provider().checkOperations());

            final Optional<FilesListInformation> resultF = ProviderHelper.list(provider(), 0, FilterPolicy.OnlyFiles, 0, list.size());
            ProviderHelper.testList(resultF.orElseThrow(), list, FilterPolicy.OnlyFiles);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 1, FilterPolicy.Both, 0, 5);
            Assertions.assertFalse(result1.isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void delete() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            provider().root().add(directory);
            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            ProviderHelper.testList(result.orElseThrow(), List.of(directory.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().root().del(1, true);
            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 1, FilterPolicy.Both, 0, 5);
            Assertions.assertFalse(result1.isPresent());
            Assertions.assertEquals(List.of("Login.", "List: 1"), provider().checkOperations());

            final Optional<FilesListInformation> result2 = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            ProviderHelper.testList(result2.orElseThrow(), List.of(), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void exception() {
            provider().list.initialize(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    throw new NoSuchElementException(new RuntimeException());
                }
            });
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5)
            );
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
        }

        @Test
        public void concurrent() throws Exception {
            final AbstractProvider.AbstractProviderFile info = AbstractProvider.build(1, 0, false);
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch listIteratorNext = new CountDownLatch(1);
            provider().list.initialize(new Iterator<>() {
                private final AtomicBoolean got = new AtomicBoolean(true);
                @Override
                public boolean hasNext() {
                    return this.got.get();
                }

                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        listIteratorNext.await();
                    } catch (final InterruptedException exception) {
                        throw new NoSuchElementException(exception);
                    }
                    this.got.set(false);
                    return info.get();
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<Optional<FilesListInformation>> result1 = new AtomicReference<>();
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                result1.set(ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5));
                latch1.countDown();
            })).addListener(MiscellaneousUtil.exceptionListener());
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<Optional<FilesListInformation>> result2 = new AtomicReference<>();
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                result2.set(ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5));
                latch2.countDown();
            })).addListener(MiscellaneousUtil.exceptionListener());
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            ProviderHelper.testList(result1.get().orElseThrow(), List.of(info.get()), FilterPolicy.Both);
            latch2.await();
            ProviderHelper.testList(result2.get().orElseThrow(), List.of(info.get()), FilterPolicy.Both);

            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
        }

        @Test
        public void concurrentException() throws Exception {
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch listIteratorNext = new CountDownLatch(1);
            provider().list.initialize(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        listIteratorNext.await();
                    } catch (final InterruptedException exception) {
                        throw new NoSuchElementException(exception);
                    }
                    throw new NoSuchElementException(new RuntimeException());
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            WListServer.IOExecutors.submit(() -> Assertions.assertThrows(RuntimeException.class, () ->
                            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5)))
                    .addListener(f -> {if (!f.isSuccess()) HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
                        latch1.countDown();});
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<Optional<FilesListInformation>> result2 = new AtomicReference<>();
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                result2.set(ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5));
                latch2.countDown();
            })).addListener(MiscellaneousUtil.exceptionListener());
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            latch2.await();
            ProviderHelper.testList(result2.get().orElseThrow(), List.of(), FilterPolicy.Both);

            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 0"), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class InfoTest {
        @Test
        public void info() throws Exception {
            Assertions.assertTrue(ProviderHelper.info(provider(), 0, true).isPresent());
            Assertions.assertEquals(List.of("Login.", "Update: 0 d"), provider().checkOperations());

            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(directory);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FileInformation> resultD = ProviderHelper.info(provider(), 1, true);
            Assertions.assertTrue(resultD.isPresent());
            Assertions.assertEquals(directory.get(), resultD.get());
            Assertions.assertEquals(List.of("Login.", "Update: 1 d"), provider().checkOperations());

            final Optional<FileInformation> resultF = ProviderHelper.info(provider(), 1, false);
            Assertions.assertTrue(resultF.isPresent());
            Assertions.assertEquals(file.get(), resultF.get());
            Assertions.assertEquals(List.of("Login.", "Update: 1 f"), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            Assertions.assertFalse(ProviderHelper.info(provider(), 1, true).isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void update() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation newFile = new FileInformation(1, 0, "file", false, 1, null, null, "123");
            provider().root().add(new AbstractProvider.AbstractProviderFile(newFile)); // replace
            final Optional<FileInformation> resultR = ProviderHelper.info(provider(), 1, false);
            Assertions.assertTrue(resultR.isPresent());
            Assertions.assertEquals(newFile, resultR.get());
            Assertions.assertEquals(List.of("Login.", "Update: 1 f"), provider().checkOperations());
        }

        @Test
        public void delete() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().root().del(1, false);
            final Optional<FileInformation> resultD = ProviderHelper.info(provider(), 1, false);
            Assertions.assertFalse(resultD.isPresent());
            Assertions.assertEquals(List.of("Login.", "Update: 1 f"), provider().checkOperations());
        }

        @Test
        public void move() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(2, 0, false);
            provider().root().add(directory);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            ProviderHelper.list(provider(), 1, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final AbstractProvider.AbstractProviderFile newFile = AbstractProvider.build(2, 1, false);
            provider().root().del(2, false);
            provider().root().get(1, true).add(newFile);
            final Optional<FileInformation> result = ProviderHelper.info(provider(), 2, false);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(newFile.get(), result.get());
            Assertions.assertEquals(List.of("Login.", "Update: 2 f"), provider().checkOperations());
        }

        @Test
        public void moveNotIndexed() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(2, 0, false);
            provider().root().add(directory);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final AbstractProvider.AbstractProviderFile newFile = AbstractProvider.build(2, 1, false);
            provider().root().del(2, false);
            provider().root().get(1, true).add(newFile);
            final Optional<FileInformation> result = ProviderHelper.info(provider(), 2, false);
            Assertions.assertFalse(result.isPresent());
            Assertions.assertEquals(List.of("Login.", "Update: 2 f"), provider().checkOperations());
        }

        @Test
        public void exception() {
            provider().update.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.info(provider(), 0, true));
            Assertions.assertEquals(List.of("Login.", "Update: 0 d"), provider().checkOperations());

            provider().update.initialize(() -> UnionPair.fail(new RuntimeException()));
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.info(provider(), 0, true));
            Assertions.assertEquals(List.of("Login.", "Update: 0 d"), provider().checkOperations());
        }

        @Test
        public void concurrent() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch updateSupplierContinue = new CountDownLatch(1);
            provider().update.initialize(() -> {
                latch.countDown();
                try {
                    updateSupplierContinue.await();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
                return AbstractIdBaseProvider.UpdateNoRequired;
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FileInformation>, Throwable>> result1 = new AtomicReference<>();
            provider().info(1, false, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FileInformation>, Throwable>> result2 = new AtomicReference<>();
            provider().info(1, false, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            updateSupplierContinue.countDown();
            latch1.await();
            Assertions.assertTrue(result1.get().getT().isPresent());
            Assertions.assertEquals(file.get(), result1.get().getT().get());
            latch2.await();
            Assertions.assertTrue(result2.get().getT().isPresent());
            Assertions.assertEquals(file.get(), result2.get().getT().get());
            Assertions.assertEquals(List.of("Login.", "Update: 1 f", "Login.", "Update: 1 f"), provider().checkOperations());
        }

        @Test
        public void concurrentException() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch updateSupplierContinue = new CountDownLatch(1);
            provider().update.initialize(() -> {
                latch.countDown();
                try {
                    updateSupplierContinue.await();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
                throw new RuntimeException();
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FileInformation>, Throwable>> result1 = new AtomicReference<>();
            provider().info(1, false, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            provider().update.initialize(() -> {throw new NoSuchElementException();});
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FileInformation>, Throwable>> result2 = new AtomicReference<>();
            provider().info(1, false, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            updateSupplierContinue.countDown();
            latch1.await();
            Assertions.assertSame(RuntimeException.class, result1.get().getE().getClass());
            latch2.await();
            Assertions.assertSame(NoSuchElementException.class, result2.get().getE().getClass());
            Assertions.assertEquals(List.of("Login.", "Update: 1 f", "Login.", "Update: 1 f"), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class RefreshTest {
        @Test
        public void refresh() throws Exception {
            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 1);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
            TimeUnit.MILLISECONDS.sleep(100); // Refresh immediately after list will be skipped.

            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile file2 = AbstractProvider.build(2, 0, false);
            provider().root().add(file1);
            provider().root().add(file2);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 3);
            Assertions.assertTrue(result1.isPresent());
            ProviderHelper.testList(result1.get(), List.of(file1.get(), file2.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            Assertions.assertFalse(ProviderHelper.refresh(provider(), 1));
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void update() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation newFile = new FileInformation(1, 0, "file", false, 1, null, null, "123");
            provider().root().add(new AbstractProvider.AbstractProviderFile(newFile)); // replace
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(newFile), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void delete() throws Exception {
            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile file2 = AbstractProvider.build(2, 0, false);
            provider().root().add(file1);
            provider().root().add(file2);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().root().del(2, false);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Info: 2 f"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(file1.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void deleteNoInfo() throws Exception {
            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            provider().root().add(file1);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().supportInfo.set(false);
            provider().root().del(1, false);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void insert() throws Exception {
            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            provider().root().add(file1);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final AbstractProvider.AbstractProviderFile file2 = AbstractProvider.build(2, 0, false);
            provider().root().add(file2);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(file1.get(), file2.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void complex() throws Exception {
            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile file2 = AbstractProvider.build(2, 0, false);
            provider().root().add(file1);
            provider().root().add(file2);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation newFile = new FileInformation(1, 0, "file", false, 1, null, null, "123");
            provider().root().add(new AbstractProvider.AbstractProviderFile(newFile)); // replace
            final AbstractProvider.AbstractProviderFile file3 = AbstractProvider.build(3, 0, false);
            provider().root().del(2, false);
            provider().root().add(file3);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Info: 2 f"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(newFile, file3.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void exception() {
            provider().list.initialize(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    throw new NoSuchElementException(new RuntimeException());
                }
            });
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.refresh(provider(), 0)
            );
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
        }

        @Test
        public void concurrent() throws Exception {
            final AbstractProvider.AbstractProviderFile info = AbstractProvider.build(1, 0, false);
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch listIteratorNext = new CountDownLatch(1);
            provider().list.initialize(new Iterator<>() {
                private final AtomicBoolean got = new AtomicBoolean(true);
                @Override
                public boolean hasNext() {
                    return this.got.get();
                }

                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        listIteratorNext.await();
                    } catch (final InterruptedException exception) {
                        throw new NoSuchElementException(exception);
                    }
                    this.got.set(false);
                    return info.get();
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> Assertions.assertTrue(ProviderHelper.refresh(provider(), 0)))).addListener(f -> {
                if (f.cause() != null) HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> Assertions.assertTrue(ProviderHelper.refresh(provider(), 0)))).addListener(f -> {
                if (f.cause() != null) HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            TimeUnit.MILLISECONDS.sleep(300); // Wait to ensure the second thread is waiting.
            listIteratorNext.countDown();
            latch1.await();
            latch2.await();
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(info.get()), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void concurrentException() throws Exception {
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch listIteratorNext = new CountDownLatch(1);
            provider().list.initialize(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        listIteratorNext.await();
                    } catch (final InterruptedException exception) {
                        throw new NoSuchElementException(exception);
                    }
                    throw new NoSuchElementException(new RuntimeException());
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            WListServer.IOExecutors.submit(() -> Assertions.assertThrows(RuntimeException.class, () -> ProviderHelper.refresh(provider(), 0)))
                    .addListener(f -> {if (f.cause() != null) HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());latch1.countDown();});
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            WListServer.IOExecutors.submit(HExceptionWrapper.wrapRunnable(() -> Assertions.assertTrue(ProviderHelper.refresh(provider(), 0))))
                    .addListener(f -> {if (f.cause() != null) HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());latch2.countDown();});
            Assumptions.assumeTrue(latch2.getCount() == 1);

            TimeUnit.MILLISECONDS.sleep(300);
            listIteratorNext.countDown();
            latch1.await();
            latch2.await();
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class TrashTest {
        @Test
        public void trash() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(2, 0, false);
            provider().root().add(directory);
            provider().root().add(file);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<Boolean> resultF = ProviderHelper.trash(provider(), 1, true);
            Assertions.assertTrue(resultF.orElseThrow().booleanValue());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 d"), provider().checkOperations());

            final Optional<Boolean> resultD = ProviderHelper.trash(provider(), 2, false);
            Assertions.assertTrue(resultD.orElseThrow().booleanValue());
            Assertions.assertEquals(List.of("Login.", "Trash: 2 f"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
            ProviderHelper.testList(result.get(), List.of(), FilterPolicy.Both);
        }

        @Test
        public void notAvailable() throws Exception {
            final Optional<Boolean> resultF = ProviderHelper.trash(provider(), 1, false);
            Assertions.assertFalse(resultF.orElseThrow().booleanValue());
            Assertions.assertEquals(List.of(), provider().checkOperations());

            final Optional<Boolean> resultD = ProviderHelper.trash(provider(), 1, true);
            Assertions.assertFalse(resultD.orElseThrow().booleanValue());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void root() throws Exception {
            Assertions.assertFalse(ProviderHelper.trash(provider(), 0, true).isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void recursive() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(AbstractProvider.build(2, 1, false));
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 1));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            provider().supportTrashRecursively.set(false);
            Assertions.assertFalse(ProviderHelper.trash(provider(), 1, true).isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void directory() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 1));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            provider().supportTrashRecursively.set(false);
            final Optional<Boolean> result = ProviderHelper.trash(provider(), 1, true);
            Assertions.assertTrue(result.orElseThrow().booleanValue());
            Assertions.assertEquals(List.of("Login.", "List: 1", "Login.", "Trash: 1 d"), provider().checkOperations());
        }

        @Test
        public void exception() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, false));
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().trash.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.trash(provider(), 1, false));
            Assertions.assertEquals(List.of("Login.", "Trash: 1 f"), provider().checkOperations());

            provider().trash.initialize(() -> UnionPair.fail(new RuntimeException()));
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.trash(provider(), 1, false));
            Assertions.assertEquals(List.of("Login.", "Trash: 1 f"), provider().checkOperations());
        }

        @Test
        public void concurrent() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch trashContinue = new CountDownLatch(1);
            provider().trash.initialize(() -> {
                latch.countDown();
                try {
                    trashContinue.await();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
                provider().root().del(1, false);
                return AbstractIdBaseProvider.TrashSuccess;
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<Boolean>, Throwable>> result1 = new AtomicReference<>();
            provider().trash(1, false, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<Boolean>, Throwable>> result2 = new AtomicReference<>();
            provider().trash(1, false, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            trashContinue.countDown();
            latch1.await();
            Assertions.assertTrue(result1.get().getT().isPresent());
            Assertions.assertTrue(result1.get().getT().get().booleanValue());
            latch2.await();
            Assertions.assertTrue(result2.get().getT().isPresent());
            Assertions.assertFalse(result2.get().getT().get().booleanValue());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 f"), provider().checkOperations());
        }

        @Test
        public void concurrentException() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.list(provider(), 0, FilterPolicy.Both, 0, 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch trashContinue = new CountDownLatch(1);
            provider().trash.initialize(() -> {
                latch.countDown();
                try {
                    trashContinue.await();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
                throw new RuntimeException();
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<Boolean>, Throwable>> result1 = new AtomicReference<>();
            provider().trash(1, false, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            provider().trash.initialize(() -> {throw new NoSuchElementException();});
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<Boolean>, Throwable>> result2 = new AtomicReference<>();
            provider().trash(1, false, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            trashContinue.countDown();
            latch1.await();
            Assertions.assertSame(RuntimeException.class, result1.get().getE().getClass());
            latch2.await();
            Assertions.assertSame(NoSuchElementException.class, result2.get().getE().getClass());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 f", "Login.", "Trash: 1 f"), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class CreateTest {
        @Test
        public void create() throws Exception {
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation directory = new FileInformation(1, 0, "directory", true, 0, null, null, null);
            provider().create.initialize(() -> directory);
            Assertions.assertEquals(directory, ProviderHelper.create(provider(), 0, "directory", DuplicatePolicy.ERROR).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 directory"), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            Assertions.assertEquals(FailureKind.NoSuchFile, ProviderHelper.create(provider(), 1, "directory", DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyError() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "directory", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "file", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.create(provider(), 0, "directory", DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.create(provider(), 0, "file", DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyKeep() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "directory", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "file", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation d = new FileInformation(3, 0, "directory (1)", true, 0, null, null, null);
            provider().create.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.create(provider(), 0, "directory", DuplicatePolicy.KEEP).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 directory (1)"), provider().checkOperations());

            final FileInformation f = new FileInformation(4, 0, "file (1)", true, 0, null, null, null);
            provider().create.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.create(provider(), 0, "file", DuplicatePolicy.KEEP).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 file (1)"), provider().checkOperations());
        }

        @Test
        public void policyOver() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "directory", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "file", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().supportTrashRecursively.set(false);
            final FileInformation d = new FileInformation(3, 0, "directory", true, 0, null, null, null);
            provider().create.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.create(provider(), 0, "directory", DuplicatePolicy.OVER).getT());
            Assertions.assertEquals(List.of("Login.", "List: 1", "Login.", "Trash: 1 d", "Login.", "Create: 0 directory"), provider().checkOperations());

            final FileInformation f = new FileInformation(4, 0, "file", true, 0, null, null, null);
            provider().create.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.create(provider(), 0, "file", DuplicatePolicy.OVER).getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 2 f", "Login.", "Create: 0 file"), provider().checkOperations());
        }

        @Test
        public void exception() {
            provider().create.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.create(provider(), 0, "directory", DuplicatePolicy.ERROR));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "Create: 0 directory"), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class CopyTest {
        @Test
        public void copyFile() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation copied = AbstractProvider.build(2, 0, false).get();
            provider().copy.initialize(() -> copied);
            final Optional<UnionPair<FileInformation, Optional<FailureReason>>> result = ProviderHelper.copy(provider(), 1, false, 0, copied.name(), DuplicatePolicy.ERROR);
            Assertions.assertEquals(copied, result.orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Copy: 1 f 0 " + copied.name()), provider().checkOperations());
        }

        @Test
        public void copyDirectory() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(AbstractProvider.build(2, 1, false));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final FileInformation copied = AbstractProvider.build(3, 0, true).get();
            provider().copy.initialize(() -> copied);
            final Optional<UnionPair<FileInformation, Optional<FailureReason>>> result = ProviderHelper.copy(provider(), 1, true, 0, copied.name(), DuplicatePolicy.ERROR);
            Assertions.assertEquals(copied, result.orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Copy: 1 d 0 " + copied.name()), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<UnionPair<FileInformation, Optional<FailureReason>>> resultF = ProviderHelper.copy(provider(), 2, false, 0, "", DuplicatePolicy.ERROR);
            Assertions.assertEquals(FailureKind.NoSuchFile, resultF.orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());

            final Optional<UnionPair<FileInformation, Optional<FailureReason>>> resultD = ProviderHelper.copy(provider(), 1, false, 2, "", DuplicatePolicy.ERROR);
            Assertions.assertEquals(FailureKind.NoSuchFile, resultD.orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyError() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "directory", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "file", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.copy(provider(), 1, true, 0, "directory", DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.copy(provider(), 2, false, 0, "file", DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyKeep() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "directory", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "file", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation d = new FileInformation(3, 0, "directory (1)", true, -1, null, null, null);
            provider().copy.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.copy(provider(), 1, true, 0, "directory", DuplicatePolicy.KEEP).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Copy: 1 d 0 directory (1)"), provider().checkOperations());

            final FileInformation f = new FileInformation(4, 0, "file (1)", false, 0, null, null, null);
            provider().copy.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.copy(provider(), 2, false, 0, "file", DuplicatePolicy.KEEP).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Copy: 2 f 0 file (1)"), provider().checkOperations());
        }

        @Test
        public void policyOver() throws Exception {
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(1, 0, "dd", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "dd-", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(3, 0, "df", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(4, 0, "df-", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(5, 0, "fd", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(6, 0, "fd-", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(7, 0, "ff", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(8, 0, "ff-", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation dd = new FileInformation(10, 0, "dd", true, -1, null, null, "r");
            provider().copy.initialize(() -> dd);
            Assertions.assertEquals(dd, ProviderHelper.copy(provider(), 2, true, 0, "dd", DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 d", "Login.", "Copy: 2 d 0 dd"), provider().checkOperations());

            final FileInformation df = new FileInformation(11, 0, "df", true, -1, null, null, "r");
            provider().copy.initialize(() -> df);
            Assertions.assertEquals(df, ProviderHelper.copy(provider(), 4, true, 0, "df", DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 3 f", "Login.", "Copy: 4 d 0 df"), provider().checkOperations());

            final FileInformation fd = new FileInformation(12, 0, "fd", false, 0, null, null, "r");
            provider().copy.initialize(() -> fd);
            Assertions.assertEquals(fd, ProviderHelper.copy(provider(), 6, false, 0, "fd", DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 5 d", "Login.", "Copy: 6 f 0 fd"), provider().checkOperations());

            final FileInformation ff = new FileInformation(13, 0, "ff", false, 0, null, null, "r");
            provider().copy.initialize(() -> ff);
            Assertions.assertEquals(ff, ProviderHelper.copy(provider(), 8, false, 0, "ff", DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 7 f", "Login.", "Copy: 8 f 0 ff"), provider().checkOperations());
        }

        @Test
        public void self() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(2, 0, true);
            provider().root().add(file);
            provider().root().add(directory);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.copy(provider(), 1, false, 0, file.get().name(), DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.copy(provider(), 2, true, 0, directory.get().name(), DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void inside() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(AbstractProvider.build(2, 1, true));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            ProviderHelper.refresh(provider(), 2);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1", "Login.", "List: 2"), provider().checkOperations());

            Assertions.assertFalse(ProviderHelper.copy(provider(), 1, true, 1, "", DuplicatePolicy.ERROR).orElseThrow().getE().isPresent());
            Assertions.assertFalse(ProviderHelper.copy(provider(), 1, true, 2, "", DuplicatePolicy.ERROR).orElseThrow().getE().isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void exception() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().copy.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.copy(provider(), 1, false, 0, "", DuplicatePolicy.ERROR));
            Assertions.assertEquals(List.of("Login.", "Copy: 1 f 0 "), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class MoveTest {
        @Test
        public void moveFile() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(2, 0, false);
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().add(file);
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final FileInformation moved = AbstractProvider.build(2, 1, false).get();
            provider().move.initialize(() -> moved);
            Assertions.assertEquals(moved, ProviderHelper.move(provider(), 2, false, 1, DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Move: 2 f 1 " + file.get().name()), provider().checkOperations());
        }

        @Test
        public void moveDirectory() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(2, 0, true);
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().add(directory);
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final FileInformation moved = AbstractProvider.build(2, 1, true).get();
            provider().move.initialize(() -> moved);
            Assertions.assertEquals(moved, ProviderHelper.move(provider(), 2, true, 1, DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Move: 2 d 1 " + directory.get().name()), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.NoSuchFile, ProviderHelper.copy(provider(), 2, false, 0, "", DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());

            Assertions.assertEquals(FailureKind.NoSuchFile, ProviderHelper.copy(provider(), 2, false, 1, "", DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyError() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(new AbstractProvider.AbstractProviderFile(new FileInformation(4, 1, "m", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "m", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(3, 0, "m", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.move(provider(), 2, true, 1, DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.move(provider(), 3, false, 1, DuplicatePolicy.ERROR).orElseThrow().getE().orElseThrow().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyKeep() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(new AbstractProvider.AbstractProviderFile(new FileInformation(4, 1, "m", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "m", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(3, 0, "m", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final FileInformation d = new FileInformation(2, 1, "m (1)", true, -1, null, null, null);
            provider().move.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.move(provider(), 2, true, 1, DuplicatePolicy.KEEP).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Move: 2 d 1 m (1)"), provider().checkOperations());

            final FileInformation f = new FileInformation(3, 1, "m (2)", false, 0, null, null, null);
            provider().move.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.move(provider(), 3, false, 1, DuplicatePolicy.KEEP).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Move: 3 f 1 m (2)"), provider().checkOperations());
        }

        @Test
        public void policyOver() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(new AbstractProvider.AbstractProviderFile(new FileInformation(4, 1, "m", false, 0, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(2, 0, "m", true, -1, null, null, null)));
            provider().root().add(new AbstractProvider.AbstractProviderFile(new FileInformation(3, 0, "m", false, 0, null, null, null)));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1"), provider().checkOperations());

            final FileInformation d = new FileInformation(2, 1, "m", true, -1, null, null, null);
            provider().move.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.move(provider(), 2, true, 1, DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 4 f", "Login.", "Move: 2 d 1 m"), provider().checkOperations());

            final FileInformation f = new FileInformation(3, 1, "m", false, 0, null, null, null);
            provider().move.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.move(provider(), 3, false, 1, DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 2 d", "Login.", "Move: 3 f 1 m"), provider().checkOperations());
        }

        @Test
        public void self() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(2, 0, true);
            provider().root().add(file);
            provider().root().add(directory);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(file.get(), ProviderHelper.move(provider(), 1, false, 0, DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(directory.get(), ProviderHelper.move(provider(), 2, true, 0, DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void inside() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().get(1, true).add(AbstractProvider.build(2, 1, true));
            provider().root().get(1, true).get(2, true).add(AbstractProvider.build(3, 2, true));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 1);
            ProviderHelper.refresh(provider(), 2);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 1", "Login.", "List: 2"), provider().checkOperations());

            Assertions.assertFalse(ProviderHelper.move(provider(), 1, true, 2, DuplicatePolicy.ERROR).orElseThrow().getE().isPresent());
            Assertions.assertFalse(ProviderHelper.move(provider(), 1, true, 3, DuplicatePolicy.ERROR).orElseThrow().getE().isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void exception() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(2, 0, false);
            provider().root().add(AbstractProvider.build(1, 0, true));
            provider().root().add(file);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().move.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.move(provider(), 2, false, 1, DuplicatePolicy.ERROR));
            Assertions.assertEquals(List.of("Login.", "List: 1", "Login.", "Move: 2 f 1 " + file.get().name()), provider().checkOperations());
        }
    }

    @Nested
    @SuppressWarnings("UnqualifiedMethodAccess")
    public final class RenameTest {
        @Test
        public void rename() throws Exception {
            provider().root().add(AbstractProvider.build(1, 0, false));
            provider().root().add(AbstractProvider.build(2, 0, true));
            ProviderHelper.refresh(provider(), 0);
            ProviderHelper.refresh(provider(), 2);
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "List: 2"), provider().checkOperations());

            final FileInformation f = new FileInformation(1, 0, "f", false, 0, null, null, null);
            provider().rename.initialize(() -> f);
            Assertions.assertEquals(f, ProviderHelper.rename(provider(), 1, false, "f", DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Rename: 1 f f"), provider().checkOperations());

            final FileInformation d = new FileInformation(2, 0, "d", true, 0, null, null, null);
            provider().rename.initialize(() -> d);
            Assertions.assertEquals(d, ProviderHelper.rename(provider(), 2, true, "d", DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Rename: 2 d d"), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.NoSuchFile, ProviderHelper.rename(provider(), 1, false, "", DuplicatePolicy.ERROR).orElseThrow().getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyError() throws Exception {
            final AbstractProvider.AbstractProviderFile name = AbstractProvider.build(1, 0, true);
            provider().root().add(name);
            provider().root().add(AbstractProvider.build(2, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.rename(provider(), 2, false, name.get().name(), DuplicatePolicy.ERROR).orElseThrow().getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyKeep() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, true);
            provider().root().add(file);
            provider().root().add(AbstractProvider.build(2, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
            final String name = file.get().name();

            final FileInformation r = new FileInformation(2, 0, name + " (1)", false, 0, null, null, null);
            provider().rename.initialize(() -> r);
            Assertions.assertEquals(r, ProviderHelper.rename(provider(), 2, false, name, DuplicatePolicy.KEEP).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Rename: 2 f " + name + " (1)"), provider().checkOperations());
        }

        @Test
        public void policyOver() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, true);
            provider().root().add(file);
            provider().root().add(AbstractProvider.build(2, 0, false));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
            final String name = file.get().name();

            final FileInformation r = new FileInformation(2, 0, name, false, 0, null, null, null);
            provider().rename.initialize(() -> r);
            Assertions.assertEquals(r, ProviderHelper.rename(provider(), 2, false, name, DuplicatePolicy.OVER).orElseThrow().getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 d", "Login.", "Rename: 2 f " + name), provider().checkOperations());
        }

        @Test
        public void self() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(file.get(), ProviderHelper.rename(provider(), 1, false, file.get().name(), DuplicatePolicy.ERROR).orElseThrow().getT());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void exception() throws Exception {
            final AbstractProvider.AbstractProviderFile file = AbstractProvider.build(1, 0, false);
            provider().root().add(file);
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().rename.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.rename(provider(), 1, false, "", DuplicatePolicy.ERROR));
            Assertions.assertEquals(List.of("Login.", "Rename: 1 f "), provider().checkOperations());
        }
    }
}
