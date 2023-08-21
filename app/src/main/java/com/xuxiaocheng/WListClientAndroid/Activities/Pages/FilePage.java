package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.AndroidSupports.FileLocationSupporter;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.SpecialDriverName;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClient.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.Utils.RecyclerViewAdapterWrapper;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileContentBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileUploadBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FilePage implements MainTab.MainTabPage {
    @NonNull protected final Activity activity;
    @NonNull protected final InetSocketAddress address;

    public FilePage(@NonNull final Activity activity, @NonNull final InetSocketAddress address) {
        super();
        this.activity = activity;
        this.address = address;
    }

    @NonNull protected final HInitializer<PageFileContentBinding> pageCache = new HInitializer<>("FilePage");
    @SuppressLint("ClickableViewAccessibility")
    @Override
    @NonNull public ConstraintLayout onShow() {
        final PageFileContentBinding cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache.getRoot();
        final PageFileContentBinding page = PageFileContentBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.initialize(page);
        page.pageFileContentName.setText(R.string.app_name);
        page.pageFileContentList.setLayoutManager(new LinearLayoutManager(this.activity));
        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() ->
                        this.setFileList(this.address, new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0))));
        final AtomicBoolean scrolling = new AtomicBoolean();
        final AtomicInteger startX = new AtomicInteger(), startY = new AtomicInteger();
        page.pageFileContentUploader.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    scrolling.set(false);
                    startX.set(Float.floatToIntBits(v.getX()));
                    startY.set(Float.floatToIntBits(v.getY()));
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (scrolling.get()) {
                        final float parentX = page.pageFileContentList.getX(), parentY = page.pageFileContentList.getY();
                        v.setX(HMathHelper.clamp(v.getX() + e.getX() - parentX, 0, page.pageFileContentList.getWidth()) + parentX - v.getWidth() / 2.0f);
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, -50, page.pageFileContentList.getHeight()) + parentY - v.getHeight() / 2.0f);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f)
                        scrolling.set(true);
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get())
                        FilePage.this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", v.getX()).putFloat("y", v.getY()).apply();
                    else return v.performClick();
                }
            }
            return true;
        });
        Main.runOnUiThread(this.activity, () -> {
            final float x, y;
            final SharedPreferences preferences = this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
            if (preferences.contains("x") && preferences.contains("y")) {
                x = preferences.getFloat("x", 0);
                y = preferences.getFloat("y", 0);
            } else {
                final DisplayMetrics displayMetrics = this.activity.getResources().getDisplayMetrics();
                x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileContentUploader.getWidth()) * 0.9f);
                y = preferences.getFloat("y", displayMetrics.heightPixels * 0.6f);
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            }
            page.pageFileContentUploader.setX(x);
            page.pageFileContentUploader.setY(y);
        }, 100, TimeUnit.MILLISECONDS);
        page.pageFileContentUploader.setOnClickListener(u -> {
            final PageFileUploadBinding upload = PageFileUploadBinding.inflate(this.activity.getLayoutInflater());
            final AlertDialog uploader = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_upload).setView(upload.getRoot())
                    .setPositiveButton(R.string.page_file_upload_cancel, (d, v) -> {}).create();
            final AtomicBoolean nonclickable = new AtomicBoolean(false);
            upload.pageFileUploadDirectory.setOnClickListener(v -> {
                if (!nonclickable.compareAndSet(false, true)) return;
                HLogManager.getInstance("DefaultLogger").log("", "On directory.");
                uploader.cancel();
            });
            upload.pageFileUploadDirectoryText.setOnClickListener(v -> upload.pageFileUploadDirectory.performClick());
            upload.pageFileUploadFile.setOnClickListener(v -> {
                if (!nonclickable.compareAndSet(false, true)) return;
                HLogManager.getInstance("DefaultLogger").log("", "On file.");
                uploader.cancel();
            });
            upload.pageFileUploadFileText.setOnClickListener(v -> upload.pageFileUploadFile.performClick());
            uploader.show();
        });
        return page.getRoot();
    }

    @NonNull protected final AtomicBoolean clickable = new AtomicBoolean(true);
    @NonNull protected final Deque<Pair.ImmutablePair<FileLocation, CharSequence>> fileListStack = new ArrayDeque<>(); // TODO: save adapter.

    protected void setFileList(@NonNull final InetSocketAddress address, @NonNull final FileLocation directoryLocation) throws WrongStateException, IOException, InterruptedException {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final AtomicInteger currentPage = new AtomicInteger(0);
        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> lists;
        // TODO loading anim
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            // TODO: more configurable params.
            lists = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                    Options.DirectoriesOrFiles.Both, 50, currentPage.get(), Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
        }
        if (lists == null) {
            Main.runOnUiThread(FilePage.this.activity, () -> {
                FilePage.this.onBackPressed();
                Main.showToast(FilePage.this.activity, R.string.page_file_unavailable_directory);
            });
            return;
        }
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(directoryLocation));
        final int allPage = MiscellaneousUtil.calculatePartCount(lists.getB().intValue(), 50);
        final RecyclerViewAdapterWrapper<VisibleFileInformation, CellViewHolder> adapterWrapper = new RecyclerViewAdapterWrapper<>() {
            @Override
            @NonNull protected CellViewHolder createViewHolder(@NonNull final ViewGroup parent) {
                return new CellViewHolder(RecyclerViewAdapterWrapper.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileContentList), information -> {
                    if (FilePage.this.clickable.compareAndSet(true, false)) {
                        FilePage.this.fileListStack.push(Pair.ImmutablePair.makeImmutablePair(directoryLocation, ((TextView) page.pageFileContentName).getText()));
                        final AtomicBoolean failure = new AtomicBoolean(true);
                        Main.runOnBackgroundThread(FilePage.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                            if (FileInformationGetter.isDirectory(information)) {
                                FilePage.this.setFileList(address, FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(directoryLocation), FileInformationGetter.id(information)));
                                Main.runOnUiThread(FilePage.this.activity, () -> page.pageFileContentName.setText(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information)));
                            } else {
                                // TODO: show file.
                                throw new UnsupportedOperationException("Show file is unsupported now!");
                            }
                            failure.set(false);
                        }, () -> {
                            FilePage.this.clickable.set(true);
                            if (failure.get())
                                FilePage.this.fileListStack.pop();
                        }));
                    }
                }, isRoot);
            }

            @Override
            protected void bindViewHolder(@NonNull final CellViewHolder holder, @NonNull final VisibleFileInformation information) {
                holder.onBind(information);
            }
        };
        adapterWrapper.addDataRange(lists.getC());
        if (allPage == 1)
            adapterWrapper.addTailor(RecyclerViewAdapterWrapper.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, page.pageFileContentList));
        // TODO: register broadcast listener.
        final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
            @NonNull private final AtomicBoolean onLoading = new AtomicBoolean(false);
            @NonNull private final AtomicBoolean noMore = new AtomicBoolean(false);
            @Override
            public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                if (!recyclerView.canScrollVertically(1) && !this.noMore.get() && this.onLoading.compareAndSet(false, true)) {
                    adapterWrapper.addTailor(RecyclerViewAdapterWrapper.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, page.pageFileContentList));
                    Main.runOnBackgroundThread(FilePage.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> list;
                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                            list = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                                    Options.DirectoriesOrFiles.Both, 50, currentPage.incrementAndGet(), Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
                        }
                        if (list == null) {
                            Main.showToast(FilePage.this.activity, R.string.page_file_unavailable_directory);
                            FilePage.this.onBackPressed();
                            return;
                        }
                        Main.runOnUiThread(FilePage.this.activity, () -> adapterWrapper.addDataRange(list.getC()));
                    }, () -> {
                        this.onLoading.set(false);
                        Main.runOnUiThread(FilePage.this.activity, () -> {
                            adapterWrapper.removeTailor(0);
                            if (currentPage.get() >= allPage - 1) {
                                adapterWrapper.addTailor(RecyclerViewAdapterWrapper.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, page.pageFileContentList));
                                this.noMore.set(true);
                                if (page.pageFileContentList.getAdapter() == adapterWrapper) // Confuse: Why must call setAdapter?
                                    page.pageFileContentList.setAdapter(adapterWrapper);
                            } else
                               this.onScrollStateChanged(recyclerView, newState);
                        });
                    }));
                }
            }
        };
        Main.runOnUiThread(this.activity, () -> {
            final ImageView backer = page.pageFileContentBacker;
            if (isRoot) {
                backer.setImageResource(R.mipmap.page_file_backer_nonclickable);
                backer.setOnClickListener(null);
                backer.setClickable(false);
            } else {
                backer.setImageResource(R.mipmap.page_file_backer);
                backer.setOnClickListener(v -> this.onBackPressed());
                backer.setClickable(true);
            }
            page.pageFileContentCounter.setText(String.valueOf(lists.getB()));
            final RecyclerView content = page.pageFileContentList;
            content.setAdapter(adapterWrapper);
            content.setHasFixedSize(true);
            content.clearOnScrollListeners();
            if (allPage > 1) {
                content.addOnScrollListener(scrollListener);
                scrollListener.onScrollStateChanged(content, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        final Pair.ImmutablePair<FileLocation, CharSequence> p = this.fileListStack.poll();
        if (p == null) return false;
        if (this.clickable.compareAndSet(true, false))
            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                this.setFileList(this.address, p.getFirst());
                Main.runOnUiThread(this.activity, () -> this.pageCache.getInstance().pageFileContentName.setText(p.getSecond()));
            }, () -> this.clickable.set(true)));
        return true;
    }

    @Override
    @NonNull public String toString() {
        return "FilePage{" +
                "address=" + this.address +
                ", pageCache=" + this.pageCache +
                ", clickable=" + this.clickable +
                ", fileListStack=" + this.fileListStack +
                '}';
    }

    protected static class CellViewHolder extends RecyclerViewAdapterWrapper.WrappedViewHolder<VisibleFileInformation, ConstraintLayout> {
        @NonNull protected final Consumer<VisibleFileInformation> clicker;
        protected final boolean isRoot;
        @NonNull protected final ImageView image;
        @NonNull protected final TextView name;
        @NonNull protected final TextView tips;
        @NonNull protected final View option;

        protected CellViewHolder(@NonNull final ConstraintLayout cell, @NonNull final Consumer<VisibleFileInformation> clicker, final boolean isRoot) {
            super(cell);
            this.clicker = clicker;
            this.isRoot = isRoot;
            this.image = (ImageView) cell.getViewById(R.id.page_file_cell_image);
            this.name = (TextView) cell.getViewById(R.id.page_file_cell_name);
            this.tips = (TextView) cell.getViewById(R.id.page_file_cell_tips);
            this.option = cell.getViewById(R.id.page_file_cell_option);
        }

        public void onBind(@NonNull final VisibleFileInformation information) {
            this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
            CellViewHolder.setFileImage(this.image, information);
            this.name.setText(this.isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information));
            final LocalDateTime update = FileInformationGetter.updateTime(information);
            this.tips.setText(update == null ? "unknown" : update.format(DateTimeFormatter.ISO_DATE_TIME).replace('T', ' '));
//            this.option.setOnClickListener(v -> {
//                // TODO
//            });
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof CellViewHolder holder)) return false;
            return this.image.equals(holder.image) && this.name.equals(holder.name) && this.tips.equals(holder.tips) && this.option.equals(holder.option);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.image, this.name, this.tips, this.option);
        }

        @Override
        @NonNull public String toString() {
            return "FilePage$CellViewHolder{" +
                    "clicker=" + this.clicker +
                    ", image=" + this.image +
                    ", name=" + this.name +
                    ", tips=" + this.tips +
                    ", option=" + this.option +
                    '}';
        }

        protected static void setFileImage(@NonNull final ImageView image, @NonNull final VisibleFileInformation information) {
            if (FileInformationGetter.isDirectory(information)) {
                image.setImageResource(R.mipmap.page_file_image_directory);
                return;
            }
            final String name = FileInformationGetter.name(information);
            final int index = name.lastIndexOf('.');
            // TODO: cached Drawable.
            image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT)) {
                case "bat", "cmd", "sh", "run" -> R.mipmap.page_file_image_bat;
                case "doc", "docx" -> R.mipmap.page_file_image_docx;
                case "exe", "bin" -> R.mipmap.page_file_image_exe;
                case "jpg", "jpeg", "png", "bmp", "psd", "tga" -> R.mipmap.page_file_image_jpg;
                case "mp3", "flac", "wav", "wma", "aac", "ape" -> R.mipmap.page_file_image_mp3;
                case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
                case "txt", "log" -> R.mipmap.page_file_image_txt;
                case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
                case "zip", "7z", "rar", "gz", "tar" -> R.mipmap.page_file_image_zip;
                default -> R.mipmap.page_file_image_file;
            });
        }
    }
}
