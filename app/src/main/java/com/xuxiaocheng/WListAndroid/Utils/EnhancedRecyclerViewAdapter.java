package com.xuxiaocheng.WListAndroid.Utils;

import android.content.Context;
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

public abstract class EnhancedRecyclerViewAdapter<T, VH extends EnhancedRecyclerViewAdapter.WrappedViewHolder<?, T>> extends RecyclerView.Adapter<EnhancedRecyclerViewAdapter.WrappedViewHolder<?, T>> {
    protected final @NotNull List<View> headers = new ArrayList<>();
    protected final @NotNull List<View> tailors = new ArrayList<>();
    protected final @NotNull List<T> data = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static @NotNull <V extends View> V buildView(final @NotNull LayoutInflater inflater, @LayoutRes final int cell, final @NotNull RecyclerView parent) {
        return (V) inflater.inflate(cell, parent, false);
    }

    public abstract static class WrappedViewHolder<V extends View, T> extends RecyclerView.ViewHolder {
        protected WrappedViewHolder(final @NotNull V itemView) {
            super(itemView);
        }
        public abstract void onBind(final @NotNull T data);
    }

    @Override
    public int getItemViewType(final int position) {
        if (position < this.headers.size() || this.headers.size() + this.data.size() - 1 < position)
            return -1;
        return position - this.headers.size();
    }

    protected static class HeaderAndTailorViewHolder<T> extends WrappedViewHolder<View, T> {
        protected HeaderAndTailorViewHolder(final @NotNull Context context) {
            super(new WrappedView(context, true));
        }
        @Override
        public void onBind(@NotNull final T data) {
        }
    }

    @Override
    public @NotNull WrappedViewHolder<?, T> onCreateViewHolder(final @NotNull ViewGroup parent, final int viewType) {
        if (viewType < 0) return new HeaderAndTailorViewHolder<>(parent.getContext());
        return this.createDataViewHolder(parent, viewType);
    }

    protected abstract @NotNull VH createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition);

    @Override
    public void onBindViewHolder(final @NotNull WrappedViewHolder<?, T> holder, final int position) {
        final int viewType = this.getItemViewType(position);
        if (viewType < 0)
            ((WrappedView) holder.itemView).setView(position < this.headers.size() ? this.headers.get(position)
                    : this.tailors.get(position - this.headers.size() - this.data.size()));
        else
            holder.onBind(this.data.get(viewType));
    }

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
        this.data.set(index, data);
        this.notifyItemChanged(this.headers.size() + index);
    }

    private final @NotNull @UnmodifiableView List<T> unmodifiableData = Collections.unmodifiableList(this.data);
    @SuppressWarnings("SuspiciousGetterSetter")
    public @NotNull @UnmodifiableView List<T> getData() {
        return this.unmodifiableData;
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

    private final @NotNull @UnmodifiableView List<@NotNull View> unmodifiableHeaders = Collections.unmodifiableList(this.headers);
    @SuppressWarnings("SuspiciousGetterSetter")
    public @NotNull @UnmodifiableView List<@NotNull View> getHeaders() {
        return this.unmodifiableHeaders;
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

    private final @NotNull @UnmodifiableView List<@NotNull View> unmodifiableTailors = Collections.unmodifiableList(this.tailors);
    @SuppressWarnings("SuspiciousGetterSetter")
    public @NotNull @UnmodifiableView List<View> getTailors() {
        return this.unmodifiableTailors;
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
        if (layoutManager instanceof final GridLayoutManager gridLayoutManager)
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(final int position) {
                    return EnhancedRecyclerViewAdapter.this.getItemViewType(position) == 0 ? 1 : gridLayoutManager.getSpanCount();
                }
            });
    }

    @Override
    public void onViewAttachedToWindow(final @NotNull WrappedViewHolder<?, T> holder) {
        super.onViewAttachedToWindow(holder);
        final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params instanceof final StaggeredGridLayoutManager.LayoutParams layoutParams)
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
