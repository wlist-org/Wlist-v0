package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FilesListInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.EmptyRecyclerAdapter;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@SuppressWarnings("ClassHasNoToStringMethod")
public class PageFilePartList {
    protected final @NotNull PageFile pageFile;

    public PageFilePartList(final @NotNull PageFile pageFile) {
        super();
        this.pageFile = pageFile;
    }

    private @NotNull ActivityMain activity() {
        return this.pageFile.activity();
    }
    private @NotNull PageFileBinding page() {
        return this.pageFile.getPage();
    }
    private @NotNull InetSocketAddress address() {
        return this.activity().address();
    }
    private @NotNull String username() {
        return this.activity().username();
    }


    @UiThread
    private @NotNull View listLoadingView() {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(this.activity().getLayoutInflater(), R.layout.page_file_tailor_loading, this.page().pageFileList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        ViewUtil.startDrawableAnimation(image);
        return loading;
    }
    
    @UiThread
    private @NotNull View listNoMoreView() {
        return EnhancedRecyclerViewAdapter.buildView(this.activity().getLayoutInflater(), R.layout.page_file_tailor_no_more, this.page().pageFileList);
    }

    private static final @NotNull HInitializer<String> listLoadingAnimationMessage = new HInitializer<>("PageFileListLoadingAnimationMessage");
    private static final @NotNull HInitializer<String> listLoadingAnimationPercent = new HInitializer<>("PageFileListLoadingAnimationPercent");
    @WorkerThread
    protected void listLoadingAnimation(final boolean show, final long current, final long total) {
        final PageFileBinding page = this.page();
        PageFilePartList.listLoadingAnimationMessage.initializeIfNot(() -> this.activity().getString(R.string.page_file_loading_text));
        PageFilePartList.listLoadingAnimationPercent.initializeIfNot(() -> this.activity().getString(R.string.page_file_loading_percent));
        final double percent = total <= 0 ? show ? 0 : 1 : ((double) current) / total;
        final String textPercent = MessageFormat.format(PageFilePartList.listLoadingAnimationPercent.getInstance(), percent);
        final String textMessage = MessageFormat.format(PageFilePartList.listLoadingAnimationMessage.getInstance(), current, total);
        //noinspection NumericCastThatLosesPrecision
        final float guidelinePercent = ((float) percent) * 0.8f + 0.1f;
        Main.runOnUiThread(this.activity(), () -> {
            page.pageFileGuidelineLoaded.setGuidelinePercent(guidelinePercent);
            page.pageFileLoadingPercent.setText(textPercent);
            page.pageFileLoadingText.setText(textMessage);
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
        final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
        this.listLoadingAnimation(true, pair.getFirst().longValue(), pair.getSecond().longValue());
    }

    protected int getCurrentPosition() {
        final RecyclerView list = this.page().pageFileList;
        final RecyclerView.LayoutManager manager = list.getLayoutManager();
        final RecyclerView.Adapter<?> adapter = list.getAdapter();
        assert manager instanceof LinearLayoutManager;
        assert adapter instanceof EnhancedRecyclerViewAdapter<?,?>;
        return ((LinearLayoutManager) manager).findFirstVisibleItemPosition() - ((EnhancedRecyclerViewAdapter<?, ?>) adapter).headersSize();
    }


    private final @NotNull AtomicReference<FileLocation> currentLocation = new AtomicReference<>();
    private final @NotNull Deque<Triad.@NotNull ImmutableTriad<@NotNull FileLocation, @NotNull VisibleFileInformation, @NotNull AtomicLong>> stacks = new ArrayDeque<>();

    protected boolean isOnRoot() {
        final FileLocation location = this.currentLocation.get();
        return location == null || IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location));
    }

    protected @NotNull FileLocation currentLocation() {
        return this.currentLocation.get();
    }

    @WorkerThread
    protected void listenBroadcast(final BroadcastAssistant.@NotNull BroadcastSet set) {
//         TODO auto insert.
//        final Runnable onRoot = () -> {
//            if (this.stacks.isEmpty())
//                Main.runOnUiThread(this.activity(), () -> this.onRootPage(this.getCurrentPosition()));
//        };
//        set.ProviderInitialized.register(s -> onRoot.run());
//        set.ProviderUninitialized.register(s -> onRoot.run());
//        final Consumer<Pair.ImmutablePair<FileLocation, Boolean>> update = s -> {
//            final FileLocation location = this.currentLocation.get();
//            if (FileLocationGetter.storage(location).equals(FileLocationGetter.storage(s.getFirst()))
//                    && this.currentDoubleIds.contains(FileSqlInterface.getDoubleId(FileLocationGetter.id(s.getFirst()), s.getSecond().booleanValue())))
//                this.partList.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), this.currentLocation.get(), this.partList.getCurrentPosition(this));
//        };
//        set.FileTrash.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
//        set.FileUpdate.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
//        set.FileUpload.register(s -> Main.runOnUiThread(this.activity, () -> {
//            final FileLocation location = this.currentLocation.get();
//            if (FileLocationGetter.storage(location).equals(s.getFirst()) && FileLocationGetter.id(location) == FileInformationGetter.parentId(s.getSecond()))
//                this.partList.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), location, this.partList.getCurrentPosition(this));
//        }));
    }


    @UiThread
    private void updatePage(final @NotNull FileLocation location, final long position, final @NotNull AtomicBoolean clickable,
                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> clicker,
                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> operation) {
        final PageFileBinding page = this.page();
        this.currentLocation.set(location);
        final AtomicBoolean onLoading = new AtomicBoolean(false);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(PageFilePartList.this.activity().getLayoutInflater(), R.layout.page_file_cell, page.pageFileList), information -> {
                    if (!clickable.compareAndSet(true, false)) return;
                    if (FileInformationGetter.isDirectory(information)) {
                        final int p = PageFilePartList.this.getCurrentPosition();
                        PageFilePartList.this.stacks.push(Triad.ImmutableTriad.makeImmutableTriad(location, this.getData(p), new AtomicLong(p)));
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
                    adapter.addTailor(PageFilePartList.this.listLoadingView());
                else
                    adapter.addHeader(PageFilePartList.this.listLoadingView());
                Main.runOnBackgroundThread(PageFilePartList.this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
                    VisibleFilesListInformation list = null;
                    final int limit;
                    final AtomicBoolean showed = new AtomicBoolean(false);
                    try {
                        limit = ClientConfigurationSupporter.limitPerPage(ClientConfigurationSupporter.get());
                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
                        list = FilesAssistant.list(PageFilePartList.this.pageFile.address(), PageFilePartList.this.pageFile.username(), location, null, null,
                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, c -> {
                                    PageFilePartList.this.listLoadingAnimation(true, 0, 0);
                                    showed.set(true);
                                    return true;
                                }, PageFilePartList.this::listLoadingCallback);
                    } catch (final IllegalStateException exception) { // Server closed.
                        Main.runOnUiThread(PageFilePartList.this.activity(), PageFilePartList.this.activity()::disconnect);
                        return;
                    } finally {
                        if (showed.get()) {
                            final long total = list == null ? 0 : FilesListInformationGetter.total(list);
                            PageFilePartList.this.listLoadingAnimation(false, total, total);
                        }
                    }
                    if (list == null) {
                        Main.showToast(PageFilePartList.this.activity(), R.string.page_file_unavailable_directory);
                        Main.runOnUiThread(PageFilePartList.this.activity(), PageFilePartList.this::popFileList);
                        return;
                    }
                    if (isDown)
                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
                    else
                        noMoreUp.set(loadedUp.get() <= 0);
                    final VisibleFilesListInformation l = list;
                    Main.runOnUiThread(PageFilePartList.this.activity(), () -> {
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
                    Main.runOnUiThread(PageFilePartList.this.activity(), () -> {
                        if (isDown) {
                            if (noMoreDown.get())
                                adapter.setTailor(0, PageFilePartList.this.listNoMoreView());
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
        final PageFileBinding page = this.page();
        page.pageFileBacker.setImageResource(R.drawable.backer_nonclickable);
        page.pageFileBacker.setOnClickListener(null);
        page.pageFileBacker.setClickable(false);
        page.pageFileName.setText(R.string.app_name);
        this.updatePage(new FileLocation(IdentifierNames.RootSelector, 0), position, new AtomicBoolean(true),
                (information, c) -> this.onInsidePage(FileInformationGetter.name(information),
                                new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0),
                this.pageFile.partOperation::rootOperation);
    }

    @UiThread
    protected void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
        final PageFileBinding page = this.page();
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageFileBacker.setImageResource(R.drawable.backer);
        page.pageFileBacker.setOnClickListener(v -> {
            if (clickable.compareAndSet(true, false))
                this.popFileList();
        });
        page.pageFileBacker.setClickable(true);
        page.pageFileName.setText(name);
        this.updatePage(location, position, clickable, (information, c) -> {
            if (FileInformationGetter.isDirectory(information))
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
            else
                this.pageFile.partPreview.preview(FileLocationGetter.storage(location), information, c);
        }, (information, c) -> this.pageFile.partOperation.insideOperation(FileLocationGetter.storage(location), information, c));
    }

    @AnyThread
    private void backToRootPage() {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> r = this.stacks.pollLast();
        final long position = r == null || !IdentifierNames.RootSelector.equals(FileLocationGetter.storage(r.getA())) ? 0 : r.getC().get();
        Main.runOnUiThread(this.activity(), () -> this.onRootPage(position));
    }

    @UiThread
    protected boolean popFileList() {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> p = this.stacks.poll();
        if (p == null && this.isOnRoot()) return false;
        final PageFileBinding page = this.page();
        page.pageFileBacker.setClickable(false);
        page.pageFileCounter.setVisibility(View.GONE);
        page.pageFileCounterText.setVisibility(View.GONE);
        page.pageFileList.clearOnScrollListeners();
        page.pageFileList.setAdapter(EmptyRecyclerAdapter.Instance);
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            final VisibleFileInformation current;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.pageFile.address())) {
                current = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(this.pageFile.address(), this.pageFile.username()), this.currentLocation(), true);
            }
            if (current == null) {
                this.backToRootPage();
                return;
            }
            final FileLocation parent;
            final long position;
            if (p == null || FileInformationGetter.parentId(current) != FileLocationGetter.id(p.getA())) {
                this.stacks.clear();
                parent = new FileLocation(FileLocationGetter.storage(this.currentLocation()), FileInformationGetter.parentId(current));
                position = 0;
            } else {
                parent = p.getA();
                position = p.getC().get();
            }
            final VisibleFileInformation directory;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.pageFile.address())) {
                directory = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(this.pageFile.address(), this.pageFile.username()), parent, true);
            }
            if (directory == null) {
                this.backToRootPage();
                return;
            }
            if (FileInformationGetter.id(current) == FileInformationGetter.id(directory))
                Main.runOnUiThread(this.activity(), () -> this.onRootPage(position));
            else
                Main.runOnUiThread(this.activity(), () -> this.onInsidePage(FileInformationGetter.name(directory), parent, position));
        }));
        return true;
    }
}
