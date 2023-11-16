package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import com.xuxiaocheng.WListAndroid.UIs.Main.Main.SPageMainFragmentPart;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public abstract class SFragmentFilePart extends SPageMainFragmentPart<FragmentFile> {
    protected SFragmentFilePart(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected @NotNull PageFileBinding fragmentContent() {
        return (PageFileBinding) super.fragmentContent();
    }
}
