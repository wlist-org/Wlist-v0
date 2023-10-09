package com.xuxiaocheng.WListClientAndroid.UIs;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

class PageFileViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout> {
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

    public void onBind(final @NotNull VisibleFileInformation information) {
        this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
        PageFileViewHolder.setFileImage(this.image, information);
        this.name.setText(FileInformationGetter.name(information));
        this.tips.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown").replace('T', ' '));
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

    protected static void setFileImage(final @NotNull ImageView image, final @NotNull VisibleFileInformation information) {
        if (FileInformationGetter.isDirectory(information)) {
            image.setImageResource(R.mipmap.page_file_image_directory);
            return;
        }
        final String name = FileInformationGetter.name(information);
        final int index = name.lastIndexOf('.');
        // TODO: cached Drawable.
        image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT)) {
            case "bat", "cmd", "sh", "run" -> R.mipmap.page_file_image_bat;
            case "doc", "docx" -> R.mipmap.page_file_image_docx;
            case "exe", "bin" -> R.mipmap.page_file_image_exe;
            case "jpg", "jpeg", "png", "bmp", "psd", "tga" -> R.mipmap.page_file_image_jpg;
            case "mp3", "flac", "wav", "wma", "aac", "ape" -> R.mipmap.page_file_image_mp3;
            case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
            case "txt", "log" -> R.mipmap.page_file_image_txt;
            case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
            case "zip", "7z", "rar", "gz", "tar" -> R.mipmap.page_file_image_zip;
            default -> R.mipmap.page_file_image_file;
        });
    }
}
