package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Tasks;

import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.CPage;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskBinding;
import org.jetbrains.annotations.NotNull;

public class PageTasks extends CPage<PageTaskBinding> {
    @Override
    protected @NotNull PageTaskBinding iOnInflater() {
        return PageTaskBinding.inflate(this.activity().getLayoutInflater());
    }
}
