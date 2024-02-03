package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.RenameTasksManager;
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

    public static class RenameFailureTaskStateFragment extends SimpleFailureTaskStateFragment<RenameTasksManager.RenameTask, RenameTasksManager.RenameFailure> {
        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, ?, ?, RenameTasksManager.RenameFailure> getManager() {
            return RenameTasksManager.getInstance();
        }
    }

    public static class RenameWorkingTaskStateFragment extends SimpleWorkingTaskStateFragment<SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<RenameTasksManager.RenameWorking>, RenameTasksManager.RenameTask, RenameTasksManager.RenameWorking> {
        protected RenameWorkingTaskStateFragment() {
            super(WrappedPageTaskListSimpleWorkingCellBinding::new);
        }

        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, RenameTasksManager.RenameWorking, ?, ?> getManager() {
            return RenameTasksManager.getInstance();
        }

        @Override
        protected void onPreparing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<RenameTasksManager.RenameWorking> cell, final boolean animate) {
            super.onPreparing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_rename_working_preparing);
        }

        @Override
        protected void onFinishing(final @NotNull WrappedPageTaskListSimpleWorkingCellBinding<RenameTasksManager.RenameWorking> cell, final boolean animate) {
            super.onFinishing(cell, animate);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_rename_working_finishing);
        }

        @Override
        protected void onWorking(@NotNull final WrappedPageTaskListSimpleWorkingCellBinding<RenameTasksManager.RenameWorking> cell, final boolean animate, @NotNull final Pair.ImmutablePair<@NotNull Long, @NotNull Long> m) {
            super.onWorking(cell, animate, m);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(true);
        }
    }

    public static class RenameSuccessTaskStateFragment extends SimpleSuccessTaskStateFragment<RenameTasksManager.RenameTask, RenameTasksManager.RenameSuccess> {
        @Override
        protected @NotNull AbstractTasksManager<RenameTasksManager.RenameTask, ?, RenameTasksManager.RenameSuccess, ?> getManager() {
            return RenameTasksManager.getInstance();
        }
    }
}
