package com.xuxiaocheng.WListClientAndroid.UIs;

import android.annotation.SuppressLint;
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
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FilesListInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileContentBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileOptionBinding;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PageFile implements ActivityMainChooser.MainPage {
    @NotNull
    protected final ActivityMain activity;

    public PageFile(final @NotNull ActivityMain activity) {
        super();
        this.activity = activity;
    }

    protected @NotNull InetSocketAddress address() {
        return this.activity.address.getInstance();
    }

    protected @NotNull String username() {
        return this.activity.username.getInstance();
    }


    protected final @NotNull HInitializer<PageFileContentBinding> pageCache = new HInitializer<>("PageFile");
    @Override
    public @NotNull ConstraintLayout onShow() {
        final PageFileContentBinding cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache.getRoot();
        final PageFileContentBinding page = PageFileContentBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.initialize(page);
        page.pageFileContentList.setLayoutManager(new LinearLayoutManager(this.activity));
        page.pageFileContentList.setHasFixedSize(true);
        this.onRootPage();
        this.buildUploader();
        Main.runOnBackgroundThread(this.activity, () -> {
            final BroadcastAssistant.BroadcastSet set = BroadcastAssistant.get(this.address());
            set.ProviderInitialized.register(s -> {

            });
        });
        return page.getRoot();
//        PermissionsHelper.getPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, success -> {
//            if (!success.booleanValue())
//                Main.showToast(this, R.string.toast_no_read_permissions);
//        });
//        PermissionsHelper.getPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, success -> {
//            if (!success.booleanValue())
//                Main.showToast(this, R.string.toast_no_write_permissions);
//        });
    }

    @UiThread
    private static void setLoading(final @NotNull ImageView loading) {
        final Animation loadingAnimation = new RotateAnimation(0, 360 << 10, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        loadingAnimation.setDuration(500 << 10);
        loadingAnimation.setInterpolator(new LinearInterpolator());
        loadingAnimation.setRepeatCount(Animation.INFINITE);
        loading.startAnimation(loadingAnimation);
    }

    @UiThread
    private @NotNull View listLoadingView() {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, this.pageCache.getInstance().pageFileContentList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        PageFile.setLoading(image);
        return loading;
    }

    @UiThread
    private @NotNull View listNoMoreView() {
        return EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, this.pageCache.getInstance().pageFileContentList);
    }

    private static final @NotNull HInitializer<String> listLoadingAnimationPattern = new HInitializer<>("ListLoadingAnimationPattern");
    @UiThread
    private void listLoadingAnimation(final boolean show, final long current, final long total) {
        final PageFileContentBinding page = this.pageCache.getInstance();
        PageFile.listLoadingAnimationPattern.initializeIfNot(() -> this.activity.getString(R.string.page_file_loading_text));
        page.pageFileContentLoadingText.setText(MessageFormat.format(PageFile.listLoadingAnimationPattern.getInstance(), current, total));
        if (show) {
            page.pageFileContentLoading.setVisibility(View.VISIBLE);
            page.pageFileContentLoadingText.setVisibility(View.VISIBLE);
            PageFile.setLoading(page.pageFileContentLoading);
        } else {
            page.pageFileContentLoading.setVisibility(View.GONE);
            page.pageFileContentLoadingText.setVisibility(View.GONE);
            page.pageFileContentLoading.clearAnimation();
        }
    }


    protected final @NotNull PageFileStacks stacks = new PageFileStacks();

    @UiThread
    protected void updatePage(final boolean isRoot, final @NotNull CharSequence name, final @NotNull FileLocation location,
                              final @NotNull Consumer<? super @NotNull VisibleFileInformation> clicker, final @NotNull Consumer<@NotNull VisibleFileInformation> option) {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final ImageView backer = page.pageFileContentBacker;
        if (isRoot) {
            backer.setImageResource(R.mipmap.page_file_backer_nonclickable);
            backer.setOnClickListener(null);
            backer.setClickable(false);
        } else {
            backer.setImageResource(R.mipmap.page_file_backer);
            backer.setOnClickListener(v -> this.popFileList());
            backer.setClickable(true);
        }
        page.pageFileContentName.setText(name);
        final AtomicLong counter = new AtomicLong();
        final HInitializer<RecyclerView.OnScrollListener> listenerHInitializer = new HInitializer<>("OnScrollListener");
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileContentList),
                        information -> {PageFile.this.stacks.push(name, counter, location, this, listenerHInitializer.getInstance());clicker.accept(information);}, option);
            }

            @Override
            protected void bindViewHolder(final @NotNull PageFileViewHolder holder, final @NotNull VisibleFileInformation information) {
                holder.onBind(information);
            }
        };
        page.pageFileContentCounter.setVisibility(View.GONE);
        page.pageFileContentCounterText.setVisibility(View.GONE); // Set visible in listener.
        final AtomicLong position = new AtomicLong(0);
        final RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
            private final @NotNull AtomicBoolean onLoading = new AtomicBoolean(false);
            private final @NotNull AtomicBoolean noMore = new AtomicBoolean(false);
            @UiThread
            @Override
            public void onScrollStateChanged(final @NotNull RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                if (recyclerView.canScrollVertically(1) || this.noMore.get() || !this.onLoading.compareAndSet(false, true))
                    return;
                adapter.addTailor(PageFile.this.listLoadingView());
                Main.runOnBackgroundThread(PageFile.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    final VisibleFilesListInformation list;
                    this.noMore.set(false); // prevent retry forever when server error.
                    Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, 0, 0));
                    try {
                        final ClientConfiguration configuration = ClientConfigurationSupporter.get();
                        final Options.FilterPolicy filter = ClientConfigurationSupporter.filterPolicy(configuration);
                        final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders = ClientConfigurationSupporter.fileOrders(configuration);
                        final int limit = ClientConfigurationSupporter.limitPerPage(configuration);
                        list = FilesAssistant.list(PageFile.this.address(), PageFile.this.username(), location, filter, orders, position.getAndAdd(limit), limit, Main.ClientExecutors, s -> {
                            if (s == null) return;
                            long current = 0, total = 0;
                            for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                current += pair.getFirst().longValue();
                                total += pair.getSecond().longValue();
                            }
                            final long c = current, t = total;
                            Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, c, t));
                        });
                    } finally {
                        Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(false, 0, 0));
                    }
                    if (list == null) {
                        Main.showToast(PageFile.this.activity, R.string.page_file_unavailable_directory);
                        Main.runOnUiThread(PageFile.this.activity, PageFile.this::popFileList);
                        return;
                    }
                    this.noMore.set(FilesListInformationGetter.informationList(list).isEmpty());
                    counter.set(FilesListInformationGetter.total(list));
                    Main.runOnUiThread(PageFile.this.activity, () -> {
                        page.pageFileContentCounter.setText(String.valueOf(FilesListInformationGetter.total(list)));
                        page.pageFileContentCounter.setVisibility(View.VISIBLE);
                        page.pageFileContentCounterText.setVisibility(View.VISIBLE);
                        adapter.addDataRange(FilesListInformationGetter.informationList(list));
                    });
                }, e -> {
                    this.onLoading.set(false);
                    Main.runOnUiThread(PageFile.this.activity, () -> {
                        if (this.noMore.get() || e != null) {
                            adapter.setTailor(0, PageFile.this.listNoMoreView());
                            if (page.pageFileContentList.getAdapter() == adapter) // Confuse: Why must call 'setAdapter' again?
                                page.pageFileContentList.setAdapter(adapter);
                        } else {
                            adapter.removeTailor(0);
                            if (page.pageFileContentList.getAdapter() == adapter) // Automatically load more if still in this list page.
                                this.onScrollStateChanged(recyclerView, newState);
                        }
                    });
                }, false));
            }
        };
        listenerHInitializer.initialize(listener);
        final RecyclerView content = page.pageFileContentList;
        content.setAdapter(adapter);
        content.clearOnScrollListeners();
        content.addOnScrollListener(listener);
        listener.onScrollStateChanged(content, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
    }

    @UiThread
    protected void onRootPage() {
        final PageFileContentBinding page = this.pageCache.getInstance();
        page.pageFileContentName.setText(R.string.app_name);
        this.stacks.nonCachedStacks.clear();
        this.stacks.cachedStacks.clear();
        this.updatePage(true, ((TextView) page.pageFileContentName).getText(),
                new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), 0),
                information -> this.onInsidePage(FileInformationGetter.name(information),
                        new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information))), information ->
                        Main.runOnBackgroundThread(this.activity, () -> {throw new UnsupportedOperationException("WIP");}) // TODO
        );
    }

    @UiThread
    protected void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location) {
        this.updatePage(false, name, location, information -> {
            if (FileInformationGetter.isDirectory(information))
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)));
            else
                Main.runOnBackgroundThread(this.activity, () -> {throw new UnsupportedOperationException("WIP");}); // TODO
        }, information -> {
            final PageFileOptionBinding optionBinding = PageFileOptionBinding.inflate(this.activity.getLayoutInflater());
            optionBinding.pageFileOptionName.setText(FileInformationGetter.name(information));
            final long size = FileInformationGetter.size(information);
            optionBinding.pageFileOptionSize.setText(size < 1 ? "unknown" : String.valueOf(size));
            optionBinding.pageFileOptionCreate.setText(FileInformationGetter.createTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown"));
            optionBinding.pageFileOptionUpdate.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown"));
            final AlertDialog modifier = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_option).setView(optionBinding.getRoot())
                    .setPositiveButton(R.string.cancel, null).create();
            final AtomicBoolean clickable = new AtomicBoolean(true);
//                optionBinding.pageFileOptionRename.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final PageFileEditorBinding editor = PageFileEditorBinding.inflate(this.page.activity.getLayoutInflater());
//                    editor.pageFileEditor.setText(FileInformationGetter.name(information));
//                    if (editor.pageFileEditor.requestFocus()) {
//                        editor.pageFileEditor.setSelectAllOnFocus(true);
//                        editor.pageFileEditor.setSelection(Objects.requireNonNull(editor.pageFileEditor.getText()).length());
//                    }
//                    new AlertDialog.Builder(this.page.activity).setTitle(R.string.page_file_option_rename).setView(editor.getRoot())
//                            .setNegativeButton(R.string.cancel, (d, w) -> {})
//                            .setPositiveButton(R.string.confirm, (d, w) -> {
//                                final Editable editable = editor.pageFileEditor.getText();
//                                final String name = editable == null ? "" : editable.toString();
//                                if (FileInformationGetter.name(information).equals(name)) return;
//                                final ImageView loading = new ImageView(this.page.activity);
//                                loading.setImageResource(R.mipmap.page_file_loading);
//                                PageFile.setLoading(loading);
//                                final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                                        .setTitle(R.string.page_file_option_rename).setView(loading).setCancelable(false).show();
//                                Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
//                                            ParametersMap.create().add("address", this.page.address).add("location", location).add("name", name));
//                                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                                        OperateFilesHelper.renameFile(client, TokenManager.getToken(this.page.address), location, name, Options.DuplicatePolicy.ERROR);
//                                    }
//                                    Main.runOnUiThread(this.page.activity, () -> {
//                                        Main.showToast(this.page.activity, R.string.page_file_option_rename_success);
//                                        // TODO: auto refresh.
//                                        this.page.popFileList();
//                                        this.page.pushFileList(record.name, record.location);
//                                    });
//                                }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                            }).show();
//                });
//                optionBinding.pageFileOptionRenameIcon.setOnClickListener(u -> optionBinding.pageFileOptionRename.performClick());
//                optionBinding.pageFileOptionMove.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    Main.runOnBackgroundThread(this.page.activity, () -> {
//                        // TODO: move file.
//                        throw new UnsupportedOperationException("Move file is unsupported now!");
//                    });
////                    final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
////                        @Override
////                        protected @NotNull CellViewHolder createViewHolder(final @NotNull ViewGroup parent) {
////                            final CellViewHolder holder = new CellViewHolder(EnhancedRecyclerViewAdapter.buildView(CellViewHolder.this.page.activity.getLayoutInflater(), R.layout.page_file_cell, (RecyclerView) parent), information -> {
//////                                PageFile.this.pushFileList(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information),
//////                                        FileLocationGetter.create(isRoot ? FileInformationGetter.name(information) : FileLocationGetter.driver(location), FileInformationGetter.id(information)));
////                            }, isRoot, CellViewHolder.this.page);
////                            holder.option.setVisibility(View.GONE);
////                            return holder;
////                        }
////
////                        @Override
////                        protected void bindViewHolder(final @NotNull CellViewHolder holder, final @NotNull VisibleFileInformation information) {
////                            holder.itemView.setOnClickListener(v -> holder.clicker.accept(information));
////                            CellViewHolder.setFileImage(holder.image, information);
////                            holder.name.setText(holder.isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information));
////                            holder.tips.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown").replace('T', ' '));
////                            // TODO: same as 'onBind' except 'option'.
////                        }
////                    };
////                    final AlertDialog.Builder chooser = new AlertDialog.Builder(this.page.activity).setTitle(R.string.page_file_option_move)
////                            .setView()
//                });
//                optionBinding.pageFileOptionMoveIcon.setOnClickListener(u -> optionBinding.pageFileOptionMove.performClick());
//                optionBinding.pageFileOptionCopy.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    Main.runOnBackgroundThread(this.page.activity, () -> {
//                        // TODO: copy file.
//                        throw new UnsupportedOperationException("Copy file is unsupported now!");
//                    });
//                });
//                optionBinding.pageFileOptionCopyIcon.setOnClickListener(u -> optionBinding.pageFileOptionCopy.performClick());
//                optionBinding.pageFileOptionDelete.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final ImageView loading = new ImageView(this.page.activity);
//                    loading.setImageResource(R.mipmap.page_file_loading);
//                    PageFile.setLoading(loading);
//                    final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                            .setTitle(R.string.page_file_option_delete).setView(loading).setCancelable(false).show();
//                    Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
//                                ParametersMap.create().add("address", this.page.address).add("location", location));
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                            OperateFilesHelper.trashFileOrDirectory(client, TokenManager.getToken(this.page.address), location);
//                        }
//                        Main.runOnUiThread(this.page.activity, () -> {
//                            Main.showToast(this.page.activity, R.string.page_file_option_delete_success);
//                            // TODO: auto remove
//                            this.page.popFileList();
//                            this.page.pushFileList(record.name, record.location);
//                        });
//                    }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                });
//                optionBinding.pageFileOptionDeleteIcon.setOnClickListener(u -> optionBinding.pageFileOptionDelete.performClick());
//                optionBinding.pageFileOptionDownload.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final ImageView loading = new ImageView(this.page.activity);
//                    loading.setImageResource(R.mipmap.page_file_loading);
//                    PageFile.setLoading(loading);
//                    final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                            .setTitle(R.string.page_file_option_download).setView(loading).setCancelable(false).show();
//                    Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
//                                ParametersMap.create().add("address", this.page.address).add("location", location));
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                            final Pair.ImmutablePair<Long, String> id = OperateFilesHelper.requestDownloadFile(client, TokenManager.getToken(this.page.address), location, 0, Long.MAX_VALUE);
//                            if (id == null)
//                                throw new IllegalStateException("File not exist.");
//                            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist/" + FileInformationGetter.name(information));
//                            HFileHelper.ensureFileExist(file.toPath(), false);
//                            try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
//                                int chunk = 0;
//                                while (true) {
//                                    final ByteBuf buffer = OperateFilesHelper.downloadFile(client, TokenManager.getToken(this.page.address), id.getSecond(), chunk++);
//                                    if (buffer == null) break;
//                                    try (final InputStream buf = new ByteBufInputStream(buffer)) {
//                                        AIOStream.transferTo(buf, stream);
//                                    } finally {
//                                        buffer.release();
//                                    }
//                                }
//                            }
//                        }
//                        Main.showToast(this.page.activity, R.string.page_file_option_download_success);
//                    }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                });
//                optionBinding.pageFileOptionDownloadIcon.setOnClickListener(u -> optionBinding.pageFileOptionDownload.performClick());
            modifier.show();
        });
    }

    @UiThread
    protected boolean popFileList() {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final UnionPair<PageFileStacks.CachedStackRecord, PageFileStacks.NonCachedStackRecord> p = this.stacks.pop();
        if (p == null) {
            this.onRootPage();
            return false;
        }
        if (p.isSuccess()) {
            final PageFileStacks.CachedStackRecord record = p.getT();
            page.pageFileContentName.setText(record.name);
            page.pageFileContentCounter.setText(String.valueOf(record.counter.get()));
            final RecyclerView content = page.pageFileContentList;
            content.setAdapter(record.adapter);
            content.clearOnScrollListeners();
            content.addOnScrollListener(record.listener);
        } else {
            final PageFileStacks.NonCachedStackRecord record = p.getE();
            this.onInsidePage(record.name, record.location); // TODO: keep position.
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("ClickableViewAccessibility")
    protected <C extends StorageConfiguration> void buildUploader() {
        final PageFileContentBinding page = this.pageCache.getInstance();
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
                        PageFile.this.activity.getSharedPreferences("android.page.uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", v.getX()).putFloat("y", v.getY()).apply();
                    else return v.performClick();
                }
            }
            return true;
        });
        Main.runOnBackgroundThread(this.activity, () -> {
            final float x, y;
            final SharedPreferences preferences = this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
            if (preferences.contains("x") && preferences.contains("y")) {
                x = preferences.getFloat("x", 0);
                y = preferences.getFloat("y", 0);
            } else {
                final DisplayMetrics displayMetrics = this.activity.getResources().getDisplayMetrics();
                x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileContentUploader.getWidth()) * 0.7f);
                y = preferences.getFloat("y", displayMetrics.heightPixels * 0.6f);
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            }
            Main.runOnUiThread(this.activity, () -> {
                page.pageFileContentUploader.setX(x);
                page.pageFileContentUploader.setY(y);
            });
        });
        page.pageFileContentUploader.setOnClickListener(u -> {
            if (this.stacks.isEmpty()) { // Root selector
                final String[] storages = StorageTypes.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
                final AtomicInteger choice = new AtomicInteger(-1);
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_provider_add)
                        .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String identifier = storages[choice.get()];
                            final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypes.get(identifier));
                            PageFileProviderConfigurations.getConfiguration(PageFile.this.activity, type, null, configuration -> Main.runOnUiThread(this.activity, () -> {
                                final ImageView loading = new ImageView(PageFile.this.activity);
                                loading.setImageResource(R.mipmap.page_file_loading);
                                PageFile.setLoading(loading);
                                final AlertDialog dialog = new AlertDialog.Builder(PageFile.this.activity)
                                        .setTitle(R.string.page_file_provider_add).setView(loading)
                                        .setCancelable(false).show();
                                Main.runOnBackgroundThread(PageFile.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(PageFile.this.address())) {
                                        OperateProvidersHelper.addProvider(client, TokenAssistant.getToken(PageFile.this.address(), PageFile.this.username()),
                                                configuration.getName(), type, configuration);
                                    }
                                }, () -> Main.runOnUiThread(PageFile.this.activity, dialog::cancel)));
                            }));
                        }).show();
                return;
            }
