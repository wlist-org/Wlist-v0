package com.xuxiaocheng.WListClientAndroid;

import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;

public final class Main {
    private Main() {
        super();
    }

    @NonNull public static final EventExecutorGroup ThreadPool =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutor"));

    @NonNull public static final FutureListener<? super Object> ThrowableListener = f -> {
        if (f.cause() != null)
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
    };
}
