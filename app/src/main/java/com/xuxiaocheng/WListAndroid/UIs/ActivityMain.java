package com.xuxiaocheng.WListAndroid.UIs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityMain extends AppCompatActivity {
    public static void start(final @NotNull Activity activity, final @NotNull InetSocketAddress address, final @NotNull String username, final boolean isInternal) {
        final Intent intent = new Intent(activity, ActivityMain.class);
        intent.putExtra("host", address.getHostName()).putExtra("port", address.getPort());
        intent.putExtra("name", username).putExtra("service", isInternal);
        Main.runOnUiThread(activity, () -> activity.startActivity(intent));
    }

    protected boolean extraAddress() {
        final Intent intent = this.getIntent();
        final String host = intent.getStringExtra("host");
        final int port = intent.getIntExtra("port", -1);
        final String name = intent.getStringExtra("name");
        if (host == null || port == -1 || name == null)
            return true;
        final boolean isInternal = intent.getBooleanExtra("service", false);
        this.address.reinitialize(new InetSocketAddress(host, port));
        this.username.reinitialize(name);
        if (isInternal)
            this.bindService(new Intent(this, InternalServerService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder service) {
                    ActivityMain.this.binder.reinitialize(service);
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    ActivityMain.this.binder.uninitializeNullable();
                }
            }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
        return false;
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("ActivityMainAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("ActivityMainUsername");
    protected final @NotNull HInitializer<IBinder> binder = new HInitializer<>("ActivityMainServiceBinder");

    protected final @NotNull AtomicReference<ActivityMainChooser.MainChoice> currentChoice = new AtomicReference<>();
    protected final @NotNull Map<ActivityMainChooser.MainChoice, ActivityMainChooser.MainPage> pages = new EnumMap<>(ActivityMainChooser.MainChoice.class); {
        this.pages.put(ActivityMainChooser.MainChoice.File, new PageFile(this));
        this.pages.put(ActivityMainChooser.MainChoice.User, new PageUser(this));
        this.pages.put(ActivityMainChooser.MainChoice.Trans, new PageTrans(this));
    }

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityMain.");
        final ActivityMainBinding activity = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(activity.getRoot());
        if (this.extraAddress()) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No address received."));
            this.close();
            return;
        }
        final ActivityMainChooser chooser = new ActivityMainChooser(
                new ActivityMainChooser.ButtonGroup(this, activity.activityMainChooserFileButton, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose,
                        activity.activityMainChooserFileText, activity.activityMainChooserFile),
                new ActivityMainChooser.ButtonGroup(this, activity.activityMainChooserUserButton, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose,
                        activity.activityMainChooserUserText, activity.activityMainChooserUser),
                new ActivityMainChooser.ButtonGroup(this, activity.activityMainTrans, R.mipmap.main_chooser_trans, R.mipmap.main_chooser_trans_chose, null)
        );
        final AtomicReference<View> currentView = new AtomicReference<>(null);
        final ConstraintLayout.LayoutParams contentParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        contentParams.bottomToTop = R.id.activity_main_guideline_chooser;
        contentParams.leftToLeft = R.id.activity_main;
        contentParams.rightToRight = R.id.activity_main;
        contentParams.topToBottom = R.id.activity_main_guideline_title;
        chooser.setOnChangeListener(choice -> {
            final View oldView;
            final ActivityMainChooser.MainChoice oldChoice;
            synchronized (currentView) {
                oldView = currentView.getAndSet(null);
                oldChoice = this.currentChoice.getAndSet(null);
            }
            if (oldView != null)
                activity.getRoot().removeView(oldView);
            if (oldChoice != null)
                Objects.requireNonNull(this.pages.get(oldChoice)).onHide();
            final View newView = Objects.requireNonNull(this.pages.get(choice)).onShow();
            final boolean ok;
            synchronized (currentView) {
                ok = currentView.compareAndSet(null, newView);
                this.currentChoice.set(choice);
            }
            if (ok)
                activity.getRoot().addView(newView, contentParams);
        });
        this.pages.values().forEach(ActivityMainChooser.MainPage::onActivityCreateHook);
        Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
            BroadcastAssistant.start(this.address.getInstance());
            ClientConfigurationSupporter.location().reinitialize(new File(this.getExternalFilesDir("client"), "client.yaml"));
            ClientConfigurationSupporter.parseFromFile();
            Main.runOnUiThread(this, () -> chooser.click(ActivityMainChooser.MainChoice.File));
        }));
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @Override
    public void onBackPressed() {
        final ActivityMainChooser.MainChoice choice = this.currentChoice.get();
        if (choice != null) {
            final ActivityMainChooser.MainPage page = this.pages.get(choice);
            assert page != null;
            if (page.onBackPressed())
                return;
        }
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.lastBackPressedTime != null && Duration.between(this.lastBackPressedTime, now).toMillis() < 2000) {
            super.onBackPressed(); // this.finish();
            return;
        }
        Main.showToast(this, R.string.toast_press_again_to_exit);
        this.lastBackPressedTime = now;
    }

    @Override
    protected void onPause() {
        super.onPause();
        WListClientManager.removeAllListeners(this.address.getInstance());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (WListClientManager.instances.isNotInitialized(this.address.getInstance())) {
            Main.showToast(this, R.string.activity_main_server_closed);
            this.close();
            return;
        }
        WListClientManager.addListener(this.address.getInstance(), i -> {
            if (!i.booleanValue()) {
                Main.runOnUiThread(this, () -> {
                    Main.showToast(this, R.string.activity_main_server_closed);
                    this.close();
                });
                WListClientManager.removeAllListeners(this.address.getInstance());
            }
        });
    }

    public void close() {
        this.startActivity(new Intent(this, ActivityLogin.class));
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying ActivityMain.");
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "minTabChoice=" + this.currentChoice +
                ", address=" + this.address +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                ", super=" + super.toString() +
                '}';
    }
}
