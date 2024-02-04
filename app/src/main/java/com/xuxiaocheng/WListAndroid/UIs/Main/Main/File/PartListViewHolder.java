package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.content.Context;
import android.view.View;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileCellBinding;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.function.Consumer;

class PartListViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout, VisibleFileInformation> {
    protected final @NotNull Consumer<VisibleFileInformation> clicker;
    protected final @NotNull Consumer<VisibleFileInformation> listener;
    protected final @NotNull PageFileCellBinding cell;
    protected final boolean selectingMode;

    protected PartListViewHolder(final @NotNull PageFileCellBinding cell, @UiThread final @NotNull Consumer<@NotNull VisibleFileInformation> clicker, @UiThread final @NotNull Consumer<@NotNull VisibleFileInformation> operation, final boolean selectingMode) {
        super(cell.getRoot());
        this.clicker = clicker;
        this.listener = operation;
        this.cell = cell;
        this.selectingMode = selectingMode;
    }

    @Override
    public void onBind(final @NotNull VisibleFileInformation information) {
        this.cell.getRoot().setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
        ViewUtil.setFileImage(this.cell.pageFileCellImage, FileInformationGetter.isDirectory(information), FileInformationGetter.name(information));
        this.cell.pageFileCellName.setText(FileInformationGetter.name(information));
        final Context context = this.cell.getRoot().getContext();
        final String time = ViewUtil.formatTime(FileInformationGetter.updateTime(information), context.getString(R.string.unknown));
        final String size = ViewUtil.formatSize(FileInformationGetter.size(information), context.getString(R.string.unknown));
        this.cell.pageFileCellTips.setText(MessageFormat.format(context.getString(R.string.page_file_tips), time, size));
        this.cell.pageFileCellOperation.setOnClickListener(v -> this.listener.accept(information));
        this.cell.pageFileCellOperation.setVisibility(this.selectingMode ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public @NotNull String toString() {
        return super.toString();
    }
}
