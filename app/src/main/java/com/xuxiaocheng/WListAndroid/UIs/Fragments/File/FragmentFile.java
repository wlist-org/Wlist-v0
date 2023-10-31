package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.view.LayoutInflater;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.File.Task.PageTask;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.IFragment;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMainAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

public class FragmentFile extends IFragment<PageFileBinding, FragmentFile> {
    protected final @NotNull PartConnect partConnect = new PartConnect(this);
    protected final @NotNull PartOptions partOptions = new PartOptions(this);
    protected final @NotNull PartList partList = new PartList(this);
    protected final @NotNull PartOperation partOperation = new PartOperation(this);
    protected final @NotNull PartUpload partUpload = new PartUpload(this);
    protected final @NotNull PartPreview partPreview = new PartPreview(this);
    protected final @NotNull PageTask pageTask = new PageTask(this);

    public FragmentFile() {
        super();
        this.parts.add(this.partConnect);
        this.parts.add(this.partOptions);
        this.parts.add(this.partList);
        this.parts.add(this.partOperation);
        this.parts.add(this.partUpload);
        this.parts.add(this.partPreview);
        this.parts.add(this.pageTask);
    }

    @Override
    protected @NotNull PageFileBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageFileBinding.inflate(inflater);
    }

    @Override
    public boolean onBackPressed(final @NotNull ActivityMain activity) {
        if (activity.currentChoice() != ActivityMainAdapter.FragmentTypes.File)
            return false;
        return this.connected.get() && this.partList.popFileList();
    }

    @Override
    public @NotNull String toString() {
        return "FragmentFile{" +
                "partConnect=" + this.partConnect +
                ", partOptions=" + this.partOptions +
                ", partList=" + this.partList +
                ", partOperation=" + this.partOperation +
                ", partUpload=" + this.partUpload +
                ", partPreview=" + this.partPreview +
                ", super=" + super.toString() +
                '}';
    }
}
