package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ProviderUtil {
    private ProviderUtil() {
        super();
    }

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    public static final @NotNull String InvalidPhoneNumber = "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801";
    public static final @NotNull String InvalidMailAddress = "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u53f7";

    public static final int DefaultLimitPerRequestPage = 100;
    public static final Options.@NotNull OrderPolicy DefaultOrderPolicy = null;//Options.OrderPolicy.FileName;
    public static final Options.@NotNull OrderDirection DefaultOrderDirection = Options.OrderDirection.ASCEND;

    // TODO: HtmlUnit
    private static final @NotNull Iterable<@NotNull Pattern> HtmlCommentsTags = List.of(Pattern.compile("<!--.*?-->"));
    public static @NotNull String removeHtmlComments(final @Nullable String html) {
        if (html == null)
            return "";
        String res = html;
        for (final Pattern pattern: ProviderUtil.HtmlCommentsTags) {
            res = pattern.matcher(res).replaceAll("");
        }
        return res;
    }

    private static final @NotNull String scriptStartTag = "<script type=\"text/javascript\">";
    private static final @NotNull String scriptEndTag = "</script>";
    public static @NotNull List<@NotNull String> findScripts(final @NotNull String html) {
        final List<String> scripts = new ArrayList<>();
        int index = 0;
        while (true) {
            index = html.indexOf(ProviderUtil.scriptStartTag, index);
            if (index == -1) break;
            final int endIndex = html.indexOf(ProviderUtil.scriptEndTag, index);
            if (endIndex == -1) break;
            scripts.add(html.substring(index + ProviderUtil.scriptStartTag.length(), endIndex));
            index = endIndex;
        }
        return scripts;
    }

    private static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> getIteratorWrappedSuppliers(final @NotNull Runnable supplier, final @NotNull BlockingQueue<? extends @NotNull I> filesQueue, final @NotNull AtomicBoolean noNext, final @NotNull AtomicBoolean threadRunning, final @NotNull Consumer<? super @Nullable Exception> callback) {
        return Pair.ImmutablePair.makeImmutablePair(new Iterator<>() {
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
                    throw new NoSuchElementException();
                return this.next();
            }
        }, () -> {
            if (noNext.compareAndSet(false, true))
                callback.accept(new CancellationException());
        });
    }

    // TODO
    public static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> wrapSuppliersInPages(final @NotNull FunctionE<? super @NotNull Integer, ? extends @Nullable Collection<@NotNull I>> supplierInPage, final @NotNull Consumer<? super @Nullable Exception> callback) {
        final AtomicBoolean noNext = new AtomicBoolean(false);
        final BlockingQueue<I> filesQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean threadRunning = new AtomicBoolean(false);
        final AtomicInteger nextPage = new AtomicInteger(0);
        return ProviderUtil.getIteratorWrappedSuppliers(() -> {
            if (threadRunning.compareAndSet(false, true))
                CompletableFuture.runAsync(() -> {
                    try {
                        while (filesQueue.size() < ProviderUtil.DefaultLimitPerRequestPage) {
                            if (noNext.get())
                                return;
                            final Collection<I> page;
                            try {
                                page = supplierInPage.apply(nextPage.getAndIncrement());
                            } catch (final Exception exception) {
                                if (noNext.compareAndSet(false, true))
                                    callback.accept(exception);
                                throw new NoSuchElementException(exception);
                            }
                            if (page == null) {
                                if (noNext.compareAndSet(false, true))
                                    callback.accept(null);
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
        }, filesQueue, noNext, threadRunning, callback);
    }

    @Deprecated // TODO
    public static <I> Pair.@NotNull ImmutablePair<@NotNull Iterator<@NotNull I>, @NotNull Runnable> wrapSuppliersInPages(final int pageCount, final @NotNull FunctionE<? super @NotNull Integer, @NotNull Collection<@NotNull I>> supplierInPage, final @NotNull Consumer<? super @Nullable Exception> callback) {
        throw new UnsupportedOperationException();
//        final AtomicBoolean noNext = new AtomicBoolean(false);
//        final BlockingQueue<I> filesQueue = new LinkedBlockingQueue<>();
//        final AtomicBoolean threadRunning = new AtomicBoolean(false);
//        final AtomicInteger nextPage = new AtomicInteger(0);
//        final Supplier<@Nullable Collection<I>> wrappedSupplier = () -> {
//            final Collection<I> page;
//            try {
//                page = supplierInPage.apply(nextPage.getAndIncrement());
//            } catch (final Exception exception) {
//                if (noNext.compareAndSet(false, true))
//                    callback.accept(exception);
//                throw new NoSuchElementException(exception);
//            }
//            return page;
//        };
//        return ProviderUtil.getIteratorWrappedSuppliers(() -> {
//            if (threadRunning.compareAndSet(false, true))
//                CompletableFuture.runAsync(() -> {
//                    try {
//                        // Multi-call wapperedSupplier.
//                    } finally {
//                        threadRunning.set(false);
//                    }
//                }, WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
//        }, filesQueue, noNext, threadRunning, callback);
    }
}
