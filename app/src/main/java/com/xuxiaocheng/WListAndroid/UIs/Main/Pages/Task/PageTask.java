package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.CPage;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PageTask extends CPage<PageTaskBinding> {
    @Override
    protected @NotNull PageTaskBinding iOnInflater() {
        return PageTaskBinding.inflate(this.activity().getLayoutInflater());
    }

    protected final @NotNull AtomicReference<PageTaskAdapter.Types> currentType = new AtomicReference<>();
    protected PageTaskAdapter.@NotNull Types currentTypes() {
        return this.currentType.get();
    }

    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        final int type = savedInstanceState != null ? savedInstanceState.getInt("w:page_task:current_type", -1) : -1;
        this.currentType.set(type == -1 ? this.getSuggestedChoice() : PageTaskAdapter.Types.fromPosition(type));
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        outState.putInt("w:page_task:current_type", PageTaskAdapter.Types.toPosition(this.currentType.get()));
    }

    private PageTaskAdapter.@NotNull Types getSuggestedChoice() {
        return PageTaskAdapter.Types.Download; // TODO
    }

    @Override
    protected void iOnBuildPage(final @NotNull PageTaskBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        page.pageTaskBacker.setOnClickListener(v -> this.activity().onBackPressed());
        final ChooserButtonGroup download = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserDownload);
        final ChooserButtonGroup upload = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserUpload);
        final ChooserButtonGroup trash = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserTrash);
        final ChooserButtonGroup copy = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserCopy);
        final ChooserButtonGroup move = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserMove);
        final ChooserButtonGroup rename = new ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskChooserRename);
        page.pageTaskChooserDownload.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Download)));
        page.pageTaskChooserUpload.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Upload)));
        page.pageTaskChooserTrash.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Trash)));
        page.pageTaskChooserCopy.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Copy)));
        page.pageTaskChooserMove.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Move)));
        page.pageTaskChooserRename.setOnClickListener(v -> page.pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(PageTaskAdapter.Types.Rename)));
        page.pageTaskPager.setAdapter(new PageTaskAdapter(this));
        page.pageTaskPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final PageTaskAdapter.Types current = PageTaskAdapter.Types.fromPosition(position);
                PageTask.this.currentType.set(current);
                final ChooserButtonGroup currentGroup = switch (current) {
                    case Download -> download;
                    case Upload -> upload;
                    case Trash -> trash;
                    case Copy -> copy;
                    case Move -> move;
                    case Rename -> rename;
                };
                if (currentGroup.isClicked()) return;
                download.release();
                upload.release();
                trash.release();
                copy.release();
                move.release();
                rename.release();
                currentGroup.click();
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                final PageTaskAdapter.Types type = PageTaskAdapter.Types.fromPosition(position);
                final View current = switch (type) {
                    case Download -> page.pageTaskChooserDownload;
                    case Upload -> page.pageTaskChooserUpload;
                    case Trash -> page.pageTaskChooserTrash;
                    case Copy -> page.pageTaskChooserCopy;
                    case Move -> page.pageTaskChooserMove;
                    case Rename -> page.pageTaskChooserRename;
                };
                switch (type) {
                    case Download, Upload -> {
                        page.pageTaskChooserHintTop.setVisibility(View.VISIBLE);
                        page.pageTaskChooserHintTop.setAlpha(1);
                        page.pageTaskChooserHintBottom.setVisibility(View.GONE);
                        page.pageTaskChooserHintBottom.setAlpha(0);
                        page.pageTaskChooserHintTop.setX(current.getX() + positionOffset * page.pageTaskChooserHintTop.getWidth() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintTop.getLayoutParams()).leftMargin);
                    }
                    case Trash -> {
                        page.pageTaskChooserHintTop.setVisibility(View.VISIBLE);
                        page.pageTaskChooserHintBottom.setVisibility(positionOffset == 0 ? View.GONE : View.VISIBLE);
                        page.pageTaskChooserHintTop.setX(current.getX() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintTop.getLayoutParams()).leftMargin);
                        page.pageTaskChooserHintTop.setAlpha(1 - positionOffset);
                        page.pageTaskChooserHintBottom.setAlpha(positionOffset);
                    }
                    case Copy, Move, Rename -> {
                        page.pageTaskChooserHintTop.setVisibility(View.GONE);
                        page.pageTaskChooserHintTop.setAlpha(0);
                        page.pageTaskChooserHintBottom.setVisibility(View.VISIBLE);
                        page.pageTaskChooserHintBottom.setAlpha(1);
                        page.pageTaskChooserHintBottom.setX(current.getX() + positionOffset * page.pageTaskChooserHintBottom.getWidth() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintBottom.getLayoutParams()).leftMargin);
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.content().pageTaskPager.setCurrentItem(PageTaskAdapter.Types.toPosition(this.currentType.get()), false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<? extends @NotNull SPageTaskFragment> existingFragments() {
        return (List<SPageTaskFragment>) super.existingFragments();
    }

    private static final class ChooserButtonGroup {
        private final @NotNull Context context;
        private final @NotNull TextView text;
        private final @NotNull AtomicBoolean clicked = new AtomicBoolean(false);

        private ChooserButtonGroup(final @NotNull Context context, final @NotNull TextView text) {
            super();
            this.context = context;
            this.text = text;
        }

        @UiThread
        private void click() {
            if (!this.clicked.compareAndSet(false, true)) return;
            this.text.setTextColor(this.context.getColor(R.color.text_chose));
        }

        @UiThread
        private void release() {
            if (!this.clicked.compareAndSet(true, false)) return;
            this.text.setTextColor(this.context.getColor(R.color.text_normal));
        }

        @AnyThread
        private boolean isClicked() {
            return this.clicked.get();
        }

        @Override
        public @NotNull String toString() {
            return "PageTask{" +
                    "clicked=" + this.clicked +
                    '}';
        }
    }
}
