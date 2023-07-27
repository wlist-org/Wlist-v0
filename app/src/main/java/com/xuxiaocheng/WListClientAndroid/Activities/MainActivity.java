package com.xuxiaocheng.WListClientAndroid.Activities;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.AndroidSupports.FileLocationSupporter;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.SpecialDriverName;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.databinding.FileListContentBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.UserListContentBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    public static void start(@NonNull final Activity activity, @NonNull final InetSocketAddress address) {
        final Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("host", address.getHostName()).putExtra("port", address.getPort());
        activity.runOnUiThread(() -> activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()));
    }

    @Nullable protected InetSocketAddress extraAddress() {
        final Intent intent = this.getIntent();
        final String host = intent.getStringExtra("host");
        final int port = intent.getIntExtra("port", -1);
        if (host == null || port == -1)
            return null;
        return new InetSocketAddress(host, port);
    }

    @NonNull protected final AtomicReference<MainTab.TabChoice> minTabChoice = new AtomicReference<>();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating MainActivity.");
        this.setContentView(R.layout.main_activity);
        final InetSocketAddress address = this.extraAddress();
        if (address == null) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No address received."));
            this.finish();
            return;
        }
        final MainTab mainTab = new MainTab(
            new MainTab.ButtonGroup(this, R.id.main_tab_file, R.id.main_tab_file_button, R.id.main_tab_file_text,
                    R.drawable.main_tab_file, R.drawable.main_tab_file_chose, R.color.black, R.color.red),
            new MainTab.ButtonGroup(this, R.id.main_tab_user, R.id.main_tab_user_button, R.id.main_tab_user_text,
                    R.drawable.main_tab_user, R.drawable.main_tab_user_chose, R.color.black, R.color.red)
        );
        final AtomicReference<View> currentView = new AtomicReference<>(new View(this));
        final ConstraintLayout activity = this.findViewById(R.id.main_activity);
        final ConstraintLayout.LayoutParams contentParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        contentParams.bottomToTop = R.id.main_tab_guideline;
        contentParams.leftToLeft = R.id.main_activity;
        contentParams.rightToRight = R.id.main_activity;
        contentParams.topToBottom = R.id.main_title_guideline;
        mainTab.setOnChangeListener(choice -> {
            logger.log(HLogLevel.DEBUG, "Choosing main tab: ", choice);
            synchronized (currentView) {
                final View oldView = currentView.getAndSet(null);
                if (oldView != null)
                    activity.removeView(oldView);
            }
            final View newView = switch (choice) {
                case File -> this.onChangeFile(address);
                case User -> this.onChangeUser(address);
            };
            synchronized (currentView) {
                if (currentView.compareAndSet(null, newView))
                    activity.addView(newView, contentParams);
            }
            this.minTabChoice.set(choice);
        });
        mainTab.click(MainTab.TabChoice.File);
    }

    @NonNull private final AtomicReference<View> UserPageCache = new AtomicReference<>();
    @NonNull private View onChangeUser(@NonNull final InetSocketAddress address) {
        final View cache = this.UserPageCache.get();
        if (cache != null)
            return cache;
        final ConstraintLayout page = UserListContentBinding.inflate(this.getLayoutInflater()).getRoot();
        final TextView disconnection = (TextView) page.getViewById(R.id.user_list_close_internal_server);
        final AtomicBoolean closed = new AtomicBoolean(false);
        disconnection.setOnClickListener(v -> {
            if (!closed.compareAndSet(false, true))
                return;
            Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                final boolean success;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                    success = OperateServerHelper.closeServer(client, TokenManager.getToken());
                }
                if (success) {
                    this.runOnUiThread(() -> this.startActivity(new Intent(this, LoginActivity.class)));
                    this.finish();
                } else
                    closed.set(false);
            })).addListener(Main.ThrowableListenerWithToast(this));
        });
        this.UserPageCache.set(page);
        return page;
    }

    @NonNull private final AtomicReference<View> FilePageCache = new AtomicReference<>();
    @NonNull private final Deque<Triad.ImmutableTriad<Pair.ImmutablePair<FileLocation, Integer>, Pair.ImmutablePair<InetSocketAddress, ConstraintLayout>, CharSequence>> FileListStack = new ArrayDeque<>();
    @NonNull private View onChangeFile(@NonNull final InetSocketAddress address) {
        final View cache = this.FilePageCache.get();
        if (cache != null)
            return cache;
        final ConstraintLayout page = FileListContentBinding.inflate(this.getLayoutInflater()).getRoot();
        page.getViewById(R.id.file_list_backer).setOnClickListener(v -> this.onBackPressed());
        ((TextView) page.getViewById(R.id.file_list_name)).setText(R.string.app_name);
        Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> this.setFileList(address,
                        new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0), 0, page)))
                .addListener(Main.ThrowableListenerWithToast(MainActivity.this));
        this.FilePageCache.set(page);
        return page;
    }

    private void setFileList(@NonNull final InetSocketAddress address, @NonNull final FileLocation directoryLocation, final int currentPage, @NonNull final ConstraintLayout page) throws WrongStateException, IOException, InterruptedException {
        final TextView name = (TextView) page.getViewById(R.id.file_list_name);
        final TextView count = (TextView) page.getViewById(R.id.file_list_counter);
        final ListView content = (ListView) page.getViewById(R.id.file_list_content);
        final TextView pageCurrent = (TextView) page.getViewById(R.id.file_list_page_current);
        final TextView pageAll = (TextView) page.getViewById(R.id.file_list_page_all);
        final TextView left = (TextView) page.getViewById(R.id.file_list_page_left_bottom);
        final TextView right = (TextView) page.getViewById(R.id.file_list_page_right_bottom);
        final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list;
        // TODO loading anim
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> tmp = OperateFileHelper.listFiles(client, TokenManager.getToken(), directoryLocation,
                    20, currentPage, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
            list = tmp == null ? Pair.ImmutablePair.makeImmutablePair(-1L, List.of()) : tmp;
        }
        final List<Map<String, Object>> resources = new ArrayList<>(list.getSecond().size());
        for (final VisibleFileInformation information: list.getSecond()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("image", R.mipmap.app_logo);
            map.put("name", FileInformationGetter.name(information));
            final LocalDateTime update = FileInformationGetter.updateTime(information);
            map.put("tip", update == null ? "unknown" : update.format(DateTimeFormatter.ISO_DATE_TIME));
            resources.add(map);
        }
        final int allPage = MiscellaneousUtil.calculatePartCount(list.getFirst().intValue(), 20);
        final boolean isRoot = SpecialDriverName.RootDriver.getIdentifier().equals(FileLocationSupporter.driver(directoryLocation));
        final String countS = String.format(Locale.getDefault(), "%d", list.getFirst());
        final String currentPageS = String.format(Locale.getDefault(), "%d", currentPage + 1);
        final String allPageS = String.format(Locale.getDefault(), "%d", Math.max(allPage, 1));
        final ListAdapter adapter = new SimpleAdapter(this, resources, R.layout.file_list_cell,
                new String[] {"image", "name", "tip"},
                new int[] {R.id.file_list_image, R.id.file_list_name, R.id.file_list_tip});
        this.runOnUiThread(() -> {
            count.setText(countS);
            pageCurrent.setText(currentPageS);
            pageAll.setText(allPageS);
            if (currentPage <= 0) {
                left.setTextColor(this.getResources().getColor(R.color.nonclickable, this.getTheme()));
                left.setOnClickListener(null);
                left.setClickable(false);
            } else {
                left.setTextColor(this.getResources().getColor(R.color.black, this.getTheme()));
                left.setOnClickListener(v -> Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() ->
                                this.setFileList(address, directoryLocation, currentPage - 1, page)))
                        .addListener(Main.ThrowableListenerWithToast(MainActivity.this)));
                left.setClickable(true);
            }
            if (currentPage >= allPage - 1) {
                right.setTextColor(this.getResources().getColor(R.color.nonclickable, this.getTheme()));
                right.setOnClickListener(null);
                right.setClickable(false);
            } else {
                right.setTextColor(this.getResources().getColor(R.color.black, this.getTheme()));
                right.setOnClickListener(v -> Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() ->
                                this.setFileList(address, directoryLocation, currentPage + 1, page)))
                        .addListener(Main.ThrowableListenerWithToast(MainActivity.this)));
                right.setClickable(true);
            }
            content.setAdapter(adapter);
            final AtomicBoolean clickable = new AtomicBoolean(true);
            content.setOnItemClickListener((a, v, i, l) -> {
                if (!clickable.compareAndSet(true, false))
                    return;
                this.FileListStack.push(Triad.ImmutableTriad.makeImmutableTriad(Pair.ImmutablePair.makeImmutablePair(directoryLocation, currentPage),
                        Pair.ImmutablePair.makeImmutablePair(address, page), name.getText()));
                final VisibleFileInformation information = list.getSecond().get(i);
                if (FileInformationGetter.isDirectory(information))
                    Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                        final FileLocation location;
                        if (isRoot)
                            location = FileLocationSupporter.create(FileInformationGetter.name(information), 0);
                        else
                            location = FileLocationSupporter.create(FileLocationSupporter.driver(directoryLocation), FileInformationGetter.id(information));
                        this.setFileList(address, location, 0, page);
                        name.setText(FileInformationGetter.name(information));
                    })).addListener(Main.ThrowableListenerWithToast(MainActivity.this));
            });
        });
    }

    @Override
    public void onBackPressed() {
        final MainTab.TabChoice choice = this.minTabChoice.get();
        if (choice != null)
            switch (choice) {
                case File -> {
                    final Triad.ImmutableTriad<Pair.ImmutablePair<FileLocation, Integer>, Pair.ImmutablePair<InetSocketAddress, ConstraintLayout>, CharSequence> p = this.FileListStack.poll();
                    if (p == null)
                        break;
                    Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                                this.setFileList(p.getB().getFirst(), p.getA().getFirst(),
                                        p.getA().getSecond().intValue(), p.getB().getSecond());
                                this.runOnUiThread(() -> ((TextView) p.getB().getSecond().getViewById(R.id.file_list_name)).setText(p.getC()));
                            })).addListener(Main.ThrowableListenerWithToast(MainActivity.this));
                    return;
                }
                case User -> {} // TODO
            }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying MainActivity.");
    }
}
