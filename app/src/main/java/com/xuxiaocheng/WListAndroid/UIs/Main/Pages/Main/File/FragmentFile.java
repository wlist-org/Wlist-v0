package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.PageMainAdapter;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.SPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FragmentFile extends SPageMainFragment<PageFileBinding> {
    private final @NotNull PartConnect partConnect = new PartConnect(this);
    private final @NotNull PartOptions partOptions = new PartOptions(this);
    private final @NotNull PartList partList = new PartList(this);
    private final @NotNull PartOperation partOperation = new PartOperation(this);
    private final @NotNull PartUpload partUpload = new PartUpload(this);
    private final @NotNull PartPreview partPreview = new PartPreview(this);
    private final @NotNull PartTask partTask = new PartTask(this);

    public FragmentFile() {
        super();
        this.parts().add(this.partConnect);
        this.parts().add(this.partOptions);
        this.parts().add(this.partList);
        this.parts().add(this.partOperation);
        this.parts().add(this.partUpload);
        this.parts().add(this.partPreview);
        this.parts().add(this.partTask);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull List<SFragmentFilePart> parts() {
        return (List<SFragmentFilePart>) super.parts();
    }

    @Override
    protected boolean iOnBackPressed() {
        if (this.currentFragmentTypes() != PageMainAdapter.Types.File)
            return false;
        return this.isConnected() && this.partList.popFileList();
    }

    @Override
    protected @NotNull PageFileBinding iOnInflater() {
        return PageFileBinding.inflate(this.activity().getLayoutInflater());
    }

    @NotNull PartConnect partConnect() {
        return this.partConnect;
    }

    @NotNull PartOptions partOptions() {
        return this.partOptions;
    }

    @NotNull PartList partList() {
        return this.partList;
    }

    @NotNull PartOperation partOperation() {
        return this.partOperation;
    }

    @NotNull PartUpload partUpload() {
        return this.partUpload;
    }

    @NotNull PartPreview partPreview() {
        return this.partPreview;
    }

    @NotNull PartTask partTask() {
        return this.partTask;
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
