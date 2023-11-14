package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

class PartListViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout, VisibleFileInformation> {
    protected final @NotNull Consumer<VisibleFileInformation> clicker;
    protected final @NotNull Consumer<VisibleFileInformation> listener;
    protected final @NotNull ImageView image;
    protected final @NotNull TextView name;
    protected final @NotNull TextView tips;
    protected final @NotNull View operation;

    protected PartListViewHolder(final @NotNull ConstraintLayout cell, @UiThread final @NotNull Consumer<@NotNull VisibleFileInformation> clicker, @UiThread final @NotNull Consumer<@NotNull VisibleFileInformation> operation) {
        super(cell);
        this.clicker = clicker;
        this.listener = operation;
        this.image = (ImageView) cell.getViewById(R.id.page_file_cell_image);
        this.name = (TextView) cell.getViewById(R.id.page_file_cell_name);
        this.tips = (TextView) cell.getViewById(R.id.page_file_cell_tips);
        this.operation = cell.getViewById(R.id.page_file_cell_operation);
    }

    @Override
    public void onBind(final @NotNull VisibleFileInformation information) {
        this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
        ViewUtil.setFileImage(this.image, FileInformationGetter.isDirectory(information), FileInformationGetter.name(information));
        this.name.setText(FileInformationGetter.name(information));
        this.tips.setText(ViewUtil.formatTime(FileInformationGetter.updateTime(information), this.image.getContext().getString(R.string.unknown)));
        this.operation.setOnClickListener(v -> this.listener.accept(information));
    }

    @Override
    public @NotNull String toString() {
        return "PartListViewHolder{}";
    }
}
