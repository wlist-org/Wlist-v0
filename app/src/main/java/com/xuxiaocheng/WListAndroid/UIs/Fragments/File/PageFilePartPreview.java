package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import androidx.annotation.UiThread;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class PageFilePartPreview {
    protected final @NotNull FragmentFile fragmentFile;

    public PageFilePartPreview(final @NotNull FragmentFile fragmentFile) {
        super();
        this.fragmentFile = fragmentFile;
    }

    private @NotNull ActivityMain activity() {
        return this.fragmentFile.getMainActivity();
    }


    @UiThread
    protected void preview(final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean clickable) {
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            throw new UnsupportedOperationException("WIP");
        }, () -> clickable.set(true))); // TODO
    }
}
