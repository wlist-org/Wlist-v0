package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.AndroidSupports.FileLocationSupporter;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.SpecialDriverName;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileContentBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileUploadBinding;

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
import java.util.concurrent.atomic.AtomicLong;
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
        page.pageFileContentList.setHasFixedSize(true);
        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() ->
                        this.pushFileList(page.pageFileContentName.getText(), new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0))));
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

    @NonNull protected final Deque<LocationStackRecord> locationStack = new ArrayDeque<>();
    protected static class LocationStackRecord {
        protected final boolean isRoot;
        @NonNull protected final CharSequence name;
        @NonNull protected final AtomicLong counter;
        @NonNull protected final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter;
        @NonNull protected final RecyclerView.OnScrollListener listener;

        protected LocationStackRecord(final boolean isRoot, @NonNull final CharSequence name, @NonNull final AtomicLong counter,
                                      @NonNull final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter,
                                      @NonNull final RecyclerView.OnScrollListener listener) {
            super();
            this.isRoot = isRoot;
            this.name = name;
            this.counter = counter;
            this.adapter = adapter;
            this.listener = listener;
        }

        @Override
        @NonNull public String toString() {
            return "LocationStackRecord{" +
                    "isRoot=" + this.isRoot +
                    ", name=" + this.name +
                    ", counter=" + this.counter +
                    ", adapter=" + this.adapter +
                    ", listener=" + this.listener +
                    '}';
        }
    }

    private void setBacker(final boolean isRoot) {
        final ImageView backer = this.pageCache.getInstance().pageFileContentBacker;
        if (isRoot) {
            backer.setImageResource(R.mipmap.page_file_backer_nonclickable);
            backer.setOnClickListener(null);
            backer.setClickable(false);
        } else {
            backer.setImageResource(R.mipmap.page_file_backer);
            backer.setOnClickListener(v -> this.popFileList());
            backer.setClickable(true);
        }
    }

    protected void pushFileList(@NonNull final CharSequence name, @NonNull final FileLocation location) {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(location));
        final AtomicInteger currentPage = new AtomicInteger(0);
        final AtomicLong counter = new AtomicLong(0);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapterWrapper = new EnhancedRecyclerViewAdapter<>() {
            @Override
            @NonNull protected CellViewHolder createViewHolder(@NonNull final ViewGroup parent) {
                return new CellViewHolder(EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileContentList), information -> {
                   if (FileInformationGetter.isDirectory(information))
                        FilePage.this.pushFileList(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information),
                                FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(location), FileInformationGetter.id(information)));
                    else {
                        // TODO: show file.
                        throw new UnsupportedOperationException("Show file is unsupported now!");
                    }
                }, isRoot);
            }

            @Override
            protected void bindViewHolder(@NonNull final CellViewHolder holder, @NonNull final VisibleFileInformation information) {
                holder.onBind(information);
            }
        };
        final RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
            @NonNull private final AtomicBoolean onLoading = new AtomicBoolean(false);
            @NonNull private final AtomicBoolean noMore = new AtomicBoolean(false);
            @Override
            public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                // TODO: register broadcast listener.
                if (!recyclerView.canScrollVertically(1) && !this.noMore.get() && this.onLoading.compareAndSet(false, true)) {
                    final ConstraintLayout loadingTailor = EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, page.pageFileContentList);
                    final ImageView loading = (ImageView) loadingTailor.getViewById(R.id.page_file_tailor_loading_image);
                    final Animation loadingAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    loadingAnimation.setDuration(800);
                    loadingAnimation.setFillAfter(true);
                    loadingAnimation.setRepeatCount(Animation.INFINITE);
                    loadingAnimation.setInterpolator(new LinearInterpolator());
                    loading.startAnimation(loadingAnimation);
                    adapterWrapper.addTailor(loadingTailor);
                    Main.runOnBackgroundThread(FilePage.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> list;
                        // TODO: loading progress.
                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(FilePage.this.address)) {
                            // TODO: more configurable params.
                            list = OperateFileHelper.listFiles(client, TokenManager.getToken(FilePage.this.address), location,
                                    Options.DirectoriesOrFiles.Both, 50, currentPage.getAndIncrement(), Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
                        }
                        if (list == null) {
                            Main.runOnUiThread(FilePage.this.activity, () -> {
                                Main.showToast(FilePage.this.activity, R.string.page_file_unavailable_directory);
                                FilePage.this.popFileList();
                            });
                            return;
                        }
                        this.noMore.set(list.getC().isEmpty());
                        counter.set(list.getA().longValue());
                        Main.runOnUiThread(FilePage.this.activity, () -> {
                            page.pageFileContentCounter.setText(String.valueOf(list.getA()));
                            adapterWrapper.addDataRange(list.getC());
                        });
                    }, () -> {
                        this.onLoading.set(false);
                        Main.runOnUiThread(FilePage.this.activity, () -> {
                            loading.clearAnimation();
                            if (this.noMore.get()) {
                                adapterWrapper.setTailor(0, EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, page.pageFileContentList));
                                if (page.pageFileContentList.getAdapter() == adapterWrapper) // Confuse: Why must call 'setAdapter' again?
                                    page.pageFileContentList.setAdapter(adapterWrapper);
                            } else {
                                adapterWrapper.removeTailor(0);
                                this.onScrollStateChanged(recyclerView, newState);
                            }
                        });
                    }));
                }
            }
        };
        FilePage.this.locationStack.push(new LocationStackRecord(isRoot, name, counter, adapterWrapper, listener));
        Main.runOnUiThread(this.activity, () -> {
            this.setBacker(isRoot);
            page.pageFileContentName.setText(name);
            final RecyclerView content = page.pageFileContentList;
            content.setAdapter(adapterWrapper);
            content.clearOnScrollListeners();
            content.addOnScrollListener(listener);
            listener.onScrollStateChanged(content, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        });
    }

    @UiThread
    protected boolean popFileList() {
        final PageFileContentBinding page = this.pageCache.getInstanceNullable();
        if (page == null) return false;
        final LocationStackRecord record = this.locationStack.poll();
        if (record == null) return false;
        this.setBacker(record.isRoot);
        page.pageFileContentName.setText(record.name);
        page.pageFileContentCounter.setText(String.valueOf(record.counter));
        page.pageFileContentList.setAdapter(record.adapter);
        page.pageFileContentList.clearOnScrollListeners();
        page.pageFileContentList.addOnScrollListener(record.listener);
        return true;
    }

    @Override
    public boolean onBackPressed() {
        return this.popFileList();
    }

    @Override
    @NonNull public String toString() {
        return "FilePage{" +
                "address=" + this.address +
                ", pageCache=" + this.pageCache +
                ", locationStack=" + this.locationStack +
                '}';
    }

    protected static class CellViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<VisibleFileInformation, ConstraintLayout> {
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
