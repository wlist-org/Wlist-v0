package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import org.jetbrains.annotations.NotNull;

public class TaskRename extends SPageTaskFragment {
    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new RenameFailureTaskStateFragment();
            case Working -> new RenameWorkingTaskStateFragment();
            case Success -> new RenameSuccessTaskStateFragment();
        };
    }

    public static class RenameFailureTaskStateFragment extends FailureTaskStateFragment {
    }

    public static class RenameWorkingTaskStateFragment extends WorkingTaskStateFragment {
    }

    public static class RenameSuccessTaskStateFragment extends SuccessTaskStateFragment {
    }
}
