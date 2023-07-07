package com.xuxiaocheng.WListClientAndroid.UI;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.UI.CustomView.MainTab;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final HLog logger = HLog.createInstance("DefaultLogger", Integer.MIN_VALUE, false, new OutputStream() {
        private final ByteArrayOutputStream cache = new ByteArrayOutputStream(256);
        @Override
        public void write(final int i) {
            if (i != '\n') {
                this.cache.write(i);
                return;
            }
            final String message = this.cache.toString();
            this.cache.reset();
            int priority = Log.INFO;
            if (message.contains("[VERBOSE]"))
                priority = Log.VERBOSE;
            if (message.contains("[DEBUG]"))
                priority = Log.DEBUG;
            if (message.contains("[WARN]"))
                priority = Log.WARN;
            if (message.contains("[ERROR]"))
                priority = Log.ERROR;
            if (message.contains("[FAULT]"))
                priority = Log.ASSERT;
            Log.println(priority, "HLog", message);
        }
    });

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        MainActivity.logger.log(HLogLevel.FINE, "Hello WList Client (Android Version).");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> MainActivity.logger.log(HLogLevel.FAULT,
                "Uncaught Exception!!!", ParametersMap.create().add("thread", t.getName()).add("exception", e)));
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main_activity);
        final MainTab mainTab = new MainTab(
                new MainTab.ButtonGroup(this, R.id.main_tab_file, R.id.main_tab_file_button, R.id.main_tab_file_text,
                        R.mipmap.main_tab_file, R.mipmap.main_tab_file_chose, R.color.black, R.color.red),
                new MainTab.ButtonGroup(this, R.id.main_tab_user, R.id.main_tab_user_button, R.id.main_tab_user_text,
                        R.mipmap.main_tab_user, R.mipmap.main_tab_user_chose, R.color.black, R.color.red)
        );
        final ListView list = this.findViewById(R.id.main_content);

        final List<Map<String, Object>> resources = new ArrayList<>(2);
        final Map<String, Object> map = new java.util.HashMap<>();
        map.put("image", R.mipmap.app_logo);
        map.put("name", R.string.tab_file);
        resources.add(map);
        final ListAdapter fileAdapter = new SimpleAdapter(this, resources, R.layout.file_list_cell,
                new String[] {"image", "name"},
                new int[] {R.id.file_list_image, R.id.file_list_name});
        list.setOnItemClickListener((adapter, view, i, l) -> {
            Toast.makeText(this, ((Map<String, Object>) adapter.getItemAtPosition(i)).get("name").toString(),
                    Toast.LENGTH_SHORT).show();
        });
        mainTab.setOnChangeListener(choice -> {
            MainActivity.logger.log(HLogLevel.DEBUG, "Choosing main tab: ", choice);
            if (choice == MainTab.TabChoice.File)
                list.setAdapter(fileAdapter);
            if (choice == MainTab.TabChoice.User)
                list.setAdapter(new SimpleAdapter(this, new ArrayList<>(), R.layout.file_list_cell,
                        new String[] {"image", "name"},
                        new int[] {R.id.file_list_image, R.id.file_list_name}));
        });
        mainTab.click(MainTab.TabChoice.File);
    }
}
