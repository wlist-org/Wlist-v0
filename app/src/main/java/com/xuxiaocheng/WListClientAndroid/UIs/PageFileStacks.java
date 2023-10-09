package com.xuxiaocheng.WListClientAndroid.UIs;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

class PageFileStacks {
    protected final @NotNull Deque<CachedStackRecord> cachedStacks = new ArrayDeque<>();
    protected final @NotNull Deque<NonCachedStackRecord> nonCachedStacks = new ArrayDeque<>();

    public final void push(final @NotNull CharSequence name, final @NotNull AtomicLong counter, final @NotNull FileLocation location,
                           final @NotNull EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter,
                           final @NotNull RecyclerView.OnScrollListener listener) {
        this.cachedStacks.addFirst(new CachedStackRecord(name, counter, adapter, listener, location));
        if (this.cachedStacks.size() > 5) {
            final CachedStackRecord cached = this.cachedStacks.removeLast();
            final RecyclerView.LayoutManager manager = cached.adapter.getRecyclerView().getLayoutManager();
            assert manager instanceof LinearLayoutManager;
            final int position = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
            final NonCachedStackRecord record = new NonCachedStackRecord(cached.name, cached.counter, cached.location, cached.adapter.getData(position), position);
            this.nonCachedStacks.addFirst(record);
        }
    }

    public final @Nullable UnionPair<CachedStackRecord, NonCachedStackRecord> pop() {
        final CachedStackRecord cached = this.cachedStacks.pollFirst();
        if (cached != null)
            return UnionPair.ok(cached);
        final NonCachedStackRecord nonCached = this.nonCachedStacks.pollFirst();
        if (nonCached != null)
            return UnionPair.fail(nonCached);
        return null;
    }

    public boolean isEmpty() {
        return this.cachedStacks.isEmpty() && this.nonCachedStacks.isEmpty();
    }

    protected static class CachedStackRecord {
        protected final @NotNull CharSequence name;
        protected final @NotNull AtomicLong counter;
        protected final @NotNull EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter;
        protected final @NotNull RecyclerView.OnScrollListener listener;

        protected final @NotNull FileLocation location;

        protected CachedStackRecord(final @NotNull CharSequence name, final @NotNull AtomicLong counter,
                                    final @NotNull EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter,
                                    final @NotNull RecyclerView.OnScrollListener listener,
                                    final @NotNull FileLocation location) {
            super();
            this.name = name;
            this.counter = counter;
            this.adapter = adapter;
            this.listener = listener;
            this.location = location;
        }

        @Override
        public @NotNull String toString() {
            return "CachedStackRecord{" +
                    "name=" + this.name +
                    ", counter=" + this.counter +
                    ", adapter=" + this.adapter +
                    ", listener=" + this.listener +
                    ", location=" + this.location +
                    '}';
        }
    }

    protected static class NonCachedStackRecord {
        protected final @NotNull CharSequence name;
        protected final @NotNull AtomicLong counter;
        protected final @NotNull FileLocation location;
        protected final @NotNull VisibleFileInformation pointer;
        protected final long index;

        protected NonCachedStackRecord(final @NotNull CharSequence name, final @NotNull AtomicLong counter, final @NotNull FileLocation location,
                                       final @NotNull VisibleFileInformation pointer, final long index) {
            super();
            this.name = name;
            this.counter = counter;
            this.location = location;
            this.pointer = pointer;
            this.index = index;
        }

        @Override
        public @NotNull String toString() {
            return "NonCachedStackRecord{" +
                    "name=" + this.name +
                    ", counter=" + this.counter +
                    ", location=" + this.location +
                    ", pointer=" + this.pointer +
                    ", index=" + this.index +
                    '}';
        }
    }

    @Override
    public @NotNull String toString() {
        return "PageFileStacks{" +
                "cachedStacks=" + this.cachedStacks +
                ", nonCachedStacks=" + this.nonCachedStacks +
                '}';
    }
}
