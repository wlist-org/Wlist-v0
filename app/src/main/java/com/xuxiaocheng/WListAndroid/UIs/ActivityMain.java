package com.xuxiaocheng.WListAndroid.UIs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
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


    protected final @NotNull HInitializer<ActivityMainBinding> contentCache = new HInitializer<>("ActivityMainCache");
    protected final @NotNull FragmentsAdapter fragmentsAdapterInstance = new FragmentsAdapter(this);

    public @NotNull ActivityMainBinding getContent() {
        return this.contentCache.getInstance();
    }

    protected final @NotNull AtomicReference<FragmentsAdapter.FragmentTypes> currentChoice = new AtomicReference<>();

    public FragmentsAdapter.@NotNull FragmentTypes currentChoice() {
        return this.currentChoice.get();
    }


    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("ActivityMainAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("ActivityMainUsername");
    protected final @NotNull HInitializer<IBinder> binder = new HInitializer<>("ActivityMainServiceBinder");

    public @NotNull InetSocketAddress address() {
        return this.address.getInstance();
    }

    public @NotNull String username() {
        return this.username.getInstance();
    }


    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityMain.");
        final ActivityMainBinding activity = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(activity.getRoot());
        this.contentCache.reinitialize(activity);


        if (this.extraAddress()) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No address received."));
            this.close();
            return;
        }

        activity.activityMainContent.setAdapter(this.fragmentsAdapterInstance);
        final ChooserButtonGroup fileButton = new ChooserButtonGroup(this, activity.activityMainChooserFileImage, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose, activity.activityMainChooserFileText, activity.activityMainChooserFile);
        final ChooserButtonGroup userButton = new ChooserButtonGroup(this, activity.activityMainChooserUserImage, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose, activity.activityMainChooserUserText, activity.activityMainChooserUser);
        final ChooserButtonGroup transButton = new ChooserButtonGroup(this, activity.activityMainTrans, R.mipmap.main_chooser_trans, R.mipmap.main_chooser_trans_chose, null);
        fileButton.setOnClickListener(v -> activity.activityMainContent.setCurrentItem(FragmentsAdapter.FragmentTypes.toPosition(FragmentsAdapter.FragmentTypes.File)));
        userButton.setOnClickListener(v -> activity.activityMainContent.setCurrentItem(FragmentsAdapter.FragmentTypes.toPosition(FragmentsAdapter.FragmentTypes.User)));
        transButton.setOnClickListener(v -> activity.activityMainContent.setCurrentItem(FragmentsAdapter.FragmentTypes.toPosition(FragmentsAdapter.FragmentTypes.Trans)));
        activity.activityMainContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final FragmentsAdapter.FragmentTypes now = FragmentsAdapter.FragmentTypes.fromPosition(position);
                switch (now) {
                    case File -> {
                        if (fileButton.isClicked()) return;
                        fileButton.setClickable(false);
                        userButton.setClickable(true);
                        transButton.setClickable(true);
                    }
                    case User -> {
                        if (userButton.isClicked()) return;
                        fileButton.setClickable(true);
                        userButton.setClickable(false);
                        transButton.setClickable(true);
                    }
                    case Trans -> {
                        if (transButton.isClicked()) return;
                        fileButton.setClickable(true);
                        userButton.setClickable(true);
                        transButton.setClickable(false);
                    }
                }
                final FragmentsAdapter.FragmentTypes old = ActivityMain.this.currentChoice.getAndSet(now);
                if (old != null)
                    ActivityMain.this.fragmentsAdapterInstance.getFragment(old).onHide();
            }
        });
        Arrays.stream(FragmentsAdapter.FragmentTypes.values()).forEach(t -> this.fragmentsAdapterInstance.getFragment(t).onActivityCreateHook());
        fileButton.callOnClick();
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @Override
    public void onBackPressed() {
        final FragmentsAdapter.FragmentTypes choice = this.currentChoice.get();
        if (choice != null && this.fragmentsAdapterInstance.getFragment(choice).onBackPressed()) return;
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

    @UiThread
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
