package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.app.Activity;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import com.xuxiaocheng.WListClientAndroid.databinding.FileListContentBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        final ConstraintLayout page = FileListContentBinding.inflate(this.activity.getLayoutInflater()).getRoot();
        this.pageCache.initialize(page);
        ((TextView) page.getViewById(R.id.file_list_name)).setText(R.string.app_name);
        Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() ->
                        this.setFileList(this.address, new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0))))
                .addListener(Main.exceptionListenerWithToast(this.activity));
        return page;
    }

    @NonNull protected final AtomicBoolean clickable = new AtomicBoolean(true);
    @NonNull protected final Deque<Pair.ImmutablePair<FileLocation, CharSequence>> fileListStack = new ArrayDeque<>();

    protected void setFileList(@NonNull final InetSocketAddress address, @NonNull final FileLocation directoryLocation) throws WrongStateException, IOException, InterruptedException {
        final ConstraintLayout page = this.onChange();
        final TextView backer = (TextView) page.getViewById(R.id.file_list_backer);
        final TextView name = (TextView) page.getViewById(R.id.file_list_name);
        final TextView counter = (TextView) page.getViewById(R.id.file_list_counter);
        final RecyclerView content = (RecyclerView) page.getViewById(R.id.file_list_content);
        final AtomicInteger currentPage = new AtomicInteger(0);
        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> lists;
        // TODO loading anim
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            // TODO: more configurable params.
            lists = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                    Options.DirectoriesOrFiles.Both, 1, currentPage.get(), Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
        }
        if (lists == null) {
            // TODO: directory not exists.
            return;
        }
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(directoryLocation));
        final List<VisibleFileInformation> list = new ArrayList<>(lists.getC());
        final int allPage = MiscellaneousUtil.calculatePartCount( lists.getB().intValue(), 1);
        final FileListAdapter adapter = new FileListAdapter(isRoot, list, this.activity.getLayoutInflater(), information -> {
            if (this.clickable.compareAndSet(true, false)) {
                this.fileListStack.push(Pair.ImmutablePair.makeImmutablePair(directoryLocation, name.getText()));
                final AtomicBoolean failure = new AtomicBoolean(true);
                Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                    if (FileInformationGetter.isDirectory(information)) {
                        this.setFileList(address, FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(directoryLocation), FileInformationGetter.id(information)));
                        this.activity.runOnUiThread(() -> name.setText(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information)));
                    } else {
                        // TODO: show file.
                        throw new UnsupportedOperationException("Show file is unsupported now!");
                    }
                    failure.set(false);
                }, () -> {
                    this.clickable.set(true);
                    if (failure.get())
                        this.fileListStack.pop();
                })).addListener(Main.exceptionListenerWithToast(this.activity));
            }
        });
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
            content.setAdapter(adapter);
            content.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
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
                            if (l == null)
                                FilePage.this.activity.runOnUiThread(() -> {
                                    FilePage.this.onBackPressed();
                                    Toast.makeText(FilePage.this.activity, R.string.toast_directory_not_available, Toast.LENGTH_SHORT).show();
                                });
                            else {
                                final int pos = list.size();
                                list.addAll(l.getC());
                                FilePage.this.activity.runOnUiThread(() -> adapter.notifyItemRangeInserted(pos, l.getC().size()));
                            }
                        }, () -> FilePage.this.clickable.set(true))).addListener(Main.exceptionListenerWithToast(FilePage.this.activity));
                    }
                }
            });
        });
    }

    @Override
    public boolean onBackPressed() {
        final Pair.ImmutablePair<FileLocation, CharSequence> p = this.fileListStack.poll();
        if (p == null) return false;
        if (this.clickable.compareAndSet(true, false))
            Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                this.setFileList(this.address, p.getFirst());
                this.activity.runOnUiThread(() -> ((TextView) this.onChange().getViewById(R.id.file_list_name)).setText(p.getSecond()));
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
}
