package com.xuxiaocheng.WListAndroid.UIs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Animatable;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.qw.soul.permission.bean.Permissions;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FilesListInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.AndroidSupports.StorageTypeGetter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.EmptyRecyclerAdapter;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileDirectoryBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileRenameBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileUploadBinding;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PageFile implements ActivityMainChooser.MainPage {
    protected final @NotNull ActivityMain activity;

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

    protected final @NotNull HInitializer<PageFileBinding> pageCache = new HInitializer<>("PageFile");
    @Override
    public @NotNull ConstraintLayout onShow() {
        final PageFileBinding cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache.getRoot();
        final PageFileBinding page = PageFileBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.initialize(page);
        page.pageFileList.setLayoutManager(new LinearLayoutManager(this.activity));
        page.pageFileList.setHasFixedSize(true);
        this.onRootPage(0);
        this.buildUploader();
        Main.runOnBackgroundThread(this.activity, () -> { // TODO auto insert.
            final BroadcastAssistant.BroadcastSet set;
            try {
                set = BroadcastAssistant.get(this.address());
            } catch (final IllegalStateException exception) {
                Main.runOnUiThread(this.activity, this.activity::close);
                return;
            }
            set.ServerClose.register(id -> Main.runOnUiThread(this.activity, this.activity::close));
            final Runnable onRoot = () -> {
                if (this.stacks.isEmpty())
                    Main.runOnUiThread(this.activity, () -> this.onRootPage(this.getCurrentPosition()));
            };
            set.ProviderInitialized.register(s -> onRoot.run());
            set.ProviderUninitialized.register(s -> onRoot.run());
            final Consumer<Pair.ImmutablePair<FileLocation, Boolean>> update = s -> {
                final FileLocation location = this.currentLocation.get();
                if (FileLocationGetter.storage(location).equals(FileLocationGetter.storage(s.getFirst()))
                        && this.currentDoubleIds.contains(FileSqlInterface.getDoubleId(FileLocationGetter.id(s.getFirst()), s.getSecond().booleanValue())))
                    this.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), this.currentLocation.get(), this.getCurrentPosition());
            };
            set.FileTrash.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
            set.FileUpdate.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
            set.FileUpload.register(s -> Main.runOnUiThread(this.activity, () -> {
                final FileLocation location = this.currentLocation.get();
                if (FileLocationGetter.storage(location).equals(s.getFirst()) && FileLocationGetter.id(location) == FileInformationGetter.parentId(s.getSecond()))
                    this.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), location, this.getCurrentPosition());
            }));
        });
        return page.getRoot();
    }

    @UiThread
    private void setLoading(final @NotNull ImageView loading) {
        if (loading.getDrawable() instanceof Animatable animatable)
            animatable.start();
    }

    private @NotNull ImageView loadingView() {
        final ImageView loading = new ImageView(this.activity);
        loading.setImageResource(R.drawable.loading);
        this.setLoading(loading);
        return loading;
    }

    @UiThread
    private @NotNull AlertDialog loadingDialog(@StringRes final int title) {
        return new AlertDialog.Builder(this.activity).setTitle(title).setView(this.loadingView()).setCancelable(false).show();
    }

    @UiThread
    private @NotNull View listLoadingView() {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, this.pageCache.getInstance().pageFileList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        this.setLoading(image);
        return loading;
    }

    @UiThread
    private @NotNull View listNoMoreView() {
        return EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, this.pageCache.getInstance().pageFileList);
    }

    private static final @NotNull HInitializer<String> listLoadingAnimationMessage = new HInitializer<>("listLoadingAnimationMessage");
    private static final @NotNull HInitializer<String> listLoadingAnimationPercent = new HInitializer<>("listLoadingAnimationPercent");
    @WorkerThread
    private void listLoadingAnimation(final boolean show, final long current, final long total) {
        final PageFileBinding page = this.pageCache.getInstance();
        PageFile.listLoadingAnimationMessage.initializeIfNot(() -> this.activity.getString(R.string.page_file_loading_text));
        PageFile.listLoadingAnimationPercent.initializeIfNot(() -> this.activity.getString(R.string.page_file_loading_percent));
        final double p = show ? total <= 0 ? 0 : ((double) current) / total : 1;
        final String percent = MessageFormat.format(PageFile.listLoadingAnimationPercent.getInstance(), p);
        final String text = MessageFormat.format(PageFile.listLoadingAnimationMessage.getInstance(), current, total);
        //noinspection NumericCastThatLosesPrecision
        final float g = ((float) p) * 0.8f + 0.1f;
        Main.runOnUiThread(this.activity, () -> {
            page.pageFileGuidelineLoaded.setGuidelinePercent(g);
            page.pageFileLoadingPercent.setText(percent);
            page.pageFileLoadingText.setText(text);
            if (show) {
                ViewUtil.fadeIn(page.pageFileLoading, 100);
                ViewUtil.fadeIn(page.pageFileLoadingBar, 100);
                ViewUtil.fadeIn(page.pageFileLoadingPercent, 200);
                ViewUtil.fadeIn(page.pageFileLoadingText, 200);
            } else {
                ViewUtil.fadeOut(page.pageFileLoading, 300);
                ViewUtil.fadeOut(page.pageFileLoadingBar, 300);
                ViewUtil.fadeOut(page.pageFileLoadingPercent, 300);
                ViewUtil.fadeOut(page.pageFileLoadingText, 300);
            }
        });
    }

    @WorkerThread
    private void listLoadingCallback(final @NotNull InstantaneousProgressState state) {
        long current = 0, total = 0;
        for (final Pair.ImmutablePair<Long, Long> pair: InstantaneousProgressStateGetter.stages(state)) {
            current += pair.getFirst().longValue();
            total += pair.getSecond().longValue();
        }
        final long c = current, t = total;
        PageFile.this.listLoadingAnimation(true, c, t);
    }

    private int getCurrentPosition() {
        final RecyclerView list = this.pageCache.getInstance().pageFileList;
        final RecyclerView.LayoutManager manager = list.getLayoutManager();
        final RecyclerView.Adapter<?> adapter = list.getAdapter();
        assert manager instanceof LinearLayoutManager;
        assert adapter instanceof EnhancedRecyclerViewAdapter<?,?>;
        return ((LinearLayoutManager) manager).findFirstVisibleItemPosition() - ((EnhancedRecyclerViewAdapter<?, ?>) adapter).headersSize();
    }


    protected final @NotNull AtomicReference<FileLocation> currentLocation = new AtomicReference<>();
    protected final @NotNull Set<Long> currentDoubleIds = ConcurrentHashMap.newKeySet();
    protected final @NotNull Deque<Triad.@NotNull ImmutableTriad<@NotNull FileLocation, @NotNull VisibleFileInformation, @NotNull AtomicLong>> stacks = new ArrayDeque<>();

    @UiThread
    private void updatePage(final @NotNull FileLocation location, final long position,
                              final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> clicker, final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> operation) {
        final PageFileBinding page = this.pageCache.getInstance();
        this.currentLocation.set(location);
        this.currentDoubleIds.clear();
        final AtomicBoolean onLoading = new AtomicBoolean(false);
        final AtomicBoolean clickable = new AtomicBoolean(true);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileList), information -> {
                    if (onLoading.get()) return;
                    if (!clickable.compareAndSet(true, false)) return;
                    if (FileInformationGetter.isDirectory(information)) {
                        final int p = PageFile.this.getCurrentPosition();
                        PageFile.this.stacks.push(Triad.ImmutableTriad.makeImmutableTriad(location, this.getData(p), new AtomicLong(p)));
                    }
                    clicker.accept(information, clickable);
                }, f -> {
                    if (clickable.compareAndSet(true, false))
                        operation.accept(f, clickable);
                });
            }
        };
        page.pageFileCounter.setVisibility(View.GONE);
        page.pageFileCounterText.setVisibility(View.GONE);
        final AtomicLong loadedUp = new AtomicLong(position);
        final AtomicLong loadedDown = new AtomicLong(position);
        final AtomicBoolean noMoreUp = new AtomicBoolean(position <= 0);
        final AtomicBoolean noMoreDown = new AtomicBoolean(false);
        final RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
            @UiThread
            @Override
            public void onScrollStateChanged(final @NotNull RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                final boolean isDown;
                if (!recyclerView.canScrollVertically(1) && !noMoreDown.get())
                    isDown = true;
                else if (!recyclerView.canScrollVertically(-1) && !noMoreUp.get())
                    isDown = false;
                else return;
                if (!onLoading.compareAndSet(false, true)) return;
                if (isDown)
                    adapter.addTailor(PageFile.this.listLoadingView());
                else
                    adapter.addHeader(PageFile.this.listLoadingView());
                Main.runOnBackgroundThread(PageFile.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
                    VisibleFilesListInformation list = null;
                    final int limit;
                    final AtomicBoolean showed = new AtomicBoolean(false);
                    try {
                        limit = ClientConfigurationSupporter.limitPerPage(ClientConfigurationSupporter.get());
                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
                        list = FilesAssistant.list(PageFile.this.address(), PageFile.this.username(), location, null, null,
                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, c -> {
                                    PageFile.this.listLoadingAnimation(true, 0, 0);
                                    showed.set(true);
                                    return true;
                                }, PageFile.this::listLoadingCallback);
                    } catch (final IllegalStateException exception) {
                        Main.runOnUiThread(PageFile.this.activity, PageFile.this.activity::close);
                        return;
                    } finally {
                        if (showed.get()) {
                            final long num = list == null ? 0 : FilesListInformationGetter.total(list);
                            PageFile.this.listLoadingAnimation(false, num, num);
                        }
                    }
                    if (list == null) {
                        Main.showToast(PageFile.this.activity, R.string.page_file_unavailable_directory);
                        Main.runOnUiThread(PageFile.this.activity, PageFile.this::popFileList);
                        return;
                    }
                    if (isDown)
                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
                    else
                        noMoreUp.set(loadedUp.get() <= 0);
                    PageFile.this.currentDoubleIds.addAll(AndroidSupporter.streamToList(FilesListInformationGetter.informationList(list).stream()
                            .map(information -> FileSqlInterface.getDoubleId(FileInformationGetter.id(information), FileInformationGetter.isDirectory(information)))));
                    final VisibleFilesListInformation l = list;
                    Main.runOnUiThread(PageFile.this.activity, () -> {
                        page.pageFileCounter.setText(String.valueOf(FilesListInformationGetter.total(l)));
                        page.pageFileCounter.setVisibility(View.VISIBLE);
                        page.pageFileCounterText.setVisibility(View.VISIBLE);
                        if (isDown)
                            adapter.addDataRange(FilesListInformationGetter.informationList(l));
                        else
                            adapter.addDataRange(0, FilesListInformationGetter.informationList(l));
                    });
                }, e -> {
                    onLoading.set(false);
                    Main.runOnUiThread(PageFile.this.activity, () -> {
                        if (isDown) {
                            if (noMoreDown.get())
                                adapter.setTailor(0, PageFile.this.listNoMoreView());
                            else
                                adapter.removeTailor(0);
                        } else
                            adapter.removeHeader(0);
                    });
                }, false));
            }
        };
        page.pageFileList.setAdapter(adapter);
        page.pageFileList.clearOnScrollListeners();
        page.pageFileList.addOnScrollListener(listener);
        listener.onScrollStateChanged(page.pageFileList, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
    }

    @UiThread
    protected void onRootPage(final long position) {
        final PageFileBinding page = this.pageCache.getInstance();
        page.pageFileBacker.setImageResource(R.drawable.backer_nonclickable);
        page.pageFileBacker.setOnClickListener(null);
        page.pageFileBacker.setClickable(false);
        page.pageFileName.setText(R.string.app_name);
        this.updatePage(new FileLocation(IdentifierNames.RootSelector, 0), position, (information, c) ->
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0), (information, c) ->
                Main.runOnBackgroundThread(this.activity, () -> {c.set(true);throw new UnsupportedOperationException("WIP");}) // TODO
        );
    }

    @UiThread
    protected void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
        final PageFileBinding page = this.pageCache.getInstance();
        page.pageFileBacker.setImageResource(R.drawable.backer);
        page.pageFileBacker.setOnClickListener(v -> this.popFileList());
        page.pageFileBacker.setClickable(true);
        page.pageFileName.setText(name);
        this.updatePage(location, position, (information, c) -> {
            if (FileInformationGetter.isDirectory(information)) {
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
                return;
            }
            Main.runOnBackgroundThread(this.activity, () -> {c.set(true);throw new UnsupportedOperationException("WIP");}); // TODO
        }, (information, c) -> {
            final PageFileOperationBinding operationBinding = PageFileOperationBinding.inflate(this.activity.getLayoutInflater());
            operationBinding.pageFileOperationName.setText(FileInformationGetter.name(information));
            final long size = FileInformationGetter.size(information);
            final String unknown = this.activity.getString(R.string.unknown);
            operationBinding.pageFileOperationSize.setText(ViewUtil.formatSizeDetail(size, unknown));
            operationBinding.pageFileOperationCreate.setText(ViewUtil.formatTime(FileInformationGetter.createTime(information), unknown));
            operationBinding.pageFileOperationUpdate.setText(ViewUtil.formatTime(FileInformationGetter.updateTime(information), unknown));
            final AlertDialog modifier = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_operation).setView(operationBinding.getRoot())
                    .setOnCancelListener(d -> c.set(true))
                    .setPositiveButton(R.string.cancel, (d, w) -> c.set(true)).create();
            final FileLocation current = new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information));
            final AtomicBoolean clickable = new AtomicBoolean(true);
            operationBinding.pageFileOperationRename.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                final PageFileRenameBinding renamer = PageFileRenameBinding.inflate(this.activity.getLayoutInflater());
                renamer.pageFileRenameName.setText(FileInformationGetter.name(information));
                if (renamer.pageFileRenameName.requestFocus()) {
                    renamer.pageFileRenameName.setSelectAllOnFocus(true);
                    renamer.pageFileRenameName.setSelection(Objects.requireNonNull(renamer.pageFileRenameName.getText()).length());
                }
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_operation_rename).setView(renamer.getRoot())
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String renamed = ViewUtil.getText(renamer.pageFileRenameName);
                            if (AndroidSupporter.isBlank(renamed) || FileInformationGetter.name(information).equals(renamed)) return;
                            final AlertDialog dialog = this.loadingDialog(R.string.page_file_operation_rename);
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
                                        ParametersMap.create().add("address", this.address()).add("information", information).add("renamed", renamed));
                                final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.rename(this.address(), this.username(), current, FileInformationGetter.isDirectory(information), renamed,
                                        Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> this.queryNotSupportedOperation()));
                                if (res == null) return;
                                if (res.isFailure())
                                    Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                                else
                                    Main.showToast(this.activity, R.string.page_file_operation_rename_success);
                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                        }).show();
            });
            operationBinding.pageFileOperationRenameIcon.setOnClickListener(u -> operationBinding.pageFileOperationRename.performClick());
            operationBinding.pageFileOperationMove.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    final FileLocation target = this.queryTargetDirectory(R.string.page_file_operation_move/*, FileInformationGetter.isDirectory(information) ? current : null*/);
                    if (target == null) return;
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Moving.",
                            ParametersMap.create().add("address", this.address()).add("information", information).add("target", target));
                    Main.runOnUiThread(this.activity, () -> {
                        final AlertDialog dialog = this.loadingDialog(R.string.page_file_operation_move);
                        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                            final AtomicBoolean queried = new AtomicBoolean(false);
                            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.move(this.address(), this.username(), current, FileInformationGetter.isDirectory(information), target, Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
                                if (queried.getAndSet(true)) return true;
                                return this.queryNotSupportedOperation();
                            }));
                            if (res == null) return;
                            if (res.isFailure())
                                Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                            else
                                Main.showToast(this.activity, R.string.page_file_operation_move_success);
                        }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                    });
                }));
            });
            operationBinding.pageFileOperationMoveIcon.setOnClickListener(u -> operationBinding.pageFileOperationMove.performClick());
            operationBinding.pageFileOperationCopy.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    final FileLocation target = this.queryTargetDirectory(R.string.page_file_operation_copy/*, FileInformationGetter.isDirectory(information) ? current : null*/);
                    if (target == null) return;
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Copying.",
                            ParametersMap.create().add("address", this.address()).add("information", information).add("target", target));
                    Main.runOnUiThread(this.activity, () -> {
                        final AlertDialog dialog = this.loadingDialog(R.string.page_file_operation_copy);
                        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                            final AtomicBoolean queried = new AtomicBoolean(false);
                            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copy(this.address(), this.username(), current, FileInformationGetter.isDirectory(information), target, FileInformationGetter.name(information), Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
                                if (queried.getAndSet(true)) return true;
                                return this.queryNotSupportedOperation();
                            }));
                            if (res == null) return;
                            if (res.isFailure())
                                Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                            else
                                Main.showToast(this.activity, R.string.page_file_operation_copy_success);
                        }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                    });
                }));
            });
            operationBinding.pageFileOperationCopyIcon.setOnClickListener(u -> operationBinding.pageFileOperationCopy.performClick());
            operationBinding.pageFileOperationDelete.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_operation_delete)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final AlertDialog dialog = this.loadingDialog(R.string.page_file_operation_delete);
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
                                        ParametersMap.create().add("address", this.address()).add("information", information));
                                if (FilesAssistant.trash(this.address(), this.username(), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), FileInformationGetter.isDirectory(information), HExceptionWrapper.wrapPredicate(unused -> this.queryNotSupportedOperation())))
                                    Main.showToast(this.activity, R.string.page_file_operation_delete_success);
                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                        }).show();
            });
            operationBinding.pageFileOperationDeleteIcon.setOnClickListener(u -> operationBinding.pageFileOperationDelete.performClick());
            if (FileInformationGetter.isDirectory(information)) {
                operationBinding.pageFileOperationDownload.setVisibility(View.GONE);
                operationBinding.pageFileOperationDownloadIcon.setVisibility(View.GONE);
            } else {
                operationBinding.pageFileOperationDownload.setOnClickListener(u -> {
                    if (!clickable.compareAndSet(true, false)) return;
                    modifier.cancel();
                    new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_operation_download)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm, (d, w) -> {
                                final AlertDialog loader = new AlertDialog.Builder(this.activity).setTitle(FileInformationGetter.name(information)).setCancelable(false).show();
                                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                    final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist/" + FileInformationGetter.name(information));
                                    PermissionUtil.tryGetPermission(this.activity, Permissions.build(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE));
                                    HFileHelper.ensureFileAccessible(file, true);
                                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
                                            ParametersMap.create().add("address", this.address()).add("information", information).add("file", file));
                                    final VisibleFailureReason res;
                                    try {
                                        PageFile.this.listLoadingAnimation(true, 0, 0); // TODO: download progress.
                                        res = FilesAssistant.download(this.address(), this.username(), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), file, PredicateE.truePredicate(), s -> {
                                            long curr = 0, total = 0;
                                            for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                                curr += pair.getFirst().longValue();
                                                total += pair.getSecond().longValue();
                                            }
                                            final long l = curr, t = total;
                                            PageFile.this.listLoadingAnimation(true, l, t);
                                        });
                                    } finally {
                                        PageFile.this.listLoadingAnimation(false, 0, 0);
                                    }
                                    if (res != null)
                                        Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res) + FailureReasonGetter.message(res), Toast.LENGTH_SHORT).show());
                                    else
                                        Main.showToast(this.activity, R.string.page_file_operation_download_success);
                                }, () -> Main.runOnUiThread(this.activity, loader::cancel)));
                            }).show();
                });
                operationBinding.pageFileOperationDownloadIcon.setOnClickListener(u -> operationBinding.pageFileOperationDownload.performClick());
            }
            modifier.show();
        });
    }

    @WorkerThread
    public boolean queryNotSupportedOperation() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean continuer = new AtomicBoolean(false);
        Main.runOnUiThread(this.activity, () -> new AlertDialog.Builder(this.activity)
                .setTitle(R.string.page_file_operation_complex)
                .setOnCancelListener(a -> latch.countDown())
                .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
                .setPositiveButton(R.string.confirm, (a, k) -> Main.runOnBackgroundThread(this.activity, () -> {
                    continuer.set(true);
                    latch.countDown();
                })).show());
        latch.await();
        return continuer.get();
    }

    @WorkerThread
    public @Nullable FileLocation queryTargetDirectory(@StringRes final int title/*, final @Nullable FileLocation current*/) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<FileLocation> result = new AtomicReference<>();
        final PageFile page = new PageFile(this.activity); // TODO
        Main.runOnUiThread(this.activity, () -> {
            final View p = this.pageCache.getInstance().getRoot();
            new AlertDialog.Builder(this.activity)
                    .setTitle(title).setView(page.onShow())
                    .setOnCancelListener(a -> latch.countDown())
                    .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
                    .setPositiveButton(R.string.confirm, (a, k) -> {
                        result.set(page.currentLocation.get());
                        latch.countDown();
                    }).show()
                    .getWindow().setLayout(p.getWidth(), p.getHeight());
            page.pageCache.getInstance().getRoot().setLayoutParams(new FrameLayout.LayoutParams(p.getWidth(), p.getHeight()));
            page.pageCache.getInstance().pageFileUploader.setVisibility(View.GONE);
        });
        latch.await();
        return result.get();
    }

    @UiThread
    protected boolean popFileList() {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> p = this.stacks.poll();
        if (p == null) return false;
        final PageFileBinding page = this.pageCache.getInstance();
        page.pageFileBacker.setClickable(false);
        page.pageFileCounter.setVisibility(View.GONE);
        page.pageFileCounterText.setVisibility(View.GONE);
        page.pageFileList.clearOnScrollListeners();
        page.pageFileList.setAdapter(EmptyRecyclerAdapter.Instance);
        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
            final VisibleFileInformation directory;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
                directory = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(this.address(), this.username()), p.getA(), true);
            }
            Main.runOnUiThread(this.activity, () -> {
                if (directory == null)
                    this.popFileList();
                else if (IdentifierNames.RootSelector.equals(FileInformationGetter.name(directory)))
                    this.onRootPage(p.getC().get());
                else
                    this.onInsidePage(FileInformationGetter.name(directory), p.getA(), p.getC().get());
            });
        }));
        return true;
    }

    protected final @NotNull HInitializer<ActivityResultLauncher<String>> chooserLauncher = new HInitializer<>("PageFileChooserLauncher");

    @SuppressWarnings("unchecked")
    @SuppressLint("ClickableViewAccessibility")
    private <C extends StorageConfiguration> void buildUploader() {
        final PageFileBinding page = this.pageCache.getInstance();
        final AtomicBoolean scrolling = new AtomicBoolean();
        final AtomicInteger startX = new AtomicInteger(), startY = new AtomicInteger();
        page.pageFileUploader.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    scrolling.set(false);
                    startX.set(Float.floatToIntBits(v.getX()));
                    startY.set(Float.floatToIntBits(v.getY()));
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (scrolling.get()) {
                        final float parentX = page.pageFileList.getX(), parentY = page.pageFileList.getY();
                        v.setX(HMathHelper.clamp(v.getX() + e.getX() - parentX, 0, page.pageFileList.getWidth()) + parentX - v.getWidth() / 2.0f);
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, -50, page.pageFileList.getHeight()) + parentY - v.getHeight() / 2.0f);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f)
                        scrolling.set(true);
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get())
                        PageFile.this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
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
                x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileUploader.getWidth()) * 0.7f);
                y = preferences.getFloat("y", displayMetrics.heightPixels * 0.7f);
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            }
            Main.runOnUiThread(this.activity, () -> {
                page.pageFileUploader.setX(x);
                page.pageFileUploader.setY(y);
                page.pageFileUploader.setVisibility(View.VISIBLE);
            }, 300, TimeUnit.MILLISECONDS);
        });
        page.pageFileUploader.setOnClickListener(u -> {
            final BottomSheetDialog dialog = new BottomSheetDialog(this.activity, R.style.BottomSheetDialog);
            final PageFileUploadBinding uploader = PageFileUploadBinding.inflate(this.activity.getLayoutInflater());
            uploader.pageFileUploadCancel.setOnClickListener(v -> dialog.cancel());
            final AtomicBoolean clickable = new AtomicBoolean(true);
            uploader.pageFileUploadStorageImage.setOnClickListener(v -> {
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                final String[] storages = StorageTypeGetter.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
                final AtomicInteger choice = new AtomicInteger(-1);
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_create_storage)
                        .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            if (choice.get() == -1) return;
                            final String identifier = storages[choice.get()];
                            final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypeGetter.get(identifier));
                            PageFileProviderConfigurations.getConfiguration(PageFile.this.activity, type, null, configuration -> Main.runOnUiThread(this.activity, () -> {
                                final AlertDialog loading = new AlertDialog.Builder(PageFile.this.activity)
                                        .setTitle(R.string.page_file_create_storage).setView(this.loadingView())
                                        .setCancelable(false).show();
                                Main.runOnBackgroundThread(PageFile.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(PageFile.this.address())) {
                                        OperateProvidersHelper.addProvider(client, TokenAssistant.getToken(PageFile.this.address(), PageFile.this.username()),
                                                configuration.getName(), type, configuration);
                                    }
                                }, () -> Main.runOnUiThread(PageFile.this.activity, loading::cancel)));
                            }));
                        }).show();
            });
            uploader.pageFileUploadStorageText.setOnClickListener(v -> uploader.pageFileUploadStorageImage.performClick());
            uploader.pageFileUploadDirectoryImage.setOnClickListener(v -> {
                if (this.stacks.isEmpty()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                final FileLocation location = this.currentLocation.get();
                final PageFileDirectoryBinding editor = PageFileDirectoryBinding.inflate(this.activity.getLayoutInflater());
                editor.pageFileDirectoryName.setText(R.string.page_file_upload_directory_name);
                editor.pageFileDirectoryName.setHint(R.string.page_file_upload_directory_hint);
                if (editor.pageFileDirectoryName.requestFocus()) {
                    editor.pageFileDirectoryName.setSelectAllOnFocus(true);
                    editor.pageFileDirectoryName.setSelection(Objects.requireNonNull(editor.pageFileDirectoryName.getText()).length());
                }
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_create_directory)
                        .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String name = ViewUtil.getText(editor.pageFileDirectoryName);
                            final AlertDialog loading = new AlertDialog.Builder(this.activity)
                                    .setTitle(R.string.page_file_create_directory).setView(this.loadingView()).setCancelable(false).show();
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
                                        ParametersMap.create().add("address", this.address()).add("location", location).add("name", name));
                                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
                                    OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(this.address(), this.username()), location, name, Options.DuplicatePolicy.ERROR);
                                }
                                Main.showToast(this.activity, R.string.page_file_upload_success_directory);
                            }, () -> Main.runOnUiThread(this.activity, loading::cancel)));
                        }).show();
            });
            uploader.pageFileUploadDirectoryText.setOnClickListener(v -> uploader.pageFileUploadDirectoryImage.performClick());
            uploader.pageFileUploadFileImage.setOnClickListener(v -> {
                if (this.stacks.isEmpty()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("*/*");
            });
            uploader.pageFileUploadFileText.setOnClickListener(v -> uploader.pageFileUploadFileImage.performClick());
            uploader.pageFileUploadPictureImage.setOnClickListener(v -> {
                if (this.stacks.isEmpty()) return;
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("image/*");
            });
            uploader.pageFileUploadPictureText.setOnClickListener(v -> uploader.pageFileUploadPictureImage.performClick());
            uploader.pageFileUploadVideoImage.setOnClickListener(v -> {
                if (this.stacks.isEmpty()) return;
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
        this.chooserLauncher.reinitialize(this.activity.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                final FileLocation location = this.currentLocation.get();
                // TODO: serialize uploading task.
                // uri.toString()  -->  Uri.parse(...)
                final String filename;
                final long size;
                try (final Cursor cursor = this.activity.getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                    if (cursor == null || !cursor.moveToFirst()) return;
                    filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
                }
                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading files.",
                        ParametersMap.create().add("address", this.address()).add("location", location).add("filename", filename).add("size", size).add("uri", uri));
                this.listLoadingAnimation(true, 0, 0);
                Main.runOnUiThread(this.activity, () -> {
                    final AlertDialog loader = new AlertDialog.Builder(this.activity).setTitle(filename).setCancelable(false).show();
                    Main.runOnUiThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.uploadStream(this.address(), this.username(), HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
                            try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
                                AndroidSupporter.skipNBytes(stream, pair.getFirst().longValue());
                                consumer.accept(stream);
                            }
                        }), size, filename, location, PredicateE.truePredicate(), s -> { // TODO upload rogress.
                            long current = 0, total = 0;
                            for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                current += pair.getFirst().longValue();
                                total += pair.getSecond().longValue();
                            }
                            final long c = current, t = total;
                            PageFile.this.listLoadingAnimation(true, c, t);
                        });
                        assert res != null;
                        if (res.isFailure()) // TODO
                            Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                        else
                            Main.showToast(this.activity, R.string.page_file_upload_success_file);
                    }, () -> {
                        this.listLoadingAnimation(false, 0, 0);
                        Main.runOnUiThread(this.activity, loader::cancel);
                    }));
                });
            }));
        }));
        final ImageView options = this.activity.findViewById(R.id.activity_main_options);
        options.setOnClickListener(v -> {
            if (this.activity.currentChoice.get() != ActivityMainChooser.MainChoice.File) return; // TODO
            final ListPopupWindow popup = new ListPopupWindow(this.activity);
            popup.setWidth(this.pageCache.getInstance().pageFileList.getWidth() >> 1);
            popup.setAnchorView(options);
            popup.setAdapter(new SimpleAdapter(this.activity, List.of(
                    Map.of("image", R.drawable.page_file_refresh, "name", this.activity.getResources().getString(R.string.page_file_options_refresh)),
                    Map.of("image", R.drawable.page_file_sorter, "name", this.activity.getResources().getString(R.string.page_file_options_sorter)),
                    Map.of("image", R.drawable.page_file_filter, "name", this.activity.getResources().getString(R.string.page_file_options_filter))
            ), R.layout.page_file_options_cell, new String[]{"image", "name"},
                    new int[]{R.id.activity_main_options_cell_image, R.id.activity_main_options_cell_name}));
            popup.setOnItemClickListener((p, w, pos, i) -> {
                popup.dismiss();
                if (pos == 0) { // Refresh.
                    final FileLocation location = this.currentLocation.get();
                    Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        this.listLoadingAnimation(true, 0, 0);
                        FilesAssistant.refresh(this.address(), this.username(), location, Main.ClientExecutors, this::listLoadingCallback);
                    }, () -> this.listLoadingAnimation(false, 0, 0)));
                }
                if (pos == 1) { // Sort
                    // TODO
                }
                if (pos == 2) { // Filter
                    // TODO
                }
            });
            popup.show();
        });
    }

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
