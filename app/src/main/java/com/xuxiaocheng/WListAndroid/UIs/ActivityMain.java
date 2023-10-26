package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import android.os.IBinder;
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
import java.util.concurrent.atomic.AtomicReference;

public class ActivityMain extends AppCompatActivity {
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

    public boolean isConnected() {
        return this.address.isInitialized() && this.username.isInitialized();
    }

    public @NotNull InetSocketAddress address() {
        return this.address.getInstance();
    }

    public @NotNull String username() {
        return this.username.getInstance();
    }

    public void connect(final @NotNull InetSocketAddress address, final @NotNull String username, final @Nullable IBinder binder) {
        this.address.reinitialize(address);
        this.username.reinitialize(username);
        this.binder.reinitializeNullable(binder);
        Main.runOnUiThread(this, () -> this.fragmentsAdapterInstance.notifyConnected(true, this.currentChoice.get()));
        Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
            BroadcastAssistant.start(this.address());
            ClientConfigurationSupporter.location().reinitialize(new File(this.getExternalFilesDir("client"), "client.yaml"));
            ClientConfigurationSupporter.parseFromFile();
            final BroadcastAssistant.BroadcastSet set;
            try {
                set = BroadcastAssistant.get(this.address());
            } catch (final IllegalStateException exception) {
                Main.runOnUiThread(this, this::close);
                return;
            }
            set.ServerClose.register(id -> Main.runOnUiThread(this, this::close));
            this.fragmentsAdapterInstance.getAllFragments().forEach(IFragment::onConnected);
        }));
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
        activity.activityMainContent.setAdapter(this.fragmentsAdapterInstance);
        final ChooserButtonGroup fileButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.File, activity.activityMainChooserFileImage, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose, activity.activityMainChooserFileText, activity.activityMainChooserFile);
        final ChooserButtonGroup userButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.User, activity.activityMainChooserUserImage, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose, activity.activityMainChooserUserText, activity.activityMainChooserUser);
        final ChooserButtonGroup transButton = new ChooserButtonGroup(this, FragmentsAdapter.FragmentTypes.Trans, activity.activityMainTrans, R.mipmap.main_chooser_trans, R.mipmap.main_chooser_trans_chose, null);
        final AtomicReference<IFragment<?>> oldFragment = new AtomicReference<>();
        activity.activityMainContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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
                final IFragment<?> now = ActivityMain.this.fragmentsAdapterInstance.getFragment(current);
                final IFragment<?> old = oldFragment.getAndSet(now);
                if (old != null) old.onHide();
                now.onShow();
            }
        });
        this.fragmentsAdapterInstance.getAllFragments().forEach(IFragment::onActivityCreateHook);
        fileButton.callOnClick();
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @Override
    @UiThread
    public void onBackPressed() {
        final FragmentsAdapter.FragmentTypes choice = this.currentChoice.get();
        if (choice != null && this.fragmentsAdapterInstance.getFragment(choice).onBackPressed()) return;
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.lastBackPressedTime != null && Duration.between(this.lastBackPressedTime, now).toMillis() < 2000) {
            this.close();
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
    }

    @UiThread
    public void close() {
        this.address.uninitializeNullable();
        this.username.uninitializeNullable();
        this.fragmentsAdapterInstance.notifyConnected(false, this.currentChoice.get());
        this.fragmentsAdapterInstance.getAllFragments().forEach(IFragment::onDisconnected);
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
