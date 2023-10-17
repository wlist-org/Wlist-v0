package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.BiConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Operations.Helpers.IdsHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ProviderUtil {
    private ProviderUtil() {
        super();
    }

    public static final @NotNull Pattern PhoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern MailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    private static final int DefaultLimitPerRequestPage = 100;

    private static final int highWaterMark = ProviderUtil.DefaultLimitPerRequestPage; // run until filesQueue.size() >= highWaterMark
    private static final int lowWaterMark = ProviderUtil.DefaultLimitPerRequestPage >> 1; // run if filesQueue.size() < lowWaterMark
    public static final @NotNull UnionPair<Boolean, Throwable> WrapNotAvailable = UnionPair.ok(Boolean.FALSE);
    public static final @NotNull UnionPair<Boolean, Throwable> WrapAvailable = UnionPair.ok(Boolean.TRUE);
    public static final @NotNull UnionPair<Pair.ImmutablePair<Collection<FileInformation>, Boolean>, Throwable> WrapNoMore = UnionPair.ok(Pair.ImmutablePair.makeImmutablePair(Set.of(), Boolean.TRUE));
    /**
     * A tool for listing in page with unknown page count.
     * Each function will be call sequentially. {@code available(); supplierInPage(0); supplierInPage(1); ...}
     * @param supplierInPage get information list from page count. Start from 0 and end when it returns true. (list, noMore)
     */
    public static void wrapSuppliersInPages(final @NotNull ConsumerE<? super @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>>> available, final @NotNull Executor executor, final @NotNull BiConsumerE<? super @NotNull Integer, ? super @NotNull Consumer<? super @NotNull UnionPair<Pair.@NotNull ImmutablePair<@NotNull @Unmodifiable Collection<@NotNull FileInformation>, @NotNull Boolean>, Throwable>>> supplierInPage, final @NotNull Consumer<? super UnionPair<Optional<Iterator<FileInformation>>, Throwable>> consumer) throws Exception {
        available.accept((Consumer<? super UnionPair<Boolean, Throwable>>) a -> {
            if (a.isFailure()) {
                consumer.accept(UnionPair.fail(a.getE()));
                return;
            }
            if (!a.getT().booleanValue()) {
                consumer.accept(AbstractIdBaseProvider.ListNotExisted);
                return;
            }
            final AtomicBoolean noNext = new AtomicBoolean(false);
            final BlockingQueue<FileInformation> filesQueue = new LinkedBlockingQueue<>();
            final AtomicBoolean threadRunning = new AtomicBoolean(false);
            final AtomicInteger nextPage = new AtomicInteger(0);
            final AtomicReference<Throwable> throwable = new AtomicReference<>();
            final Runnable supplier = () -> {
                if (threadRunning.compareAndSet(false, true))
                    CompletableFuture.runAsync(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                supplierInPage.accept(nextPage.getAndIncrement(), (Consumer<? super UnionPair<Pair.ImmutablePair<Collection<FileInformation>, Boolean>, Throwable>>) p -> {
                                    boolean flag = true;
                                    try {
                                        if (p.isFailure())
                                            throw p.getE();
                                        synchronized (threadRunning) {
                                            filesQueue.addAll(p.getT().getFirst());
                                            threadRunning.notifyAll();
                                        }
                                        if (p.getT().getSecond().booleanValue()) {
                                            noNext.set(true);
                                            return;
                                        }
                                        if (filesQueue.size() < ProviderUtil.highWaterMark) {
                                            this.run();
                                            flag = false;
                                        }
                                    } catch (final Throwable exception) {
                                        noNext.set(true);
                                        if (!throwable.compareAndSet(null, exception))
                                            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                                    } finally {
                                        if (flag) {
                                            synchronized (threadRunning) {
                                                threadRunning.set(false);
                                                threadRunning.notifyAll();
                                            }
                                        }
                                    }
                                });
                            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                noNext.set(true);
                                if (!throwable.compareAndSet(null, exception))
                                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                                synchronized (threadRunning) {
                                    threadRunning.set(false);
                                    threadRunning.notifyAll();
                                }
                            }
                        }
                    }, executor).exceptionally(MiscellaneousUtil.exceptionHandler());
            };
            consumer.accept(UnionPair.ok(Optional.of(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    if (throwable.get() != null)
                        throw new NoSuchElementException(throwable.get());
                    if (filesQueue.peek() != null)
                        return true;
                    if (noNext.get())
                        return false;
                    supplier.run();
                    synchronized (threadRunning) {
                        while (threadRunning.get() && filesQueue.peek() == null)
                            try {
                                threadRunning.wait();
                            } catch (final InterruptedException ignore) {
                                return false;
                            }
                    }
                    return this.hasNext();
                }

                @Override
                public @NotNull FileInformation next() {
                    if (throwable.get() != null)
                        throw new NoSuchElementException(throwable.get());
                    final FileInformation item = filesQueue.poll();
                    if (item != null) {
                        if (filesQueue.size() < ProviderUtil.lowWaterMark)
                            supplier.run();
                        return item;
                    }
                    if (!this.hasNext())
                        throw new NoSuchElementException("No more information.");
                    return this.next();
                }
            })));
        });
    }

    @Contract(pure = true)
    private static int getHighWaterMark(@SuppressWarnings("unused") final int concurrence) { // run until filesQueue.size() >= highWaterMark
        return ProviderUtil.DefaultLimitPerRequestPage;
    }
    @Contract(pure = true)
    private static int getLowWaterMark(final int concurrence) { // run if filesQueue.size() < lowWaterMark
        return ProviderUtil.DefaultLimitPerRequestPage >> Math.min(concurrence, 2);
    }
    /**
     * A tool for listing in page with known page count.
     * @param concurrence max parallel request count.
     * @param supplierInPage get information list from page count. Range: 0 ~ {@code pageCount} - 1
     */
    public static void wrapSuppliersInPages(final int pageCount, final int concurrence, final @NotNull Executor executor, final @NotNull BiConsumerE<? super @NotNull Integer, ? super @NotNull Consumer<? super @NotNull UnionPair<@Unmodifiable Collection<@NotNull FileInformation>, Throwable>>> supplierInPage, final @NotNull Consumer<? super UnionPair<Optional<Iterator<FileInformation>>, Throwable>> consumer) {
        assert concurrence > 0;
        final int highWaterMark = ProviderUtil.getHighWaterMark(concurrence);
        final int lowWaterMark = ProviderUtil.getLowWaterMark(concurrence);
        final BlockingQueue<FileInformation> filesQueue = new LinkedBlockingQueue<>();
        final AtomicInteger threadRunning = new AtomicInteger(0);
        final AtomicInteger nextPage = new AtomicInteger(0);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        final Runnable supplier = () -> {
            while (threadRunning.getAndIncrement() < concurrence)
                CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final int page = nextPage.getAndIncrement();
                            if (page >= pageCount) {
                                nextPage.getAndDecrement();
                                synchronized (threadRunning) {
                                    threadRunning.getAndDecrement();
                                    threadRunning.notifyAll();
                                }
                                return;
                            }
                            supplierInPage.accept(page, (Consumer<? super UnionPair<Collection<FileInformation>, Throwable>>) p -> {
                                boolean flag = true;
                                try {
                                    if (p.isFailure())
                                        throw p.getE();
                                    synchronized (threadRunning) {
                                        filesQueue.addAll(p.getT());
                                        threadRunning.notifyAll();
                                    }
                                    if (filesQueue.size() < highWaterMark) {
                                        this.run();
                                        flag = false;
                                    }
                                } catch (final Throwable exception) {
                                    nextPage.set(pageCount);
                                    if (!throwable.compareAndSet(null, exception))
                                        HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                                } finally {
                                    if (flag) {
                                        synchronized (threadRunning) {
                                            threadRunning.getAndDecrement();
                                            threadRunning.notifyAll();
                                        }
                                    }
                                }
                            });
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            nextPage.set(pageCount);
                            if (!throwable.compareAndSet(null, exception))
                                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                            synchronized (threadRunning) {
                                threadRunning.getAndDecrement();
                                threadRunning.notifyAll();
                            }
                        }
                    }
                }, executor).exceptionally(MiscellaneousUtil.exceptionHandler());
            threadRunning.getAndDecrement();
        };
        consumer.accept(UnionPair.ok(Optional.of(new Iterator<>() {
            @Override
            public boolean hasNext() {
                if (throwable.get() != null)
                    throw new NoSuchElementException(throwable.get());
                if (filesQueue.peek() != null)
                    return true;
                if (nextPage.get() >= pageCount) {
                    if (threadRunning.get() <= 0)
                        return false;
                } else
                    supplier.run();
                synchronized (threadRunning) {
                    while (threadRunning.get() > 0 && filesQueue.peek() == null)
                        try {
                            threadRunning.wait();
                        } catch (final InterruptedException ignore) {
                            return false;
                        }
                }
                return this.hasNext();
            }

            @Override
            public @NotNull FileInformation next() {
                if (throwable.get() != null)
                    throw new NoSuchElementException(throwable.get());
                final FileInformation item = filesQueue.poll();
                if (item != null) {
                    if (filesQueue.size() < lowWaterMark)
                        supplier.run();
                    return item;
                }
                if (!this.hasNext())
                    throw new NoSuchElementException("No more information.");
                return this.next();
            }
        })));
    }

    private static final @NotNull Map<@NotNull FileLocation, @NotNull HttpUrl> downloadUrlCache = new ConcurrentHashMap<>();
    public static void setDownloadUrlCache(final @NotNull FileLocation location, final @NotNull HttpUrl url, final @Nullable ZonedDateTime expire) {
        ProviderUtil.downloadUrlCache.put(location, url);
        if (expire != null)
            IdsHelper.CleanerExecutors.schedule(() -> ProviderUtil.downloadUrlCache.remove(location, url),
                    Duration.between(MiscellaneousUtil.now(), expire).toSeconds(), TimeUnit.SECONDS).addListener(IdsHelper.noCancellationExceptionListener());
    }
    public static void removeDownloadUrlCache(final @NotNull FileLocation location) {
        ProviderUtil.downloadUrlCache.remove(location);
    }
    public static @Nullable HttpUrl getDownloadUrlCache(final @NotNull FileLocation location) {
        return ProviderUtil.downloadUrlCache.get(location);
    }

    // Example: "1.txt" ==> ".txt"
    public static @Nullable String getFileSuffix(final @NotNull String name) {
        final int index = name.lastIndexOf('.');
        if (index < 0)
            return null;
        return name.substring(index);
    }

    // Example: "1.txt" ==> "1"
    public static @NotNull String discardFileSuffix(final @NotNull String name) {
        final int index = name.lastIndexOf('.');
        if (index < 0)
            return name;
        return name.substring(0, index);
    }
}
