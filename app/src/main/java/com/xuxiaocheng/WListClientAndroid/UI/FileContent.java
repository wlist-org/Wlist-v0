package com.xuxiaocheng.WListClientAndroid.UI;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Helpers.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Client.FileHelper;
import com.xuxiaocheng.WListClientAndroid.Helpers.SpecialDriverName;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.databinding.FileListHeaderBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FileContent {
    private FileContent() {
        super();
    }

    @NonNull public static View onChange(@NonNull final MainActivity activity) {

        // TODO get list.
        final ConstraintLayout header = FileListHeaderBinding.inflate(activity.getLayoutInflater()).getRoot();
        final TextView counter = (TextView) header.getViewById(R.id.file_list_counter);

        final ListView content = new ListView(activity);
        WListClientManager.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
            TokenManager.ensureToken("admin", "123456");
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list = FileHelper.getFileList(new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0));
            if (list != null) {
                FileContent.setList(activity, list, counter, content);
                content.setOnItemClickListener((adapter, view, i, l) -> {
                    Toast.makeText(activity, FileInformationGetter.name(list.getSecond().get(i)), Toast.LENGTH_SHORT).show();
                });
            }
        }));
        final LinearLayoutCompat match = new LinearLayoutCompat(activity);
        match.setOrientation(LinearLayoutCompat.VERTICAL);
        match.addView(header, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        match.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return match;
    }

    private static void setList(@NonNull final MainActivity activity, @NonNull final Pair.ImmutablePair<Long, ? extends List<VisibleFileInformation>> list, @NonNull final TextView counter, @NonNull final ListView content) {
        final List<Map<String, Object>> resources = new ArrayList<>();
        for (final VisibleFileInformation information: list.getSecond()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("image", R.mipmap.app_logo);
            map.put("name", FileInformationGetter.name(information));
            map.put("tip", FileInformationGetter.id(information));
            resources.add(map);
        }
        final ListAdapter adapter = new SimpleAdapter(activity, resources, R.layout.file_list_cell,
                new String[] {"image", "name", "tip"},
                new int[] {R.id.file_list_image, R.id.file_list_name, R.id.file_list_tip});
        activity.runOnUiThread(() -> {
            counter.setText(String.format(Locale.getDefault(), "%d", list.getFirst()));
            content.setAdapter(adapter);
        });
    }
}
