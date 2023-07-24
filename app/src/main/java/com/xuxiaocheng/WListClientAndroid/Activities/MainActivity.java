package com.xuxiaocheng.WListClientAndroid.Activities;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
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

import java.net.InetSocketAddress;
import java.util.ArrayList;
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
        });
        mainTab.click(MainTab.TabChoice.File);
    }

    @NonNull private View onChangeUser(@NonNull final InetSocketAddress address) {
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
                }
            })).addListener(Main.ThrowableListenerWithToast(this));
        });
        return page;
    }

    @NonNull private View onChangeFile(@NonNull final InetSocketAddress address) {
        final ConstraintLayout page = FileListContentBinding.inflate(this.getLayoutInflater()).getRoot();
        final TextView counter = (TextView) page.getViewById(R.id.file_list_counter);
        final ListView content = (ListView) page.getViewById(R.id.file_list_content);
        // TODO get list.
        Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                list = OperateFileHelper.listFiles(client, TokenManager.getToken(), new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0),
                        20, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
            }
            if (list != null) {
                this.setList(list, counter, content);
                content.setOnItemClickListener((adapter, view, i, l) -> {
                    Toast.makeText(this, FileInformationGetter.name(list.getSecond().get(i)), Toast.LENGTH_SHORT).show();
                });
            }
        })).addListener(Main.ThrowableListenerWithToast(MainActivity.this));
        return page;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setList(@NonNull final Pair.ImmutablePair<Long, ? extends List<VisibleFileInformation>> list, @NonNull final TextView counter, @NonNull final ListView content) {
        final List<Map<String, Object>> resources = new ArrayList<>();
        for (final VisibleFileInformation information: list.getSecond()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("image", R.mipmap.app_logo);
            map.put("name", FileInformationGetter.name(information));
            map.put("tip", FileInformationGetter.id(information));
            resources.add(map);
        }
        final String count = String.format(Locale.getDefault(), "%d", list.getFirst());
        final ListAdapter adapter = new SimpleAdapter(this, resources, R.layout.file_list_cell,
                new String[] {"image", "name", "tip"},
                new int[] {R.id.file_list_image, R.id.file_list_name, R.id.file_list_tip});
        this.runOnUiThread(() -> {
            counter.setText(count);
            content.setAdapter(adapter);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying MainActivity.");
    }
}
