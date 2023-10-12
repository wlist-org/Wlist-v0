package com.xuxiaocheng.WListClientAndroid.Utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class EnhancedRecyclerViewAdapter<T, VH extends EnhancedRecyclerViewAdapter.WrappedViewHolder<?>> extends RecyclerView.Adapter<EnhancedRecyclerViewAdapter.WrappedViewHolder<?>> {
    protected final @NotNull List<View> headers = new ArrayList<>();
    protected final @NotNull List<View> tailors = new ArrayList<>();
    protected final @NotNull List<T> data = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static @NotNull <V extends View> V buildView(final @NotNull LayoutInflater inflater, @LayoutRes final int cell, final @NotNull RecyclerView parent) {
        return (V) inflater.inflate(cell, parent, false);
    }

    public abstract static class WrappedViewHolder<V extends View> extends RecyclerView.ViewHolder {
        protected WrappedViewHolder(final @NotNull V itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(final int position) {
        if (position < this.headers.size())
            return position + 1; // TYPE_HEADER > 0
        if (position > this.headers.size() + this.data.size() - 1)
            return -position + this.headers.size() + this.data.size() - 1; // TYPE_TAILOR < 0
        return 0;
    }

    protected static class HeaderAndTailorViewHolder<V extends View> extends WrappedViewHolder<V> {
        protected HeaderAndTailorViewHolder(final @NotNull V itemView) {
            super(itemView);
        }
    }

    @Override
    public @NotNull WrappedViewHolder<?> onCreateViewHolder(final @NotNull ViewGroup parent, final int viewType) {
        if (viewType > 0) return new HeaderAndTailorViewHolder<>(this.headers.get(viewType - 1));
        if (viewType < 0) return new HeaderAndTailorViewHolder<>(this.tailors.get(-viewType - 1));
        return this.createViewHolder(parent);
    }

    protected abstract @NotNull VH createViewHolder(final @NotNull ViewGroup parent);

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(final @NotNull WrappedViewHolder<?> holder, final int position) {
        if (this.getItemViewType(position) == 0)
            this.bindViewHolder((VH) holder, this.data.get(position - this.headers.size()));
    }

    protected abstract void bindViewHolder(final @NotNull VH holder, final @NotNull T information);

    @Override
    public int getItemCount() {
        return this.headers.size() + this.data.size() + this.tailors.size();
    }

    @UiThread
    public void addData(final @NotNull T data) {
        this.data.add(data);
        this.notifyItemInserted(this.headers.size() + this.data.size() - 1);
    }

    @UiThread
    public void addData(final int index, final @NotNull T data) {
        this.data.add(index, data);
        this.notifyItemInserted(this.headers.size() + index);
    }

    @UiThread
    public void addDataRange(final @NotNull Collection<? extends T> data) {
        this.data.addAll(data);
        this.notifyItemRangeInserted(this.headers.size() + this.data.size() - data.size(), data.size());
    }

    @UiThread
    public void addDataRange(final int index, final @NotNull Collection<? extends T> data) {
        this.data.addAll(index, data);
        this.notifyItemRangeInserted(this.headers.size() + index, data.size());
    }

    @UiThread
    public void removeData(final int index) {
        this.data.remove(index);
        this.notifyItemRemoved(this.headers.size() + this.data.size() - 1);
    }

    @UiThread
    public void removeDataRange(final int index, final int count) {
        for (int i = 0; i < count; ++i)
            this.data.remove(index);
        this.notifyItemRangeRemoved(this.headers.size() + index, count);
    }

    @UiThread
    public void changeData(final int index, final @NotNull T data) {
        this.data.remove(index);
        this.data.add(index, data);
        this.notifyItemChanged(this.headers.size() + index);
    }

    public @NotNull @UnmodifiableView List<T> getData() {
        return Collections.unmodifiableList(this.data);
    }

    public @NotNull T getData(final int index) {
        return this.data.get(index);
    }

    public int dataSize() {
        return this.data.size();
    }

    @UiThread
    public void addHeader(final @NotNull View header) {
        this.headers.add(header);
        super.notifyItemInserted(this.headers.size() - 1);
    }

    @UiThread
    public void setHeader(final int index, final @NotNull View header) {
        this.headers.set(index, header);
        super.notifyItemChanged(index);
    }

    @UiThread
    public void removeHeader(final int index) {
        this.headers.remove(index);
        super.notifyItemRemoved(index);
    }

    public @NotNull @UnmodifiableView List<@NotNull View> getHeaders() {
        return Collections.unmodifiableList(this.headers);
    }

    public @NotNull View getHeader(final int index) {
        return this.headers.get(index);
    }

    public int headersSize() {
        return this.headers.size();
    }

    @UiThread
    public void addTailor(final @NotNull View tailor) {
        this.tailors.add(tailor);
        super.notifyItemInserted(this.headers.size() + this.data.size() + this.tailors.size() - 1);
    }

    @UiThread
    public void setTailor(final int index, final @NotNull View tailor) {
        this.tailors.set(index, tailor);
        super.notifyItemChanged(this.headers.size() + this.data.size() + index);
    }

    @UiThread
    public void removeTailor(final int index) {
        this.tailors.remove(index);
        super.notifyItemRemoved(this.headers.size() + this.data.size() + index);
    }

    public @NotNull @UnmodifiableView List<View> getTailors() {
        return Collections.unmodifiableList(this.tailors);
    }

    public @NotNull View getTailor(final int index) {
        return this.tailors.get(index);
    }

    public int tailorsSize() {
        return this.tailors.size();
    }

    @Override
    public void onAttachedToRecyclerView(final @NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager gridLayoutManager)
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(final int position) {
                    return EnhancedRecyclerViewAdapter.this.getItemViewType(position) == 0 ? 1 : gridLayoutManager.getSpanCount();
                }
            });
    }

    @Override
    public void onViewAttachedToWindow(final @NotNull WrappedViewHolder<?> holder) {
        super.onViewAttachedToWindow(holder);
        final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params instanceof StaggeredGridLayoutManager.LayoutParams layoutParams)
            layoutParams.setFullSpan(this.getItemViewType(holder.getLayoutPosition()) != 0);
    }

    @Override
    public @NotNull String toString() {
        return "EnhancedRecyclerViewAdapter{" +
                "headers=" + this.headers +
                ", tailors=" + this.tailors +
                ", data=" + this.data +
                ", super=" + super.toString() +
                '}';
    }
}