//            final PageFileStacks.CachedStackRecord record = this.locationStack.getFirst();
//            final FileLocation location = record.location;
//            final PageFileUploadBinding upload = PageFileUploadBinding.inflate(this.activity.getLayoutInflater());
//            final AlertDialog uploader = new AlertDialog.Builder(this.activity)
//                    .setTitle(R.string.page_file_upload).setView(upload.getRoot())
//                    .setPositiveButton(R.string.cancel, (d, w) -> {}).create();
//            final AtomicBoolean clickable = new AtomicBoolean(true);
//            upload.pageFileUploadDirectory.setOnClickListener(v -> {
//                if (!clickable.compareAndSet(true, false)) return;
//                uploader.cancel();
//                final PageFileEditorBinding editor = PageFileEditorBinding.inflate(this.activity.getLayoutInflater());
//                editor.pageFileEditor.setText(R.string.page_file_upload_directory_name);
//                editor.pageFileEditor.setHint(R.string.page_file_upload_directory_hint);
//                if (editor.pageFileEditor.requestFocus()) {
//                    editor.pageFileEditor.setSelectAllOnFocus(true);
//                    editor.pageFileEditor.setSelection(Objects.requireNonNull(editor.pageFileEditor.getText()).length());
//                }
//                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_upload_directory)
//                        .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
//                        .setNegativeButton(R.string.cancel, (d, w) -> {})
//                        .setPositiveButton(R.string.confirm, (d, w) -> {
//                            final Editable editable = editor.pageFileEditor.getText();
//                            final String name = editable == null ? "" : editable.toString();
//                            final ImageView loading = new ImageView(this.activity);
//                            loading.setImageResource(R.mipmap.page_file_loading);
//                            PageFile.setLoading(loading);
//                            final AlertDialog dialog = new AlertDialog.Builder(this.activity)
//                                    .setTitle(R.string.page_file_upload_directory).setView(loading).setCancelable(false).show();
//                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
//                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
//                                        ParametersMap.create().add("address", this.address()).add("location", location).add("name", name));
//                                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
//                                    OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(this.address(), this.username()), location, name, Options.DuplicatePolicy.ERROR);
//                                }
//                                Main.runOnUiThread(this.activity, () -> {
//                                    Main.showToast(this.activity, R.string.page_file_upload_success_directory);
//                                    // TODO: auto add.
//                                    this.popFileList();
//                                    this.onInsidePage(name, location);
//                                });
//                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
//                        }).show();
//            });
//            upload.pageFileUploadDirectoryText.setOnClickListener(v -> upload.pageFileUploadDirectory.performClick());
//            final Consumer<String> uploadFile = type -> {
//                if (!clickable.compareAndSet(true, false)) return;
//                uploader.cancel();
//                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType(type);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                this.activity.startActivityForResult(intent, "SelectFiles".hashCode());
//            };
//            upload.pageFileUploadFile.setOnClickListener(v -> uploadFile.accept("*/*"));
//            upload.pageFileUploadFileText.setOnClickListener(v -> upload.pageFileUploadFile.performClick());
//            upload.pageFileUploadPicture.setOnClickListener(v -> uploadFile.accept("image/*"));
//            upload.pageFileUploadPictureText.setOnClickListener(v -> upload.pageFileUploadPicture.performClick());
//            upload.pageFileUploadVideo.setOnClickListener(v -> uploadFile.accept("video/*"));
//            upload.pageFileUploadVideoText.setOnClickListener(v -> upload.pageFileUploadVideo.performClick());
//            uploader.show();
        });
    }

