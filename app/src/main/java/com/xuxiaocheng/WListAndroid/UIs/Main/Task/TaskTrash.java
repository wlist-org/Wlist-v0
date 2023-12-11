package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.TrashTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashWorkingCellBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TaskTrash extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskTrash() {
        super(PageTaskAdapter.Types.Trash, TaskTrash.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, TrashTasksManager.TrashTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new TrashFailureTaskStateFragment();
            case Working -> new TrashWorkingTaskStateFragment();
            case Success -> new TrashSuccessTaskStateFragment();
        };
    }

    public static class TrashFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListTrashFailureCellBinding, TrashTasksManager.TrashTask, TrashTasksManager.TrashFailure> {
        public TrashFailureTaskStateFragment() {
            super(PageTaskListTrashFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<TrashTasksManager.TrashTask, ?, ?, TrashTasksManager.TrashFailure> getManager() {
            return TrashTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListTrashFailureCellBinding cell, final TrashTasksManager.@NotNull TrashTask task, final TrashTasksManager.@NotNull TrashFailure data) {
            ViewUtil.setFileImage(cell.pageTaskListTrashFailureCellImage, true, task.getFilename());
            cell.pageTaskListTrashFailureCellName.setText(task.getFilename());
            cell.pageTaskListTrashFailureCellRemove.setOnClickListener(v -> new AlertDialog.Builder(this.activity())
                    .setTitle(R.string.page_task_remove)
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        this.getManager().removeFailureTask(this.activity(), task);
                    }))).show());
            cell.pageTaskListTrashFailureCellRetry.setOnClickListener(v -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                this.getManager().removeFailureTask(this.activity(), task);
                this.getManager().addTask(this.activity(), task);
            })));
        }
    }

    public static class TrashWorkingTaskStateFragment extends WorkingTaskStateFragment<TrashWorkingTaskStateFragment.WrappedPageTaskListTrashWorkingCellBinding, TrashTasksManager.TrashTask, TrashTasksManager.TrashWorking> {
        protected static class WrappedPageTaskListTrashWorkingCellBinding implements ViewBinding {
            protected final @NotNull PageTaskListTrashWorkingCellBinding cell;
            protected TrashTasksManager.@Nullable TrashWorking working;

            protected WrappedPageTaskListTrashWorkingCellBinding(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup group, final boolean attachRoot) {
                super();
                this.cell = PageTaskListTrashWorkingCellBinding.inflate(inflater, group, attachRoot);
            }

            @Override
            public @NotNull View getRoot() {
                return this.cell.getRoot();
            }

            @Override
            public @NotNull String toString() {
                return "WrappedPageTaskListTrashWorkingCellBinding{" +
                        "cell=" + this.cell +
                        ", working=" + this.working +
                        '}';
            }
        }

        public TrashWorkingTaskStateFragment() {
            super(WrappedPageTaskListTrashWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<TrashTasksManager.TrashTask, TrashTasksManager.TrashWorking, ?, ?> getManager() {
            return TrashTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull WrappedPageTaskListTrashWorkingCellBinding cell, final TrashTasksManager.@NotNull TrashTask task, final TrashTasksManager.@NotNull TrashWorking data) {
            ViewUtil.setFileImage(cell.cell.pageTaskListTrashWorkingCellImage, true, task.getFilename());
            cell.cell.pageTaskListTrashWorkingCellName.setText(task.getFilename());
            cell.working = data;
            cell.cell.pageTaskListTrashWorkingCellProgress.setMin(0);
            cell.cell.pageTaskListTrashWorkingCellProgress.setIndeterminate(false);
            if (data.isStarted())
                this.resetProgress(cell, false);
            else
                this.onPending(cell);
            data.getUpdateCallbacks().registerNamedForce(this.getClass().getName(), () -> Main.runOnUiThread(this.activity(), () ->
                    this.resetProgress(cell, true)));
        }

        @UiThread
        protected void onPending(final @NotNull WrappedPageTaskListTrashWorkingCellBinding cell) {
            final Context context = cell.getRoot().getContext();
            cell.cell.pageTaskListTrashWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 0));
            cell.cell.pageTaskListTrashWorkingCellProgress.setProgress(0, false);
            cell.cell.pageTaskListTrashWorkingCellItems.setText(R.string.page_task_waiting);
        }

        @UiThread
        protected void resetProgress(final @NotNull WrappedPageTaskListTrashWorkingCellBinding cell, final boolean animate) {
            final TrashTasksManager.TrashWorking working = cell.working;
            assert working != null;
            final AtomicLong done = working.getDone(), total = working.getTotal();
            assert done != null && total != null;
            final long d = done.get(), t = total.get();
            final Context context = cell.getRoot().getContext();
            final float percent = 1.0f * d / t;
            cell.cell.pageTaskListTrashWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), percent));
            //noinspection NumericCastThatLosesPrecision // TODO
            cell.cell.pageTaskListTrashWorkingCellProgress.setMax((int) t);
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListTrashWorkingCellProgress.setProgress((int) d);
            cell.cell.pageTaskListTrashWorkingCellItems.setText(MessageFormat.format(context.getString(R.string.page_task_trash_time), d, t));
        }

        @Override
        protected void onUnbind(final @NotNull WrappedPageTaskListTrashWorkingCellBinding cell) {
            super.onUnbind(cell);
            if (cell.working != null)
                cell.working.getUpdateCallbacks().unregisterNamed(this.getClass().getName());
            cell.working = null;
        }
    }

    public static class TrashSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListTrashSuccessCellBinding, TrashTasksManager.TrashTask, TrashTasksManager.TrashSuccess> {
        public TrashSuccessTaskStateFragment() {
            super(PageTaskListTrashSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<TrashTasksManager.TrashTask, ?, TrashTasksManager.TrashSuccess, ?> getManager() {
            return TrashTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListTrashSuccessCellBinding cell, final TrashTasksManager.@NotNull TrashTask task, final TrashTasksManager.@NotNull TrashSuccess data) {
            ViewUtil.setFileImage(cell.pageTaskListTrashSuccessCellImage, true, task.getFilename());
            cell.pageTaskListTrashSuccessCellName.setText(task.getFilename());
            cell.pageTaskListTrashSuccessCellHint.setText(MessageFormat.format(this.getString(R.string.page_task_trash_time), ViewUtil.formatTime(task.getTime(), this.getString(R.string.unknown))));
            cell.pageTaskListTrashSuccessCellRemove.setOnClickListener(v -> new AlertDialog.Builder(this.activity())
                    .setTitle(R.string.page_task_remove)
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() ->
                            this.getManager().removeSuccessTask(this.activity(), task))))
                    .show());
        }
    }
}
