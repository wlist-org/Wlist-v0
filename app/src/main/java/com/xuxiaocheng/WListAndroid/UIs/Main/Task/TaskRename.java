package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.RenameTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListRenameFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListRenameSuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListRenameWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class TaskRename extends SPageTaskFragment {
    protected static final @NotNull AtomicReference<PageTaskStateAdapter.Types> CurrentState = new AtomicReference<>();

    public TaskRename() {
        super(PageTaskAdapter.Types.Rename, TaskRename.CurrentState);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, RenameTasksManager.RenameTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new RenameFailureTaskStateFragment();
            case Working -> new RenameWorkingTaskStateFragment();
            case Success -> new RenameSuccessTaskStateFragment();
        };
    }

    public static class RenameFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListRenameFailureCellBinding, RenameTasksManager.RenameTask, RenameTasksManager.RenameFailure> {
        public RenameFailureTaskStateFragment() {
            super(PageTaskListRenameFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, ?, ?, RenameTasksManager.RenameFailure> getManager() {
            return RenameTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListRenameFailureCellBinding cell, final RenameTasksManager.@NotNull RenameTask task, final RenameTasksManager.@NotNull RenameFailure data) {
        }
    }

    public static class RenameWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListRenameWorkingCellBinding, RenameTasksManager.RenameTask, RenameTasksManager.RenameWorking> {
        public RenameWorkingTaskStateFragment() {
            super(PageTaskListRenameWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, RenameTasksManager.RenameWorking, ?, ?> getManager() {
            return RenameTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListRenameWorkingCellBinding cell, final RenameTasksManager.@NotNull RenameTask task, final RenameTasksManager.@NotNull RenameWorking data) {
        }
    }

    public static class RenameSuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListRenameSuccessCellBinding, RenameTasksManager.RenameTask, RenameTasksManager.RenameSuccess> {
        public RenameSuccessTaskStateFragment() {
            super(PageTaskListRenameSuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, ?, RenameTasksManager.RenameSuccess, ?> getManager() {
            return RenameTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListRenameSuccessCellBinding cell, final RenameTasksManager.@NotNull RenameTask task, final RenameTasksManager.@NotNull RenameSuccess data) {
        }
    }
}
