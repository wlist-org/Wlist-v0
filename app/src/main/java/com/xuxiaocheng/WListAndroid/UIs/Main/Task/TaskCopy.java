package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.CopyTasksManager;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListCopyFailureCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListCopySuccessCellBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListCopyWorkingCellBinding;
import org.jetbrains.annotations.NotNull;

public class TaskCopy extends SPageTaskFragment {
    public TaskCopy() {
        super(PageTaskAdapter.Types.Copy);
    }

    @Override
    protected @NotNull SPageTaskStateFragment<?, CopyTasksManager.CopyTask, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new CopyFailureTaskStateFragment();
            case Working -> new CopyWorkingTaskStateFragment();
            case Success -> new CopySuccessTaskStateFragment();
        };
    }

    public static class CopyFailureTaskStateFragment extends FailureTaskStateFragment<PageTaskListCopyFailureCellBinding, CopyTasksManager.CopyTask, CopyTasksManager.CopyFailure> {
        public CopyFailureTaskStateFragment() {
            super(PageTaskListCopyFailureCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, ?, ?, CopyTasksManager.CopyFailure> getManager() {
            return CopyTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListCopyFailureCellBinding cell, final CopyTasksManager.@NotNull CopyTask task, final CopyTasksManager.@NotNull CopyFailure data) {
        }
    }

    public static class CopyWorkingTaskStateFragment extends WorkingTaskStateFragment<PageTaskListCopyWorkingCellBinding, CopyTasksManager.CopyTask, CopyTasksManager.CopyWorking> {
        public CopyWorkingTaskStateFragment() {
            super(PageTaskListCopyWorkingCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, CopyTasksManager.CopyWorking, ?, ?> getManager() {
            return CopyTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListCopyWorkingCellBinding cell, final CopyTasksManager.@NotNull CopyTask task, final CopyTasksManager.@NotNull CopyWorking data) {
        }
    }

    public static class CopySuccessTaskStateFragment extends SuccessTaskStateFragment<PageTaskListCopySuccessCellBinding, CopyTasksManager.CopyTask, CopyTasksManager.CopySuccess> {
        public CopySuccessTaskStateFragment() {
            super(PageTaskListCopySuccessCellBinding::inflate);
        }

        @Override
        protected @NotNull AbstractTasksManager<CopyTasksManager.CopyTask, ?, CopyTasksManager.CopySuccess, ?> getManager() {
            return CopyTasksManager.getInstance();
        }

        @Override
        protected void onBind(final @NotNull PageTaskListCopySuccessCellBinding cell, final CopyTasksManager.@NotNull CopyTask task, final CopyTasksManager.@NotNull CopySuccess data) {
        }
    }
}
