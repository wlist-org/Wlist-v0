package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.CellViewHolder> {
    protected final boolean isRoot;
    @NonNull protected final List<VisibleFileInformation> data;
    @NonNull protected final LayoutInflater inflate;
    @NonNull protected final Consumer<VisibleFileInformation> clicker;

    public FileListAdapter(final boolean isRoot, @NonNull final List<VisibleFileInformation> data, @NonNull final LayoutInflater inflater, @NonNull final Consumer<VisibleFileInformation> clicker) {
        super();
        this.isRoot = isRoot;
        this.data = data;
        this.inflate = inflater;
        this.clicker = clicker;
    }

    @Override
    @NonNull public CellViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new CellViewHolder((ConstraintLayout) this.inflate.inflate(R.layout.file_list_cell, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NonNull final CellViewHolder holder, final int position) {
        holder.setItem(this.data.get(position), this.isRoot);
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    @Override
    @NonNull public String toString() {
        return "FileListAdapter{" +
                "isRoot=" + this.isRoot +
                ", data=" + this.data +
                ", inflate=" + this.inflate +
                ", super=" + super.toString() +
                '}';
    }

    protected static class CellViewHolder extends RecyclerView.ViewHolder {
        @NonNull protected final ConstraintLayout cell;
        @NonNull protected final FileListAdapter adapter;
        @NonNull protected final ImageView image;
        @NonNull protected final TextView name;
        @NonNull protected final TextView tip;
        @NonNull protected final View option;

        protected CellViewHolder(@NonNull final ConstraintLayout cell, @NonNull final FileListAdapter adapter) {
            super(cell);
            this.cell = cell;
            this.adapter = adapter;
            this.image = (ImageView) cell.getViewById(R.id.file_list_cell_image);
            this.name = (TextView) cell.getViewById(R.id.file_list_cell_name);
            this.tip = (TextView) cell.getViewById(R.id.file_list_cell_tip);
            this.option = cell.getViewById(R.id.file_list_cell_option);
        }

        public void setItem(@NonNull final VisibleFileInformation information, final boolean isRoot) {
            this.cell.setOnClickListener(v -> this.adapter.clicker.accept(information)); // TODO: select on long click.

            CellViewHolder.setFileImage(this.image, information);
            this.name.setText(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information));
            final LocalDateTime update = FileInformationGetter.updateTime(information);
            this.tip.setText(update == null ? "unknown" : update.format(DateTimeFormatter.ISO_DATE_TIME).replace('T', ' '));

//            this.option.setOnClickListener(v -> {
//                // TODO
//            });
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof CellViewHolder holder)) return false;
            return this.cell.equals(holder.cell) && this.image.equals(holder.image) && this.name.equals(holder.name) && this.tip.equals(holder.tip) && this.option.equals(holder.option);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.cell, this.image, this.name, this.tip, this.option);
        }

        @Override
        @NonNull public String toString() {
            return "FileListAdapter$CellViewHolder{" +
                    "cell=" + this.cell +
                    ", image=" + this.image +
                    ", name=" + this.name +
                    ", tip=" + this.tip +
                    ", option=" + this.option +
                    '}';
        }

        protected static void setFileImage(@NonNull final ImageView image, @NonNull final VisibleFileInformation information) {
            if (FileInformationGetter.isDirectory(information)) {
                image.setImageResource(R.mipmap.page_file_image_directory);
                return;
            }
            final String name = FileInformationGetter.name(information).toLowerCase(Locale.ROOT);
            final int index = name.lastIndexOf('.');
            // TODO: cached Drawable.
            image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1)) {
                case "doc", "docx" -> R.mipmap.page_file_image_docx;
                case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
                case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
                default -> R.mipmap.page_file_image_file;
            });
        }
    }
}
