package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.TrashTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListTrashWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

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
        }
    }

    public static class TrashWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListTrashWorkingCellBinding, TrashTasksManager.TrashTask, TrashTasksManager.TrashWorking> {
        public TrashWorkingTaskStateFragment() {
            super(PageTaskListTrashWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<TrashTasksManager.TrashTask, TrashTasksManager.TrashWorking, ?, ?> getManager() {
            return TrashTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListTrashWorkingCellBinding cell, final TrashTasksManager.@NotNull TrashTask task, final TrashTasksManager.@NotNull TrashWorking data) {
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
        }
    }
}
