package com.xuxiaocheng.WListClientAndroid.Activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.SpecialDriverName;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Client.FileHelper;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.databinding.FileListHeaderBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating MainActivity.");
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
            logger.log(HLogLevel.DEBUG, "Choosing main tab: ", choice);
            synchronized (currentView) {
                final View oldView = currentView.getAndSet(null);
                if (oldView != null)
                    activity.removeView(oldView);
            }
            final View newView = switch (choice) {
                case File -> this.onChangeFile();
                case User -> this.onChangeUser();
            };
            synchronized (currentView) {
                if (currentView.compareAndSet(null, newView))
                    activity.addView(newView, contentParams);
            }
        });
        mainTab.click(MainTab.TabChoice.File);
    }

    @NonNull private View onChangeUser() {
        return new View(this);
    }

    @NonNull private View onChangeFile() {
        // TODO get list.
        final ConstraintLayout header = FileListHeaderBinding.inflate(this.getLayoutInflater()).getRoot();
        final TextView counter = (TextView) header.getViewById(R.id.file_list_counter);

        final ListView content = new ListView(this);
        Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
            TokenManager.ensureToken("admin", "XJfx7x3J");
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list = FileHelper.getFileList(new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0));
            if (list != null) {
                this.setList(list, counter, content);
                content.setOnItemClickListener((adapter, view, i, l) -> {
                    Toast.makeText(this, FileInformationGetter.name(list.getSecond().get(i)), Toast.LENGTH_SHORT).show();
                });
            }
        }));
        final LinearLayoutCompat match = new LinearLayoutCompat(this);
        match.setOrientation(LinearLayoutCompat.VERTICAL);
        match.addView(header, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        match.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return match;
    }

    private void setList(@NonNull final Pair.ImmutablePair<Long, ? extends List<VisibleFileInformation>> list, @NonNull final TextView counter, @NonNull final ListView content) {
        final List<Map<String, Object>> resources = new ArrayList<>();
        for (final VisibleFileInformation information: list.getSecond()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("image", R.mipmap.app_logo);
            map.put("name", FileInformationGetter.name(information));
            map.put("tip", FileInformationGetter.id(information));
            resources.add(map);
        }
        final ListAdapter adapter = new SimpleAdapter(this, resources, R.layout.file_list_cell,
                new String[] {"image", "name", "tip"},
                new int[] {R.id.file_list_image, R.id.file_list_name, R.id.file_list_tip});
        this.runOnUiThread(() -> {
            counter.setText(String.format(Locale.getDefault(), "%d", list.getFirst()));
            content.setAdapter(adapter);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying MainActivity.");
    }
}
