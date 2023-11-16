package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import org.jetbrains.annotations.NotNull;

public class TaskTrash extends SPageTaskFragment {
    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new TrashFailureTaskStateFragment();
            case Working -> new TrashWorkingTaskStateFragment();
            case Success -> new TrashSuccessTaskStateFragment();
        };
    }

    public static class TrashFailureTaskStateFragment extends FailureTaskStateFragment {
    }

    public static class TrashWorkingTaskStateFragment extends WorkingTaskStateFragment {
    }

    public static class TrashSuccessTaskStateFragment extends SuccessTaskStateFragment {
    }
}
