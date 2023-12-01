package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;

public abstract class IActivity<C extends ViewBinding> extends AppCompatActivity {
    protected final @NotNull HInitializer<C> contentCache = new HInitializer<>("ContentCache");
    public @NotNull C content() {
        return this.contentCache.getInstance();
    }

    @UiThread
    protected abstract @NotNull C iOnInflater();
    @UiThread
    protected void iOnRestoreInstanceState(final @Nullable Bundle savedInstanceState) {
    }
    @UiThread
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
    }
    @UiThread
    protected void iOnBuildActivity(final @NotNull C content, final boolean isFirstTime) {
    }
    @UiThread
    protected boolean iOnBackPressed() {
        return false;
    }


    @UiThread
    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Creating activity.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
        final C content = this.iOnInflater();
        this.contentCache.reinitialize(content);
        this.setContentView(content.getRoot());
        this.iOnRestoreInstanceState(savedInstanceState == null ? null : savedInstanceState.getBundle("i:activity"));
        this.iOnBuildActivity(content, savedInstanceState == null);
    }

    @UiThread
    @Override
    protected void onStart() {
        super.onStart();
    }

    @UiThread
    @Override
    protected void onResume() {
        super.onResume();
    }

    @UiThread
    @Override
    protected void onPause() {
        super.onPause();
    }

    @UiThread
    @Override
    protected void onStop() {
        super.onStop();
    }

    @UiThread
    @Override
    protected void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final Bundle bundle = new Bundle();
        this.iOnSaveInstanceState(bundle);
        outState.putBundle("i:activity", bundle);
    }

    @UiThread
    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("UiLogger").log(HLogLevel.VERBOSE, "Destroyed activity.", ParametersMap.create()
                .add("class", this.getClass().getSimpleName()));
    }

    protected @Nullable ZonedDateTime lastBackPressedTime = null;
    @UiThread
    @Override
    public void onBackPressed() {
        if (this.iOnBackPressed()) return;
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.lastBackPressedTime != null && Duration.between(this.lastBackPressedTime, now).toMillis() < 2000) {
            super.onBackPressed();
            this.lastBackPressedTime = null;
            return;
        }
        Toaster.show(R.string.toast_press_again_to_exit);
        this.lastBackPressedTime = now;
    }

    @Override
    public @NotNull String toString() {
        return "IActivity{" +
                "contentCache=" + this.contentCache.isInitialized() +
                ", lastBackPressedTime=" + this.lastBackPressedTime +
                '}';
    }
}
