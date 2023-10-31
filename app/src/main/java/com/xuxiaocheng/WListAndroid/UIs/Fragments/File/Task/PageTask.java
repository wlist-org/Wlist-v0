package com.xuxiaocheng.WListAndroid.UIs.Fragments.File.Task;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.UiThread;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.File.FragmentFile;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.IPage;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskBinding;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class PageTask extends IPage<PageTaskBinding, PageFileBinding, FragmentFile> {
    public PageTask(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void onBuild(final @NotNull PageFileBinding fragment) {
        super.onBuild(fragment);
        this.activity().getContent().activityMainTasks.setOnClickListener(v -> this.start(this.activity()));
    }

    protected final @NotNull HInitializer<PageTaskAdapter> pageTaskAdapter = new HInitializer<>("PageTaskAdapter");
    protected final @NotNull AtomicReference<PageTaskAdapter.TaskTypes> currentChoice = new AtomicReference<>(PageTaskAdapter.TaskTypes.Download);
    public PageTaskAdapter.@NotNull TaskTypes currentChoice() {
        return this.currentChoice.get();
    }

    @Override
    protected void onAttach() {
        this.pageTaskAdapter.initialize(new PageTaskAdapter(this.fragment));
        super.onAttach();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        this.pageTaskAdapter.uninitializeNullable();
    }

    @Override
    protected @NotNull PageTaskBinding inflate(final @NotNull LayoutInflater inflater) {
        return PageTaskBinding.inflate(inflater);
    }

    @Override
    protected void onBuildPage(final @NotNull PageTaskBinding page) {
        super.onBuildPage(page);
        page.pageTaskBacker.setOnClickListener(v -> this.activity().onBackPressed());
        page.pageTaskPager.setAdapter(this.pageTaskAdapter.getInstance());
        final ChooserButtonGroup download = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Download, page.pageTaskChooserDownload, page.pageTaskPager);
        final ChooserButtonGroup upload = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Upload, page.pageTaskChooserUpload, page.pageTaskPager);
        final ChooserButtonGroup trash = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Trash, page.pageTaskChooserTrash, page.pageTaskPager);
        final ChooserButtonGroup copy = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Copy, page.pageTaskChooserCopy, page.pageTaskPager);
        final ChooserButtonGroup move = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Move, page.pageTaskChooserMove, page.pageTaskPager);
        final ChooserButtonGroup rename = new ChooserButtonGroup(PageTaskAdapter.TaskTypes.Rename, page.pageTaskChooserRename, page.pageTaskPager);
        page.pageTaskPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final PageTaskAdapter.TaskTypes current = PageTaskAdapter.TaskTypes.fromPosition(position);
                PageTask.this.currentChoice.set(current);
                final ChooserButtonGroup currentGroup = switch (current) {
                    case Download -> download;
                    case Upload -> upload;
                    case Trash -> trash;
                    case Copy -> copy;
                    case Move -> move;
                    case Rename -> rename;
                };
                if (currentGroup.isClicked()) return;
                download.setClickable(true);
                upload.setClickable(true);
                trash.setClickable(true);
                copy.setClickable(true);
                move.setClickable(true);
                rename.setClickable(true);
                currentGroup.setClickable(false);
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                final PageTaskAdapter.TaskTypes type = PageTaskAdapter.TaskTypes.fromPosition(position);
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
                        page.pageTaskChooserHintBottom.setVisibility(View.GONE);
                        page.pageTaskChooserHintTop.setX(current.getX() + positionOffset * page.pageTaskChooserHintTop.getWidth() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintTop.getLayoutParams()).leftMargin);
                    }
                    case Copy, Move, Rename -> {
                        page.pageTaskChooserHintTop.setVisibility(View.GONE);
                        page.pageTaskChooserHintBottom.setVisibility(View.VISIBLE);
                        page.pageTaskChooserHintBottom.setX(current.getX() + positionOffset * page.pageTaskChooserHintBottom.getWidth() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintBottom.getLayoutParams()).leftMargin);
                    }
                    case Trash -> {
                        page.pageTaskChooserHintTop.setVisibility(View.VISIBLE);
                        page.pageTaskChooserHintBottom.setVisibility(positionOffset == 0 ? View.GONE : View.VISIBLE);
                        page.pageTaskChooserHintTop.setX(current.getX() + ((ViewGroup.MarginLayoutParams) page.pageTaskChooserHintTop.getLayoutParams()).leftMargin);
                        page.pageTaskChooserHintTop.setAlpha(1 - positionOffset);
                        page.pageTaskChooserHintBottom.setAlpha(positionOffset);
                    }
                }
            }
        });
        page.pageTaskPager.setCurrentItem(PageTaskAdapter.TaskTypes.toPosition(this.currentChoice.get()), false);
    }

    @Override
    protected void onConnected(final @NotNull ActivityMain activity) {
        super.onConnected(activity);
        Main.runOnUiThread(activity, () -> activity.getContent().activityMainTasks.setVisibility(View.VISIBLE));
    }

    @Override
    protected void onDisconnected(final @NotNull ActivityMain activity) {
        super.onDisconnected(activity);
        Main.runOnUiThread(activity, () -> activity.getContent().activityMainTasks.setVisibility(View.GONE));
    }

    private static class ChooserButtonGroup {
        private final @NotNull Context context;
        private final @NotNull TextView text;

        protected ChooserButtonGroup(final PageTaskAdapter.@NotNull TaskTypes type, final @NotNull TextView text, final @NotNull ViewPager2 pager) {
            super();
            this.context = pager.getContext();
            this.text = text;
            this.text.setOnClickListener(v -> pager.setCurrentItem(PageTaskAdapter.TaskTypes.toPosition(type)));
        }

        @UiThread
        public void setClickable(final boolean clickable) {
            if (this.text.isClickable() == clickable)
                return;
            this.text.setClickable(clickable);
            this.text.setTextColor(this.context.getResources().getColor(clickable ? R.color.text_normal : R.color.text_warning, this.context.getTheme()));
        }

        public boolean isClicked() {
            return !this.text.isClickable();
        }

        @Override
        public @NotNull String toString() {
            return "ChooserButtonGroup{" +
                    "context=" + this.context +
                    ", text=" + this.text +
                    '}';
        }
    }
}
