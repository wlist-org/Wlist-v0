package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.app.Activity;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.databinding.FileListContentBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
                        this.setFileList(this.address, new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0), 0)))
                .addListener(Main.exceptionListenerWithToast(this.activity));
        return page;
    }

    @NonNull protected final AtomicBoolean clickable = new AtomicBoolean(true);
    @NonNull protected final Deque<Triad<FileLocation, Integer, CharSequence>> fileListStack = new ArrayDeque<>();

    protected void setFileList(@NonNull final InetSocketAddress address, @NonNull final FileLocation directoryLocation, final int currentPage) throws WrongStateException, IOException, InterruptedException {
        final ConstraintLayout page = this.onChange();
        final TextView backer = (TextView) page.getViewById(R.id.file_list_backer);
        final TextView name = (TextView) page.getViewById(R.id.file_list_name);
        final TextView counter = (TextView) page.getViewById(R.id.file_list_counter);
        final RecyclerView content = (RecyclerView) page.getViewById(R.id.file_list_content);
        final Triad.ImmutableTriad<Long, Long, List<VisibleFileInformation>> list;
        // TODO loading anim
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            // TODO: more configurable params.
            list = OperateFileHelper.listFiles(client, TokenManager.getToken(address), directoryLocation,
                    Options.DirectoriesOrFiles.Both, 50, currentPage, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
        }
        if (list == null) {
            // TODO: directory not exists.
            return;
        }
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(directoryLocation));
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
            counter.setText(String.valueOf(list.getB()));
            content.setLayoutManager(new LinearLayoutManager(this.activity));
//        TODO    final int allPage = MiscellaneousUtil.calculatePartCount( list.getB().intValue(), 50);
            content.setAdapter(new FileListAdapter(isRoot, list.getC(), this.activity.getLayoutInflater(), information -> {
                if (this.clickable.compareAndSet(true, false)) {
                    this.fileListStack.push(Triad.makeTriad(directoryLocation, currentPage, name.getText()));
                    final AtomicBoolean failure = new AtomicBoolean(true);
                    Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                        if (FileInformationGetter.isDirectory(information)) {
                            this.setFileList(address, FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(directoryLocation), FileInformationGetter.id(information)), 0);
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
            }));
//    TODO            left.setOnClickListener(v -> {
//                    if (this.clickable.compareAndSet(true, false))
//                        Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() ->
//                                        this.setFileList(address, directoryLocation, currentPage - 1),
//                                () -> this.clickable.set(true))).addListener(Main.exceptionListenerWithToast(this.activity));
//                });
        });
    }

    @Override
    public boolean onBackPressed() {
        final Triad<FileLocation, Integer, CharSequence> p = this.fileListStack.poll();
        if (p == null) return false;
        if (this.clickable.compareAndSet(true, false))
            Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                this.setFileList(this.address, p.getA(), p.getB().intValue());
                this.activity.runOnUiThread(() -> ((TextView) this.onChange().getViewById(R.id.file_list_name)).setText(p.getC()));
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
