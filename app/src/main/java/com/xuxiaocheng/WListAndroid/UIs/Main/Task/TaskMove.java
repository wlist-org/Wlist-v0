package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import org.jetbrains.annotations.NotNull;

public class TaskMove extends SPageTaskFragment {
    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new MoveFailureTaskStateFragment();
            case Working -> new MoveWorkingTaskStateFragment();
            case Success -> new MoveSuccessTaskStateFragment();
        };
    }

    public static class MoveFailureTaskStateFragment extends TaskRename.FailureTaskStateFragment {
    }

    public static class MoveWorkingTaskStateFragment extends TaskRename.WorkingTaskStateFragment {
    }

    public static class MoveSuccessTaskStateFragment extends TaskRename.SuccessTaskStateFragment {
    }
}
