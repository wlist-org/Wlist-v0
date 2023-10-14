package com.xuxiaocheng.WListClientAndroid.UIs;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.Utils.ViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

class ActivityFileChooserViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<ConstraintLayout, File> {
    protected final @NotNull BiConsumer<@NotNull File, @NotNull Boolean> choose;
    protected final @NotNull AtomicBoolean chose = new AtomicBoolean(false);
    protected final @NotNull ImageView image;
    protected final @NotNull TextView name;
    protected final @NotNull ImageView chooser;

    protected ActivityFileChooserViewHolder(final @NotNull ConstraintLayout cell, final @NotNull BiConsumer<@NotNull File, @NotNull Boolean> choose) {
        super(cell);
        this.choose = choose;
        this.image = (ImageView) cell.getViewById(R.id.activity_file_chooser_cell_image);
        this.name = (TextView) cell.getViewById(R.id.activity_file_chooser_cell_name);
        this.chooser = (ImageView) cell.getViewById(R.id.activity_file_chooser_cell_chooser);
    }

    @Override
    public void onBind(final @NotNull File file) {
        ViewUtil.setFileImage(this.image, file.isDirectory(), file.getName());
        this.name.setText(file.getName());
        if (file.isDirectory()) {
            this.chooser.setVisibility(View.GONE);
            this.chooser.setOnClickListener(null);
        } else {
            this.chooser.setVisibility(View.VISIBLE);
            this.chooser.setOnClickListener(v -> {
                if (this.chose.get()) {
                    this.chose.set(false);
                    this.choose.accept(file, false);
                } else {
                    this.chose.set(true);
                    this.choose.accept(file, true);
                }
            });
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityFileChooserViewHolder that)) return false;
        return this.choose.equals(that.choose) && this.name.equals(that.name) && this.chooser.equals(that.chooser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.choose, this.name, this.chooser);
    }

    @Override
    public @NotNull String toString() {
        return "ActivityFileChooser$ViewHolder{" +
                "choose=" + this.choose +
                ", name=" + this.name +
                ", chooser=" + this.chooser +
                '}';
    }
}
