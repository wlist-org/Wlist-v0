package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
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
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
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
import com.xuxiaocheng.WListAndroid.databinding.PageFileLoadingBinding;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Objects;
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

    @UiThread
    private @NotNull View listLoadingView(final @NotNull LayoutInflater inflater) {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_loading, this.pageFile.getPage().pageFileList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        ViewUtil.startDrawableAnimation(image);
        return loading;
    }
    
    @UiThread
    private @NotNull View listNoMoreView(final @NotNull LayoutInflater inflater) {
        return EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_no_more, this.pageFile.getPage().pageFileList);
    }

    private static final @NotNull HInitializer<String> listLoadingAnimationMessage = new HInitializer<>("PageFileListLoadingAnimationMessage");
    private static final @NotNull HInitializer<String> listLoadingAnimationPercent = new HInitializer<>("PageFileListLoadingAnimationPercent");
    private final @NotNull HInitializer<PageFileLoadingBinding> listLoadingAnimationBinding = new HInitializer<>("PageFileListLoadingAnimationBinding");
    private final @NotNull HInitializer<AlertDialog> listLoadingAnimationDialog = new HInitializer<>("PageFileListLoadingAnimationDialog");

    @WorkerThread
    protected void listLoadingAnimation(final @NotNull ActivityMain activity, final @NotNull CharSequence title, final boolean show, final long current, final long total) {
        PageFilePartList.listLoadingAnimationMessage.initializeIfNot(() -> activity.getString(R.string.page_file_loading_text));
        PageFilePartList.listLoadingAnimationPercent.initializeIfNot(() -> activity.getString(R.string.page_file_loading_percent));
        this.listLoadingAnimationBinding.initializeIfNot(() -> PageFileLoadingBinding.inflate(activity.getLayoutInflater()));
        final float percent = total <= 0 ? show ? 0 : 1 : ((float) current) / total;
        final String textPercent = MessageFormat.format(PageFilePartList.listLoadingAnimationPercent.getInstance(), percent);
        final String textMessage = MessageFormat.format(PageFilePartList.listLoadingAnimationMessage.getInstance(), current, total);
        Main.runOnUiThread(activity, () -> {
            this.listLoadingAnimationDialog.initializeIfNot(() -> new AlertDialog.Builder(activity).setView(this.listLoadingAnimationBinding.getInstance().getRoot())
                    .setTitle(title).setCancelable(false).setPositiveButton(R.string.cancel, null).show());
            this.listLoadingAnimationDialog.getInstance().setTitle(title);
            this.listLoadingAnimationBinding.getInstance().pageFileLoadingGuideline.setGuidelinePercent(percent);
            this.listLoadingAnimationBinding.getInstance().pageFileLoadingPercent.setText(textPercent);
            this.listLoadingAnimationBinding.getInstance().pageFileLoadingText.setText(textMessage);
            if (show)
                this.listLoadingAnimationDialog.getInstance().show();
            else
                this.listLoadingAnimationDialog.getInstance().cancel();
        });
    }

    @WorkerThread
    private void listLoadingCallback(final @NotNull ActivityMain activity, final @NotNull CharSequence title, final @NotNull InstantaneousProgressState state) {
        final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
        this.listLoadingAnimation(activity, title, true, pair.getFirst().longValue(), pair.getSecond().longValue());
    }

    protected int getCurrentPosition() {
        final RecyclerView list = this.pageFile.getPage().pageFileList;
        final RecyclerView.LayoutManager manager = list.getLayoutManager();
        final RecyclerView.Adapter<?> adapter = list.getAdapter();
        assert manager instanceof LinearLayoutManager;
        assert adapter instanceof EnhancedRecyclerViewAdapter<?,?>;
        return ((LinearLayoutManager) manager).findFirstVisibleItemPosition() - ((EnhancedRecyclerViewAdapter<?, ?>) adapter).headersSize();
    }


    private final @NotNull AtomicReference<FileLocation> currentLocation = new AtomicReference<>();
    private final @NotNull AtomicReference<AtomicLong> currentLoadingUp = new AtomicReference<>();
    private final @NotNull AtomicReference<AtomicLong> currentLoadingDown = new AtomicReference<>();
    private final @NotNull Deque<Triad.@NotNull ImmutableTriad<@NotNull FileLocation, @NotNull VisibleFileInformation, @NotNull AtomicLong>> stacks = new ArrayDeque<>();

    protected boolean isOnRoot() {
        final FileLocation location = this.currentLocation.get();
        return location == null || IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location));
    }

    protected @NotNull FileLocation currentLocation() {
        return this.currentLocation.get();
    }

    public static final @NotNull HInitializer<Comparator<VisibleFileInformation>> comparator = new HInitializer<>("InformationComparator");
    @SuppressWarnings("Convert2MethodRef") // AndroidSupport
    private int compareInformation(final @NotNull VisibleFileInformation a, final @NotNull VisibleFileInformation b) {
        PageFilePartList.comparator.initializeIfNot(() -> FileInformationGetter.buildComparator());
        return PageFilePartList.comparator.getInstance().compare(a, b);
    }

    @WorkerThread
    @SuppressWarnings("unchecked")
    protected void listenBroadcast(final @NotNull ActivityMain activity, final BroadcastAssistant.@NotNull BroadcastSet set) {
        set.ProviderInitialized.register(HExceptionWrapper.wrapConsumer(p -> {
            final VisibleFileInformation information;
            try (final WListClientInterface client = this.pageFile.client(activity)) {
                information = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(activity), new FileLocation(p.getFirst(), p.getSecond().longValue()), true);
            }
            if (information == null) return;
            final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = (EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder>) Objects.requireNonNull(this.pageFile.getPage().pageFileList.getAdapter());
            if (this.isOnRoot()) {
                final VisibleFileInformation[] list = FileInformationGetter.asArray(adapter.getData());
                if (list.length == 0)
                    adapter.addData(information);
                else {
                    final int index = FileInformationGetter.binarySearch(list, information, this::compareInformation);
                    if (index >= 0)
                        adapter.addData(index, information);
                    else
                        adapter.addData(-index - 1, information);
                }
            } else {
                final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> last = this.stacks.peekLast();
                if (last != null && IdentifierNames.RootSelector.equals(FileLocationGetter.storage(last.getA())) && this.compareInformation(information, last.getB()) < 0)
                    last.getC().getAndIncrement();
            }
        }));
