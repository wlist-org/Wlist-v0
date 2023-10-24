package com.xuxiaocheng.WListAndroid.UIs;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.UiThread;
import com.xuxiaocheng.WListAndroid.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public final class ActivityMainChooser {
    public static final class ButtonGroup {
        private final @NotNull ImageView button;
        private final @Nullable TextView text;
        private final @NotNull Collection<@NotNull View> layouts = new ArrayList<>();

        @DrawableRes private final int image;
        @DrawableRes private final int imageChose;
        @ColorInt private final int color;
        @ColorInt private final int colorChose;

        public ButtonGroup(final @NotNull Activity activity, final @NotNull ImageView button, @DrawableRes final int image, @DrawableRes final int imageChose,
                           final @Nullable TextView text, final @NotNull View @NotNull ... layouts) {
            super();
            this.button = button;
            this.text = text;
            this.image = image;
            this.imageChose = imageChose;
            this.color = activity.getResources().getColor(R.color.text_normal, activity.getTheme());
            this.colorChose = activity.getResources().getColor(R.color.text_warning, activity.getTheme());
            this.layouts.add(button);
            if (this.text != null)
                this.layouts.add(this.text);
            this.layouts.addAll(Arrays.asList(layouts));
        }

        public void setOnClickListener(final View.@Nullable OnClickListener listener) {
            this.button.setOnClickListener(listener);
            if (this.text != null)
                this.text.setOnClickListener(listener);
            this.layouts.forEach(v -> v.setOnClickListener(listener));
        }

        public boolean callOnClick() {
            return this.button.callOnClick();
        }

        @UiThread
        public void setClickable(final boolean clickable) {
            if (this.button.isClickable() == clickable)
                return;
            this.button.setClickable(clickable);
            this.button.setImageResource(clickable ? this.image : this.imageChose);
            if (this.text != null) {
                this.text.setClickable(clickable);
                this.text.setTextColor(clickable ? this.color : this.colorChose);
            }
            this.layouts.forEach(v -> v.setClickable(clickable));
        }

        public boolean isClicked() {
            return !this.button.isClickable();
        }

        @Override
        public @NotNull String toString() {
            return "ActivityMainChooser$ButtonGroup{" +
                    "button=" + this.button +
                    ", text=" + this.text +
                    ", layouts=" + this.layouts +
                    '}';
        }
    }

    @FunctionalInterface
    public interface MainPage {
        @UiThread
        @NotNull View onShow();
        @UiThread
        default boolean onBackPressed() {
            return false;
        }
        @UiThread
        default void onActivityCreateHook() {
        }
        @UiThread
        default void onHide() {
        }
    }

    private final @NotNull ButtonGroup fileButton;
    private final @NotNull ButtonGroup userButton;
    private final @NotNull ButtonGroup transButton;

    public enum MainChoice {
        File,
        User,
        Trans,
    }

    public ActivityMainChooser(final @NotNull ButtonGroup fileButton, final @NotNull ButtonGroup userButton, final @NotNull ButtonGroup transButton) {
        super();
        this.fileButton = fileButton;
        this.userButton = userButton;
        this.transButton = transButton;
    }

    public void setOnChangeListener(final @NotNull Consumer<? super MainChoice> onChangeListener) {
        this.fileButton.setOnClickListener(v -> {
            if (this.fileButton.isClicked()) return;
            this.fileButton.setClickable(false);
            this.userButton.setClickable(true);
            this.transButton.setClickable(true);
            onChangeListener.accept(MainChoice.File);
        });
        this.userButton.setOnClickListener(v -> {
            if (this.userButton.isClicked()) return;
            this.fileButton.setClickable(true);
            this.userButton.setClickable(false);
            this.transButton.setClickable(true);
            onChangeListener.accept(MainChoice.User);
        });
        this.transButton.setOnClickListener(v -> {
            if (this.transButton.isClicked()) return;
            this.fileButton.setClickable(true);
            this.userButton.setClickable(true);
            this.transButton.setClickable(false);
            onChangeListener.accept(MainChoice.Trans);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean click(final ActivityMainChooser.@NotNull MainChoice choice) {
        return (switch (choice) {
            case File -> this.fileButton;
            case User -> this.userButton;
            case Trans -> this.transButton;
        }).callOnClick();
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMainChooser{" +
                "fileButton=" + this.fileButton +
                ", userButton=" + this.userButton +
                ", transButton=" + this.transButton +
                ", super=" + super.toString() +
                '}';
    }
}
