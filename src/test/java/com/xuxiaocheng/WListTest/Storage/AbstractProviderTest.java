package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
    public void reset() throws Exception {
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, list.size());
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), list, Options.FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> resultD = ProviderHelper.list(provider(), 0, Options.FilterPolicy.OnlyDirectories, 0, list.size());
            Assertions.assertTrue(resultD.isPresent());
            ProviderHelper.testList(resultD.get(), list, Options.FilterPolicy.OnlyDirectories);
            Assertions.assertEquals(List.of(), provider().checkOperations());

            final Optional<FilesListInformation> resultF = ProviderHelper.list(provider(), 0, Options.FilterPolicy.OnlyFiles, 0, list.size());
            Assertions.assertTrue(resultF.isPresent());
            ProviderHelper.testList(resultF.get(), list, Options.FilterPolicy.OnlyFiles);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 1, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertFalse(result1.isPresent());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void delete() throws Exception {
            final AbstractProvider.AbstractProviderFile directory = AbstractProvider.build(1, 0, true);
            provider().root().add(directory);
            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(directory.get()), Options.FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().root().del(1, true);
            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 1, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertFalse(result1.isPresent());
            Assertions.assertEquals(List.of("Login.", "List: 1"), provider().checkOperations());

            final Optional<FilesListInformation> result2 = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result2.isPresent());
            ProviderHelper.testList(result2.get(), List.of(), Options.FilterPolicy.Both);
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
                    ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5)
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
            final AtomicReference<UnionPair<Optional<FilesListInformation>, Throwable>> result1 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FilesListInformation>, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            Assertions.assertTrue(result1.get().getT().isPresent());
            ProviderHelper.testList(result1.get().getT().get(), List.of(info.get()), Options.FilterPolicy.Both);
            latch2.await();
            Assertions.assertTrue(result2.get().getT().isPresent());
            ProviderHelper.testList(result2.get().getT().get(), List.of(info.get()), Options.FilterPolicy.Both);

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
            final AtomicReference<UnionPair<Optional<FilesListInformation>, Throwable>> result1 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<Optional<FilesListInformation>, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            Assertions.assertSame(RuntimeException.class, result1.get().getE().getClass());
            latch2.await();
            Assertions.assertTrue(result2.get().getT().isPresent());
            ProviderHelper.testList(result2.get().getT().get(), List.of(), Options.FilterPolicy.Both);

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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
            ProviderHelper.list(provider(), 1, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
        @AfterEach
        public void finish() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(500); // Wait for broadcast finish.
        }

        @Test
        public void refresh() throws Exception {
            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 1);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), Options.FilterPolicy.Both);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());
            TimeUnit.MILLISECONDS.sleep(100); // Refresh immediately after list will be skipped.

            final AbstractProvider.AbstractProviderFile file1 = AbstractProvider.build(1, 0, false);
            final AbstractProvider.AbstractProviderFile file2 = AbstractProvider.build(2, 0, false);
            provider().root().add(file1);
            provider().root().add(file2);
            Assertions.assertTrue(ProviderHelper.refresh(provider(), 0));
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result1 = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 3);
            Assertions.assertTrue(result1.isPresent());
            ProviderHelper.testList(result1.get(), List.of(file1.get(), file2.get()), Options.FilterPolicy.Both);
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(newFile), Options.FilterPolicy.Both);
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(file1.get()), Options.FilterPolicy.Both);
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), Options.FilterPolicy.Both);
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(file1.get(), file2.get()), Options.FilterPolicy.Both);
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

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(newFile, file3.get()), Options.FilterPolicy.Both);
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
            provider().refreshDirectory(0, p -> {
                latch1.countDown();
                Assertions.assertTrue(p.getT().booleanValue());
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            provider().refreshDirectory(0, p -> {
                latch2.countDown();
                Assertions.assertTrue(p.getT().booleanValue());
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            latch2.await();
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(info.get()), Options.FilterPolicy.Both);
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
            provider().refreshDirectory(0,  p -> {
                latch1.countDown();
                Assertions.assertSame(RuntimeException.class, p.getE().getClass());
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            provider().refreshDirectory(0, p -> { // Not work due to the policy that ignore rapid refresh.
                latch2.countDown();
                Assertions.assertTrue(p.getT().booleanValue());
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);

            listIteratorNext.countDown();
            latch1.await();
            latch2.await();
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), Options.FilterPolicy.Both);
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
            Assertions.assertTrue(resultF.isPresent() && resultF.get().booleanValue());
            Assertions.assertEquals(List.of("Login.", "Trash: 1 d"), provider().checkOperations());

            final Optional<Boolean> resultD = ProviderHelper.trash(provider(), 2, false);
            Assertions.assertTrue(resultD.isPresent() && resultD.get().booleanValue());
            Assertions.assertEquals(List.of("Login.", "Trash: 2 f"), provider().checkOperations());

            final Optional<FilesListInformation> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 5);
            Assertions.assertTrue(result.isPresent());
            ProviderHelper.testList(result.get(), List.of(), Options.FilterPolicy.Both);
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            final Optional<Boolean> resultF = ProviderHelper.trash(provider(), 1, false);
            Assertions.assertTrue(resultF.isPresent() && !resultF.get().booleanValue());
            Assertions.assertEquals(List.of(), provider().checkOperations());

            final Optional<Boolean> resultD = ProviderHelper.trash(provider(), 1, true);
            Assertions.assertTrue(resultD.isPresent() && !resultD.get().booleanValue());
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
            Assertions.assertTrue(result.isPresent() && result.get().booleanValue());
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, 0, 0);
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
            Assertions.assertEquals(directory, ProviderHelper.create(provider(), 0, "directory", Options.DuplicatePolicy.ERROR).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 directory"), provider().checkOperations());
        }

        @Test
        public void notAvailable() throws Exception {
            Assertions.assertEquals(FailureKind.NoSuchFile, ProviderHelper.create(provider(), 1, "directory", Options.DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyError() throws Exception {
            final FileInformation d = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation f = new FileInformation(2, 0, "file", false, 0, null, null, null);
            provider().root().add(new AbstractProvider.AbstractProviderFile(d));
            provider().root().add(new AbstractProvider.AbstractProviderFile(f));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.create(provider(), 0, "directory", Options.DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.create(provider(), 0, "file", Options.DuplicatePolicy.ERROR).getE().kind());
            Assertions.assertEquals(List.of(), provider().checkOperations());
        }

        @Test
        public void policyKeep() throws Exception {
            final FileInformation d = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation f = new FileInformation(2, 0, "file", false, 0, null, null, null);
            provider().root().add(new AbstractProvider.AbstractProviderFile(d));
            provider().root().add(new AbstractProvider.AbstractProviderFile(f));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            final FileInformation d1 = new FileInformation(3, 0, "directory (1)", true, 0, null, null, null);
            provider().create.initialize(() -> d1);
            Assertions.assertEquals(d1, ProviderHelper.create(provider(), 0, "directory", Options.DuplicatePolicy.KEEP).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 directory (1)"), provider().checkOperations());

            final FileInformation f1 = new FileInformation(4, 0, "file (1)", true, 0, null, null, null);
            provider().create.initialize(() -> f1);
            Assertions.assertEquals(f1, ProviderHelper.create(provider(), 0, "file", Options.DuplicatePolicy.KEEP).getT());
            Assertions.assertEquals(List.of("Login.", "Create: 0 file (1)"), provider().checkOperations());
        }

        @Test
        public void policyOver() throws Exception {
            final FileInformation d = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation f = new FileInformation(2, 0, "file", false, 0, null, null, null);
            provider().root().add(new AbstractProvider.AbstractProviderFile(d));
            provider().root().add(new AbstractProvider.AbstractProviderFile(f));
            ProviderHelper.refresh(provider(), 0);
            Assertions.assertEquals(List.of("Login.", "List: 0"), provider().checkOperations());

            provider().supportTrashRecursively.set(false);
            final FileInformation d1 = new FileInformation(3, 0, "directory", true, 0, null, null, null);
            provider().create.initialize(() -> d1);
            Assertions.assertEquals(d1, ProviderHelper.create(provider(), 0, "directory", Options.DuplicatePolicy.OVER).getT());
            Assertions.assertEquals(List.of("Login.", "List: 1", "Login.", "Trash: 1 d", "Login.", "Create: 0 directory"), provider().checkOperations());

            final FileInformation f1 = new FileInformation(4, 0, "file", true, 0, null, null, null);
            provider().create.initialize(() -> f1);
            Assertions.assertEquals(f1, ProviderHelper.create(provider(), 0, "file", Options.DuplicatePolicy.OVER).getT());
            Assertions.assertEquals(List.of("Login.", "Trash: 2 f", "Login.", "Create: 0 file"), provider().checkOperations());
        }

        @Test
        public void exception() {
            provider().create.initialize(() -> {throw new RuntimeException();});
            Assertions.assertThrows(RuntimeException.class, () ->
                    ProviderHelper.create(provider(), 0, "directory", Options.DuplicatePolicy.ERROR));
            Assertions.assertEquals(List.of("Login.", "List: 0", "Login.", "Create: 0 directory"), provider().checkOperations());
        }
    }

//    @Nested
//    @SuppressWarnings("UnqualifiedMethodAccess")
//    public class CopyTest {
//        @Test
//        public void copy() throws Exception {
//            final FileInformation information = new FileInformation(1, 0, "file", false, 0, null, null, null);
//            list.set(List.of(information).iterator());
//            ProviderHelper.refresh(provider(), 0); // list
//            final FileInformation copied = new FileInformation(2, 0, "copied", false, 0, null, null, null);
//            copy.set(copied);
//            Assertions.assertEquals(copied, ProviderHelper.copy(provider(), 1, 0, "copied", Options.DuplicatePolicy.ERROR).getT().get());
//        }
//
//        @Test
//        public void duplicate() throws Exception {
//            final FileInformation information = new FileInformation(1, 0, "file", false, 0, null, null, null);
//            final FileInformation directory = new FileInformation(10, 0, "directory", true, -1, null, null, null);
//            list.set(List.of(information, directory).iterator());
//            ProviderHelper.refresh(provider(), 0);
//
//            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.copy(provider(), 1, 0, "file", Options.DuplicatePolicy.ERROR).getE().kind());
//
//            final FileInformation keep = new FileInformation(2, 0, "file (1)", false, 0, null, null, null);
//            copy.set(keep);
//            Assertions.assertEquals(keep, ProviderHelper.copy(provider(), 1, 0, "file", Options.DuplicatePolicy.KEEP).getT().get());
//
//            final FileInformation overed = new FileInformation(3, 10, "file", false, 1, null, null, null);
//            list.set(List.of(overed).iterator());
//            ProviderHelper.refresh(provider(), 10);
//            final FileInformation over = new FileInformation(4, 10, "file", false, 0, null, null, null);
//            copy.set(over);
//            Assertions.assertEquals(over, ProviderHelper.copy(provider(), 1, 10, "file", Options.DuplicatePolicy.OVER).getT().get());
//            Assertions.assertEquals(overed, trash.uninitialize());
//
//            // Copy itself.
//            Assertions.assertTrue(ProviderHelper.copy(provider(), 1, 0, "file", Options.DuplicatePolicy.OVER).getT().isEmpty());
//        }
//    }
//
//    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess", "OptionalGetWithoutIsPresent"})
//    @Nested
//    public class MoveTest {
//        @Test
//        public void move() throws Exception {
//            final FileInformation information = new FileInformation(1, 0, "file", false, 123, null, null, null);
//            final FileInformation directory = new FileInformation(2, 0, "directory", true, -1, null, null, null);
//            list.set(List.of(information, directory).iterator());
//            ProviderHelper.refresh(provider(), 0);
//            list.set(Collections.emptyIterator());
//            ProviderHelper.list(provider(), 2, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
//
//            final FileInformation moved = new FileInformation(1, 2, "file", false, 123, null, null, null);
//            move.set(moved);
//            Assertions.assertEquals(moved, ProviderHelper.move(provider(), 1, false, 2, Options.DuplicatePolicy.ERROR).getT().get());
//        }
//    }
}