//    @Override
//    public boolean onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent data) {
//        if (resultCode == Activity.RESULT_OK && requestCode == "SelectFiles".hashCode() && data != null) {
//            final Collection<Uri> uris = new ArrayList<>();
//            if (data.getData() != null)
//                uris.add(data.getData());
//            else {
//                final ClipData clipData = data.getClipData();
//                for (int i = 0; i < clipData.getItemCount(); ++i)
//                    uris.add(clipData.getItemAt(i).getUri());
//            }
//            final ImageView loading = new ImageView(this.activity);
//            loading.setImageResource(R.mipmap.page_file_loading);
//            PageFile.setLoading(loading);
//            final AlertDialog dialog = new AlertDialog.Builder(this.activity)
//                    .setTitle(R.string.page_file_upload_file).setView(loading).setCancelable(false).show();
//            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
//                final CountDownLatch latch = new CountDownLatch(uris.size());
//                for (final Uri uri: uris)
//                    Main.runOnNewBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        // TODO: serialize uploading task.
//                        final String filename;
//                        final long size;
//                        try (final Cursor cursor = this.activity.getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
//                            if (cursor == null || !cursor.moveToFirst())
//                                return;
//                            filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
//                            size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
//                        }
//                        final MessageDigest digester = HMessageDigestHelper.MD5.getDigester();
//                        try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
//                            HMessageDigestHelper.updateMessageDigest(digester, stream);
//                        }
//                        final String md5 = HMessageDigestHelper.MD5.digest(digester);
//                        final CachedStackRecord record = this.locationStack.getFirst(); // .peek();
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading file.",
//                                ParametersMap.create().add("address", this.address).add("location", record.location).add("uri", uri)
//                                        .add("filename", filename).add("size", size).add("md5", md5));
//                        final UnionPair<UnionPair<VisibleFileInformation, String>, FailureReason> request;
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
//                            request = OperateFilesHelper.requestUploadFile(client, TokenManager.getToken(this.address), record.location, filename, size, md5, Options.DuplicatePolicy.KEEP);
//                            if (request.isFailure()) // TODO
//                                throw new RuntimeException(FailureReason.handleFailureReason(request.getE()));
//                            if (request.getT().isFailure()) {
//                                final String id = request.getT().getE();
//                                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
//                                try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
//                                    int chunk = 0;
//                                    while (true) {
//                                        buffer.writeBytes(stream, NetworkTransmission.FileTransferBufferSize);
//                                        final UnionPair<VisibleFileInformation, Boolean> result = OperateFilesHelper.uploadFile(client, TokenManager.getToken(this.address), id, chunk++, buffer.retain());
//                                        if (result == null || result.isSuccess() || !result.getE().booleanValue() || stream.available() == 0)
//                                            break;
//                                        buffer.clear();
//                                    }
//                                } finally {
//                                    buffer.release();
//                                }
//                            }
//                        }
//            }, latch::countDown));
//                latch.await();
//                Main.runOnUiThread(this.activity, () -> {
//                    dialog.cancel();
//                    Main.showToast(this.activity, R.string.page_file_upload_success_file);
//                    // TODO: auto add.
//                    final CachedStackRecord record = this.locationStack.getFirst(); // .peek();
//                    this.popFileList();
//                    this.pushFileList(record.name, record.location);
//                });
//            }));
//            return true;
//        }
//        return false;
//    }

    @Override
    public boolean onBackPressed() {
        return this.popFileList();
    }

    @Override
    public @NotNull String toString() {
        return "PageFile{" +
                "activity=" + this.activity +
                ", pageCache=" + this.pageCache +
                ", stacks=" + this.stacks +
                '}';
    }
}