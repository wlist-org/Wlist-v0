package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import org.jetbrains.annotations.NotNull;

public class TaskCopy extends SPageTaskFragment {
    public TaskCopy() {
        super(PageTaskAdapter.Types.Copy);
    }

    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new CopyFailureTaskStateFragment();
            case Working -> new CopyWorkingTaskStateFragment();
            case Success -> new CopySuccessTaskStateFragment();
        };
    }

    public static class CopyFailureTaskStateFragment extends FailureTaskStateFragment {
    }

    public static class CopyWorkingTaskStateFragment extends WorkingTaskStateFragment {
    }

    public static class CopySuccessTaskStateFragment extends SuccessTaskStateFragment {
    }
}
