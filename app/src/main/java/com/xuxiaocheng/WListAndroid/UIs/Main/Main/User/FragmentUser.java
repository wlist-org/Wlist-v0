package com.xuxiaocheng.WListAndroid.UIs.Main.Main.User;

import com.xuxiaocheng.WListAndroid.UIs.Main.Main.SPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentUser extends SPageMainFragment<PageUserBinding> {
    @Override
    protected @NotNull PageUserBinding iOnInflater() {
        return PageUserBinding.inflate(this.getLayoutInflater());
    }
}
