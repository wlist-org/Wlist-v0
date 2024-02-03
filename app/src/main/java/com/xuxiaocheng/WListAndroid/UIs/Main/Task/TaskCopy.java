package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.view.View;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.CopyTasksManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class TaskCopy extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskCopy() {
        super(PageTaskAdapter.Types.Copy, TaskCopy.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, CopyTasksManager.CopyTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new CopyFailureTaskStateFragment();
            case Working -> new CopyWorkingTaskStateFragment();
            case Success -> new CopySuccessTaskStateFragment();
        };
    }

    public static class CopyFailureTaskStateFragment extends SimpleFailureTaskStateFragment<CopyTasksManager.CopyTask, CopyTasksManager.CopyFailure> {
        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, ?, ?, CopyTasksManager.CopyFailure> getManager() {
            return CopyTasksManager.getInstance();
        }
    }

    public static class CopyWorkingTaskStateFragment extends SimpleWorkingTaskStateFragment<SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<CopyTasksManager.CopyWorking>, CopyTasksManager.CopyTask, CopyTasksManager.CopyWorking> {
        public CopyWorkingTaskStateFragment() {
            super(WrappedPageTaskListSimpleWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, CopyTasksManager.CopyWorking, ?, ?> getManager() {
            return CopyTasksManager.getInstance();
        }

        @Override
        protected void onPreparing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<CopyTasksManager.CopyWorking> cell, final boolean animate) {
            super.onPreparing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_copy_working);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(true);
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setVisibility(View.INVISIBLE);
        }
    }

    public static class CopySuccessTaskStateFragment extends SimpleSuccessTaskStateFragment<CopyTasksManager.CopyTask, CopyTasksManager.CopySuccess> {
        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, ?, CopyTasksManager.CopySuccess, ?> getManager() {
            return CopyTasksManager.getInstance();
        }
    }
}
