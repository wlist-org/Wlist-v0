package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
public class AbstractProviderTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.parseFromFile();
        StorageManager.initialize(new File(AbstractProviderTest.directory, "configs"), new File(AbstractProviderTest.directory, "caches"));
    }

    public static class AbstractConfiguration extends StorageConfiguration {
    }

    protected final AtomicBoolean loggedIn = new AtomicBoolean();
    protected final AtomicReference<Iterator<FileInformation>> list = new AtomicReference<>();
    protected final AtomicReference<Supplier<UnionPair<FileInformation, Boolean>>> updated = new AtomicReference<>();
    protected final AtomicBoolean supportedInfo = new AtomicBoolean();
    protected final Map<Long, FileInformation> info = new HashMap<>();
    protected final HInitializer<FileInformation> trash = new HInitializer<>("Trashed");
    protected final AtomicReference<FileInformation> create = new AtomicReference<>();

    public class AbstractProvider extends AbstractIdBaseProvider<AbstractConfiguration> {
        @Override
        public @NotNull StorageTypes<AbstractConfiguration> getType() {
            throw new RuntimeException("Unreachable.");
        }

        @Override
        protected void loginIfNot() {
            AbstractProviderTest.this.loggedIn.set(true);
        }

        @Override
        protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) {
            return AbstractProviderTest.this.list.getAndSet(null);
        }

        @Override
        protected @NotNull UnionPair<FileInformation, Boolean> update0(final @NotNull FileInformation oldInformation) {
            return Objects.requireNonNull(AbstractProviderTest.this.updated.getAndSet(null)).get();
        }

        @Override
        protected boolean isSupportedInfo() {
            return AbstractProviderTest.this.supportedInfo.get();
        }
        @Override
        protected @Nullable FileInformation info0(final long id, final boolean isDirectory) {
            return AbstractProviderTest.this.info.get(id);
        }

        @Override
        protected boolean isSupportedNotEmptyDirectoryTrash() {
            return true;
        }

        @Override
        protected void trash0(final @NotNull FileInformation information) {
            AbstractProviderTest.this.trash.initialize(information);
        }

        @Override
        protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) {
            throw new UnsupportedOperationException("Not tested.");
        }

        @Override
        protected @NotNull CheckRule<@NotNull String> directoryNameChecker() {
            return CheckRule.allAllow();
        }

        @Override
        protected void createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) {
            final FileInformation information = AbstractProviderTest.this.create.getAndSet(null);
            if (information == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, directoryName, "For test."))));
                return;
            }
            Assertions.assertEquals(parentId, information.parentId());
            Assertions.assertEquals(directoryName, information.name());
            consumer.accept(UnionPair.ok(UnionPair.ok(information)));
        }

        @Override
        protected @NotNull CheckRule<@NotNull String> fileNameChecker() {
            return CheckRule.allAllow();
        }

        @Override
        protected void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) {

        }
    }

    protected static final ThreadLocal<ProviderInterface<AbstractConfiguration>> provider = new ThreadLocal<>();
    public ProviderInterface<?> provider() {
        return AbstractProviderTest.provider.get();
    }

    protected HInitializer<Supplier<ProviderInterface<AbstractConfiguration>>> ProviderCore = new HInitializer<>("Provider", AbstractProvider::new);

    @BeforeEach
    public void reset() throws Exception {
        this.loggedIn.set(false);
        this.list.set(null);
        this.updated.set(null);
        this.supportedInfo.set(false);
        this.info.clear();
        this.trash.uninitializeNullable();
        this.create.set(null);
        final ProviderInterface<AbstractConfiguration> provider = this.ProviderCore.getInstance().get();
        AbstractProviderTest.provider.set(provider);
        final AbstractConfiguration configuration = new AbstractConfiguration();
        synchronized (AbstractProviderTest.class) {
            configuration.setName(String.valueOf(System.currentTimeMillis()));
            TimeUnit.MILLISECONDS.sleep(10);
        }
        provider.initialize(configuration);
    }

    @AfterEach
    public void unset() throws Exception {
        this.provider().uninitialize(true);
    }

    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess"})
    @Nested
    @Disabled // Ok
    public class ListTest {
        public static Stream<List<FileInformation>> list() {
            return Stream.of(null,
                    List.of(new FileInformation(1, 0, "directory", true, -1, null, null, null),
                            new FileInformation(2, 0, "file", false, 1, null, null, null)),
                    List.of(new FileInformation(1, 0, "directory", true, -1, null, null, null),
                            new FileInformation(2, 0, "file1", false, 1, null, null, null),
                            new FileInformation(3, 0, "file2", false, 1, null, null, null)),
                    List.of(new FileInformation(1, 0, "directory1", true, -1, null, null, null),
                            new FileInformation(2, 0, "directory2", true, -1, null, null, null),
                            new FileInformation(3, 0, "file", false, 1, null, null, null))
            ).skip(1);
        }

        @ParameterizedTest
        @MethodSource
        public void list(final @NotNull Collection<FileInformation> collection) throws Exception {
            list.set(collection.iterator());
            final UnionPair<FilesListInformation, Boolean> resultD = ProviderHelper.list(provider(), 0, Options.FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, collection.size());
            Assertions.assertTrue(loggedIn.get());
            Assertions.assertNull(list.get());
            Assertions.assertTrue(resultD.isSuccess());
            ProviderHelper.testList(resultD.getT(), collection, Options.FilterPolicy.OnlyDirectories);

            loggedIn.set(false);
            final UnionPair<FilesListInformation, Boolean> resultF = ProviderHelper.list(provider(), 0, Options.FilterPolicy.OnlyFiles, VisibleFileInformation.emptyOrder(), 0, collection.size());
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertNull(list.get());
            Assertions.assertTrue(resultF.isSuccess());
            ProviderHelper.testList(resultF.getT(), collection, Options.FilterPolicy.OnlyFiles);
        }

        @Test
        public void notAvailable() throws Exception {
            list.set(null);
            final UnionPair<FilesListInformation, Boolean> result = ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5);
            Assertions.assertTrue(loggedIn.get());
            Assertions.assertTrue(result.getE().booleanValue());

            loggedIn.set(false);
            final UnionPair<FilesListInformation, Boolean> result1 = ProviderHelper.list(provider(), 1, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5);
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertFalse(result1.getE().booleanValue());
        }

        @Test
        public void exception() {
            list.set(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    throw new NoSuchElementException("For test.");
                }
            });
            Assertions.assertThrows(NoSuchElementException.class, () ->
                    ProviderHelper.list(provider(), 0, Options.FilterPolicy.OnlyDirectories, VisibleFileInformation.emptyOrder(), 0, 5)
            );
        }

        @Test
        public void concurrent() throws Exception {
            final FileInformation info = new FileInformation(1, 0, "file", false, 1, null, null, null);
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch latch0 = new CountDownLatch(1);
            list.set(new Iterator<>() {
                private final AtomicBoolean got = new AtomicBoolean(true);
                @Override
                public boolean hasNext() {
                    return got.get();
                }

                @SuppressWarnings("IteratorNextCanNotThrowNoSuchElementException")
                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        latch0.await();
                    } catch (final InterruptedException ignore) {
                    }
                    got.set(false);
                    return info;
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> result1 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            Assertions.assertTrue(loggedIn.get());
            final CountDownLatch latch2 = new CountDownLatch(1);
            loggedIn.set(false);
            final AtomicReference<UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);
            list.set(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @SuppressWarnings("IteratorNextCanNotThrowNoSuchElementException")
                @Override
                public FileInformation next() {
                    throw new RuntimeException("Shouldn't be got.");
                }
            });
            latch0.countDown();
            latch1.await();
            ProviderHelper.testList(result1.get().getT().getT(), List.of(info), Options.FilterPolicy.Both);
            latch2.await();
            Assertions.assertFalse(loggedIn.get());
            ProviderHelper.testList(result2.get().getT().getT(), List.of(info), Options.FilterPolicy.Both);
        }

        @Test
        public void concurrentException() throws Exception {
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch latch0 = new CountDownLatch(1);
            list.set(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public FileInformation next() {
                    latch.countDown();
                    try {
                        latch0.await();
                    } catch (final InterruptedException ignore) {
                    }
                    throw new NoSuchElementException("For test.");
                }
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> result1 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            Assertions.assertTrue(loggedIn.get());
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<UnionPair<FilesListInformation, Boolean>, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);
            list.set(Collections.emptyIterator());
            latch0.countDown();
            latch1.await();
            Assertions.assertInstanceOf(NoSuchElementException.class, result1.get().getE());
            latch2.await();
            ProviderHelper.testList(result2.get().getT().getT(), List.of(), Options.FilterPolicy.Both);
        }
    }

    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess"})
    @Nested
    @Disabled // Ok
    public class InfoTest {
        @Test
        public void info() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(1, 0, "file", false, 1, null, null, null);
            list.set(List.of(directory, file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 1, true).getT(), directory, false);
            Assertions.assertNull(updated.get());

            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 1, false).getT(), file, false);
        }

        @Test
        public void notAvailable() throws Exception {
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            Assertions.assertFalse(ProviderHelper.info(provider(), 0, true).getT().getSecond().booleanValue());
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertNull(updated.get());

            Assertions.assertFalse(ProviderHelper.info(provider(), 0, false).getE().booleanValue());
            Assertions.assertFalse(loggedIn.get());
        }

        @Test
        public void update() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(1, 0, "file", false, 1, null, null, null);
            list.set(List.of(directory, file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            final FileInformation newFile = new FileInformation(1, 0, "file", false, 1, null, null, "123");
            updated.set(() -> UnionPair.ok(newFile));
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 1, false).getT(), newFile, true);

            updated.set(() -> AbstractIdBaseProvider.UpdateNotExisted);
            Assertions.assertTrue(ProviderHelper.info(provider(), 1, false).getE().booleanValue());
            Assertions.assertFalse(ProviderHelper.info(provider(), 1, false).getE().booleanValue());
        }

        @Test
        public void concurrent() throws Exception {
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            final FileInformation info = ProviderHelper.info(provider(), 0, true).getT().getFirst();
            final FileInformation newInfo = new FileInformation(0, 0, "123", true, info.size(),
                    info.createTime(), info.updateTime(), "123");

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch latch0 = new CountDownLatch(1);
            updated.set(() -> {
                latch.countDown();
                try {
                    latch0.await();
                } catch (final InterruptedException ignore) {
                }
                return UnionPair.ok(newInfo);
            });
            final CountDownLatch latch1 = new CountDownLatch(1);
            final AtomicReference<UnionPair<UnionPair<Pair.ImmutablePair<FileInformation, Boolean>, Boolean>, Throwable>> result1 = new AtomicReference<>();
            provider().info(0, true, p -> {
                result1.set(p);
                latch1.countDown();
            });
            latch.await();
            Assumptions.assumeTrue(latch1.getCount() == 1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<UnionPair<Pair.ImmutablePair<FileInformation, Boolean>, Boolean>, Throwable>> result2 = new AtomicReference<>();
            provider().info(0, true, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assumptions.assumeTrue(latch2.getCount() == 1);
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            latch0.countDown();
            latch1.await();
            ProviderHelper.testInfo(result1.get().getT().getT(), newInfo, true);
            latch2.await();
            Assertions.assertFalse(loggedIn.get());
            ProviderHelper.testInfo(result2.get().getT().getT(), newInfo, false);
        }
    }

    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess"})
    @Nested
    @Disabled // Ok
    public class RefreshTest {
        @Test
        public void refresh() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(2, 0, "file", false, 1, null, null, null);
            list.set(List.of(directory, file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            final FileInformation directory0 = new FileInformation(1, 0, "directory0", true, -1, null, null, null);
            list.set(List.of(directory0, file).iterator());
            ProviderHelper.testRefresh(ProviderHelper.refresh(provider(), 0).getT(), Set.of(), Set.of());

            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 1, true).getT(), directory0, false);
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 2, false).getT(), file, false);
        }

        @Test
        public void delete() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(2, 0, "file", false, 1, null, null, null);
            list.set(List.of(directory, file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            list.set(List.of(file).iterator());
            ProviderHelper.testRefresh(ProviderHelper.refresh(provider(), 0).getT(), Set.of(), Set.of());

            Assertions.assertFalse(ProviderHelper.info(provider(), 1, true).getE().booleanValue());
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 2, false).getT(), file, false);
        }

        @Test
        public void insert() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(2, 0, "file", false, 1, null, null, null);
            list.set(List.of(file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            list.set(List.of(directory, file).iterator());
            ProviderHelper.testRefresh(ProviderHelper.refresh(provider(), 0).getT(), Set.of(), Set.of());

            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 1, true).getT(), directory, false);
            updated.set(() -> AbstractIdBaseProvider.UpdateNoRequired);
            ProviderHelper.testInfo(ProviderHelper.info(provider(), 2, false).getT(), file, false);
        }

        // No concurrent test due to same code as 'list'.
    }

    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess"})
    @Nested
    @Disabled // Ok
    public class TrashTest {
        @Test
        public void trash() throws Exception {
            final FileInformation directory = new FileInformation(1, 0, "directory", true, -1, null, null, null);
            final FileInformation file = new FileInformation(1, 0, "file", false, 1, null, null, null);
            list.set(List.of(directory, file).iterator());
            ProviderHelper.list(provider(), 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
            loggedIn.set(false);

            Assertions.assertTrue(ProviderHelper.trash(provider(), 1, true));
            Assertions.assertEquals(directory, trash.uninitialize());
            Assertions.assertTrue(ProviderHelper.trash(provider(), 1, false));
            Assertions.assertEquals(file, trash.uninitialize());
        }

        @Test
        public void notAvailable() throws Exception {
            Assertions.assertFalse(ProviderHelper.trash(provider(), 0, true));
            Assertions.assertFalse(ProviderHelper.trash(provider(), 0, false));
            Assertions.assertFalse(loggedIn.get());
        }

        public class TrashTestProvider extends AbstractProvider {
            private final Collection<String> operations = new ArrayList<>();
            private final Map<Long, Iterator<List<FileInformation>>> lister = new HashMap<>();

            @Override
            public void refreshDirectory(final long directoryId, final @NotNull Consumer<? super UnionPair<UnionPair<Pair.ImmutablePair<Set<Long>, Set<Long>>, Boolean>, Throwable>> consumer) {
                operations.add("Refresh: " + directoryId);
                consumer.accept(ProviderInterface.RefreshNoUpdater);
            }

            @Override
            protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) {
                operations.add("List: " + directoryId);
                if (lister.get(directoryId).hasNext())
                    return lister.get(directoryId).next().iterator();
                return Collections.emptyIterator();
            }

            @Override
            protected boolean isSupportedNotEmptyDirectoryTrash() {
                return false;
            }

            @Override
            protected void trash0(final @NotNull FileInformation information) {
                operations.add("Trash: " + information.id());
            }

            @Override
            public @NotNull String toString() {
                return "TrashTestProvider{" +
                        "operations=" + operations +
                        ", super=" + super.toString() +
                        '}';
            }
        }

        @Test
        public void trashRecursively() throws Exception {
            AbstractProviderTest.this.unset();
            ProviderCore.reinitialize(TrashTestProvider::new);
            AbstractProviderTest.this.reset();
            try {
                Assumptions.assumeTrue(provider() instanceof TrashTestProvider);
                final TrashTestProvider provider = (TrashTestProvider) provider();
                provider.lister.put(0L, List.of(
                        List.of(
                                new FileInformation(1, 0, "", true, -1, null, null, null)
                        )
                ).iterator());
                ProviderHelper.list(provider, 0, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 0);
                provider.lister.remove(0L);

                provider.lister.put(1L, List.of(
                        List.of(
                                new FileInformation(2, 1, "", true, -1, null, null, null)
                        )
                ).iterator());
                provider.lister.put(2L, List.of(
                        List.of(
                                new FileInformation(3, 2, "", false, 1, null, null, null)
                        )
                ).iterator());

                Assertions.assertTrue(ProviderHelper.trash(provider, 1, true));

                Assertions.assertEquals(List.of(
                        "List: 0", "List: 1", "List: 2", "Trash: 3", "Refresh: 2", "Trash: 2", "Refresh: 1", "Trash: 1"
                ), provider.operations);
            } finally {
                ProviderCore.reinitialize(AbstractProvider::new);
            }
        }
    }

    @SuppressWarnings({"UnqualifiedMethodAccess", "UnqualifiedFieldAccess"})
    @Nested
