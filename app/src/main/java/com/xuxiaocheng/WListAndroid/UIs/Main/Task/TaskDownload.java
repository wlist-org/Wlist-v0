package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.content.Context;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListDownloadWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class TaskDownload extends SPageTaskFragment {
    public TaskDownload() {
        super(PageTaskAdapter.Types.Download);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new DownloadFailureTaskStateFragment();
            case Working -> new DownloadWorkingTaskStateFragment();
            case Success -> new DownloadSuccessTaskStateFragment();
        };
    }

    public static class DownloadFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadFailure> {
        public DownloadFailureTaskStateFragment() {
            super(PageTaskListDownloadWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, ?, DownloadTasksManager.DownloadFailure> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadFailure data) {

        }
    }

    public static class DownloadWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking> {
        public DownloadWorkingTaskStateFragment() {
            super(PageTaskListDownloadWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking, ?, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadWorking data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadWorkingCellImage, false, task.getFilename());
            cell.pageTaskListDownloadWorkingCellName.setText(task.getFilename());
        }
    }

    public static class DownloadSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListDownloadSuccessCellBinding, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadSuccess> {
        public DownloadSuccessTaskStateFragment() {
            super(PageTaskListDownloadSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, DownloadTasksManager.DownloadSuccess, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadSuccessCellImage, false, task.getFilename());
            cell.pageTaskListDownloadSuccessCellName.setText(task.getFilename());
            final Context context = cell.getRoot().getContext();
            cell.pageTaskListDownloadSuccessCellSize.setText(ViewUtil.formatSize(task.getSavePath().length(), context.getString(R.string.unknown)));
            cell.pageTaskListDownloadSuccessCellPath.setText(MessageFormat.format(context.getString(R.string.page_task_download_path), task.getSavePath().getAbsolutePath()));
        }
    }
}
