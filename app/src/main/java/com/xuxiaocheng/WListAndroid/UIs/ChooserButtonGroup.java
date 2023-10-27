package com.xuxiaocheng.WListAndroid.UIs;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.UiThread;
import com.xuxiaocheng.WListAndroid.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class ChooserButtonGroup {
    private final @NotNull Activity activity;
    private final @NotNull ImageView button;
    private final @Nullable TextView text;
    private final @NotNull Collection<@NotNull View> layouts = new ArrayList<>();

    @DrawableRes
    private final int image;
    @DrawableRes
    private final int imageChose;

    public ChooserButtonGroup(final @NotNull ActivityMain activity, final FragmentsAdapter.@NotNull FragmentTypes type, final @NotNull ImageView button,
                              @DrawableRes final int image, @DrawableRes final int imageChose,
                              final @Nullable TextView text, final @NotNull View @NotNull ... layouts) {
        super();
        this.activity = activity;
        this.button = button;
        this.text = text;
        this.image = image;
        this.imageChose = imageChose;
        this.layouts.add(button);
        if (this.text != null)
            this.layouts.add(this.text);
        this.layouts.addAll(Arrays.asList(layouts));
        this.layouts.forEach(v -> v.setOnClickListener(u -> this.button.performClick()));
        this.button.setOnClickListener(v -> activity.getContent().activityMainContent.setCurrentItem(FragmentsAdapter.FragmentTypes.toPosition(type)));
    }

    @UiThread
    public void setClickable(final boolean clickable) {
        if (this.button.isClickable() == clickable)
            return;
        this.button.setClickable(clickable);
        this.button.setImageResource(clickable ? this.image : this.imageChose);
        if (this.text != null) {
            this.text.setClickable(clickable);
            this.text.setTextColor(this.activity.getResources().getColor(clickable ? R.color.text_normal : R.color.text_warning, this.activity.getTheme()));
        }
        this.layouts.forEach(v -> v.setClickable(clickable));
    }

    public boolean isClicked() {
        return !this.button.isClickable();
    }

    @Override
    public @NotNull String toString() {
        return "ButtonGroup{" +
                "button=" + this.button +
                ", text=" + this.text +
                ", layouts=" + this.layouts +
                ", image=" + this.image +
                ", imageChose=" + this.imageChose +
                '}';
    }
}
