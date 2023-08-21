package com.xuxiaocheng.WListClientAndroid.Utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class EnhancedRecyclerViewAdapter<T, VH extends EnhancedRecyclerViewAdapter.WrappedViewHolder<T, ?>> extends RecyclerView.Adapter<EnhancedRecyclerViewAdapter.WrappedViewHolder<T, ?>> {
    @NonNull protected final List<View> headers = new ArrayList<>();
    @NonNull protected final List<View> tailors = new ArrayList<>();
    @NonNull protected final List<T> data = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @NonNull public static <V extends View> V buildView(@NonNull final LayoutInflater inflater, @LayoutRes final int cell, @NonNull final RecyclerView parent) {
        return (V) inflater.inflate(cell, parent, false);
    }

    public abstract static class WrappedViewHolder<T, V extends View> extends RecyclerView.ViewHolder {
        protected WrappedViewHolder(@NonNull final V itemView) {
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

    protected static class HeaderAndTailorViewHolder<T, V extends View> extends WrappedViewHolder<T, V> {
        protected HeaderAndTailorViewHolder(@NonNull final V itemView) {
            super(itemView);
        }
    }

    @Override
    @NonNull public WrappedViewHolder<T, ?> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (viewType > 0) return new HeaderAndTailorViewHolder<>(this.headers.get(viewType - 1));
        if (viewType < 0) return new HeaderAndTailorViewHolder<>(this.tailors.get(-viewType - 1));
        return this.createViewHolder(parent);
    }

    @NonNull protected abstract VH createViewHolder(@NonNull final ViewGroup parent);

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull final WrappedViewHolder<T, ?> holder, final int position) {
        if (this.getItemViewType(position) == 0)
            this.bindViewHolder((VH) holder, this.data.get(position - this.headers.size()));
    }

    protected abstract void bindViewHolder(@NonNull final VH holder, @NonNull final T information);

    @UiThread
    public void addDataRange(@NonNull final Collection<? extends T> data) {
        this.data.addAll(data);
        this.notifyItemRangeInserted(this.headers.size() + this.data.size() - data.size(), data.size());
    }

    // TODO: more data struct changer.

    @Override
    public int getItemCount() {
        return this.headers.size() + this.data.size() + this.tailors.size();
    }

    @UiThread
    public void addHeader(@NonNull final View header) {
        this.headers.add(header);
        super.notifyItemInserted(this.headers.size() - 1);
    }

    @UiThread
    public void setHeader(final int index, @NonNull final View header) {
        this.headers.set(index, header);
        super.notifyItemChanged(index);
    }

    @UiThread
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

    @UiThread
    public void addTailor(@NonNull final View tailor) {
        this.tailors.add(tailor);
        super.notifyItemInserted(this.headers.size() + this.data.size() + this.tailors.size() - 1);
    }

    @UiThread
    public void setTailor(final int index, @NonNull final View tailor) {
        this.tailors.set(index, tailor);
        super.notifyItemChanged(this.headers.size() + this.data.size() + index);
    }

    @UiThread
    public void removeTailor(final int index) {
        this.tailors.remove(index);
        super.notifyItemRemoved(this.headers.size() + this.data.size() + index);
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
                    return EnhancedRecyclerViewAdapter.this.getItemViewType(position) == 0 ? 1 : gridLayoutManager.getSpanCount();
                }
            });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull final WrappedViewHolder<T, ?> holder) {
        super.onViewAttachedToWindow(holder);
        final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params instanceof StaggeredGridLayoutManager.LayoutParams layoutParams)
            layoutParams.setFullSpan(this.getItemViewType(holder.getLayoutPosition()) != 0);
    }

    @Override
    @NonNull public String toString() {
        return "EnhancedRecyclerViewAdapter{" +
                "headers=" + this.headers +
                ", tailors=" + this.tailors +
                ", data=" + this.data +
                ", super=" + super.toString() +
                '}';
    }
}
