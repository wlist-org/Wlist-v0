package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityMain extends AppCompatActivity {
    protected final @NotNull HInitializer<ActivityMainBinding> contentCache = new HInitializer<>("ActivityMainCache");
    public @NotNull ActivityMainBinding getContent() {
        return this.contentCache.getInstance();
    }
    protected final @NotNull HInitializer<FragmentsAdapter> fragmentsAdapter = new HInitializer<>("FragmentsAdapter");

    protected final @NotNull AtomicReference<FragmentsAdapter.FragmentTypes> currentChoice = new AtomicReference<>();
    public FragmentsAdapter.@NotNull FragmentTypes currentChoice() {
        return this.currentChoice.get();
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("ActivityMainAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("ActivityMainUsername");
    protected final @NotNull HInitializer<IBinder> binder = new HInitializer<>("ActivityMainServiceBinder");
    public boolean isConnected() {
        return this.address.isInitialized() && this.username.isInitialized();
    }
    public @NotNull InetSocketAddress address() {
        InetSocketAddress address = this.address.getInstanceNullable();
        if (address == null) {
            this.disconnect();
            address = this.address.getInstance();
        }
        return address;
    }
    public @NotNull String username() {
        String username = this.username.getInstanceNullable();
        if (username == null) {
            this.disconnect();
            username = this.username.getInstance();
        }
        return username;
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
        final FragmentsAdapter fragmentsAdapter = new FragmentsAdapter(this);
        activity.activityMainContent.setAdapter(fragmentsAdapter);
        this.fragmentsAdapter.reinitialize(fragmentsAdapter);
        final ChooserButtonGroup fileButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.File, activity.activityMainChooserFileImage, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose, activity.activityMainChooserFileText, activity.activityMainChooserFile);
        final ChooserButtonGroup userButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.User, activity.activityMainChooserUserImage, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose, activity.activityMainChooserUserText, activity.activityMainChooserUser);
        final ChooserButtonGroup transButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.Trans, activity.activityMainTrans, R.mipmap.main_chooser_trans, R.mipmap.main_chooser_trans_chose, null);
        activity.activityMainContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private final @NotNull AtomicReference<IFragment<?>> oldFragment = new AtomicReference<>();

            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final FragmentsAdapter.FragmentTypes current = FragmentsAdapter.FragmentTypes.fromPosition(position);
                ActivityMain.this.currentChoice.set(current);
                switch (current) {
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
                final IFragment<?> now = fragmentsAdapter.getFragment(current);
                final IFragment<?> old = this.oldFragment.getAndSet(now);
                if (old != null) old.onHide();
                now.onShow();
            }
        });
        fragmentsAdapter.getAllFragments().forEach(f -> f.onActivityCreateHook(this));
        activity.activityMainContent.setCurrentItem(FragmentsAdapter.FragmentTypes.toPosition(FragmentsAdapter.FragmentTypes.File), false);
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @UiThread
    @Override
    public void onBackPressed() {
        final FragmentsAdapter.FragmentTypes choice = this.currentChoice.get();
        if (choice != null && this.fragmentsAdapter.getInstance().getFragment(choice).onBackPressed()) return;
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.lastBackPressedTime != null && Duration.between(this.lastBackPressedTime, now).toMillis() < 2000) {
            this.disconnect();
            super.onBackPressed(); // this.finish();
            return;
        }
        Main.showToast(this, R.string.toast_press_again_to_exit);
        this.lastBackPressedTime = now;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.isConnected())
            WListClientManager.removeAllListeners(this.address.getInstance());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.isConnected()) {
            if (WListClientManager.instances.isNotInitialized(this.address.getInstance())) {
                Main.showToast(this, R.string.activity_main_server_closed);
                this.disconnect();
                return;
            }
            WListClientManager.addListener(this.address.getInstance(), i -> {
                if (!i.booleanValue()) {
                    Main.runOnUiThread(this, () -> {
                        Main.showToast(this, R.string.activity_main_server_closed);
                        this.disconnect();
                    });
                    WListClientManager.removeAllListeners(this.address.getInstance());
                }
            });
        } else
            Main.runOnBackgroundThread(this, this::disconnect, 500, TimeUnit.MILLISECONDS);
    }

    @AnyThread
    public void connect(final @NotNull InetSocketAddress address, final @NotNull String username, final @Nullable IBinder binder) {
        this.address.reinitialize(address);
        this.username.reinitialize(username);
        this.binder.reinitializeNullable(binder);
        if (binder != null)
            try {
                binder.linkToDeath(this::disconnect, 0);
            } catch (final RemoteException ignore) {
                this.disconnect();
                return;
            }
        Main.runOnUiThread(this, () -> {
            this.fragmentsAdapter.getInstance().notifyConnected(true, this.currentChoice.get());
            Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
                BroadcastAssistant.start(this.address());
                ClientConfigurationSupporter.location().reinitialize(new File(this.getExternalFilesDir("client"), "client.yaml"));
                ClientConfigurationSupporter.parseFromFile();
                final BroadcastAssistant.BroadcastSet set;
                try {
                    set = BroadcastAssistant.get(this.address());
                } catch (final IllegalStateException exception) {
                    Main.runOnUiThread(this, this::disconnect);
                    return;
                }
                set.ServerClose.register(id -> Main.runOnUiThread(this, this::disconnect));
                this.fragmentsAdapter.getInstance().getAllFragments().forEach(f -> f.onConnected(this));
            }));
        });
    }

    @AnyThread
    public void disconnect() {
        this.address.uninitializeNullable();
        this.username.uninitializeNullable();
        Main.runOnUiThread(this, () -> {
            this.fragmentsAdapter.getInstance().notifyConnected(false, this.currentChoice.get());
            Main.runOnBackgroundThread(this, () -> this.fragmentsAdapter.getInstance().getAllFragments().forEach(f -> f.onDisconnected(this)));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying ActivityMain.");
        this.fragmentsAdapter.uninitializeNullable();
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "contentCache=" + this.contentCache +
                ", currentChoice=" + this.currentChoice +
                ", address=" + this.address +
                ", username=" + this.username +
                ", binder=" + this.binder.isInitialized() +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                '}';
    }
}
