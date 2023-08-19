package com.xuxiaocheng.WListClientAndroid.Utils;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WARNING: When calling method 'notifyItem*', the position should add {@link #headersSize()}.
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
            return position + 1; // TYPE_HEADER
        if (position > this.headers.size() + this.adapter.getItemCount() - 1)
            return -position + this.headers.size() + this.adapter.getItemCount() - 1; // TYPE_TAILOR;
        return 0;
    }

    @Override
    @NonNull public WrappedViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (viewType > 0)
            return new WrappedViewHolder<>(this.headers.get(viewType - 1));
        if (viewType < 0)
            return new WrappedViewHolder<>(this.tailors.get(-viewType - 1));
        return this.adapter.onCreateViewHolder(parent, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull final WrappedViewHolder<?> holder, final int position) {
        if (this.getItemViewType(position) != 0)
            return;
        this.adapter.onBindViewHolder((VH) holder, position);
    }

    @Override
    public int getItemCount() {
        return this.headers.size() + this.adapter.getItemCount() + this.tailors.size();
    }

    public void addHeader(@NonNull final View header) {
        this.headers.add(header);
        super.notifyItemInserted(this.headers.size() - 1);
    }

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

    public void addTailor(@NonNull final View tailor) {
        this.tailors.add(tailor);
        super.notifyItemInserted(this.headers.size() + this.adapter.getItemCount() + this.tailors.size() - 1);
    }

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
    @NonNull public String toString() {
        return "RecyclerViewHeadersAndTailorsAdapterWrapper{" +
                "adapter=" + this.adapter +
                ", headers=" + this.headers +
                ", tailors=" + this.tailors +
                ", super=" + super.toString() +
                '}';
    }
}
