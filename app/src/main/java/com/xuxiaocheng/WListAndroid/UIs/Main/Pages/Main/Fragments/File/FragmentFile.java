package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.File;

import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.IPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentFile extends IPageMainFragment<PageFileBinding> {
    @Override
    protected @NotNull PageFileBinding iOnInflater() {
        return PageFileBinding.inflate(this.activity().getLayoutInflater());
    }
}
