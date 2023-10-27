package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SimpleAdapter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.FragmentsAdapter;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileUploadBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PageFile extends IFragment<PageFileBinding> {
    protected final @NotNull PageFilePartList partList = new PageFilePartList(this);
    protected final @NotNull PageFilePartOptions partOptions = new PageFilePartOptions(this);
    protected final @NotNull PageFilePartOperation partOperation = new PageFilePartOperation(this);
    protected final @NotNull PageFilePartPreview partPreview = new PageFilePartPreview(this);
    protected final @NotNull PageFilePartUpload partUpload = new PageFilePartUpload(this);

    public PageFile(final @NotNull ActivityMain activity) {
        super(activity);
    }

    protected @NotNull InetSocketAddress address() {
        return this.mainActivity.address();
    }

    protected @NotNull String username() {
        return this.mainActivity.username();
    }


    @Override
    public void onShow() {
        this.mainActivity.getContent().activityMainOptions.setVisibility(View.VISIBLE);
    }

    @Override
    public void onHide() {
        this.mainActivity.getContent().activityMainOptions.setVisibility(View.GONE);
    }

    @Override
    protected @NotNull PageFileBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageFileBinding.inflate(inflater);
    }

    @Override
    public void onBuild(final @NotNull PageFileBinding page) {
        page.pageFileList.setLayoutManager(new LinearLayoutManager(this.mainActivity));
        page.pageFileList.setHasFixedSize(true);
        this.partList.onRootPage(0);
        this.buildUploader();
    }


    private final @NotNull HInitializer<ActivityResultLauncher<String>> chooserLauncher = new HInitializer<>("PageFileChooserLauncher");

    @SuppressLint("ClickableViewAccessibility")
    private void buildUploader() {
        final PageFileBinding page = this.pageCache.getInstance();
        final AtomicBoolean scrolling = new AtomicBoolean();
        final AtomicInteger startX = new AtomicInteger(), startY = new AtomicInteger();
        final AtomicReference<ZonedDateTime> startTime = new AtomicReference<>();
        page.pageFileUploader.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    scrolling.set(false);
                    startX.set(Float.floatToIntBits(v.getX()));
                    startY.set(Float.floatToIntBits(v.getY()));
                    startTime.set(ZonedDateTime.now());
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (scrolling.get()) {
                        final float parentX = page.pageFileList.getX(), parentY = page.pageFileList.getY();
                        final float halfWidth = v.getWidth() / 2.0f, halfHeight = v.getHeight() / 2.0f;
                        v.setX(HMathHelper.clamp(v.getX() + e.getX() - parentX, halfWidth, page.pageFileList.getWidth() - halfWidth) + parentX - halfWidth);
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, halfHeight, page.pageFileList.getHeight() - halfHeight) + parentY- halfHeight);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f
                            || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f
                            || Duration.between(startTime.get(), ZonedDateTime.now()).toMillis() >= 500) {
                        scrolling.set(true);
                        this.getPage().pageFileList.requestDisallowInterceptTouchEvent(true);
                    }
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get()) {
                        this.mainActivity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", v.getX()).putFloat("y", v.getY()).apply();
                        this.getPage().pageFileList.requestDisallowInterceptTouchEvent(false);
                    } else return v.performClick();
                }
            }
            return true;
        });
        Main.runOnBackgroundThread(this.mainActivity, () -> {
            final SharedPreferences preferences = this.mainActivity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
            final DisplayMetrics displayMetrics = this.mainActivity.getResources().getDisplayMetrics();
            final float x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileUploader.getWidth()) * 0.8f);
            final float y = preferences.getFloat("y", (displayMetrics.heightPixels - page.pageFileUploader.getHeight()) * 0.7f);
            if (!preferences.contains("x") || !preferences.contains("y"))
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            Main.runOnUiThread(this.mainActivity, () -> {
                page.pageFileUploader.setX(x);
                page.pageFileUploader.setY(y);
                page.pageFileUploader.setVisibility(View.VISIBLE);
            });
        }, 300, TimeUnit.MILLISECONDS);
        page.pageFileUploader.setOnClickListener(u -> {
            if (this.partList.isOnRoot()) {
                this.partUpload.addStorage();
                return;
            }
            final BottomSheetDialog dialog = new BottomSheetDialog(this.mainActivity, R.style.BottomSheetDialog);
            final PageFileUploadBinding uploader = PageFileUploadBinding.inflate(this.mainActivity.getLayoutInflater());
            uploader.pageFileUploadCancel.setOnClickListener(v -> dialog.cancel());
            final AtomicBoolean clickable = new AtomicBoolean(true);
            uploader.pageFileUploadStorageImage.setOnClickListener(v -> {
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.partUpload.addStorage();
            });
            uploader.pageFileUploadStorageText.setOnClickListener(v -> uploader.pageFileUploadStorageImage.performClick());
            uploader.pageFileUploadDirectoryImage.setOnClickListener(v -> {
                if (this.partList.isOnRoot()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.partUpload.createDirectory();
            });
            uploader.pageFileUploadDirectoryText.setOnClickListener(v -> uploader.pageFileUploadDirectoryImage.performClick());
            uploader.pageFileUploadFileImage.setOnClickListener(v -> {
                if (this.partList.isOnRoot()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("*/*");
            });
            uploader.pageFileUploadFileText.setOnClickListener(v -> uploader.pageFileUploadFileImage.performClick());
            uploader.pageFileUploadPictureImage.setOnClickListener(v -> {
                if (this.partList.isOnRoot()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("image/*");
            });
            uploader.pageFileUploadPictureText.setOnClickListener(v -> uploader.pageFileUploadPictureImage.performClick());
            uploader.pageFileUploadVideoImage.setOnClickListener(v -> {
                if (this.partList.isOnRoot()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("video/*");
            });
            uploader.pageFileUploadVideoText.setOnClickListener(v -> uploader.pageFileUploadVideoImage.performClick());
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(uploader.getRoot());
            dialog.show();
        });
    }

    @Override
    public void onActivityCreateHook() {
        this.chooserLauncher.reinitialize(this.mainActivity.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null)
                this.partUpload.uploadFile(uri);
        }));
        this.mainActivity.getContent().activityMainOptions.setOnClickListener(v -> {
            if (this.mainActivity.currentChoice() != FragmentsAdapter.FragmentTypes.File) return;
            final ListPopupWindow popup = new ListPopupWindow(this.mainActivity);
            popup.setWidth(this.pageCache.getInstance().pageFileList.getWidth() >> 1);
            popup.setAnchorView(this.mainActivity.getContent().activityMainOptions);
            popup.setAdapter(new SimpleAdapter(this.mainActivity, List.of(
                    Map.of("image", R.drawable.page_file_options_refresh, "name", this.mainActivity.getResources().getString(R.string.page_file_options_refresh)),
                    Map.of("image", R.drawable.page_file_options_sorter, "name", this.mainActivity.getResources().getString(R.string.page_file_options_sorter)),
                    Map.of("image", R.drawable.page_file_options_filter, "name", this.mainActivity.getResources().getString(R.string.page_file_options_filter))
            ), R.layout.page_file_options_cell, new String[]{"image", "name"},
                    new int[]{R.id.activity_main_options_cell_image, R.id.activity_main_options_cell_name}));
            final AtomicBoolean clickable = new AtomicBoolean(true);
            popup.setOnItemClickListener((p, w, pos, i) -> {
                if (!clickable.compareAndSet(true, false)) return;
                popup.dismiss();
                if (pos == 0)
                    this.partOptions.refresh();
                if (pos == 1)
                    this.partOptions.sort();
                if (pos == 2)
                    this.partOptions.filter();
            });
            popup.show();
        });
    }

    @Override
    public void onConnected() {
        this.partList.listenBroadcast(BroadcastAssistant.get(this.address()));
    }

    @Override
    public boolean onBackPressed() {
        return this.partList.popFileList();
    }

    @Override
    public @NotNull String toString() {
        return "PageFile{" +
                "partList=" + this.partList +
                ", partOptions=" + this.partOptions +
                ", partOperation=" + this.partOperation +
                ", partPreview=" + this.partPreview +
                ", partUpload=" + this.partUpload +
                ", pageCache=" + this.pageCache +
                ", super=" + super.toString() +
                '}';
    }
}