//    @Disabled // Ok
    public class CreateTest {
        @Test
        public void create() throws Exception {
            list.set(Collections.emptyIterator());
            final FileInformation information = new FileInformation(1, 0, "1", true, 0, null, null, null);
            create.set(information);
            Assertions.assertEquals(information, ProviderHelper.create(provider(), 0, "1", Options.DuplicatePolicy.ERROR).getT());
        }

        @Test
        public void duplicate() throws Exception {
            list.set(Collections.emptyIterator());
            final FileInformation information = new FileInformation(1, 0, "1", true, 0, null, null, null);
            create.set(information);
            Assertions.assertEquals(information, ProviderHelper.create(provider(), 0, "1", Options.DuplicatePolicy.ERROR).getT());

            Assertions.assertEquals(FailureKind.DuplicateError, ProviderHelper.create(provider(), 0, "1", Options.DuplicatePolicy.ERROR).getE().kind());

            final FileInformation keep = new FileInformation(2, 0, "1 (1)", true, 0, null, null, null);
            create.set(keep);
            Assertions.assertEquals(keep, ProviderHelper.create(provider(), 0, "1", Options.DuplicatePolicy.KEEP).getT());

            final FileInformation over = new FileInformation(3, 0, "1", true, 0, null, null, "123");
            create.set(over);
            Assertions.assertEquals(over, ProviderHelper.create(provider(), 0, "1", Options.DuplicatePolicy.OVER).getT());
            Assertions.assertEquals(information, trash.uninitialize());
        }
    }
}
