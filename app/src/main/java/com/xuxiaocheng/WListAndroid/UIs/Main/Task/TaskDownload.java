package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListSimpleSuccessCellBinding;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TaskDownload extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskDownload() {
        super(PageTaskAdapter.Types.Download, TaskDownload.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, DownloadTasksManager.DownloadTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new DownloadFailureTaskStateFragment();
            case Working -> new DownloadWorkingTaskStateFragment();
            case Success -> new DownloadSuccessTaskStateFragment();
        };
    }

    public static class DownloadFailureTaskStateFragment extends SimpleFailureTaskStateFragment<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadFailure> {
        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, ?, DownloadTasksManager.DownloadFailure> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        @SuppressWarnings("OverlyBroadThrowsClause")
        protected void removeTask(final DownloadTasksManager.@NotNull DownloadTask task) throws Exception {
            super.removeTask(task);
            Files.deleteIfExists(task.getSavePath().toPath());
            Files.deleteIfExists(FilesAssistant.getDownloadRecordFile(task.getSavePath()).toPath());
        }
    }

    public static class DownloadWorkingTaskStateFragment extends SimpleWorkingTaskStateFragment<SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<DownloadTasksManager.DownloadWorking>, DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking> {
        public DownloadWorkingTaskStateFragment() {
            super(WrappedPageTaskListSimpleWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadWorking, ?, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected void onPreparing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<DownloadTasksManager.DownloadWorking> cell, final boolean animate) {
            super.onPreparing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_download_working_preparing);
        }

        @Override
        protected void onFinishing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<DownloadTasksManager.DownloadWorking> cell, final boolean animate) {
            super.onFinishing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_download_working_finishing);
        }

        @Override
        protected void onWorking(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<DownloadTasksManager.DownloadWorking> cell, final boolean animate, final Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long> m) {
            super.onWorking(cell, animate, m);
            assert cell.working != null;
            final List<Pair.ImmutablePair<AtomicLong, Long>> writeProgress = cell.working.getProgress();
            assert writeProgress != null;
            long current = 0, total = 0;
            for (final Pair.ImmutablePair<AtomicLong, Long> pair: writeProgress) {
                current += pair.getFirst().longValue();
                total += pair.getSecond().longValue();
            }
            final float writePercent = 1.0f * current / total;
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListSimpleWorkingCellProgress.setSecondaryProgress((int) (writePercent * 1000));
        }
    }

    public static class DownloadSuccessTaskStateFragment extends SimpleSuccessTaskStateFragment<DownloadTasksManager.DownloadTask, DownloadTasksManager.DownloadSuccess> {
        @Override
        protected @NotNull AbstractTasksManager<DownloadTasksManager.DownloadTask, ?, DownloadTasksManager.DownloadSuccess, ?> getManager() {
            return DownloadTasksManager.getInstance();
        }

        @Override
        protected boolean isNormal(final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            return task.getSavePath().isFile();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListSimpleSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            super.onBind(cell, task, data);
            cell.pageTaskListSimpleSuccessCellHint.setText(MessageFormat.format(cell.getRoot().getContext().getString(R.string.page_task_download_success_path), task.getSavePath().getAbsolutePath()));
        }

        @Override
        protected void onNormal(final @NotNull PageTaskListSimpleSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            super.onNormal(cell, task, data);
            cell.pageTaskListSimpleSuccessCellSize.setText(ViewUtil.formatSize(task.getSavePath().length(), cell.getRoot().getContext().getString(R.string.unknown)));
            cell.pageTaskListSimpleSuccessCellRemove.setOnClickListener(v -> new AlertDialog.Builder(this.activity())
                    .setTitle(R.string.page_task_remove)
                    .setNeutralButton(R.string.cancel, null)
                    .setNegativeButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() ->
                            DownloadTasksManager.getInstance().removeSuccessTask(this.activity(), task))))
                    .setPositiveButton(R.string.page_task_remove_file, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        DownloadTasksManager.getInstance().removeSuccessTask(this.activity(), task);
                        Files.deleteIfExists(task.getSavePath().toPath());
                    }))).show());
        }

        @Override
        protected void onDeleted(final @NotNull PageTaskListSimpleSuccessCellBinding cell, final DownloadTasksManager.@NotNull DownloadTask task, final DownloadTasksManager.@NotNull DownloadSuccess data) {
            super.onDeleted(cell, task, data);
            cell.pageTaskListSimpleSuccessCellSize.setText(R.string.page_task_download_success_deleted);
        }

//        @Override
//        protected void removeAll() {
//            new AlertDialog.Builder(this.activity())
//                    .setTitle(R.string.page_task_remove_all)
//                    .setNeutralButton(R.string.cancel, null)
//                    .setNegativeButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() ->
//                            DownloadTasksManager.getInstance().removeSuccessTask(this.activity(), task))))
//                    .setPositiveButton(R.string.page_task_remove_file, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                        DownloadTasksManager.getInstance().removeSuccessTask(this.activity(), task);
//                        Files.deleteIfExists(saved.toPath());
//                    }))).show();
//            ViewUtil.requireEnhancedRecyclerAdapter(this.content().pageTaskListContentList).clearData(); // Quick Response.
//            Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> this.getManager().removeAllSuccessTask(this.activity())));
//        }
    }
}
