package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import org.jetbrains.annotations.NotNull;

public class TaskDownload extends SPageTaskFragment {
    public TaskDownload() {
        super(PageTaskAdapter.Types.Download);
    }

    @Override
    protected void iOnBuildPage(final @NotNull PageTaskListBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
//        final EnhancedRecyclerViewAdapter<DownloadTasksManager.DownloadTask, DownloadWorkingViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
//            @Override
//            protected @NotNull DownloadWorkingViewHolder createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition) {
//                return new DownloadWorkingViewHolder(EnhancedRecyclerViewAdapter.buildView(TaskDownload.this.getLayoutInflater(), R.layout.page_file_cell, page.pageTaskListContent));
//            }
//        };
//        page.pageTaskListContent.setAdapter(adapter);
    }

    protected static final class DownloadWorkingViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout, DownloadTasksManager.DownloadTask> {
        private DownloadWorkingViewHolder(final @NotNull ConstraintLayout itemView) {
            super(itemView);
        }

        @Override
        public void onBind(final DownloadTasksManager.@NotNull DownloadTask data) {

        }
    }

    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new DownloadFailureTaskStateFragment();
            case Working -> new DownloadWorkingTaskStateFragment();
            case Success -> new DownloadSuccessTaskStateFragment();
        };
    }

    public static class DownloadFailureTaskStateFragment extends FailureTaskStateFragment {
    }

    public static class DownloadWorkingTaskStateFragment extends WorkingTaskStateFragment {
    }

    public static class DownloadSuccessTaskStateFragment extends SuccessTaskStateFragment {
    }
}
