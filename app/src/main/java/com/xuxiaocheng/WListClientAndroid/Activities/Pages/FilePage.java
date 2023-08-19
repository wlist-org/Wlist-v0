package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
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
import com.xuxiaocheng.WListClientAndroid.Utils.RecyclerViewHeadersAndTailorsAdapterWrapper;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileContentBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    @NonNull protected final HInitializer<ConstraintLayout> pageCache = new HInitializer<>("FilePage");
    @NonNull public ConstraintLayout onChange() {
        final ConstraintLayout cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache;
        final ConstraintLayout page = PageFileContentBinding.inflate(this.activity.getLayoutInflater()).getRoot();
        this.pageCache.initialize(page);
        ((TextView) page.getViewById(R.id.page_file_content_name)).setText(R.string.app_name);
        Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() ->
                        this.setFileList(this.address, new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0))))
                .addListener(Main.exceptionListenerWithToast(this.activity));
        return page;
    }

    @NonNull protected final AtomicBoolean clickable = new AtomicBoolean(true);
    @NonNull protected final Deque<Pair.ImmutablePair<FileLocation, CharSequence>> fileListStack = new ArrayDeque<>(); // TODO: save adapter.

    protected void setFileList(@NonNull final InetSocketAddress address, @NonNull final FileLocation directoryLocation) throws WrongStateException, IOException, InterruptedException {
        final ConstraintLayout page = this.onChange();
        final TextView backer = (TextView) page.getViewById(R.id.page_file_content_backer);
        final TextView name = (TextView) page.getViewById(R.id.page_file_content_name);
        final TextView counter = (TextView) page.getViewById(R.id.page_file_content_counter);
        final RecyclerView content = (RecyclerView) page.getViewById(R.id.page_file_content_list);
        final AtomicInteger currentPage = new AtomicInteger(0);
        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> lists;
        // TODO loading anim
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            // TODO: more configurable params.
            lists = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                    Options.DirectoriesOrFiles.Both, 50, currentPage.get(), Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
        }
        if (lists == null) {
            // TODO: directory not exists.
            return;
        }
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(directoryLocation));
        final List<VisibleFileInformation> list = new ArrayList<>(lists.getC());
        final int allPage = MiscellaneousUtil.calculatePartCount( lists.getB().intValue(), 50);
        final RecyclerViewHeadersAndTailorsAdapterWrapper<CellViewHolder> adapterWrapper = new RecyclerViewHeadersAndTailorsAdapterWrapper<>(new RecyclerView.Adapter<>() {
            @Override
            @NonNull public CellViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
                return new CellViewHolder((ConstraintLayout) FilePage.this.activity.getLayoutInflater().inflate(R.layout.page_file_cell, parent, false), information -> {
                    if (FilePage.this.clickable.compareAndSet(true, false)) {
                        FilePage.this.fileListStack.push(Pair.ImmutablePair.makeImmutablePair(directoryLocation, name.getText()));
                        final AtomicBoolean failure = new AtomicBoolean(true);
                        Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                            if (FileInformationGetter.isDirectory(information)) {
                                FilePage.this.setFileList(address, FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(directoryLocation), FileInformationGetter.id(information)));
                                FilePage.this.activity.runOnUiThread(() -> name.setText(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information)));
                            } else {
                                // TODO: show file.
                                throw new UnsupportedOperationException("Show file is unsupported now!");
                            }
                            failure.set(false);
                        }, () -> {
                            FilePage.this.clickable.set(true);
                            if (failure.get())
                                FilePage.this.fileListStack.pop();
                        })).addListener(Main.exceptionListenerWithToast(FilePage.this.activity));
                    }
                });
            }

            @Override
            public void onBindViewHolder(@NonNull final CellViewHolder holder, final int position) {
                holder.setItem(list.get(position), isRoot);
            }

            @Override
            public int getItemCount() {
                return list.size();
            }
        });
        // TODO: register broadcast listener.
        final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                if (!recyclerView.canScrollVertically(1) && FilePage.this.clickable.compareAndSet(true, false)) {
                    Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                        final int nextPage = currentPage.incrementAndGet();
                        if (nextPage >= allPage) {
                            currentPage.decrementAndGet();
                            return;
                        }
                        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> l;
                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                            l = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                                    Options.DirectoriesOrFiles.Both, 1, nextPage, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
                        }
                        if (l == null) {
                            FilePage.this.activity.runOnUiThread(() -> {
                                FilePage.this.onBackPressed();
                                Toast.makeText(FilePage.this.activity, R.string.page_file_unavailable_directory, Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }
                        final int pos = list.size();
                        list.addAll(l.getC());
                        FilePage.this.activity.runOnUiThread(() -> {
                            adapterWrapper.notifyItemRangeInserted(adapterWrapper.headersSize() + pos, l.getC().size());
                            this.onScrollStateChanged(recyclerView, newState);
                        });
                    }, () -> FilePage.this.clickable.set(true))).addListener(Main.exceptionListenerWithToast(FilePage.this.activity));
                }
            }
        };
        this.activity.runOnUiThread(() -> {
            if (isRoot) {
                backer.setTextColor(this.activity.getResources().getColor(R.color.nonclickable, this.activity.getTheme()));
                backer.setOnClickListener(null);
                backer.setClickable(false);
            } else {
                backer.setTextColor(this.activity.getResources().getColor(R.color.normal_text, this.activity.getTheme()));
                backer.setOnClickListener(v -> this.onBackPressed());
                backer.setClickable(true);
            }
            counter.setText(String.valueOf(lists.getB()));
            final LinearLayoutManager layoutManager = new LinearLayoutManager(this.activity);
            content.setLayoutManager(layoutManager);
            content.setAdapter(adapterWrapper);
            content.clearOnScrollListeners();
            content.addOnScrollListener(scrollListener);
            scrollListener.onScrollStateChanged(content, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        });
    }

    @Override
    public boolean onBackPressed() {
        final Pair.ImmutablePair<FileLocation, CharSequence> p = this.fileListStack.poll();
        if (p == null) return false;
        if (this.clickable.compareAndSet(true, false))
            Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                this.setFileList(this.address, p.getFirst());
                this.activity.runOnUiThread(() -> ((TextView) this.onChange().getViewById(R.id.page_file_content_name)).setText(p.getSecond()));
            }, () -> this.clickable.set(true))).addListener(Main.exceptionListenerWithToast(this.activity));
        return true;
    }

    @Override
    @NonNull public String toString() {
        return "FilePage{" +
                "activity=" + this.activity +
                ", address=" + this.address +
                ", pageCache=" + this.pageCache +
                ", clickable=" + this.clickable +
                ", fileListStack=" + this.fileListStack +
                '}';
    }

    protected static class CellViewHolder extends RecyclerViewHeadersAndTailorsAdapterWrapper.WrappedViewHolder<ConstraintLayout> {
        @NonNull protected final Consumer<VisibleFileInformation> clicker;
        @NonNull protected final ImageView image;
        @NonNull protected final TextView name;
        @NonNull protected final TextView tips;
        @NonNull protected final View option;

        protected CellViewHolder(@NonNull final ConstraintLayout cell, @NonNull final Consumer<VisibleFileInformation> clicker) {
            super(cell);
            this.clicker = clicker;
            this.image = (ImageView) cell.getViewById(R.id.page_file_cell_image);
            this.name = (TextView) cell.getViewById(R.id.page_file_cell_name);
            this.tips = (TextView) cell.getViewById(R.id.page_file_cell_tips);
            this.option = cell.getViewById(R.id.page_file_cell_option);
        }

        public void setItem(@NonNull final VisibleFileInformation information, final boolean isRoot) {
            this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
            CellViewHolder.setFileImage(this.image, information);
            this.name.setText(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information));
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
            final String name = FileInformationGetter.name(information).toLowerCase(Locale.ROOT);
            final int index = name.lastIndexOf('.');
            // TODO: cached Drawable.
            image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1)) {
                case "doc", "docx" -> R.mipmap.page_file_image_docx;
                case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
                case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
                case "zip", "7z", "rar", "gz" -> R.mipmap.page_file_image_zip;
                default -> R.mipmap.page_file_image_file;
            });
        }
    }
}
