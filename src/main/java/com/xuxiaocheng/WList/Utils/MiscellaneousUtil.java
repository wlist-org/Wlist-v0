package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MiscellaneousUtil {
    private MiscellaneousUtil() {
        super();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#exceptionally(Function)
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull Function<@NotNull Throwable, @Nullable T> exceptionHandler() {
        return (Function<Throwable, T>) MiscellaneousUtil.exceptionHandlerInstance;
    }
    private static final @NotNull Function<@NotNull Throwable, ? extends @Nullable Object> exceptionHandlerInstance = t -> {
        HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), t);
        return null;
    };

    /**
     * @see io.netty.util.concurrent.Future#addListener(GenericFutureListener)
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull FutureListener<T> exceptionListener() {
        return (FutureListener<T>) MiscellaneousUtil.exceptionListenerInstance;
    }
    private static final @NotNull FutureListener<?> exceptionListenerInstance = f -> {
        if (!f.isSuccess())
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
    };

    public static final @NotNull ConsumerE<@Nullable Throwable> exceptionCallback = e -> {
        if (e != null)
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
    };

    public static <K, V> @NotNull K randomKeyAndPut(final @NotNull Map<? super @NotNull K, V> map, final @NotNull Supplier<? extends @NotNull K> randomKey, final V value) {
        K k;
        while (true) {
            k = randomKey.get();
            final boolean[] flag = {false};
            final K finalK = k;
            map.computeIfAbsent(finalK, (i) -> {
                flag[0] = true;
                return value;
            });
            if (flag[0])
                break;
        }
        return k;
    }

    public static int calculatePartCount(final long total, final int limit) {
        //noinspection NumericCastThatLosesPrecision
        return (int) Math.ceil(((double) total) / limit);
    }
}
