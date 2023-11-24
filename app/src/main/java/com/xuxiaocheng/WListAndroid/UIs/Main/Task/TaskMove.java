package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.MoveTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListMoveFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListMoveSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListMoveWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

public class TaskMove extends SPageTaskFragment {
    public TaskMove() {
        super(PageTaskAdapter.Types.Move);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, MoveTasksManager.MoveTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new MoveFailureTaskStateFragment();
            case Working -> new MoveWorkingTaskStateFragment();
            case Success -> new MoveSuccessTaskStateFragment();
        };
    }

    public static class MoveFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListMoveFailureCellBinding, MoveTasksManager.MoveTask, MoveTasksManager.MoveFailure> {
        public MoveFailureTaskStateFragment() {
            super(PageTaskListMoveFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, ?, ?, MoveTasksManager.MoveFailure> getManager() {
            return MoveTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListMoveFailureCellBinding cell, final MoveTasksManager.@NotNull MoveTask task, final MoveTasksManager.@NotNull MoveFailure data) {
        }
    }

    public static class MoveWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListMoveWorkingCellBinding, MoveTasksManager.MoveTask, MoveTasksManager.MoveWorking> {
        public MoveWorkingTaskStateFragment() {
            super(PageTaskListMoveWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, MoveTasksManager.MoveWorking, ?, ?> getManager() {
            return MoveTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListMoveWorkingCellBinding cell, final MoveTasksManager.@NotNull MoveTask task, final MoveTasksManager.@NotNull MoveWorking data) {
        }
    }

    public static class MoveSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListMoveSuccessCellBinding, MoveTasksManager.MoveTask, MoveTasksManager.MoveSuccess> {
        public MoveSuccessTaskStateFragment() {
            super(PageTaskListMoveSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<MoveTasksManager.MoveTask, ?, MoveTasksManager.MoveSuccess, ?> getManager() {
            return MoveTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListMoveSuccessCellBinding cell, final MoveTasksManager.@NotNull MoveTask task, final MoveTasksManager.@NotNull MoveSuccess data) {
        }
    }
}
