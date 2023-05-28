package com.xuxiaocheng.WList.Utils;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

// TODO
public class ForkJoinEventExecutorGroup extends DefaultEventExecutorGroup {
    protected final @NotNull ForkJoinPool forkJoinPool;

    public ForkJoinEventExecutorGroup(final int nThreads) {
        super(nThreads);
        this.forkJoinPool = new ForkJoinPool();
    }

    public ForkJoinEventExecutorGroup(final int nThreads, final int forkThreads) {
        super(nThreads);
        this.forkJoinPool = new ForkJoinPool(forkThreads);
    }

    public ForkJoinEventExecutorGroup(final int nThreads, final int forkThreads, final ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
        this.forkJoinPool = new ForkJoinPool(forkThreads);
    }

    public ForkJoinEventExecutorGroup(final int nThreads, final int forkThreads, final ThreadFactory threadFactory, final int maxPendingTasks, final RejectedExecutionHandler rejectedHandler) {
        super(nThreads, threadFactory, maxPendingTasks, rejectedHandler);
        this.forkJoinPool = new ForkJoinPool(forkThreads);
    }

    @Override
    public Future<?> shutdownGracefully(final long quietPeriod, final long timeout, final TimeUnit unit) {
        final Future<?> future = super.shutdownGracefully(quietPeriod, timeout, unit);
        this.forkJoinPool.shutdown();
        return future;
    }

    public void executeJoin(final Runnable command) {
        this.forkJoinPool.execute(command);
    }

    public void executeJoin(final ForkJoinTask<?> command) {
        this.forkJoinPool.execute(command);
    }

    public ScheduledFuture<?> scheduleJoin(final Runnable command, final long delay, final TimeUnit unit) {
        return super.schedule(() -> this.forkJoinPool.execute(command), delay, unit);
    }
}
