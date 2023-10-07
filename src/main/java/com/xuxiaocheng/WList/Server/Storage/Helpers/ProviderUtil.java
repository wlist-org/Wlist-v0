package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public final class ProviderUtil {
    private ProviderUtil() {
        super();
    }

    public static final @NotNull Pattern PhoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern MailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    private static final int DefaultLimitPerRequestPage = 100;

    /**
     * A tool for list in page.
     * @param supplierInPage Get information list from by page count. Start from 0 and end when it returns null.
     * @param retry If supplier throws, the time of retry.
     */ // TODO: async.
    public static <I> @NotNull Iterator<@NotNull I> wrapSuppliersInPages(final @NotNull FunctionE<? super @NotNull Integer, ? extends @Nullable Collection<@NotNull I>> supplierInPage, final int retry) {
        final AtomicBoolean noNext = new AtomicBoolean(false);
        final BlockingQueue<I> filesQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean threadRunning = new AtomicBoolean(false);
        final AtomicInteger nextPage = new AtomicInteger(0);
        final Runnable supplier = () -> {
            if (threadRunning.compareAndSet(false, true))
                CompletableFuture.runAsync(() -> {
                    try {
                        while (filesQueue.size() < ProviderUtil.DefaultLimitPerRequestPage) {
                            if (noNext.get())
                                return;
                            Collection<I> page = null;
                            int times = 0;
                            Exception exception = null;
                            do {
                                try {
                                    page = supplierInPage.apply(nextPage.getAndIncrement());
                                    break;
                                } catch (final Exception e) {
                                    if (exception == null)
                                        exception = e;
                                    else
                                        exception.addSuppressed(e);
                                }
                                ++times;
                            } while (times < retry);
                            if (times >= retry) {
                                noNext.set(true);
                                throw new NoSuchElementException(exception);
                            }
                            if (exception != null)
                                HLog.getInstance("StorageLogger").log(HLogLevel.WARN, exception);
                            if (page == null) {
                                noNext.set(true);
                                return;
                            }
                            synchronized (threadRunning) {
                                filesQueue.addAll(page);
                                threadRunning.notifyAll();
                            }
                            if (filesQueue.size() >= ProviderUtil.DefaultLimitPerRequestPage)
                                break;
                        }
                    } finally {
                        synchronized (threadRunning) {
                            threadRunning.set(false);
                            threadRunning.notifyAll();
                        }
                    }
                }, WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
        };
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
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
            public @NotNull I next() {
                final I item = filesQueue.poll();
                if (item != null) {
                    if (filesQueue.size() < ProviderUtil.DefaultLimitPerRequestPage >> 1)
                        supplier.run();
                    return item;
                }
                if (!this.hasNext())
                    throw new NoSuchElementException("No more information in this page.");
                return this.next();
            }
        };
    }

    @Deprecated // TODO
    public static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> wrapSuppliersInPages(final int pageCount, final @NotNull FunctionE<? super @NotNull Integer, @NotNull Collection<@NotNull I>> supplierInPage, final int retry) {
        throw new UnsupportedOperationException();
    }
}
