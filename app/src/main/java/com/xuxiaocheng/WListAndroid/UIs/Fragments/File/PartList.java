package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.view.View;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragmentPart;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

class PartList extends IFragmentPart<PageFileBinding, FragmentFile> {
    protected PartList(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void onBuild(final @NotNull PageFileBinding page) {
        super.onBuild(page);
        page.pageFileList.setLayoutManager(new LinearLayoutManager(this.activity()));
        page.pageFileList.setHasFixedSize(true);
    }

    @Override
    protected void onConnected(final @NotNull ActivityMain activity) {
        super.onConnected(activity);
        Main.runOnUiThread(activity, () -> this.page().pageFileList.setVisibility(View.VISIBLE));
//        this.onRootPage(0); // TODO
//        this.listenBroadcast(BroadcastAssistant.get(this.address()));
    }

    @Override
    protected void onDisconnected(final @NotNull ActivityMain activity) {
        super.onDisconnected(activity);
        Main.runOnUiThread(activity, () -> this.page().pageFileList.setVisibility(View.GONE));
    }

    @WorkerThread
    protected void onConfigurationChanged() {
        // TODO
//                    PageFilePartList.comparator.uninitializeNullable();
//                    final FileLocation location = this.pageFile.partList.currentLocation();
//                    if (IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location)))
//                        Main.runOnUiThread(this.pageFile.activity(), () -> this.pageFile.partList.onRootPage(this.pageFile.partList.getCurrentPosition()));
//                    else
//                        Main.runOnUiThread(this.pageFile.activity(), () -> this.pageFile.partList.onInsidePage(this.pageFile.page().pageFileName.getText(), location, this.pageFile.partList.getCurrentPosition()));

    }

//    @UiThread
//    private @NotNull View listLoadingView(final @NotNull LayoutInflater inflater) {
//        final ConstraintLayout loading = EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_loading, this.pageFile.page().pageFileList);
//        final ImageView image = (ImageView) loading.getViewById(R.id.page_file_tailor_loading_image);
//        ViewUtil.startDrawableAnimation(image);
//        return loading;
//    }
//
//    @UiThread
//    private @NotNull View listNoMoreView(final @NotNull LayoutInflater inflater) {
//        return EnhancedRecyclerViewAdapter.buildView(inflater, R.layout.page_file_tailor_no_more, this.pageFile.page().pageFileList);
//    }
//
//    private static final @NotNull HInitializer<String> listLoadingAnimationMessage = new HInitializer<>("PageFileListLoadingAnimationMessage");
//    private static final @NotNull HInitializer<String> listLoadingAnimationPercent = new HInitializer<>("PageFileListLoadingAnimationPercent");
//    private final @NotNull HInitializer<PageFileLoadingBinding> listLoadingAnimationBinding = new HInitializer<>("PageFileListLoadingAnimationBinding");
//    private final @NotNull HInitializer<AlertDialog> listLoadingAnimationDialog = new HInitializer<>("PageFileListLoadingAnimationDialog");
//
//    @WorkerThread
//    protected void listLoadingAnimation(final @NotNull CharSequence title, final boolean show, final long current, final long total) {
//        PartList.listLoadingAnimationMessage.initializeIfNot(() -> this.pageFile.activity().getString(R.string.page_file_loading_text));
//        PartList.listLoadingAnimationPercent.initializeIfNot(() -> this.pageFile.activity().getString(R.string.page_file_loading_percent));
//        this.listLoadingAnimationBinding.initializeIfNot(() -> PageFileLoadingBinding.inflate(this.pageFile.activity().getLayoutInflater()));
//        final float percent = total <= 0 ? show ? 0 : 1 : ((float) current) / total;
//        final String textPercent = MessageFormat.format(PartList.listLoadingAnimationPercent.getInstance(), percent);
//        final String textMessage = MessageFormat.format(PartList.listLoadingAnimationMessage.getInstance(), current, total);
//        Main.runOnUiThread(this.pageFile.activity(), () -> {
//            this.listLoadingAnimationDialog.initializeIfNot(() -> new AlertDialog.Builder(this.pageFile.activity()).setView(this.listLoadingAnimationBinding.getInstance().getRoot())
//                    .setTitle(title).setCancelable(false).setPositiveButton(R.string.cancel, null).show());
//            this.listLoadingAnimationDialog.getInstance().setTitle(title);
//            this.listLoadingAnimationBinding.getInstance().pageFileLoadingGuideline.setGuidelinePercent(percent);
//            this.listLoadingAnimationBinding.getInstance().pageFileLoadingPercent.setText(textPercent);
//            this.listLoadingAnimationBinding.getInstance().pageFileLoadingText.setText(textMessage);
//            if (show)
//                this.listLoadingAnimationDialog.getInstance().show();
//            else
//                this.listLoadingAnimationDialog.getInstance().cancel();
//        });
//    }
//
//    @WorkerThread
//    private void listLoadingCallback(final @NotNull CharSequence title, final @NotNull InstantaneousProgressState state) {
//        final Pair.ImmutablePair<Long, Long> pair = InstantaneousProgressStateGetter.merge(state);
//        this.listLoadingAnimation(title, true, pair.getFirst().longValue(), pair.getSecond().longValue());
//    }
//
//    protected int getCurrentPosition() {
//        final RecyclerView list = this.pageFile.page().pageFileList;
//        final RecyclerView.LayoutManager manager = list.getLayoutManager();
//        final RecyclerView.Adapter<?> adapter = list.getAdapter();
//        assert manager instanceof LinearLayoutManager;
//        assert adapter instanceof EnhancedRecyclerViewAdapter<?,?>;
//        return ((LinearLayoutManager) manager).findFirstVisibleItemPosition() - ((EnhancedRecyclerViewAdapter<?, ?>) adapter).headersSize();
//    }
//
//
//    private final @NotNull AtomicReference<FileLocation> currentLocation = new AtomicReference<>();
//    private final @NotNull AtomicReference<AtomicLong> currentLoadingUp = new AtomicReference<>();
//    private final @NotNull AtomicReference<AtomicLong> currentLoadingDown = new AtomicReference<>();
//    private final @NotNull Deque<Triad.@NotNull ImmutableTriad<@NotNull FileLocation, @NotNull VisibleFileInformation, @NotNull AtomicLong>> stacks = new ArrayDeque<>();
//
//    protected boolean isOnRoot() {
//        final FileLocation location = this.currentLocation.get();
//        return location == null || IdentifierNames.RootSelector.equals(FileLocationGetter.storage(location));
//    }
//
//    protected @NotNull FileLocation currentLocation() {
//        return this.currentLocation.get();
//    }
//
//    public static final @NotNull HInitializer<Comparator<VisibleFileInformation>> comparator = new HInitializer<>("InformationComparator");
//    @SuppressWarnings("Convert2MethodRef") // AndroidSupport
//    private int compareInformation(final @NotNull VisibleFileInformation a, final @NotNull VisibleFileInformation b) {
//        PartList.comparator.initializeIfNot(() -> FileInformationGetter.buildComparator());
//        return PartList.comparator.getInstance().compare(a, b);
//    }
//
//    @WorkerThread
//    @SuppressWarnings("unchecked")
//    protected void listenBroadcast(final BroadcastAssistant.@NotNull BroadcastSet set) {
//        set.ProviderInitialized.register(HExceptionWrapper.wrapConsumer(p -> {
//            final VisibleFileInformation information;
//            try (final WListClientInterface client = this.pageFile.client()) {
//                information = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(), new FileLocation(p.getFirst(), p.getSecond().longValue()), true);
//            }
//            if (information == null) return;
//            final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = (EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder>) Objects.requireNonNull(this.pageFile.page().pageFileList.getAdapter());
//            if (this.isOnRoot()) {
//                final VisibleFileInformation[] list = FileInformationGetter.asArray(adapter.getData());
//                if (list.length == 0)
//                    adapter.addData(information);
//                else {
//                    final int index = FileInformationGetter.binarySearch(list, information, this::compareInformation);
//                    if (index >= 0 && Objects.equals(list[index], information)) return;
//                    adapter.addData(index >= 0 ? index : -index - 1, information);
//                }
//            } else {
//                final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> last = this.stacks.peekLast();
//                if (last != null && IdentifierNames.RootSelector.equals(FileLocationGetter.storage(last.getA())) && this.compareInformation(information, last.getB()) < 0)
//                    last.getC().getAndIncrement();
//            }
//        }));
////         TODO auto insert.
////        set.ProviderUninitialized.register(s -> onRoot.run());
////        final Consumer<Pair.ImmutablePair<FileLocation, Boolean>> update = s -> {
////            final FileLocation location = this.currentLocation.get();
////            if (FileLocationGetter.storage(location).equals(FileLocationGetter.storage(s.getFirst()))
////                    && this.currentDoubleIds.contains(FileSqlInterface.getDoubleId(FileLocationGetter.id(s.getFirst()), s.getSecond().booleanValue())))
////                this.partList.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), this.currentLocation.get(), this.partList.getCurrentPosition(this));
////        };
////        set.FileTrash.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
////        set.FileUpdate.register(s -> Main.runOnUiThread(this.activity, () -> update.accept(s)));
////        set.FileUpload.register(s -> Main.runOnUiThread(this.activity, () -> {
////            final FileLocation location = this.currentLocation.get();
////            if (FileLocationGetter.storage(location).equals(s.getFirst()) && FileLocationGetter.id(location) == FileInformationGetter.parentId(s.getSecond()))
////                this.partList.onInsidePage(this.pageCache.getInstance().pageFileName.getText(), location, this.partList.getCurrentPosition(this));
////        }));
//    }
//
//
//
//    @UiThread
//    private void updatePage(final @NotNull FileLocation location, final long position, final @NotNull AtomicBoolean clickable,
//                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> clicker,
//                            @UiThread final @NotNull BiConsumer<? super @NotNull VisibleFileInformation, ? super @NotNull AtomicBoolean> operation) {
//        final PageFileBinding page = this.pageFile.page();
//        this.currentLocation.set(location);
//        final AtomicBoolean onLoading = new AtomicBoolean(false);
//        final EnhancedRecyclerViewAdapter<VisibleFileInformation, PageFileViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
//            @Override
//            protected @NotNull PageFileViewHolder createViewHolder(final @NotNull ViewGroup parent) {
//                return new PageFileViewHolder(EnhancedRecyclerViewAdapter.buildView(PartList.this.pageFile.activity().getLayoutInflater(), R.layout.page_file_cell, page.pageFileList), information -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    if (FileInformationGetter.isDirectory(information)) {
//                        final int p = PartList.this.getCurrentPosition();
//                        PartList.this.stacks.push(Triad.ImmutableTriad.makeImmutableTriad(location, this.getData(p), new AtomicLong(p)));
//                    }
//                    clicker.accept(information, clickable);
//                }, f -> {
//                    if (clickable.compareAndSet(true, false))
//                        operation.accept(f, clickable);
//                });
//            }
//        };
//        page.pageFileCounter.setVisibility(View.GONE);
//        page.pageFileCounterText.setVisibility(View.GONE);
//        final AtomicLong loadedUp = new AtomicLong(position);
//        final AtomicLong loadedDown = new AtomicLong(position);
//        this.currentLoadingUp.set(loadedUp);
//        this.currentLoadingDown.set(loadedDown);
//        final AtomicBoolean noMoreUp = new AtomicBoolean(position <= 0);
//        final AtomicBoolean noMoreDown = new AtomicBoolean(false);
//        final RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
//            @UiThread
//            @Override
//            public void onScrollStateChanged(final @NotNull RecyclerView recyclerView, final int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//                // TODO: Remove the pages on the top.
//                final boolean isDown;
//                if (!recyclerView.canScrollVertically(1) && !noMoreDown.get())
//                    isDown = true;
//                else if (!recyclerView.canScrollVertically(-1) && !noMoreUp.get())
//                    isDown = false;
//                else return;
//                final ActivityMain activityMain = PartList.this.pageFile.activity();
//                if (!onLoading.compareAndSet(false, true)) return;
//                if (isDown)
//                    adapter.addTailor(PartList.this.listLoadingView(activityMain.getLayoutInflater()));
//                else
//                    adapter.addHeader(PartList.this.listLoadingView(activityMain.getLayoutInflater()));
//                Main.runOnBackgroundThread(activityMain, HExceptionWrapper.wrapRunnable(() -> {
//                    (isDown ? noMoreDown : noMoreUp).set(false); // prevent retry forever when server error.
//                    VisibleFilesListInformation list = null;
//                    final int limit;
//                    final String title = MessageFormat.format(activityMain.getString(R.string.page_file_listing_title), page.pageFileName.getText());
//                    final AtomicBoolean showed = new AtomicBoolean(false);
//                    try {
//                        limit = ClientConfigurationSupporter.limitPerPage(ClientConfigurationSupporter.get());
//                        final int need = isDown ? limit : Math.toIntExact(Math.min(loadedUp.get(), limit));
//                        list = FilesAssistant.list(PartList.this.pageFile.address(), PartList.this.pageFile.username(), location, null, null,
//                                isDown ? loadedDown.getAndAdd(need) : loadedUp.addAndGet(-need), need, Main.ClientExecutors, c -> {
//                                    PartList.this.listLoadingAnimation(title, true, 0, 0);
//                                    showed.set(true);
//                                    return true;
//                                }, s -> PartList.this.listLoadingCallback(title, s));
//                    } finally {
//                        if (showed.get()) {
//                            final long total = list == null ? 0 : FilesListInformationGetter.total(list);
//                            PartList.this.listLoadingAnimation(title, false, total, total);
//                        }
//                    }
//                    if (list == null) {
//                        Main.showToast(activityMain, R.string.page_file_unavailable_directory);
//                        Main.runOnUiThread(activityMain, PartList.this::popFileList);
//                        return;
//                    }
//                    if (isDown)
//                        noMoreDown.set(FilesListInformationGetter.informationList(list).size() < limit);
//                    else
//                        noMoreUp.set(loadedUp.get() <= 0);
//                    final VisibleFilesListInformation l = list;
//                    Main.runOnUiThread(activityMain, () -> {
//                        page.pageFileCounter.setText(String.valueOf(FilesListInformationGetter.total(l)));
//                        page.pageFileCounter.setVisibility(View.VISIBLE);
//                        page.pageFileCounterText.setVisibility(View.VISIBLE);
//                        if (isDown)
//                            adapter.addDataRange(FilesListInformationGetter.informationList(l));
//                        else
//                            adapter.addDataRange(0, FilesListInformationGetter.informationList(l));
//                    });
//                }, e -> {
//                    onLoading.set(false);
//                    Main.runOnUiThread(activityMain, () -> {
//                        if (isDown) {
//                            if (noMoreDown.get())
//                                adapter.setTailor(0, PartList.this.listNoMoreView(activityMain.getLayoutInflater()));
//                            else
//                                adapter.removeTailor(0);
//                        } else
//                            adapter.removeHeader(0);
//                    });
//                }, false));
//            }
//        };
//        page.pageFileList.setAdapter(adapter);
//        page.pageFileList.clearOnScrollListeners();
//        page.pageFileList.addOnScrollListener(listener);
//        listener.onScrollStateChanged(page.pageFileList, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
//    }
//
//    @UiThread
//    protected void onRootPage(final long position) {
//        final PageFileBinding page = this.pageFile.page();
//        page.pageFileBacker.setImageResource(R.drawable.backer_nonclickable);
//        page.pageFileBacker.setOnClickListener(null);
//        page.pageFileBacker.setClickable(false);
//        page.pageFileName.setText(R.string.app_name);
//        this.updatePage(new FileLocation(IdentifierNames.RootSelector, 0), position, new AtomicBoolean(true),
//                (information, c) -> this.onInsidePage(FileInformationGetter.name(information),
//                        new FileLocation(FileInformationGetter.name(information), FileInformationGetter.id(information)), 0),
//                this.pageFile.partOperation::rootOperation);
//    }
//
//    @UiThread
//    protected void onInsidePage(final @NotNull CharSequence name, final @NotNull FileLocation location, final long position) {
//        final PageFileBinding page = this.pageFile.page();
//        final AtomicBoolean clickable = new AtomicBoolean(true);
//        page.pageFileBacker.setImageResource(R.drawable.backer);
//        page.pageFileBacker.setOnClickListener(v -> {
//            if (clickable.compareAndSet(true, false))
//                this.popFileList();
//        });
//        page.pageFileBacker.setClickable(true);
//        page.pageFileName.setText(name);
//        this.updatePage(location, position, clickable, (information, c) -> {
//            if (FileInformationGetter.isDirectory(information))
//                this.onInsidePage(FileInformationGetter.name(information), new FileLocation(FileLocationGetter.storage(location), FileInformationGetter.id(information)), 0);
//            else
//                this.pageFile.partPreview.preview(this.pageFile.activity(), FileLocationGetter.storage(location), information, c);
//        }, (information, c) -> this.pageFile.partOperation.insideOperation(FileLocationGetter.storage(location), information, c));
//    }
//
//    @AnyThread
//    private void backToRootPage() {
//        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> r = this.stacks.pollLast();
//        final long position = r == null || !IdentifierNames.RootSelector.equals(FileLocationGetter.storage(r.getA())) ? 0 : r.getC().get();
//        Main.runOnUiThread(this.pageFile.activity(), () -> this.onRootPage(position));
//    }
//
//    @UiThread
//    protected boolean popFileList() {
//        final Triad.ImmutableTriad<FileLocation, VisibleFileInformation, AtomicLong> p = this.stacks.poll();
//        if (p == null && this.isOnRoot()) return false;
//        final PageFileBinding page = this.pageFile.page();
//        page.pageFileBacker.setClickable(false);
//        page.pageFileCounter.setVisibility(View.GONE);
//        page.pageFileCounterText.setVisibility(View.GONE);
//        page.pageFileList.clearOnScrollListeners();
//        page.pageFileList.setAdapter(EmptyRecyclerAdapter.Instance);
//        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//            final VisibleFileInformation current;
//            try (final WListClientInterface client = this.pageFile.client()) {
//                current = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(), this.currentLocation(), true);
//            }
//            if (current == null) {
//                this.backToRootPage();
//                return;
//            }
//            final FileLocation parent;
//            final long position;
//            if (p == null || FileInformationGetter.parentId(current) != FileLocationGetter.id(p.getA())) {
//                this.stacks.clear();
//                parent = new FileLocation(FileLocationGetter.storage(this.currentLocation()), FileInformationGetter.parentId(current));
//                position = 0;
//            } else {
//                parent = p.getA();
//                position = p.getC().get();
//            }
//            final VisibleFileInformation directory;
//            try (final WListClientInterface client = this.pageFile.client()) {
//                directory = OperateFilesHelper.getFileOrDirectory(client, this.pageFile.token(), parent, true);
//            }
//            if (directory == null) {
//                this.backToRootPage();
//                return;
//            }
//            if (FileInformationGetter.id(current) == FileInformationGetter.id(directory))
//                Main.runOnUiThread(this.pageFile.activity(), () -> this.onRootPage(position));
//            else
//                Main.runOnUiThread(this.pageFile.activity(), () -> this.onInsidePage(FileInformationGetter.name(directory), parent, position));
//        }));
//        return true;
//    }
}
