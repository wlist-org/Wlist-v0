package com.xuxiaocheng.WListAndroid.UIs;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AnyThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.StackWrappedView;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class ActivityMain extends AppCompatActivity {
    protected final @NotNull HInitializer<ActivityMainBinding> contentCache = new HInitializer<>("ContentCache");
    protected final @NotNull HInitializer<StackWrappedView> wrappedView = new HInitializer<>("WrappedView");
    public @NotNull ActivityMainBinding getContent() {
        return this.contentCache.getInstance();
    }
    protected final @NotNull ActivityMainAdapter fragmentsAdapter = new ActivityMainAdapter(this);
    protected final @NotNull AtomicReference<ActivityMainAdapter.FragmentTypes> currentChoice = new AtomicReference<>(ActivityMainAdapter.FragmentTypes.File);
    public ActivityMainAdapter.@NotNull FragmentTypes currentChoice() {
        return this.currentChoice.get();
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("ActivityMainAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("ActivityMainUsername");
    protected final @NotNull HInitializer<IBinder> binder = new HInitializer<>("ActivityMainServiceBinder");
    protected boolean isConnected() {
        return this.address.isInitialized() && this.username.isInitialized();
    }

    @Override
    protected void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("choice", ActivityMainAdapter.FragmentTypes.toPosition(this.currentChoice.get()));
        BundleHelper.saveClient(this.address, this.username, outState, null);
        final IBinder binder = this.binder.getInstanceNullable();
        if (binder != null)
            outState.putBinder("binder", binder);
    }

    @Override
    protected void onRestoreInstanceState(final @NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int choice = savedInstanceState.getInt("choice");
        if (choice != 0 || savedInstanceState.containsKey("choice"))
            this.currentChoice.set(ActivityMainAdapter.FragmentTypes.fromPosition(choice));
        BundleHelper.restoreClient(savedInstanceState, this.address, this.username, null);
        final IBinder binder = savedInstanceState.getBinder("binder");
        if (binder != null)
            this.binder.reinitialize(binder);
    }

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityMain. ", this.hashCode());
        final ActivityMainBinding activity = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.wrappedView.reinitialize(new StackWrappedView(this, 300, false));
        this.setContentView(this.wrappedView.getInstance());
        this.wrappedView.getInstance().push(activity.getRoot());
        activity.getRoot().setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.contentCache.reinitialize(activity);
        activity.activityMainContent.setAdapter(this.fragmentsAdapter);
        final ChooserButtonGroup fileButton = new ChooserButtonGroup(this, ActivityMainAdapter.FragmentTypes.File, activity.activityMainChooserFileImage, R.mipmap.main_chooser_file, R.mipmap.main_chooser_file_chose, activity.activityMainChooserFileText, activity.activityMainChooserFile);
        final ChooserButtonGroup userButton = new ChooserButtonGroup(this, ActivityMainAdapter.FragmentTypes.User, activity.activityMainChooserUserImage, R.mipmap.main_chooser_user, R.mipmap.main_chooser_user_chose, activity.activityMainChooserUserText, activity.activityMainChooserUser);
        activity.activityMainContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final ActivityMainAdapter.FragmentTypes current = ActivityMainAdapter.FragmentTypes.fromPosition(position);
                ActivityMain.this.currentChoice.set(current);
                switch (current) {
                    case File -> {
                        if (fileButton.isClicked()) break;
                        fileButton.setClickable(false);
                        userButton.setClickable(true);
                    }
                    case User -> {
                        if (userButton.isClicked()) break;
                        fileButton.setClickable(true);
                        userButton.setClickable(false);
                    }
                }
                ActivityMain.this.fragmentsAdapter.getAllFragments().forEach(f -> f.onPositionChanged(ActivityMain.this, current));
            }
        });
        activity.activityMainContent.setCurrentItem(ActivityMainAdapter.FragmentTypes.toPosition(this.currentChoice.get()), false);
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @UiThread
    @Override
    public void onBackPressed() {
        if (this.otherPageBackListener.get() != null && this.otherPageBackListener.get().test(null))
            return;
        if (!this.isMainPage.get()) {
            this.resetPage();
            return;
        }
        final ActivityMainAdapter.FragmentTypes choice = this.currentChoice.get();
        if (choice != null && this.fragmentsAdapter.getAllFragments().stream().anyMatch(f -> f.onBackPressed(this))) return;
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.lastBackPressedTime != null && Duration.between(this.lastBackPressedTime, now).toMillis() < 2000) {
            if (this.isConnected())
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
        if (this.isConnected() && WListClientManager.instances.isNotInitialized(this.address.getInstance())) {
            Main.showToast(this, R.string.activity_main_server_closed);
            this.disconnect();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (this.isConnected() != this.connected.get())
            if (this.isConnected())
                this.connect(this.address.getInstance(), this.username.getInstance(), this.binder.getInstanceNullable());
            else
                this.disconnect();
    }

    private final @NotNull AtomicBoolean connected = new AtomicBoolean(false);

    @AnyThread
    public void connect(final @NotNull InetSocketAddress address, final @NotNull String username, final @Nullable IBinder binder) {
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Connecting. " + this.hashCode());
        this.connected.set(true);
        this.address.reinitialize(address);
        this.username.reinitialize(username);
        this.binder.reinitializeNullable(binder);
        Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
            WListClientManager.addListener(address, i -> {
                if (!i.booleanValue()) {
                    Main.runOnUiThread(this, () -> {
                        Main.showToast(this, R.string.activity_main_server_closed);
                        this.disconnect();
                    });
                    WListClientManager.removeAllListeners(address);
                }
            });
            this.fragmentsAdapter.setArguments(address, username);
            BroadcastAssistant.start(address);
            ClientConfigurationSupporter.location().reinitialize(new File(this.getExternalFilesDir("client"), "client.yaml"));
            ClientConfigurationSupporter.parseFromFile();
            BroadcastAssistant.get(address).ServerClose.register(id -> Main.runOnUiThread(this, this::disconnect));
            this.fragmentsAdapter.getAllFragments().forEach(f -> f.onConnected(this));
        }));
    }

    @AnyThread
    public void disconnect() {
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Disconnecting. " + this.hashCode());
        this.connected.set(false);
        final InetSocketAddress address = this.address.uninitializeNullable();
        final String username = this.username.uninitializeNullable();
        if (address == null || username == null) {
            HLogManager.getInstance("DefaultLogger").log(HLogLevel.MISTAKE, "Disconnect twice. " + this.hashCode());
            return;
        }
        Main.runOnBackgroundThread(this, () -> {
            WListClientManager.removeAllListeners(address);
            BroadcastAssistant.stop(address);
            TokenAssistant.removeToken(address, username);
            WListClientManager.quicklyUninitialize(address);
            this.fragmentsAdapter.getAllFragments().forEach(f -> f.onDisconnected(this));
        });
    }

    private final @NotNull AtomicBoolean isMainPage = new AtomicBoolean(true);
    private final @NotNull AtomicReference<Predicate<@Nullable Void>> otherPageBackListener = new AtomicReference<>();

    @UiThread
    public void transferPage(final @NotNull View view, final @Nullable Predicate<@Nullable Void> backListener) {
        if (!this.isMainPage.compareAndSet(true, false)) throw new IllegalStateException("Transfer page twice. " + this.hashCode());
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Transfer page. ", this.hashCode());
        this.wrappedView.getInstance().push(view);
        this.otherPageBackListener.set(backListener);
    }

    @UiThread
    public void resetPage() {
        if (!this.isMainPage.compareAndSet(false, true)) throw new IllegalStateException("Reset page twice. " + this.hashCode());
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Reset page. ", this.hashCode());
        this.wrappedView.getInstance().pop();
        this.otherPageBackListener.set(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying ActivityMain. ", this.hashCode());
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "contentCache=" + this.contentCache.isInitialized() +
                ", currentChoice=" + this.currentChoice +
                ", binder=" + this.binder.isInitialized() +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                '}';
    }

    private static final class ChooserButtonGroup {
        private final @NotNull Activity activity;
        private final @NotNull ImageView button;
        private final @Nullable TextView text;
        private final @NotNull Collection<@NotNull View> layouts = new ArrayList<>();

        @DrawableRes
        private final int image;
        @DrawableRes
        private final int imageChose;

        private ChooserButtonGroup(final @NotNull ActivityMain activity, final ActivityMainAdapter.@NotNull FragmentTypes type, final @NotNull ImageView button,
                                   @DrawableRes final int image, @DrawableRes final int imageChose,
                                   final @Nullable TextView text, final @NotNull View @NotNull ... layouts) {
            super();
            this.activity = activity;
            this.button = button;
            this.text = text;
            this.image = image;
            this.imageChose = imageChose;
            this.layouts.add(button);
            if (this.text != null)
                this.layouts.add(this.text);
            this.layouts.addAll(Arrays.asList(layouts));
            this.layouts.forEach(v -> v.setOnClickListener(u -> this.button.performClick()));
            this.button.setOnClickListener(v -> activity.getContent().activityMainContent.setCurrentItem(ActivityMainAdapter.FragmentTypes.toPosition(type)));
        }

        @UiThread
        public void setClickable(final boolean clickable) {
            if (this.button.isClickable() == clickable)
                return;
            this.button.setClickable(clickable);
            this.button.setImageResource(clickable ? this.image : this.imageChose);
            if (this.text != null) {
                this.text.setClickable(clickable);
                this.text.setTextColor(this.activity.getResources().getColor(clickable ? R.color.text_normal : R.color.text_warning, this.activity.getTheme()));
            }
            this.layouts.forEach(v -> v.setClickable(clickable));
        }

        public boolean isClicked() {
            return !this.button.isClickable();
        }

        @Override
        public @NotNull String toString() {
            return "ButtonGroup{" +
                    "button=" + this.button +
                    ", text=" + this.text +
                    ", layouts=" + this.layouts +
                    ", image=" + this.image +
                    ", imageChose=" + this.imageChose +
                    '}';
        }
    }
}
