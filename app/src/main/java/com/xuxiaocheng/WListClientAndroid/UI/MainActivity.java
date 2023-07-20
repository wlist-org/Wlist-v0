package com.xuxiaocheng.WListClientAndroid.UI;

import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.UI.CustomView.MainTab;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.FINE, "Hello WList Client (Android Version).", ParametersMap.create().add("pid", Process.myPid()));
        this.setContentView(R.layout.main_activity);
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
            HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.DEBUG, "Choosing main tab: ", choice);
            synchronized (currentView) {
                final View oldView = currentView.getAndSet(null);
                if (oldView != null)
                    activity.removeView(oldView);
            }
            final View newView = switch (choice) {
                case File -> FileContent.onChange(this);
                case User -> UserContent.onChange(this);
            };
            synchronized (currentView) {
                if (currentView.compareAndSet(null, newView))
                    activity.addView(newView, contentParams);
            }
        });
        mainTab.click(MainTab.TabChoice.File);
    }
}
