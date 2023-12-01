package com.xuxiaocheng.WListAndroid.UIs.Main;

import android.widget.ImageView;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Main.PageMain;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import org.jetbrains.annotations.NotNull;

public class ActivityMain extends CActivity {
    public ActivityMain() {
        super(new PageMain(), "main");
    }

    @UiThread
    public @NotNull AlertDialog createLoadingDialog(@StringRes final int title) {
        final ImageView loading = new ImageView(this);
        loading.setImageResource(R.drawable.loading);
        ViewUtil.startDrawableAnimation(loading);
        return new AlertDialog.Builder(this).setTitle(title).setView(loading).setCancelable(false).create();
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "super=" + super.toString() +
                '}';
    }
}
