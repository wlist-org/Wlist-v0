package com.xuxiaocheng.WListAndroid.Utils;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

public final class EmptyRecyclerAdapter extends RecyclerView.Adapter<EmptyRecyclerAdapter.EmptyViewHolder> {
    public static final @NotNull EmptyRecyclerAdapter Instance = new EmptyRecyclerAdapter();

    protected static class EmptyViewHolder extends RecyclerView.ViewHolder {
        protected EmptyViewHolder(final @NotNull View itemView) {
            super(itemView);
        }
    }

    private EmptyRecyclerAdapter() {
        super();
    }

    @Deprecated
    @Override
    public @NotNull EmptyViewHolder onCreateViewHolder(final @NotNull ViewGroup parent, final int viewType) {
        return new EmptyViewHolder(parent.getChildAt(0));
    }

    @Deprecated
    @Override
    public void onBindViewHolder(final @NotNull EmptyRecyclerAdapter.EmptyViewHolder holder, final int position) {
    }

    @Deprecated
    @Override
    public int getItemCount() {
        return 0;
    }

    @Deprecated
    @Override
    public long getItemId(final int position) {
        return super.getItemId(position);
    }

    @Deprecated
    @Override
    public int getItemViewType(final int position) {
        return super.getItemViewType(position);
    }
}
