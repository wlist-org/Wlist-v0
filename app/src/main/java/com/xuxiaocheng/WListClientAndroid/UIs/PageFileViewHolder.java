package com.xuxiaocheng.WListClientAndroid.UIs;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.Utils.ViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

class PageFileViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout, VisibleFileInformation> {
    protected final @NotNull Consumer<VisibleFileInformation> clicker;
    protected final @NotNull Consumer<VisibleFileInformation> listener;
    protected final @NotNull ImageView image;
    protected final @NotNull TextView name;
    protected final @NotNull TextView tips;
    protected final @NotNull View option;

    protected PageFileViewHolder(final @NotNull ConstraintLayout cell, final @NotNull Consumer<@NotNull VisibleFileInformation> clicker, final @NotNull Consumer<@NotNull VisibleFileInformation> option) {
        super(cell);
        this.clicker = clicker;
        this.listener = option;
        this.image = (ImageView) cell.getViewById(R.id.page_file_cell_image);
        this.name = (TextView) cell.getViewById(R.id.page_file_cell_name);
        this.tips = (TextView) cell.getViewById(R.id.page_file_cell_tips);
        this.option = cell.getViewById(R.id.page_file_cell_option);
    }

    @Override
    public void onBind(final @NotNull VisibleFileInformation information) {
        this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
        ViewUtil.setFileImage(this.image, FileInformationGetter.isDirectory(information), FileInformationGetter.name(information));
        this.name.setText(FileInformationGetter.name(information));
        this.tips.setText(Objects.requireNonNullElse(ViewUtil.format(FileInformationGetter.updateTime(information)), this.image.getContext().getString(R.string.unknown)));
        this.option.setOnClickListener(v -> this.listener.accept(information));
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof PageFileViewHolder that)) return false;
        return this.image.equals(that.image) && this.name.equals(that.name) && this.tips.equals(that.tips) && this.option.equals(that.option);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.image, this.name, this.tips, this.option);
    }

    @Override
    public @NotNull String toString() {
        return "PageFile$CellViewHolder{" +
                "clicker=" + this.clicker +
                ", image=" + this.image +
                ", name=" + this.name +
                ", tips=" + this.tips +
                ", option=" + this.option +
                '}';
    }
}
