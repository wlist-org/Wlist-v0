package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentFile extends IFragment<PageFileBinding, FragmentFile> {
    protected final @NotNull PartConnect partConnect = new PartConnect(this);
    protected final @NotNull PartOptions partOptions = new PartOptions(this);
    protected final @NotNull PartList partList = new PartList(this);

    public FragmentFile() {
        super();
        this.parts.add(this.partConnect);
        this.parts.add(this.partOptions);
        this.parts.add(this.partList);
    }

    @Override
    protected @NotNull PageFileBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageFileBinding.inflate(inflater);
    }

//    @Override
//    public boolean onBackPressed() {
//        return this.connected.get() && this.partList.popFileList();
//    }

    @Override
    public @NotNull String toString() {
        return "FragmentFile{" +
                "partConnect=" + this.partConnect +
                ", partOptions=" + this.partOptions +
                ", partList=" + this.partList +
                ", super=" + super.toString() +
                '}';
    }
}
