package com.xuxiaocheng.WListClientAndroid.Utils;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WARNING:
 * 1. When calling method 'notifyItem*', the position should add {@link #headersSize()}.
 * 2. Method {@link #addHeader(View)} and {@link #addTailor(View)} only accept 'this.getLayoutInflater().inflate(R.layout.header_or_tailor, parent, false)'.
 */
public class RecyclerViewHeadersAndTailorsAdapterWrapper<VH extends RecyclerViewHeadersAndTailorsAdapterWrapper.WrappedViewHolder<?>> extends RecyclerView.Adapter<RecyclerViewHeadersAndTailorsAdapterWrapper.WrappedViewHolder<?>> {
    @NonNull protected final RecyclerView.Adapter<VH> adapter;
    @NonNull protected final List<View> headers = new ArrayList<>();
    @NonNull protected final List<View> tailors = new ArrayList<>();

    public static class WrappedViewHolder<V extends View> extends RecyclerView.ViewHolder {
        protected WrappedViewHolder(@NonNull final V itemView) {
            super(itemView);
        }
    }

    public RecyclerViewHeadersAndTailorsAdapterWrapper(@NonNull final RecyclerView.Adapter<VH> adapter) {
        super();
        this.adapter = adapter;
    }

    @Override
    public int getItemViewType(final int position) {
        if (position < this.headers.size())
            return position + 1; // TYPE_HEADER > 0
        if (position > this.headers.size() + this.adapter.getItemCount() - 1)
            return -position + this.headers.size() + this.adapter.getItemCount() - 1; // TYPE_TAILOR < 0
        return 0;
    }

    @Override
    @NonNull public WrappedViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (viewType > 0) return new WrappedViewHolder<>(this.headers.get(viewType - 1));
        if (viewType < 0) return new WrappedViewHolder<>(this.tailors.get(-viewType - 1));
        return this.adapter.onCreateViewHolder(parent, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull final WrappedViewHolder<?> holder, final int position) {
        if (this.getItemViewType(position) == 0)
            this.adapter.onBindViewHolder((VH) holder, position - this.headers.size());
    }

    @Override
    public int getItemCount() {
        return this.headers.size() + this.adapter.getItemCount() + this.tailors.size();
    }

    @MainThread
    public void addHeader(@NonNull final View header) {
        this.headers.add(header);
        super.notifyItemInserted(this.headers.size() - 1);
    }

    @MainThread
    public void removeHeader(final int index) {
        this.headers.remove(index);
        super.notifyItemRemoved(index);
    }

    @NonNull public @UnmodifiableView List<View> getHeaders() {
        return Collections.unmodifiableList(this.headers);
    }

    public int headersSize() {
        return this.headers.size();
    }

    @MainThread
    public void addTailor(@NonNull final View tailor) {
        this.tailors.add(tailor);
        super.notifyItemInserted(this.headers.size() + this.adapter.getItemCount() + this.tailors.size() - 1);
    }

    @MainThread
    public void removeTailor(final int index) {
        this.tailors.remove(index);
        super.notifyItemRemoved(this.headers.size() + this.adapter.getItemCount() + index);
    }

    @NonNull public @UnmodifiableView List<View> getTailors() {
        return Collections.unmodifiableList(this.tailors);
    }

    public int tailorsSize() {
        return this.tailors.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager gridLayoutManager)
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(final int position) {
                    return RecyclerViewHeadersAndTailorsAdapterWrapper.this.getItemViewType(position) == 0 ? 1 : gridLayoutManager.getSpanCount();
                }
            });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull final WrappedViewHolder<?> holder) {
        super.onViewAttachedToWindow(holder);
        final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params instanceof StaggeredGridLayoutManager.LayoutParams layoutParams)
            layoutParams.setFullSpan(this.getItemViewType(holder.getLayoutPosition()) != 0);
    }

    @Override
    @NonNull public String toString() {
        return "RecyclerViewHeadersAndTailorsAdapterWrapper{" +
                "adapter=" + this.adapter +
                ", headers=" + this.headers +
                ", tailors=" + this.tailors +
                ", super=" + super.toString() +
                '}';
    }
}
