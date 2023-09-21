package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
public class AbstractTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.parseFromFile();
        StorageManager.initialize(new File(AbstractTest.directory, "configs"), new File(AbstractTest.directory, "caches"));
    }

    public static class AbstractConfiguration extends ProviderConfiguration {
    }

    protected final AtomicBoolean loggedIn = new AtomicBoolean();
    protected final AtomicReference<Iterator<FileInformation>> list = new AtomicReference<>();

    public class AbstractProvider extends AbstractIdBaseProvider<AbstractConfiguration> {
        @Override
        public @NotNull ProviderTypes<?> getType() {
//            return ProviderTypes.Lanzou;
            throw new RuntimeException("Unreachable.");
        }

        @Override
        protected void loginIfNot() {
            AbstractTest.this.loggedIn.set(true);
        }

        @Override
        protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) {
            return AbstractTest.this.list.getAndSet(null);
        }

        @Override
        protected @NotNull UnionPair<FileInformation, Boolean> update0(final @NotNull FileInformation oldInformation) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @Nullable FileInformation info0(final long id, final boolean isDirectory) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void delete0(final @NotNull FileInformation information) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @NotNull UnionPair<DownloadRequirements, FailureReason> download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull FileLocation location) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @NotNull CheckRule<@NotNull String> nameChecker() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @NotNull UnionPair<FileInformation, FailureReason> createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull FileLocation parentLocation) {
            throw new UnsupportedOperationException();
        }
    }

    protected static final ThreadLocal<ProviderInterface<AbstractConfiguration>> provider = new ThreadLocal<>();
    public ProviderInterface<?> provider() {
        return AbstractTest.provider.get();
    }

    @BeforeEach
    public void reset() throws Exception {
        this.loggedIn.set(false);
        this.list.set(null);
        final ProviderInterface<AbstractConfiguration> provider = new AbstractProvider();
        AbstractTest.provider.set(provider);
        final AbstractConfiguration configuration = new AbstractConfiguration();
        synchronized (AbstractTest.class) {
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
    public class ListTest {
        public static Stream<List<FileInformation>> filter() {
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
        public void filter(final Collection<FileInformation> collection) throws InterruptedException {
            list.set(collection.iterator());
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.OnlyDirectories, new LinkedHashMap<>(), 0, collection.size(), p -> {
                result.set(p);
                latch.countDown();
            });
            latch.await();
            Assertions.assertTrue(loggedIn.get());
            Assertions.assertNull(list.get());
            Assertions.assertNotNull(result.get());
            Assertions.assertTrue(result.get().isSuccess());
            final FilesListInformation information = result.get().getT();
            Assertions.assertEquals(collection.size(), information.total());
            Assertions.assertEquals(collection.stream().filter(FileInformation::isDirectory).count(), information.filtered());
            Assertions.assertEquals(collection.stream().filter(FileInformation::isDirectory).collect(Collectors.toList()), information.informationList());
            loggedIn.set(false);
            final CountDownLatch latch1 = new CountDownLatch(1);
            provider().list(0, Options.FilterPolicy.OnlyFiles, new LinkedHashMap<>(), 0, collection.size(), p -> {
                result.set(p);
                latch1.countDown();
            });
            latch1.await();
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertNull(list.get());
            Assertions.assertNotNull(result.get());
            Assertions.assertTrue(result.get().isSuccess());
            final FilesListInformation information1 = result.get().getT();
            Assertions.assertEquals(collection.size(), information1.total());
            Assertions.assertEquals(collection.stream().filter(Predicate.not(FileInformation::isDirectory)).count(), information1.filtered());
            Assertions.assertEquals(collection.stream().filter(Predicate.not(FileInformation::isDirectory)).collect(Collectors.toList()), information1.informationList());
        }

        @Test
        public void notAvailable() throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
            provider().list(1, Options.FilterPolicy.OnlyDirectories, new LinkedHashMap<>(), 0, 5, p -> {
                result.set(p);
                latch.countDown();
            });
            latch.await();
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertNull(result.get());
            final CountDownLatch latch0 = new CountDownLatch(1);
            provider().list(0, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 5, p -> {
                result.set(p);
                latch0.countDown();
            });
            latch0.await();
            Assertions.assertTrue(loggedIn.get());
            Assertions.assertNull(result.get());
        }

        @Test
        public void exception() throws InterruptedException {
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
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.OnlyDirectories, new LinkedHashMap<>(), 0, 5, p -> {
                result.set(p);
                latch.countDown();
            });
            Assertions.assertEquals(1, latch.getCount());
            Assertions.assertTrue(loggedIn.get());
            latch.await();
            Assertions.assertNotNull(result.get());
            Assertions.assertTrue(result.get().isFailure());
            Assertions.assertThrows(NoSuchElementException.class, () -> {throw result.get().getE();});
        }

        @Test
        public void concurrentException() throws InterruptedException {
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
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 5, p -> {
                result.set(p);
                latch1.countDown();
            });
            latch.await();
            Assertions.assertEquals(1, latch1.getCount());
            Assertions.assertTrue(loggedIn.get());
            final CountDownLatch latch2 = new CountDownLatch(1);
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assertions.assertEquals(1, latch2.getCount());
            list.set(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @SuppressWarnings("IteratorNextCanNotThrowNoSuchElementException")
                @Override
                public FileInformation next() {
                    throw new RuntimeException("For test. Again.");
                }
            });
            latch0.countDown();
            latch1.await();
            Assertions.assertNotNull(result.get());
            Assertions.assertTrue(result.get().isFailure());
            Assertions.assertThrows(NoSuchElementException.class, () -> {throw result.get().getE();});
            latch2.await();
            Assertions.assertNotNull(result2.get());
            Assertions.assertTrue(result2.get().isFailure());
            Assertions.assertThrows(RuntimeException.class, () -> {throw result2.get().getE();});
        }

        @Test
        public void concurrent() throws InterruptedException {
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
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 5, p -> {
                result.set(p);
                latch1.countDown();
            });
            latch.await();
            Assertions.assertEquals(1, latch1.getCount());
            Assertions.assertTrue(loggedIn.get());
            final CountDownLatch latch2 = new CountDownLatch(1);
            loggedIn.set(false);
            final AtomicReference<UnionPair<FilesListInformation, Throwable>> result2 = new AtomicReference<>();
            provider().list(0, Options.FilterPolicy.Both, new LinkedHashMap<>(), 0, 5, p -> {
                result2.set(p);
                latch2.countDown();
            });
            Assertions.assertEquals(1, latch2.getCount());
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
            Assertions.assertNotNull(result.get());
            Assertions.assertTrue(result.get().isSuccess());
            Assertions.assertEquals(List.of(info), result.get().getT().informationList());
            latch2.await();
            Assertions.assertFalse(loggedIn.get());
            Assertions.assertNotNull(result2.get());
            Assertions.assertTrue(result2.get().isSuccess());
            Assertions.assertEquals(List.of(info), result2.get().getT().informationList());
        }
    }
}
