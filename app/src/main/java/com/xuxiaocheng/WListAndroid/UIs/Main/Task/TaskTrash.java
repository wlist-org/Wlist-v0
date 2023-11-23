package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

public class TaskTrash extends SPageTaskFragment {
    public TaskTrash() {
        super(PageTaskAdapter.Types.Trash);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new TrashFailureTaskStateFragment();
            case Working -> new TrashWorkingTaskStateFragment();
            case Success -> new TrashSuccessTaskStateFragment();
        };
    }

    public static class TrashFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListDownloadFailureCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadFailure> {
        public TrashFailureTaskStateFragment() {
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

    public static class TrashWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking> {
        public TrashWorkingTaskStateFragment() {
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

    public static class TrashSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListDownloadSuccessCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadSuccess> {
        public TrashSuccessTaskStateFragment() {
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
