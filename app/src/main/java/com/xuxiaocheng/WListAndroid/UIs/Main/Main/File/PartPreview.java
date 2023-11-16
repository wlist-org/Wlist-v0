package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import androidx.annotation.UiThread;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

class PartPreview extends SFragmentFilePart {
    protected PartPreview(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @UiThread
    protected void preview(final @NotNull ActivityMain activity, final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean clickable) {
        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
            throw new UnsupportedOperationException("WIP");
        }, () -> clickable.set(true))); // TODO
    }
}
