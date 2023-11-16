package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListContentBinding;
import org.jetbrains.annotations.NotNull;

public class SPageTaskStateFragment extends CFragment<PageTaskListContentBinding> {
    @Override
    protected @NotNull PageTaskListContentBinding iOnInflater() {
        return PageTaskListContentBinding.inflate(this.getLayoutInflater());
    }

    @Override
    public @NotNull PageTask page() {
        return (PageTask) super.page();
    }

    @Override
    public @NotNull ActivityMain activity() {
        return (ActivityMain) super.activity();
    }
}
