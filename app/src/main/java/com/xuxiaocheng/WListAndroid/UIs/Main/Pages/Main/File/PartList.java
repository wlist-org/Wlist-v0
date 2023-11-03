package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.os.Bundle;
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
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileLoadingBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PartList extends SFragmentFilePart {
    protected PartList(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        final PageFileBinding page = this.fragmentContent();
        page.pageFileList.setLayoutManager(new LinearLayoutManager(this.activity()));
        page.pageFileList.setHasFixedSize(true);
    }

    @Override
    public void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        if (savedInstanceState != null) {
            BundleHelper.restoreLocation(savedInstanceState, "wlist:fragment_file:part_list:current", this.currentLocation, null);
            final long position = savedInstanceState.getLong("wlist:fragment_file:part_list:position");
            this.currentLoadingUp.set(new AtomicLong(position));
            this.currentLoadingDown.set(new AtomicLong(position));
        } else {
            this.currentLocation.set(new FileLocation(IdentifierNames.RootSelector, 0));
            this.currentLoadingUp.set(new AtomicLong(0));
            this.currentLoadingDown.set(new AtomicLong(0));
        }
    }

    @Override
    public void iOnSaveInstanceState(@NotNull final Bundle outState) {
        super.iOnSaveInstanceState(outState);
        BundleHelper.saveLocation(this.currentLocation, outState, "wlist:fragment_file:part_list:current", null);
        outState.putLong("wlist:fragment_file:part_list:position", this.getCurrentPosition() + this.currentLoadingUp.get().get());
    }

    @Override
    public void cOnConnect() {
        super.cOnConnect();
        Main.runOnUiThread(this.activity(), () -> {
            final PageFileBinding page = this.fragmentContent();
            page.pageFileName.setVisibility(View.VISIBLE);
            page.pageFileBacker.setVisibility(View.VISIBLE);
            page.pageFileList.setVisibility(View.VISIBLE);
        });
        HExceptionWrapper.wrapRunnable(() -> this.toPage(this.currentLocation(), this.currentLoadingUp.get().get())).run();
        this.listenBroadcast(BroadcastAssistant.get(this.address()));
    }

    @Override
    public void cOnDisconnect() {
        super.cOnDisconnect();
        Main.runOnUiThread(this.activity(), () -> {
            final PageFileBinding page = this.fragmentContent();
            page.pageFileName.setVisibility(View.GONE);
            page.pageFileBacker.setVisibility(View.GONE);
            page.pageFileList.setVisibility(View.GONE);
            page.pageFileCounter.setVisibility(View.GONE);
            page.pageFileCounterText.setVisibility(View.GONE);
        });
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


    @UiThread
    private @NotNull View listLoadingView(final @NotNull LayoutInflater inflater) {
        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_loading, this.fragmentContent().pageFileList);
        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
        ViewUtil.startDrawableAnimation(image);
        return loading;
    }
    @UiThread
    private @NotNull View listNoMoreView(final @NotNull LayoutInflater inflater) {
        return EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_no_more, this.fragmentContent().pageFileList);
    }
    @UiThread
    private @NotNull ConstraintLayout listCellView(final @NotNull LayoutInflater inflater) {
        return EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_cell, this.fragmentContent().pageFileList);
    }

    private final @NotNull HInitializer<PageFileLoadingBinding> loadingBarBinding = new HInitializer<>("LoadingBarBinding");
    private final @NotNull HInitializer<AlertDialog> loadingBarDialog = new HInitializer<>("LoadingBarDialog");
    @WorkerThread
    protected void listLoadingAnimation(final @NotNull CharSequence title, final boolean show, final long current, final long total) {
        this.loadingBarBinding.initializeIfNot(() -> PageFileLoadingBinding.inflate(this.activity().getLayoutInflater()));
        final float percent = total <= 0 ? show ? 0 : 1 : ((float) current) / total;
        final String textPercent = MessageFormat.format(this.activity().getString(R.string.page_file_loading_percent), percent);
        final String textMessage = MessageFormat.format(this.activity().getString(R.string.page_file_loading_text), current, total);
        Main.runOnUiThread(this.activity(), () -> {
            this.loadingBarDialog.initializeIfNot(() -> new AlertDialog.Builder(this.activity()).setView(this.loadingBarBinding.getInstance().getRoot())
                    .setTitle(title).setCancelable(false).setPositiveButton(R.string.cancel, null).show());
            final PageFileLoadingBinding loading = this.loadingBarBinding.getInstance();
            loading.pageFileLoadingGuideline.setGuidelinePercent(percent);
            loading.pageFileLoadingPercent.setText(textPercent);
            loading.pageFileLoadingText.setText(textMessage);
            final AlertDialog dialog = this.loadingBarDialog.getInstance();
            dialog.setTitle(title);
            if (show) dialog.show();else dialog.cancel();
        });
    }

    @AnyThread
    protected int getCurrentPosition() {
        final RecyclerView list = this.fragmentContent().pageFileList;
        final RecyclerView.LayoutManager manager = list.getLayoutManager();
        final RecyclerView.Adapter<?> adapter = list.getAdapter();
        if (!(manager instanceof LinearLayoutManager) || !(adapter instanceof EnhancedRecyclerViewAdapter<?, ?>)) return 0;
        final int position = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
        if (position == RecyclerView.NO_POSITION) return -1;
        return position - ((EnhancedRecyclerViewAdapter<?, ?>) adapter).headersSize();
    }


    @UiThread
    private void updatePage(final @NotNull FileLocation location, final long position, final @NotNull AtomicBoolean clickable,
                            @UiThread final @NotNull Consumer<? super @NotNull VisibleFileInformation> clicker,
                            @UiThread final @NotNull Consumer<? super @NotNull VisibleFileInformation> operation) {
        this.currentLocation.set(location);
        final AtomicBoolean onLoading = new AtomicBoolean(false);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PartListViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull PartListViewHolder createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition) {
                return new PartListViewHolder(PartList.this.listCellView(PartList.this.activity().getLayoutInflater()), information -> {
                    if (!clickable.compareAndSet(true, false)) return;
                    if (FileInformationGetter.isDirectory(information)) {
                        final int p = PartList.this.getCurrentPosition();
                        PartList.this.stacks.push(Triad.ImmutableTriad.makeImmutableTriad(location, this.getData(p), new AtomicLong(p)));
                    }
                    clicker.accept(information);
                }, f -> {
                    if (clickable.compareAndSet(true, false))
                        operation.accept(f);
                });
            }
        };
        this.fragmentContent().pageFileCounter.setVisibility(View.GONE);
        this.fragmentContent().pageFileCounterText.setVisibility(View.GONE);
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
                if (recyclerView.getAdapter() != adapter) return;
                // TODO: Remove the pages on the top.
                final boolean isDown;
                if (!recyclerView.canScrollVertically(1) && !noMoreDown.get())
                    isDown = true;
                else if (!recyclerView.canScrollVertically(-1) && !noMoreUp.get())
                    isDown = false;
                else return;
                if (!onLoading.compareAndSet(false, true)) return;
                final ActivityMain activity = PartList.this.activity();
                if (isDown)
                    adapter.addTailor(PartList.this.listLoadingView(activity.getLayoutInflater()));
                else
                    adapter.addHeader(PartList.this.listLoadingView(activity.getLayoutInflater()));
                Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
                    VisibleFilesListInformation list = null;
                    final int limit;
                    final String title = MessageFormat.format(activity.getString(R.string.page_file_listing_title), PartList.this.fragmentContent().pageFileName.getText());
                    final AtomicBoolean showed = new AtomicBoolean(false);
                    try {
                        limit = ClientConfigurationSupporter.limitPerPage(ClientConfigurationSupporter.get());
                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
                        list = FilesAssistant.list(PartList.this.address(), PartList.this.username(), location, null, null,
                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, c -> {
                                    PartList.this.listLoadingAnimation(title, true, 0, 0);
                                    showed.set(true);
                                    return true;
                                }, state -> {
                                    final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
                                    PartList.this.listLoadingAnimation(title, true, pair.getFirst().longValue(), pair.getSecond().longValue());
                                });
                    } finally {
                        if (showed.get()) {
                            final long total = list == null ? 0 : FilesListInformationGetter.total(list);
                            PartList.this.listLoadingAnimation(title, false, total, total);
                        }
                    }
                    if (list == null) {
                        Main.showToast(activity, R.string.page_file_unavailable_directory);
                        Main.runOnUiThread(activity, PartList.this::popFileList);
                        return;
                    }
                    if (isDown)
                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
                    else
                        noMoreUp.set(loadedUp.get() <= 0);
                    final VisibleFilesListInformation l = list;
                    Main.runOnUiThread(activity, () -> {
                        PartList.this.fragmentContent().pageFileCounter.setText(String.valueOf(FilesListInformationGetter.total(l)));
                        PartList.this.fragmentContent().pageFileCounter.setVisibility(View.VISIBLE);
                        PartList.this.fragmentContent().pageFileCounterText.setVisibility(View.VISIBLE);
                        final boolean empty = adapter.getData().isEmpty();
                        if (isDown)
                            adapter.addDataRange(FilesListInformationGetter.informationList(l));
                        else
                            adapter.addDataRange(0, FilesListInformationGetter.informationList(l));
                        if (empty)
                            recyclerView.scrollToPosition(0);
                    });
                }, e -> {
                    onLoading.set(false);
                    Main.runOnUiThread(activity, () -> {
                        if (isDown) {
                            if (noMoreDown.get())
                                adapter.setTailor(0, PartList.this.listNoMoreView(activity.getLayoutInflater()));
                            else
                                adapter.removeTailor(0);
                        } else
                            adapter.removeHeader(0);
                        if (e == null)
                            Main.runOnUiThread(activity, () -> this.onScrollStateChanged(recyclerView, AbsListView.OnScrollListener.SCROLL_STATE_IDLE),
                                    300, TimeUnit.MILLISECONDS); // Wait animation finish.
                    });
                }, false));
            }
        };
        this.fragmentContent().pageFileList.setAdapter(adapter);
        this.fragmentContent().pageFileList.clearOnScrollListeners();
        this.fragmentContent().pageFileList.addOnScrollListener(listener);
        listener.onScrollStateChanged(this.fragmentContent().pageFileList, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
    }

    @UiThread
    private void onRootPage(final long position) {
        final PageFileBinding page = this.fragmentContent();
        page.pageFileBacker.setImageResource(R.drawable.backer_nonclickable);
        page.pageFileBacker.setOnClickListener(null);
        page.pageFileBacker.setClickable(false);
        page.pageFileName.setText(R.string.app_name);
        final AtomicBoolean clickable = new AtomicBoolean(true);
        this.updatePage(new FileLocation(IdentifierNames.RootSelector, 0), position, clickable,
                information -> this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0),
                information -> this.fragment.partOperation().rootOperation(information, clickable));
    }

    @UiThread
    private void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
        final PageFileBinding page = this.fragmentContent();
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageFileBacker.setImageResource(R.drawable.backer);
        page.pageFileBacker.setOnClickListener(v -> {
            if (clickable.compareAndSet(true, false))
                this.popFileList();
        });
        page.pageFileBacker.setClickable(true);
        page.pageFileName.setText(name);
        this.updatePage(location, position, clickable, information -> {
            if (FileInformationGetter.isDirectory(information))
                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
            else
                this.fragment.partPreview().preview(this.activity(), FileLocationGetter.storage(location), information, clickable);
        }, information -> this.fragment.partOperation().insideOperation(FileLocationGetter.storage(location), information, clickable));
    }

    @AnyThread
    private void backToRootPage() {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> r = this.stacks.pollLast();
        final long position = r == null || !IdentifierNames.RootSelector.equals(FileLocationGetter.storage(r.getA())) ? 0 : r.getC().get();
        Main.runOnUiThread(this.fragment.getActivity(), () -> this.onRootPage(position));
    }

    @WorkerThread
    protected void toPage(final @NotNull FileLocation location, final long position) throws IOException, InterruptedException, WrongStateException {
        if (IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location)))
            Main.runOnUiThread(this.fragment.getActivity(), () -> this.onRootPage(position));
        else {
            final VisibleFileInformation directory;
            try (final WListClientInterface client = this.client()) {
                directory = OperateFilesHelper.getFileOrDirectory(client, this.token(), location, true);
            }
            if (directory == null) {
                this.backToRootPage();
                return;
            }
            Main.runOnUiThread(this.fragment.getActivity(), () -> this.onInsidePage(FileInformationGetter.name(directory), location, position));
        }
    }

    @UiThread
    protected boolean popFileList() {
        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> p = this.stacks.poll();
        if (p == null && this.isOnRoot()) return false;
        final PageFileBinding page = this.fragmentContent();
        page.pageFileBacker.setClickable(false);
        page.pageFileCounter.setVisibility(View.GONE);
        page.pageFileCounterText.setVisibility(View.GONE);
        page.pageFileList.clearOnScrollListeners();
        page.pageFileList.setAdapter(null);
        final ActivityMain activity = this.activity();
        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
            final VisibleFileInformation current;
            try (final WListClientInterface client = this.client()) {
                current = OperateFilesHelper.getFileOrDirectory(client, this.token(), this.currentLocation(), true);
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
            try (final WListClientInterface client = this.client()) {
                directory = OperateFilesHelper.getFileOrDirectory(client, this.token(), parent, true);
            }
            if (directory == null) {
                this.backToRootPage();
                return;
            }
            if (FileInformationGetter.id(current) == FileInformationGetter.id(directory))
                Main.runOnUiThread(activity, () -> this.onRootPage(position));
            else
                Main.runOnUiThread(activity, () -> this.onInsidePage(FileInformationGetter.name(directory), parent, position));
        }));
        return true;
    }

    @WorkerThread
    protected void clearCurrentPosition() {
        final FileLocation location = this.currentLocation();
        if (IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location)))
            Main.runOnUiThread(this.activity(), () -> this.onRootPage(this.getCurrentPosition()));
        else
            Main.runOnUiThread(this.activity(), () -> this.onInsidePage(this.fragmentContent().pageFileName.getText(), location, this.getCurrentPosition()));
    }


    public static final @NotNull HInitializer<Comparator<VisibleFileInformation>> comparator = new HInitializer<>("InformationComparator");
    @SuppressWarnings("Convert2MethodRef") // AndroidSupport
    private int compareInformation(final @NotNull VisibleFileInformation a, final @NotNull VisibleFileInformation b) {
        PartList.comparator.initializeIfNot(() -> FileInformationGetter.buildComparator());
        return PartList.comparator.getInstance().compare(a, b);
    }

    @WorkerThread
    @SuppressWarnings("unchecked")
    protected void listenBroadcast(final BroadcastAssistant.@NotNull BroadcastSet set) {
        set.ProviderInitialized.getCallbacks().put("list", HExceptionWrapper.wrapConsumer(p -> {
            final VisibleFileInformation information;
            try (final WListClientInterface client = this.client()) {
                information = OperateFilesHelper.getFileOrDirectory(client, this.token(), new FileLocation(p.getFirst(), p.getSecond().longValue()), true);
            }
            if (information == null) return;
            final EnhancedRecyclerViewAdapter<VisibleFileInformation, PartListViewHolder> adapter = (EnhancedRecyclerViewAdapter<VisibleFileInformation, PartListViewHolder>) Objects.requireNonNull(this.fragmentContent().pageFileList.getAdapter());
            if (this.isOnRoot()) {
                final VisibleFileInformation[] list = FileInformationGetter.asArray(adapter.getData());
                if (list.length == 0)
                    adapter.addData(information);
                else {
                    final int index = FileInformationGetter.binarySearch(list, information, this::compareInformation);
                    if (index >= 0 && Objects.equals(list[index], information)) return;
                    adapter.addData(index >= 0 ? index : -index - 1, information);
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
}
