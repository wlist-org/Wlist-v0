package com.xuxiaocheng.WListAndroid.UIs.Main;

import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.PageMain;
import org.jetbrains.annotations.NotNull;

public class ActivityMain extends CActivity {
    public ActivityMain() {
        super(new PageMain(), "main");
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "super=" + super.toString() +
                '}';
    }
}