//         TODO auto insert.
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
    private void updatePage(final @NotNull ActivityMain activity, final @NotNull FileLocation location, final long position, final @NotNull AtomicBoolean clickable,
                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> clicker,
                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> operation) {
        final PageFileBinding page = this.pageFile.getPage();
        this.currentLocation.set(location);
        final AtomicBoolean onLoading = new AtomicBoolean(false);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileList), information -> {
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
        this.currentLoadingUp.set(loadedUp);
        this.currentLoadingDown.set(loadedDown);
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
                Main.runOnBackgroundThread(activity, () -> {
                    return;
                });
                if (!onLoading.compareAndSet(false, true)) return;
                if (isDown)
                    adapter.addTailor(PageFilePartList.this.listLoadingView(activity.getLayoutInflater()));
                else
                    adapter.addHeader(PageFilePartList.this.listLoadingView(activity.getLayoutInflater()));
                Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
                    VisibleFilesListInformation list = null;
                    final int limit;
                    final String title = MessageFormat.format(activity.getString(R.string.page_file_listing_title), page.pageFileName.getText());
                    final AtomicBoolean showed = new AtomicBoolean(false);
                    try {
                        limit = ClientConfigurationSupporter.limitPerPage(ClientConfigurationSupporter.get());
                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
                        list = FilesAssistant.list(PageFilePartList.this.pageFile.address(activity), PageFilePartList.this.pageFile.username(activity), location, null, null,
                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, c -> {
                                    PageFilePartList.this.listLoadingAnimation(activity, title, true, 0, 0);
                                    showed.set(true);
                                    return true;
                                }, s -> PageFilePartList.this.listLoadingCallback(activity, title, s));
                    } catch (final IllegalStateException exception) { // Server closed.
                        Main.runOnUiThread(activity, activity::disconnect);
                        return;
                    } finally {
                        if (showed.get()) {
                            final long total = list == null ? 0 : FilesListInformationGetter.total(list);
                            PageFilePartList.this.listLoadingAnimation(activity, title, false, total, total);
                        }
                    }
                    if (list == null) {
                        Main.showToast(activity, R.string.page_file_unavailable_directory);
                        Main.runOnUiThread(activity, () -> PageFilePartList.this.popFileList(activity));
                        return;
                    }
                    if (isDown)
                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
                    else
                        noMoreUp.set(loadedUp.get() <= 0);
                    final VisibleFilesListInformation l = list;
                    Main.runOnUiThread(activity, () -> {
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
                    Main.runOnUiThread(activity, () -> {
                        if (isDown) {
                            if (noMoreDown.get())
                                adapter.setTailor(0, PageFilePartList.this.listNoMoreView(activity.getLayoutInflater()));
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
    protected void onRootPage(final @NotNull ActivityMain activity, final long position) {
        final PageFileBinding page = this.pageFile.getPage();
        page.pageFileBacker.setImageResource(R.drawable.backer_nonclickable);
        page.pageFileBacker.setOnClickListener(null);
        page.pageFileBacker.setClickable(false);
        page.pageFileName.setText(R.string.app_name);
        this.updatePage(activity, new FileLocation(IdentifierNames.RootSelector, 0), position, new AtomicBoolean(true),
                (information, c) -> this.onInsidePage(activity, FileInformationGetter.name(information),
                                new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0),
                (information, c) -> this.pageFile.partOperation.rootOperation(activity, information, c));
    }

    @UiThread
    protected void onInsidePage(final @NotNull ActivityMain activity, final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
        final PageFileBinding page = this.pageFile.getPage();
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageFileBacker.setImageResource(R.drawable.backer);
        page.pageFileBacker.setOnClickListener(v -> {
            if (clickable.compareAndSet(true, false))
                this.popFileList(activity);
        });
        page.pageFileBacker.setClickable(true);
        page.pageFileName.setText(name);
        this.updatePage(activity, location, position, clickable, (information, c) -> {
            if (FileInformationGetter.isDirectory(information))
                this.onInsidePage(activity, FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
            else
                this.pageFile.partPreview.preview(activity, FileLocationGetter.storage(location), information, c);
        }, (information, c) -> this.pageFile.partOperation.insideOperation(activity, FileLocationGetter.storage(location), information, c));
    }

    @AnyThread
    private void backToRootPage(final @NotNull ActivityMain activity) {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> r = this.stacks.pollLast();
        final long position = r == null || !IdentifierNames.RootSelector.equals(FileLocationGetter.storage(r.getA())) ? 0 : r.getC().get();
        Main.runOnUiThread(activity, () -> this.onRootPage(activity, position));
    }

    @UiThread
    protected boolean popFileList(final @NotNull ActivityMain activity) {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> p = this.stacks.poll();
        if (p == null && this.isOnRoot()) return false;
        final PageFileBinding page = this.pageFile.getPage();
        page.pageFileBacker.setClickable(false);
        page.pageFileCounter.setVisibility(View.GONE);
        page.pageFileCounterText.setVisibility(View.GONE);
        page.pageFileList.clearOnScrollListeners();
        page.pageFileList.setAdapter(EmptyRecyclerAdapter.Instance);
        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
            final VisibleFileInformation current;
            try (final WListClientInterface client = this.pageFile.client(activity)) {
                current = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(activity), this.currentLocation(), true);
            }
            if (current == null) {
                this.backToRootPage(activity);
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
            try (final WListClientInterface client = this.pageFile.client(activity)) {
                directory = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(activity), parent, true);
            }
            if (directory == null) {
                this.backToRootPage(activity);
                return;
            }
            if (FileInformationGetter.id(current) == FileInformationGetter.id(directory))
                Main.runOnUiThread(activity, () -> this.onRootPage(activity, position));
            else
                Main.runOnUiThread(activity, () -> this.onInsidePage(activity, FileInformationGetter.name(directory), parent, position));
        }));
        return true;
    }
}
