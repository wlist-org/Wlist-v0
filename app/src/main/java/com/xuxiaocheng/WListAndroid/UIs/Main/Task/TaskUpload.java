package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import org.jetbrains.annotations.NotNull;

public class TaskUpload extends SPageTaskFragment {
    @Override
    protected @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type) {
        return switch (type) {
            case Failure -> new UploadFailureTaskStateFragment();
            case Working -> new UploadWorkingTaskStateFragment();
            case Success -> new UploadSuccessTaskStateFragment();
        };
    }

    public static class UploadFailureTaskStateFragment extends FailureTaskStateFragment {
    }

    public static class UploadWorkingTaskStateFragment extends WorkingTaskStateFragment {
    }

    public static class UploadSuccessTaskStateFragment extends SuccessTaskStateFragment {
    }
}
