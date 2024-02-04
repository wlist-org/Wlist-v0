package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import androidx.annotation.UiThread;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

class PartPreview extends SFragmentFilePart {
    protected PartPreview(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @UiThread
    protected void preview(final @NotNull ActivityMain activity, final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean clickable) {
        Toaster.show(R.string.page_file_preview_unsupported); // TODO
        clickable.set(true);
    }
}
