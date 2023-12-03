package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.UploadTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListSimpleSuccessCellBinding;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class TaskUpload extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskUpload() {
        super(PageTaskAdapter.Types.Upload, TaskUpload.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, UploadTasksManager.UploadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new UploadFailureTaskStateFragment();
            case Working -> new UploadWorkingTaskStateFragment();
            case Success -> new UploadSuccessTaskStateFragment();
        };
    }

    public static class UploadFailureTaskStateFragment extends SimpleFailureTaskStateFragment<UploadTasksManager.UploadTask, UploadTasksManager.UploadFailure> {
        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, ?, ?, UploadTasksManager.UploadFailure> getManager() {
            return UploadTasksManager.getInstance();
        }
    }

    public static class UploadWorkingTaskStateFragment extends SimpleWorkingTaskStateFragment<SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<UploadTasksManager.UploadWorking>, UploadTasksManager.UploadTask, UploadTasksManager.UploadWorking> {
        public UploadWorkingTaskStateFragment() {
            super(WrappedPageTaskListSimpleWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, UploadTasksManager.UploadWorking, ?, ?> getManager() {
            return UploadTasksManager.getInstance();
        }

        @Override
        protected void onPreparing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<UploadTasksManager.UploadWorking> cell, final boolean animate) {
            super.onPreparing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_upload_working_preparing);
        }

        @Override
        protected void onFinishing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<UploadTasksManager.UploadWorking> cell, final boolean animate) {
            super.onFinishing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_upload_working_finishing);
        }
    }

    public static class UploadSuccessTaskStateFragment extends SimpleSuccessTaskStateFragment<UploadTasksManager.UploadTask, UploadTasksManager.UploadSuccess> {
        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, ?, UploadTasksManager.UploadSuccess, ?> getManager() {
            return UploadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListSimpleSuccessCellBinding cell, final UploadTasksManager.@NotNull UploadTask task, final UploadTasksManager.@NotNull UploadSuccess data) {
            super.onBind(cell, task, data);
            cell.pageTaskListSimpleSuccessCellSize.setText(ViewUtil.formatSize(task.getFilesize(), cell.getRoot().getContext().getString(R.string.unknown)));
//            cell.pageTaskListSimpleSuccessCellHint.setText();
        }
    }
}
