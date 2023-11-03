package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.view.View;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.PageMainAdapter;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.SPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentFile extends SPageMainFragment<PageFileBinding> {
    @Override
    protected @NotNull PageFileBinding iOnInflater() {
        return PageFileBinding.inflate(this.activity().getLayoutInflater());
    }

    @Override
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
        super.sOnTypeChanged(type);
        this.page().content().activityMainOptions.setVisibility(switch (type) {
            case File -> View.VISIBLE;
            case User -> View.GONE;
        });
    }
}
