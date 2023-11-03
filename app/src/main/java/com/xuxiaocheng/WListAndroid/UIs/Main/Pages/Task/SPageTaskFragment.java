package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import org.jetbrains.annotations.NotNull;

public abstract class SPageTaskFragment extends CFragment<PageTaskListBinding> {
    @Override
    protected @NotNull PageTaskListBinding iOnInflater() {
        return PageTaskListBinding.inflate(this.activity().getLayoutInflater());
    }
}
