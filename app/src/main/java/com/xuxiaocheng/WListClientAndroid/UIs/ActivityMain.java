package com.xuxiaocheng.WListClientAndroid.UIs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityMain extends AppCompatActivity {
    public static void start(final @NotNull Activity activity, final @NotNull InetSocketAddress address, final @NotNull String username) {
        final Intent intent = new Intent(activity, ActivityMain.class);
        intent.putExtra("host", address.getHostName()).putExtra("port", address.getPort());
        intent.putExtra("name", username);
        Main.runOnUiThread(activity, () -> activity.startActivity(intent));
    }

    protected boolean extraAddress() {
        final Intent intent = this.getIntent();
        final String host = intent.getStringExtra("host");
        final int port = intent.getIntExtra("port", -1);
        final String name = intent.getStringExtra("name");
        if (host == null || port == -1 || name == null)
            return true;
        this.address.reinitialize(new InetSocketAddress(host, port));
        this.username.reinitialize(name);
        return false;
    }

    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("MainActivityAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("MainActivityUsername");

    protected final @NotNull AtomicReference<ActivityMainChooser.MainChoice> minTabChoice = new AtomicReference<>();
    protected final @NotNull Map<ActivityMainChooser.MainChoice, ActivityMainChooser.MainPage> pages = new EnumMap<>(ActivityMainChooser.MainChoice.class); {
        this.pages.put(ActivityMainChooser.MainChoice.File, new PageFile(this));
        this.pages.put(ActivityMainChooser.MainChoice.User, new PageUser(this));
    }

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityMain.");
        this.setContentView(R.layout.activity_main);
        if (this.extraAddress()) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No address received."));
            this.finish();
            return;
        }
        final ActivityMainChooser chooser = new ActivityMainChooser(
            new ActivityMainChooser.ButtonGroup(this, R.id.activity_main_tab_file, R.id.activity_main_tab_file_button, R.id.activity_main_tab_file_text,
                    R.mipmap.main_tab_file, R.mipmap.main_tab_file_chose, R.color.text_normal, R.color.red),
            new ActivityMainChooser.ButtonGroup(this, R.id.activity_main_tab_user, R.id.activity_main_tab_user_button, R.id.activity_main_tab_user_text,
                    R.mipmap.main_tab_user, R.mipmap.main_tab_user_chose, R.color.text_normal, R.color.red)
        );
        final AtomicReference<View> currentView = new AtomicReference<>(null);
        final ConstraintLayout activity = this.findViewById(R.id.activity_main);
        final ConstraintLayout.LayoutParams contentParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        contentParams.bottomToTop = R.id.activity_main_guideline_tab;
        contentParams.leftToLeft = R.id.activity_main;
        contentParams.rightToRight = R.id.activity_main;
        contentParams.topToBottom = R.id.activity_main_guideline_title;
        chooser.setOnChangeListener(choice -> {
            final View oldView;
            synchronized (currentView) {
                oldView = currentView.getAndSet(null);
                this.minTabChoice.set(null);
            }
            if (oldView != null)
                activity.removeView(oldView);
            final ActivityMainChooser.MainPage page = this.pages.get(choice);
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
        chooser.click(ActivityMainChooser.MainChoice.File);
    }

    protected @Nullable ZonedDateTime lastBackPressedTime;
    @Override
    public void onBackPressed() {
        final ActivityMainChooser.MainChoice choice = this.minTabChoice.get();
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
                "minTabChoice=" + this.minTabChoice +
                ", address=" + this.address +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                ", super=" + super.toString() +
                '}';
    }
}
