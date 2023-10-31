package com.xuxiaocheng.WListAndroid.UIs.Fragments.File.Task;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
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
                switch (current) {
                    case Download -> {
                        if (download.isClicked()) break;
                        download.setClickable(false);
                        upload.setClickable(true);
                        trash.setClickable(true);
                        copy.setClickable(true);
                        move.setClickable(true);
                        rename.setClickable(true);
                    }
                    case Upload -> {
                        if (upload.isClicked()) break;
                        download.setClickable(true);
                        upload.setClickable(false);
                        trash.setClickable(true);
                        copy.setClickable(true);
                        move.setClickable(true);
                        rename.setClickable(true);
                    }
                    case Trash -> {
                        if (trash.isClicked()) break;
                        download.setClickable(true);
                        upload.setClickable(true);
                        trash.setClickable(false);
                        copy.setClickable(true);
                        move.setClickable(true);
                        rename.setClickable(true);
                    }
                    case Copy -> {
                        if (copy.isClicked()) break;
                        download.setClickable(true);
                        upload.setClickable(true);
                        trash.setClickable(true);
                        copy.setClickable(false);
                        move.setClickable(true);
                        rename.setClickable(true);
                    }
                    case Move -> {
                        if (move.isClicked()) break;
                        download.setClickable(true);
                        upload.setClickable(true);
                        trash.setClickable(true);
                        copy.setClickable(true);
                        move.setClickable(false);
                        rename.setClickable(true);
                    }
                    case Rename -> {
                        if (rename.isClicked()) break;
                        download.setClickable(true);
                        upload.setClickable(true);
                        trash.setClickable(true);
                        copy.setClickable(true);
                        move.setClickable(true);
                        rename.setClickable(false);
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
