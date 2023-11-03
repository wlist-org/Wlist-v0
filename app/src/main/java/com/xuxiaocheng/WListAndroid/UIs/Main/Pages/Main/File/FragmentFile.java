package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.view.View;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.PageMainAdapter;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.SPageMainFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FragmentFile extends SPageMainFragment<PageFileBinding> {
    final @NotNull PartConnect partConnect = new PartConnect(this);
//    protected final @NotNull PartOptions partOptions = new PartOptions(this);
    final @NotNull PartList partList = new PartList(this);
//    protected final @NotNull PartOperation partOperation = new PartOperation(this);
//    protected final @NotNull PartUpload partUpload = new PartUpload(this);
//    protected final @NotNull PartPreview partPreview = new PartPreview(this);

    public FragmentFile() {
        super();
        this.parts().add(this.partConnect);
//        this.parts.add(this.partOptions);
        this.parts().add(this.partList);
//        this.parts.add(this.partOperation);
//        this.parts.add(this.partUpload);
//        this.parts.add(this.partPreview);
//        this.parts.add(this.pageTask);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull List<SFragmentFilePart> parts() {
        return (List<SFragmentFilePart>) super.parts();
    }

    @Override
    protected boolean iOnBackPressed() {
//        if (this.currentFragmentTypes() != PageMainAdapter.Types.File)
            return super.iOnBackPressed();
//        return this.isConnected() && this.partList.popFileList();
    }

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
