package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

public class TaskUpload extends SPageTaskFragment {
    public TaskUpload() {
        super(PageTaskAdapter.Types.Upload);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new UploadFailureTaskStateFragment();
            case Working -> new UploadWorkingTaskStateFragment();
            case Success -> new UploadSuccessTaskStateFragment();
        };
    }

    public static class UploadFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListDownloadFailureCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadFailure> {
        public UploadFailureTaskStateFragment() {
            super(PageTaskListDownloadFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, ?, DownloadTasksManager.DownloadFailure> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadFailureCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadFailure data) {
        }
    }

    public static class UploadWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking> {
        public UploadWorkingTaskStateFragment() {
            super(PageTaskListDownloadWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking, ?, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadWorking data) {
        }
    }

    public static class UploadSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListDownloadSuccessCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadSuccess> {
        public UploadSuccessTaskStateFragment() {
            super(PageTaskListDownloadSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, DownloadTasksManager.DownloadSuccess, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
        }
    }
}
