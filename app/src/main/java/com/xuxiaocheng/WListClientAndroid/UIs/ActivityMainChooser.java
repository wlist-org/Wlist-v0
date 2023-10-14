package com.xuxiaocheng.WListClientAndroid.UIs;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class ActivityMainChooser {
    public static final class ButtonGroup {
        private final @NotNull View layout;
        private final @NotNull ImageView button;
        private final @NotNull TextView text;

        @DrawableRes private final int image;
        @DrawableRes private final int imageChose;
        @ColorInt private final int color;
        @ColorInt private final int colorChose;

        public ButtonGroup(final @NotNull Activity activity, @IdRes final int layout, @IdRes final int button, @IdRes final int text,
                           @DrawableRes final int image, @DrawableRes final int imageChose, @ColorRes final int color, @ColorRes final int colorChose) {
            super();
            this.layout = activity.findViewById(layout);
            this.button = activity.findViewById(button);
            this.text = activity.findViewById(text);
            this.image = image;
            this.imageChose = imageChose;
            this.color = activity.getResources().getColor(color, activity.getTheme());
            this.colorChose = activity.getResources().getColor(colorChose, activity.getTheme());
        }

        public void setOnClickListener(final View.@Nullable OnClickListener listener) {
            this.layout.setOnClickListener(listener);
            this.button.setOnClickListener(listener);
            this.text.setOnClickListener(listener);
        }

        public boolean callOnClick() {
            return this.button.callOnClick();
        }

        public void setClicked(final boolean clicked) {
            if (this.button.isClickable() != clicked)
                return;
            this.layout.setClickable(!clicked);
            this.button.setClickable(!clicked);
            this.text.setClickable(!clicked);
            this.button.setImageResource(clicked ? this.imageChose : this.image);
            this.text.setTextColor(clicked ? this.colorChose : this.color);
        }

        public boolean isClicked() {
            return !this.button.isClickable();
        }

        @Override
        public @NotNull String toString() {
            return "ActivityMainChooser$ButtonGroup{" +
                    "layout=" + this.layout +
                    ", button=" + this.button +
                    ", text=" + this.text +
                    '}';
        }
    }

    public interface MainPage {
        @NotNull View onShow();
        boolean onBackPressed();
        default void onActivityCreateHook() {
        }
    }

    private final @NotNull ButtonGroup fileButton;
    private final @NotNull ButtonGroup userButton;

    public enum MainChoice {
        File,
        User,
    }

    public ActivityMainChooser(final @NotNull ButtonGroup fileButton, final @NotNull ButtonGroup userButton) {
        super();
        this.fileButton = fileButton;
        this.userButton = userButton;
    }

    public void setOnChangeListener(final @NotNull Consumer<? super MainChoice> onChangeListener) {
        this.fileButton.setOnClickListener(v -> {
            if (this.fileButton.isClicked()) return;
            this.fileButton.setClicked(true);
            this.userButton.setClicked(false);
            onChangeListener.accept(MainChoice.File);
        });
        this.userButton.setOnClickListener(v -> {
            if (this.userButton.isClicked()) return;
            this.fileButton.setClicked(false);
            this.userButton.setClicked(true);
            onChangeListener.accept(MainChoice.User);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean click(final @NotNull ActivityMainChooser.MainChoice choice) {
        if (choice == MainChoice.File)
            return this.fileButton.callOnClick();
        if (choice == MainChoice.User)
            return this.userButton.callOnClick();
        return false;
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMainChooser{" +
                "fileButton=" + this.fileButton +
                ", userButton=" + this.userButton +
                ", super=" + super.toString() +
                '}';
    }
}
