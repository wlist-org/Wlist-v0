package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.User;

import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.IPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentUser extends IPageMainFragment<PageUserBinding> {
    @Override
    protected @NotNull PageUserBinding iOnInflater() {
        return PageUserBinding.inflate(this.activity().getLayoutInflater());
    }
}
