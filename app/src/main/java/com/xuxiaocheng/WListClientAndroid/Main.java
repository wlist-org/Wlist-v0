package com.xuxiaocheng.WListClientAndroid;

import androidx.annotation.NonNull;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

public final class Main {
    private Main() {
        super();
    }

    @NonNull public static final EventExecutorGroup ThreadPool =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutor"));
}
