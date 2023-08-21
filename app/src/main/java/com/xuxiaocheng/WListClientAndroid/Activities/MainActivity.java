package com.xuxiaocheng.WListClientAndroid.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Activities.Pages.FilePage;
import com.xuxiaocheng.WListClientAndroid.Activities.Pages.UserPage;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    public static void start(@NonNull final Activity activity, @NonNull final InetSocketAddress address) {
        final Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("host", address.getHostName()).putExtra("port", address.getPort());
        Main.runOnUiThread(activity, () -> activity.startActivity(intent));
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
    @NonNull protected final HInitializer<InetSocketAddress> address = new HInitializer<>("MainActivityAddress");
    @NonNull protected final Map<MainTab.TabChoice, MainTab.MainTabPage> pages = new EnumMap<>(MainTab.TabChoice.class);

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating MainActivity.");
        this.setContentView(R.layout.activity_main);
        final InetSocketAddress address = this.extraAddress();
        if (address == null) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No address received."));
            this.finish();
            return;
        }
        this.address.initialize(address);
        final MainTab mainTab = new MainTab(
            new MainTab.ButtonGroup(this, R.id.activity_main_tab_file, R.id.activity_main_tab_file_button, R.id.activity_main_tab_file_text,
                    R.mipmap.main_tab_file, R.mipmap.main_tab_file_chose, R.color.text_normal, R.color.red),
            new MainTab.ButtonGroup(this, R.id.activity_main_tab_user, R.id.activity_main_tab_user_button, R.id.activity_main_tab_user_text,
                    R.mipmap.main_tab_user, R.mipmap.main_tab_user_chose, R.color.text_normal, R.color.red)
        );
        final AtomicReference<View> currentView = new AtomicReference<>(null);
        this.pages.clear();
        this.pages.put(MainTab.TabChoice.File, new FilePage(this, address));
        this.pages.put(MainTab.TabChoice.User, new UserPage(this, address));
        final ConstraintLayout activity = this.findViewById(R.id.activity_main);
        final ConstraintLayout.LayoutParams contentParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        contentParams.bottomToTop = R.id.activity_main_guideline_tab;
        contentParams.leftToLeft = R.id.activity_main;
        contentParams.rightToRight = R.id.activity_main;
        contentParams.topToBottom = R.id.activity_main_guideline_title;
        mainTab.setOnChangeListener(choice -> {
            final View oldView;
            synchronized (currentView) {
                oldView = currentView.getAndSet(null);
                this.minTabChoice.set(null);
            }
            if (oldView != null)
                activity.removeView(oldView);
            final MainTab.MainTabPage page = this.pages.get(choice);
            assert page != null;
            final View newView = page.onShow();
            final boolean ok;
            synchronized (currentView) {
                ok = currentView.compareAndSet(null, newView);
                this.minTabChoice.set(choice);
            }
            if (ok)
                activity.addView(newView, contentParams);
        });
        mainTab.click(MainTab.TabChoice.File);
    }

    @Nullable protected LocalDateTime lastBackPressedTime;
    @Override
    public void onBackPressed() {
        final MainTab.TabChoice choice = this.minTabChoice.get();
        if (choice != null) {
            final MainTab.MainTabPage page = this.pages.get(choice);
            assert page != null;
            if (page.onBackPressed())
                return;
        }
        final LocalDateTime now = LocalDateTime.now();
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
        this.startActivity(new Intent(this, LoginActivity.class));
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying MainActivity.");
    }

    @Override
    @NonNull public String toString() {
        return "MainActivity{" +
                "minTabChoice=" + this.minTabChoice +
                ", address=" + this.address +
                ", pages=" + this.pages +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                ", super=" + super.toString() +
                '}';
    }
}
