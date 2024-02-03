package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.view.View;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.MoveTasksManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class TaskMove extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskMove() {
        super(PageTaskAdapter.Types.Move, TaskMove.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, MoveTasksManager.MoveTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new MoveFailureTaskStateFragment();
            case Working -> new MoveWorkingTaskStateFragment();
            case Success -> new MoveSuccessTaskStateFragment();
        };
    }

    public static class MoveFailureTaskStateFragment extends SimpleFailureTaskStateFragment<MoveTasksManager.MoveTask, MoveTasksManager.MoveFailure> {
        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, ?, ?, MoveTasksManager.MoveFailure> getManager() {
            return MoveTasksManager.getInstance();
        }
    }

    public static class MoveWorkingTaskStateFragment extends SimpleWorkingTaskStateFragment<SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<MoveTasksManager.MoveWorking>, MoveTasksManager.MoveTask, MoveTasksManager.MoveWorking> {
        public MoveWorkingTaskStateFragment() {
            super(WrappedPageTaskListSimpleWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, MoveTasksManager.MoveWorking, ?, ?> getManager() {
            return MoveTasksManager.getInstance();
        }

        @Override
        protected void onPreparing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<MoveTasksManager.MoveWorking> cell, final boolean animate) {
            super.onPreparing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_move_working);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(true);
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setVisibility(View.INVISIBLE);
        }
    }

    public static class MoveSuccessTaskStateFragment extends SimpleSuccessTaskStateFragment<MoveTasksManager.MoveTask, MoveTasksManager.MoveSuccess> {
        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, ?, MoveTasksManager.MoveSuccess, ?> getManager() {
            return MoveTasksManager.getInstance();
        }
    }
}
