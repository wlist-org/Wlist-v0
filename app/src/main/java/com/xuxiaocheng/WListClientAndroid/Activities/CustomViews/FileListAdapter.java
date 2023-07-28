package com.xuxiaocheng.WListClientAndroid.Activities.CustomViews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.databinding.FileListCellBinding;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class FileListAdapter extends BaseAdapter {
    @NonNull protected final List<VisibleFileInformation> data;
    @NonNull protected final LayoutInflater inflate;

    public FileListAdapter(@NonNull final List<VisibleFileInformation> data, @NonNull final LayoutInflater inflater) {
        super();
        this.data = data;
        this.inflate = inflater;
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    @Override
    public Object getItem(final int position) {
        return this.data.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    @NonNull public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        CellViewHolder holder = convertView == null ? null : (CellViewHolder) convertView.getTag();
        if (holder == null) {
            holder = new CellViewHolder(FileListCellBinding.inflate(this.inflate).getRoot());
            holder.cell.setTag(holder);
        }
        this.setCellViewContent(position, holder, (ListView) parent);
        return holder.cell;
    }

    public static class CellViewHolder {
        @NonNull protected final ConstraintLayout cell;
        @NonNull protected final ImageView image;
        @NonNull protected final TextView name;
        @NonNull protected final TextView tip;
        @NonNull protected final View option;

        protected CellViewHolder(@NonNull final ConstraintLayout cell) {
            super();
            this.cell = cell;
            this.image = (ImageView) cell.getViewById(R.id.file_list_cell_image);
            this.name = (TextView) cell.getViewById(R.id.file_list_cell_name);
            this.tip = (TextView) cell.getViewById(R.id.file_list_cell_tip);
            this.option = cell.getViewById(R.id.file_list_cell_option);
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
            return "CellViewHolder{" +
                    "cell=" + this.cell +
                    ", image=" + this.image +
                    ", name=" + this.name +
                    ", tip=" + this.tip +
                    ", option=" + this.option +
                    '}';
        }
    }

    @Override
    @NonNull public String toString() {
        return "FileListAdapter{" +
                "data=" + this.data +
                ", inflate=" + this.inflate +
                ", super=" + super.toString() +
                '}';
    }

    protected void setCellViewContent(final int position, @NonNull final CellViewHolder view, @NonNull final ListView parent) {
        final VisibleFileInformation information = this.data.get(position);
        view.image.setImageResource(R.mipmap.app_logo);
        view.name.setText(FileInformationGetter.name(information));
        final LocalDateTime update = FileInformationGetter.updateTime(information);
        view.tip.setText(update == null ? "unknown" : update.format(DateTimeFormatter.ISO_DATE_TIME).replace('T', ' '));
        view.name.setOnClickListener(v -> parent.getOnItemClickListener().onItemClick(parent, v, position, this.getItemId(position)));
    }
}
