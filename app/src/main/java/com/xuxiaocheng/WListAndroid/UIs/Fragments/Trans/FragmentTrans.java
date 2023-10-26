package com.xuxiaocheng.WListAndroid.UIs.Fragments.Trans;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageTransBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentTrans extends IFragment<PageTransBinding> {
    public FragmentTrans(final @NotNull ActivityMain activity) {
        super(activity);
    }

    @Override
    protected @NotNull PageTransBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageTransBinding.inflate(inflater);
    }

    @Override
    protected void onShow(final @NotNull PageTransBinding page) {

    }
}
