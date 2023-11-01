package com.xuxiaocheng.WListAndroid.UIs.Pages;

import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;

public class PageMain extends IPage<ActivityMainBinding> {
    @Override
    protected @NotNull ActivityMainBinding iOnInflater() {
        return ActivityMainBinding.inflate(this.activity().getLayoutInflater());
    }


}
