package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import org.jetbrains.annotations.NotNull;

public class TaskDownload extends SPageTaskFragment {
    @Override
    protected void iOnBuildPage(final @NotNull PageTaskListBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        page.pageTaskListContent.setAdapter(new EnhancedRecyclerViewAdapter() {
            @Override
            protected @NotNull WrappedViewHolder createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition) {
                return null;
            }

            @Override
            public void onBindViewHolder(final RecyclerView.@NotNull ViewHolder holder, final int position) {

            }
        });
    }
}
