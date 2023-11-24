package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.UploadTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListUploadFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListUploadSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListUploadWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

public class TaskUpload extends SPageTaskFragment {
    public TaskUpload() {
        super(PageTaskAdapter.Types.Upload);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, UploadTasksManager.UploadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new UploadFailureTaskStateFragment();
            case Working -> new UploadWorkingTaskStateFragment();
            case Success -> new UploadSuccessTaskStateFragment();
        };
    }

    public static class UploadFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListUploadFailureCellBinding, UploadTasksManager.UploadTask, UploadTasksManager.UploadFailure> {
        public UploadFailureTaskStateFragment() {
            super(PageTaskListUploadFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, ?, ?, UploadTasksManager.UploadFailure> getManager() {
            return UploadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListUploadFailureCellBinding cell, final UploadTasksManager.@NotNull UploadTask task, final UploadTasksManager.@NotNull UploadFailure data) {
        }
    }

    public static class UploadWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListUploadWorkingCellBinding, UploadTasksManager.UploadTask, UploadTasksManager.UploadWorking> {
        public UploadWorkingTaskStateFragment() {
            super(PageTaskListUploadWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, UploadTasksManager.UploadWorking, ?, ?> getManager() {
            return UploadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListUploadWorkingCellBinding cell, final UploadTasksManager.@NotNull UploadTask task, final UploadTasksManager.@NotNull UploadWorking data) {
        }
    }

    public static class UploadSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListUploadSuccessCellBinding, UploadTasksManager.UploadTask, UploadTasksManager.UploadSuccess> {
        public UploadSuccessTaskStateFragment() {
            super(PageTaskListUploadSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<UploadTasksManager.UploadTask, ?, UploadTasksManager.UploadSuccess, ?> getManager() {
            return UploadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListUploadSuccessCellBinding cell, final UploadTasksManager.@NotNull UploadTask task, final UploadTasksManager.@NotNull UploadSuccess data) {
        }
    }
}
