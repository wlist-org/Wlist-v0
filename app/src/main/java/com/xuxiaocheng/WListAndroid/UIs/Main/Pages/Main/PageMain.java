package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AnyThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.UiThread;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.CPage;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PageMain extends CPage<ActivityMainBinding> {
    @Override
    protected @NotNull ActivityMainBinding iOnInflater() {
        return ActivityMainBinding.inflate(this.activity().getLayoutInflater());
    }

    protected final @NotNull AtomicReference<PageMainAdapter.Types> currentType = new AtomicReference<>();

    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("w:page_main:current_type"))
            this.currentType.set(PageMainAdapter.Types.fromPosition(savedInstanceState.getInt("w:page_main:current_type", PageMainAdapter.Types.toPosition(PageMainAdapter.Types.File))));
        else
            this.currentType.set(PageMainAdapter.Types.File);
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        outState.putInt("w:page_main:current_type", PageMainAdapter.Types.toPosition(this.currentType.get()));
    }

    @Override
    protected void iOnBuildPage(final @NotNull ActivityMainBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        final ChooserButtonGroup fileButton = new ChooserButtonGroup(page.getRoot().getContext(), page.activityMainChooserFile, page.activityMainChooserFileText, page.activityMainChooserFileImage, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose);
        final ChooserButtonGroup userButton = new ChooserButtonGroup(page.getRoot().getContext(), page.activityMainChooserUser, page.activityMainChooserUserText, page.activityMainChooserUserImage, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose);
        page.activityMainChooserFile.setOnClickListener(v -> page.activityMainContent.setCurrentItem(PageMainAdapter.Types.toPosition(PageMainAdapter.Types.File)));
        page.activityMainChooserUser.setOnClickListener(v -> page.activityMainContent.setCurrentItem(PageMainAdapter.Types.toPosition(PageMainAdapter.Types.User)));
        page.activityMainContent.setAdapter(new PageMainAdapter(this));
        page.activityMainContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final PageMainAdapter.Types current = PageMainAdapter.Types.fromPosition(position);
                PageMain.this.currentType.set(current);
                switch (current) {
                    case File -> {
                        if (fileButton.isClicked()) return;
                        fileButton.click();
                        userButton.release();
                    }
                    case User -> {
                        if (userButton.isClicked()) return;
                        userButton.click();
                        fileButton.release();
                    }
                }
                PageMain.this.existingFragments().forEach(f -> f.sOnTypeChanged(current));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.content().activityMainContent.setCurrentItem(PageMainAdapter.Types.toPosition(this.currentType.get()), false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<? extends @NotNull SPageMainFragment<?>> existingFragments() {
        return (List<SPageMainFragment<?>>) super.existingFragments();
    }

    private static final class ChooserButtonGroup {
        private final @NotNull Context context;
        @DrawableRes private final int image;
        @DrawableRes private final int imageChose;
        private final @NotNull View layout;
        private final @NotNull ImageView button;
        private final @NotNull TextView text;
        private final @NotNull AtomicBoolean clicked = new AtomicBoolean(false);

        private ChooserButtonGroup(final @NotNull Context context, final @NotNull View layout, final @NotNull TextView text,
                                   final @NotNull ImageView button, @DrawableRes final int image, @DrawableRes final int imageChose) {
            super();
            this.context = context;
            this.image = image;
            this.imageChose = imageChose;
            this.layout = layout;
            this.button = button;
            this.text = text;
        }

        @UiThread
        private void click() {
            if (!this.clicked.compareAndSet(false, true)) return;
            this.layout.setClickable(false);
            this.button.setImageResource(this.imageChose);
            this.text.setTextColor(this.context.getColor(R.color.text_chose));
        }

        @UiThread
        private void release() {
            if (!this.clicked.compareAndSet(true, false)) return;
            this.layout.setClickable(true);
            this.button.setImageResource(this.image);
            this.text.setTextColor(this.context.getColor(R.color.text_normal));
        }

        @AnyThread
        private boolean isClicked() {
            return this.clicked.get();
        }

        @Override
        public @NotNull String toString() {
            return "PageMain$ChooserButtonGroup{" +
                    "clicked=" + this.clicked +
                    '}';
        }
    }

    @Override
    public @NotNull String toString() {
        return "PageMain{" +
                "currentType=" + this.currentType +
                ", super=" + super.toString() +
                '}';
    }
}
