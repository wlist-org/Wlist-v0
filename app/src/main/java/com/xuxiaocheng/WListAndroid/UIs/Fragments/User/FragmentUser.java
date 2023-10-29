package com.xuxiaocheng.WListAndroid.UIs.Fragments.User;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentUser extends IFragment<PageUserBinding, FragmentUser> {
    @Override
    protected @NotNull PageUserBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageUserBinding.inflate(inflater);
    }


}
