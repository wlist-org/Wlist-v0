package com.xuxiaocheng.WListAndroid.UIs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
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
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListAndroid.Helpers.PermissionsHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.EmptyRecyclerAdapter;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileDirectoryBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOptionBinding;
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
import java.util.LinkedHashMap;
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
            final BroadcastAssistant.BroadcastSet set = BroadcastAssistant.get(this.address());
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
    private static void setLoading(final @NotNull ImageView loading) {
        final Animation loadingAnimation = new RotateAnimation(0, 360 << 10, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        loadingAnimation.setDuration(500 << 10);
        loadingAnimation.setInterpolator(new LinearInterpolator());
        loadingAnimation.setRepeatCount(Animation.INFINITE);
        loading.startAnimation(loadingAnimation);
    }

    @UiThread
    private @NotNull AlertDialog loadingDialog(@StringRes final int title) {
        final ImageView loading = new ImageView(this.activity);
        loading.setImageResource(R.mipmap.page_file_loading);
        PageFile.setLoading(loading);
        return new AlertDialog.Builder(this.activity).setTitle(title).setView(loading).setCancelable(false).show();
    }

    @UiThread
    private @NotNull View listLoadingView() {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, this.pageCache.getInstance().pageFileList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        PageFile.setLoading(image);
        return loading;
    }

    @UiThread
    private @NotNull View listNoMoreView() {
        return EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, this.pageCache.getInstance().pageFileList);
    }

    private static final @NotNull HInitializer<String> listLoadingAnimationPattern = new HInitializer<>("ListLoadingAnimationPattern");
    @UiThread
    private void listLoadingAnimation(final boolean show, final long current, final long total) {
        final PageFileBinding page = this.pageCache.getInstance();
        PageFile.listLoadingAnimationPattern.initializeIfNot(() -> this.activity.getString(R.string.page_file_loading_text));
        page.pageFileLoadingText.setText(MessageFormat.format(PageFile.listLoadingAnimationPattern.getInstance(), current, total));
        if (show) {
            page.pageFileLoading.setVisibility(View.VISIBLE);
            page.pageFileLoadingText.setVisibility(View.VISIBLE);
            PageFile.setLoading(page.pageFileLoading);
        } else {
            page.pageFileLoading.setVisibility(View.GONE);
            page.pageFileLoadingText.setVisibility(View.GONE);
            page.pageFileLoading.clearAnimation();
        }
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
                              final @NotNull Consumer<? super @NotNull VisibleFileInformation> clicker, final @NotNull Consumer<? super @NotNull VisibleFileInformation> option) {
        final PageFileBinding page = this.pageCache.getInstance();
        this.currentLocation.set(location);
        this.currentDoubleIds.clear();
        final AtomicBoolean onLoading = new AtomicBoolean(false);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(PageFile.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileList), information -> {
                    if (onLoading.get())
                        return;
                    final int p = PageFile.this.getCurrentPosition();
                    PageFile.this.stacks.push(Triad.ImmutableTriad.makeImmutableTriad(location, this.getData(p), new AtomicLong(p)));
                    clicker.accept(information);
                }, option::accept);
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
                    final VisibleFilesListInformation list;
                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
                    final int limit;
                    Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, 0, 0));
                    try {
                        final ClientConfiguration configuration = ClientConfigurationSupporter.get();
                        final Options.FilterPolicy filter = ClientConfigurationSupporter.filterPolicy(configuration);
                        final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders = ClientConfigurationSupporter.fileOrders(configuration);
                        limit = ClientConfigurationSupporter.limitPerPage(configuration);
                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
                        list = FilesAssistant.list(PageFile.this.address(), PageFile.this.username(), location, filter, orders,
                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, s -> {
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
                    if (isDown)
                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
                    else
                        noMoreUp.set(loadedUp.get() <= 0);
                    PageFile.this.currentDoubleIds.addAll(AndroidSupporter.streamToList(FilesListInformationGetter.informationList(list).stream()
                            .map(information -> FileSqlInterface.getDoubleId(FileInformationGetter.id(information), FileInformationGetter.isDirectory(information)))));
                    Main.runOnUiThread(PageFile.this.activity, () -> {
                        page.pageFileCounter.setText(String.valueOf(FilesListInformationGetter.total(list)));
                        page.pageFileCounter.setVisibility(View.VISIBLE);
                        page.pageFileCounterText.setVisibility(View.VISIBLE);
                        if (isDown)
                            adapter.addDataRange(FilesListInformationGetter.informationList(list));
                        else
                            adapter.addDataRange(0, FilesListInformationGetter.informationList(list));
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
        page.pageFileBacker.setImageResource(R.mipmap.backer_nonclickable);
        page.pageFileBacker.setOnClickListener(null);
        page.pageFileBacker.setClickable(false);
        page.pageFileName.setText(R.string.app_name);
        this.updatePage(new FileLocation(IdentifierNames.RootSelector, 0), position, information ->
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0), information ->
                Main.runOnBackgroundThread(this.activity, () -> {throw new UnsupportedOperationException("WIP");}) // TODO
        );
    }

    @UiThread
    protected void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
        final PageFileBinding page = this.pageCache.getInstance();
        page.pageFileBacker.setImageResource(R.mipmap.backer);
        page.pageFileBacker.setOnClickListener(v -> this.popFileList());
        page.pageFileBacker.setClickable(true);
        page.pageFileName.setText(name);
        this.updatePage(location, position, information -> {
            if (FileInformationGetter.isDirectory(information))
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
            else
                Main.runOnBackgroundThread(this.activity, () -> {throw new UnsupportedOperationException("WIP");}); // TODO
        }, information -> {
            final PageFileOptionBinding optionBinding = PageFileOptionBinding.inflate(this.activity.getLayoutInflater());
            optionBinding.pageFileOptionName.setText(FileInformationGetter.name(information));
            final long size = FileInformationGetter.size(information);
            final String unknown = this.activity.getString(R.string.unknown);
            optionBinding.pageFileOptionSize.setText(ViewUtil.formatSizeDetail(size, unknown));
            optionBinding.pageFileOptionCreate.setText(ViewUtil.formatTime(FileInformationGetter.createTime(information), unknown));
            optionBinding.pageFileOptionUpdate.setText(ViewUtil.formatTime(FileInformationGetter.updateTime(information), unknown));
            final AlertDialog modifier = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_option).setView(optionBinding.getRoot())
                    .setPositiveButton(R.string.cancel, null).create();
            final FileLocation current = new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information));
            final AtomicBoolean clickable = new AtomicBoolean(true);
            optionBinding.pageFileOptionRename.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                final PageFileRenameBinding renamer = PageFileRenameBinding.inflate(this.activity.getLayoutInflater());
                renamer.pageFileRenameName.setText(FileInformationGetter.name(information));
                if (renamer.pageFileRenameName.requestFocus()) {
                    renamer.pageFileRenameName.setSelectAllOnFocus(true);
                    renamer.pageFileRenameName.setSelection(Objects.requireNonNull(renamer.pageFileRenameName.getText()).length());
                }
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_option_rename).setView(renamer.getRoot())
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String renamed = ViewUtil.getText(renamer.pageFileRenameName);
                            if (AndroidSupporter.isBlank(renamed) || FileInformationGetter.name(information).equals(renamed)) return;
                            final AlertDialog dialog = this.loadingDialog(R.string.page_file_option_rename);
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
                                        ParametersMap.create().add("address", this.address()).add("information", information).add("renamed", renamed));
                                final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.rename(this.address(), this.username(), current, FileInformationGetter.isDirectory(information), renamed,
                                        Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> this.queryNotSupportedOperation()));
                                if (res == null) return;
                                if (res.isFailure())
                                    Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                                else
                                    Main.showToast(this.activity, R.string.page_file_option_rename_success);
                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                        }).show();
            });
            optionBinding.pageFileOptionRenameIcon.setOnClickListener(u -> optionBinding.pageFileOptionRename.performClick());
            optionBinding.pageFileOptionMove.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    final FileLocation target = this.queryTargetDirectory(R.string.page_file_option_move/*, FileInformationGetter.isDirectory(information) ? current : null*/);
                    if (target == null) return;
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Moving.",
                            ParametersMap.create().add("address", this.address()).add("information", information).add("target", target));
                    Main.runOnUiThread(this.activity, () -> {
                        final AlertDialog dialog = this.loadingDialog(R.string.page_file_option_move);
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
                                Main.showToast(this.activity, R.string.page_file_option_move_success);
                        }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                    });
                }));
            });
            optionBinding.pageFileOptionMoveIcon.setOnClickListener(u -> optionBinding.pageFileOptionMove.performClick());
            optionBinding.pageFileOptionCopy.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                    final FileLocation target = this.queryTargetDirectory(R.string.page_file_option_copy/*, FileInformationGetter.isDirectory(information) ? current : null*/);
                    if (target == null) return;
                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Copying.",
                            ParametersMap.create().add("address", this.address()).add("information", information).add("target", target));
                    Main.runOnUiThread(this.activity, () -> {
                        final AlertDialog dialog = this.loadingDialog(R.string.page_file_option_copy);
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
                                Main.showToast(this.activity, R.string.page_file_option_copy_success);
                        }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                    });
                }));
            });
            optionBinding.pageFileOptionCopyIcon.setOnClickListener(u -> optionBinding.pageFileOptionCopy.performClick());
            optionBinding.pageFileOptionDelete.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_option_delete)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final AlertDialog dialog = this.loadingDialog(R.string.page_file_option_delete);
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
                                        ParametersMap.create().add("address", this.address()).add("information", information));
                                if (FilesAssistant.trash(this.address(), this.username(), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), FileInformationGetter.isDirectory(information), HExceptionWrapper.wrapPredicate(unused -> this.queryNotSupportedOperation())))
                                    Main.showToast(this.activity, R.string.page_file_option_delete_success);
                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                        }).show();
            });
            optionBinding.pageFileOptionDeleteIcon.setOnClickListener(u -> optionBinding.pageFileOptionDelete.performClick());
            if (FileInformationGetter.isDirectory(information)) {
                optionBinding.pageFileOptionDownload.setVisibility(View.GONE);
                optionBinding.pageFileOptionDownloadIcon.setVisibility(View.GONE);
            } else {
                optionBinding.pageFileOptionDownload.setOnClickListener(u -> {
                    if (!clickable.compareAndSet(true, false)) return;
                    modifier.cancel();
                    new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_option_download)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm, (d, w) -> {
                                final ImageView loading = new ImageView(this.activity);
                                loading.setImageResource(R.mipmap.page_file_loading);
                                PageFile.setLoading(loading);
                                final AlertDialog loader = new AlertDialog.Builder(this.activity).setTitle(FileInformationGetter.name(information)).setCancelable(false).show();
                                Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                    final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist/" + FileInformationGetter.name(information));
                                    HFileHelper.ensureFileAccessible(file, true);
                                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
                                            ParametersMap.create().add("address", this.address()).add("information", information).add("file", file));
                                    final VisibleFailureReason res;
                                    try {
                                        Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, 0, 0));
                                        res = FilesAssistant.download(this.address(), this.username(), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), file, PredicateE.truePredicate(), s -> {
                                            long c = 0, total = 0;
                                            for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                                c += pair.getFirst().longValue();
                                                total += pair.getSecond().longValue();
                                            }
                                            final long l = c, t = total;
                                            Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, l, t));
                                        });
                                    } finally {
                                        Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(false, 0, 0));
                                    }
                                    if (res != null)
                                        Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.kind(res) + FailureReasonGetter.message(res), Toast.LENGTH_SHORT).show());
                                    else
                                        Main.showToast(this.activity, R.string.page_file_option_download_success);
                                }, () -> Main.runOnUiThread(this.activity, loader::cancel)));
                            }).show();
                });
                optionBinding.pageFileOptionDownloadIcon.setOnClickListener(u -> optionBinding.pageFileOptionDownload.performClick());
            }
            modifier.show();
        });
    }

    @WorkerThread
    public boolean queryNotSupportedOperation() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean continuer = new AtomicBoolean(false);
        Main.runOnUiThread(this.activity, () -> new AlertDialog.Builder(this.activity)
                .setTitle(R.string.page_file_option_rename_complex)
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
        this.listLoadingAnimation(true, 0, 0);
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
            if (this.stacks.isEmpty()) { // Root selector
                final String[] storages = StorageTypeGetter.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
                final AtomicInteger choice = new AtomicInteger(-1);
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_provider_add)
                        .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            if (choice.get() == -1) return;
                            final String identifier = storages[choice.get()];
                            final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypeGetter.get(identifier));
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
            final PageFileUploadBinding upload = PageFileUploadBinding.inflate(this.activity.getLayoutInflater());
            final AlertDialog uploader = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_upload).setView(upload.getRoot())
                    .setPositiveButton(R.string.cancel, null).create();
            final AtomicBoolean clickable = new AtomicBoolean(true);
            upload.pageFileUploadDirectory.setOnClickListener(v -> {
                if (!clickable.compareAndSet(true, false)) return;
                uploader.cancel();
                final FileLocation location = this.currentLocation.get();
                final PageFileDirectoryBinding editor = PageFileDirectoryBinding.inflate(this.activity.getLayoutInflater());
                editor.pageFileDirectoryName.setText(R.string.page_file_upload_directory_name);
                editor.pageFileDirectoryName.setHint(R.string.page_file_upload_directory_hint);
                if (editor.pageFileDirectoryName.requestFocus()) {
                    editor.pageFileDirectoryName.setSelectAllOnFocus(true);
                    editor.pageFileDirectoryName.setSelection(Objects.requireNonNull(editor.pageFileDirectoryName.getText()).length());
                }
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_upload_directory)
                        .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String name = ViewUtil.getText(editor.pageFileDirectoryName);
                            final ImageView loading = new ImageView(this.activity);
                            loading.setImageResource(R.mipmap.page_file_loading);
                            PageFile.setLoading(loading);
                            final AlertDialog loader = new AlertDialog.Builder(this.activity)
                                    .setTitle(R.string.page_file_upload_directory).setView(loading).setCancelable(false).show();
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
                                        ParametersMap.create().add("address", this.address()).add("location", location).add("name", name));
                                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
                                    OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(this.address(), this.username()), location, name, Options.DuplicatePolicy.ERROR);
                                }
                                Main.showToast(this.activity, R.string.page_file_upload_success_directory);
                            }, () -> Main.runOnUiThread(this.activity, loader::cancel)));
                        }).show();
            });
            upload.pageFileUploadDirectoryText.setOnClickListener(v -> upload.pageFileUploadDirectory.performClick());
            final Consumer<String> uploadFile = pattern -> {
                if (!clickable.compareAndSet(true, false)) return;
                uploader.cancel();
                PermissionsHelper.getExternalStorage(this.activity, false, success -> {
                    if (success.booleanValue())
                        this.chooserLauncher.getInstance().launch(pattern);
                    else
                        Main.showToast(this.activity, R.string.toast_no_read_permissions);
                });
            };
            upload.pageFileUploadFile.setOnClickListener(v -> uploadFile.accept("*/*"));
            upload.pageFileUploadFileText.setOnClickListener(v -> upload.pageFileUploadFile.performClick());
            upload.pageFileUploadPicture.setOnClickListener(v -> uploadFile.accept("image/*"));
            upload.pageFileUploadPictureText.setOnClickListener(v -> upload.pageFileUploadPicture.performClick());
            upload.pageFileUploadVideo.setOnClickListener(v -> uploadFile.accept("video/*"));
            upload.pageFileUploadVideoText.setOnClickListener(v -> upload.pageFileUploadVideo.performClick());
            uploader.show();
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
                Main.runOnUiThread(this.activity, () -> {
                    final ImageView loading = new ImageView(this.activity);
                    loading.setImageResource(R.mipmap.page_file_loading);
                    PageFile.setLoading(loading);
                    final AlertDialog loader = new AlertDialog.Builder(this.activity).setTitle(filename).setCancelable(false).show();
                    this.listLoadingAnimation(true, 0, 0);
                    Main.runOnUiThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.uploadStream(this.address(), this.username(), HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
                            try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
                                AndroidSupporter.skipNBytes(stream, pair.getFirst().longValue());
                                consumer.accept(stream);
                            }
                        }), size, filename, location, c -> true, s -> { // TODO
                            long current = 0, total = 0;
                            for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                current += pair.getFirst().longValue();
                                total += pair.getSecond().longValue();
                            }
                            final long c = current, t = total;
                            Main.runOnUiThread(PageFile.this.activity, () -> PageFile.this.listLoadingAnimation(true, c, t));
                        });
                        assert res != null;
                        if (res.isFailure()) // TODO
                            Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                        else
                            Main.showToast(this.activity, R.string.page_file_upload_success_file);
                    }, () -> Main.runOnUiThread(this.activity, () -> {
                        loader.cancel();
                        this.listLoadingAnimation(false, 0, 0);
                    })));
                });
            }));
        }));
        final ImageView options = this.activity.findViewById(R.id.activity_main_options);
        options.setOnClickListener(v -> {
            if (this.activity.currentChoice.get() != ActivityMainChooser.MainChoice.File) return;
            final ListPopupWindow popup = new ListPopupWindow(this.activity);
            popup.setWidth(this.pageCache.getInstance().pageFileList.getWidth() >> 1);
            popup.setAnchorView(options);
            popup.setAdapter(new SimpleAdapter(this.activity, List.of(
                    Map.of("image", R.mipmap.app_logo_round, "name", this.activity.getResources().getString(R.string.page_file_options_refresh))
            ), R.layout.activity_main_options_cell, new String[]{"image", "name"},
                    new int[]{R.id.activity_main_options_cell_image, R.id.activity_main_options_cell_name}));
            popup.setOnItemClickListener((p, w, pos, i) -> {
                popup.dismiss();
                if (pos == 0) { // Refresh.
                    final FileLocation location = this.currentLocation.get();
                    this.listLoadingAnimation(true, 0, 0);
                    Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> FilesAssistant.refresh(this.address(), this.username(), location, Main.ClientExecutors, s -> { // TODO
                        long current = 0, total = 0;
                        for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                            current += pair.getFirst().longValue();
                            total += pair.getSecond().longValue();
                        }
                        final long c = current, t = total;
                        Main.runOnUiThread(PageFile.this.activity, () -> this.listLoadingAnimation(true, c, t));
                    }), () -> Main.runOnUiThread(this.activity, () -> this.listLoadingAnimation(false, 0, 0))));
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
