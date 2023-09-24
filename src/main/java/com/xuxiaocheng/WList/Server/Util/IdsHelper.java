package com.xuxiaocheng.WList.Server.Util;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;

public final class IdsHelper {
    private IdsHelper() {
        super();
    }

    public static final @NotNull EventExecutorGroup CleanerExecutors = new DefaultEventExecutorGroup(2, new DefaultThreadFactory("CleanerExecutors"));

    public static @NotNull String randomTimerId() {
        return Long.toString(System.currentTimeMillis(), 36) + HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.AnyWords);
    }
}
