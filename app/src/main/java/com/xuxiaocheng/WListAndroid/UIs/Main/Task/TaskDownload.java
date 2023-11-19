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
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new DownloadFailureTaskStateFragment();
            case Working -> new DownloadWorkingTaskStateFragment();
            case Success -> new DownloadSuccessTaskStateFragment();
        };
    }

    public static class DownloadFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask> {
        public DownloadFailureTaskStateFragment() {
            super(PageTaskListDownloadWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask data) {
            // TODO
        }
    }

    public static class DownloadWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListDownloadWorkingCellBinding, DownloadTasksManager.DownloadTask> {
        public DownloadWorkingTaskStateFragment() {
            super(PageTaskListDownloadWorkingCellBinding::inflate);
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadWorkingCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadWorkingCellImage, false, data.getFilename());
            cell.pageTaskListDownloadWorkingCellName.setText(data.getFilename());
        }
    }

    public static class DownloadSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListDownloadSuccessCellBinding, DownloadTasksManager.DownloadTask> {
        public DownloadSuccessTaskStateFragment() {
            super(PageTaskListDownloadSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListDownloadSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask data) {
            ViewUtil.setFileImage(cell.pageTaskListDownloadSuccessCellImage, false, data.getFilename());
            cell.pageTaskListDownloadSuccessCellName.setText(data.getFilename());
            final Context context = cell.getRoot().getContext();
            cell.pageTaskListDownloadSuccessCellSize.setText(ViewUtil.formatSize(data.getSavePath().length(), context.getString(R.string.unknown)));
            cell.pageTaskListDownloadSuccessCellPath.setText(MessageFormat.format(context.getString(R.string.page_task_download_path), data.getSavePath().getAbsolutePath()));
        }
    }
}
