package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayQueueInByteBufOutByteBuf implements Closeable {
    protected final @NotNull CompositeByteBuf buffers = ByteBufAllocator.DEFAULT.compositeBuffer(Integer.MAX_VALUE);
    protected final @NotNull AtomicInteger inCounter = new AtomicInteger(0);
    protected final @NotNull AtomicInteger outCounter = new AtomicInteger(0);
    protected final @NotNull ConcurrentNavigableMap<@NotNull Integer, @NotNull ByteBuf> bufferTree = new ConcurrentSkipListMap<>();
    protected final @NotNull List<@NotNull ByteBuf> clearListener = new LinkedList<>();
    protected final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
    protected final @NotNull AtomicInteger putLock = new AtomicInteger(0);
    protected final @NotNull AtomicInteger getLock = new AtomicInteger(0);

    @Override
    public void close() throws IOException {
        synchronized (this.closed) {
            if (this.closed.get())
                return;
            this.closed.set(true);
        }
        try {
            // Ensure all put and get method is exited.
            synchronized (this.putLock) {
                while (this.putLock.get() > 0) {
                    this.putLock.wait();
                }
            }
            synchronized (this.getLock) {
                while (this.getLock.get() > 0) {
                    this.getLock.wait();
                }
            }
        } catch (final InterruptedException exception) {
            throw new IOException(exception);
        } finally {
            this.buffers.release();
            for (final Iterator<Map.Entry<Integer, ByteBuf>> iterator = this.bufferTree.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<Integer, ByteBuf> entry = iterator.next();
                final ByteBuf v = entry.getValue();
                v.release();
                iterator.remove();
            }
            this.clearListener.clear();
        }
    }

    public boolean put(final @NotNull ByteBuf buf, final int chunk) {
        synchronized (this.closed) {
            if (this.closed.get())
                return false;
        }
        this.putLock.getAndIncrement();
        try {
            if (chunk < this.inCounter.get())
                return false;
            if (this.bufferTree.putIfAbsent(chunk, buf) != null)
                return false;
            final boolean[] flag = {false};
            do {
                flag[0] = false;
                final int index = this.inCounter.get();
                this.bufferTree.computeIfPresent(index, (k, o) -> {
                    if (!this.inCounter.compareAndSet(index, index + 1))
                        return null;
                    synchronized (this.buffers) {
                        this.buffers.addComponent(true, o);
                        this.buffers.notifyAll();
                    }
                    flag[0] = true;
                    return null;
                });
            } while (flag[0]);
            return true;
        } finally {
            synchronized (this.putLock) {
                this.putLock.getAndDecrement();
                this.putLock.notify();
            }
        }
    }

    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> get(final int size) throws InterruptedException {
        synchronized (this.closed) {
            if (this.closed.get())
                return null;
        }
        this.getLock.getAndIncrement();
        try {
            final ByteBuf res;
            final int count;
            synchronized (this.buffers) {
                while (this.buffers.readableBytes() < size)
                    this.buffers.wait();
                res = this.buffers.readRetainedSlice(size);
                count = this.outCounter.getAndIncrement();
            }
            synchronized (this.clearListener) {
                this.clearListener.add(res);
            }
            return Pair.ImmutablePair.makeImmutablePair(count, res);
        } finally {
            synchronized (this.getLock) {
                this.getLock.getAndDecrement();
                this.getLock.notify();
            }
        }
    }

    public void discardSomeReadBytes() {
        synchronized (this.buffers) {
            if (this.buffers.numComponents() <= 0)
                return;
        }
        int deletable = 0;
        synchronized (this.clearListener) {
            for (final Iterator<ByteBuf> iterator = this.clearListener.iterator(); iterator.hasNext(); ) {
                final ByteBuf buf = iterator.next();
                if (buf.refCnt() > 1)
                    break;
                deletable += buf.capacity();
                iterator.remove();
            }
        }
        int index = 0;
        int deleted = 0;
        synchronized (this.buffers) {
            while (index < this.buffers.numComponents()) {
                final int cap = this.buffers.internalComponent(index).readableBytes();
                deletable -= cap;
                if (deletable >= 0) {
                    ++index;
                    deleted += cap;
                } else
                    break;
            }
            this.buffers.readerIndex(this.buffers.readerIndex() - deleted);
            this.buffers.writerIndex(this.buffers.writerIndex() - deleted);
            this.buffers.removeComponents(0, index);
        }
    }

    @Override
    public @NotNull String toString() {
        return "DelayQueueInByteBufOutByteBuf{" +
                "buffers=" + this.buffers +
                ", inCounter=" + this.inCounter +
                ", outCounter=" + this.outCounter +
                ", bufferTree=" + this.bufferTree +
                ", clearListener=" + this.clearListener +
                ", closed=" + this.closed +
                ", putLock=" + this.putLock +
                ", getLock=" + this.getLock +
                '}';
    }
}
