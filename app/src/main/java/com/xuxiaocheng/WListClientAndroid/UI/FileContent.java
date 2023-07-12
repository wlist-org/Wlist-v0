package com.xuxiaocheng.WListClientAndroid.UI;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListClient.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClientAndroid.Client.ClientManager;
import com.xuxiaocheng.WListClientAndroid.Client.SpecialDriverName;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileContent {
    private FileContent() {
        super();
    }

    @NonNull public static View onChange(@NonNull final MainActivity activity) {
        final ListView content = new ListView(activity);

        // TODO get list.
//        final FileListHeaderBinding header = FileListHeaderBinding.inflate()
//        final

        ClientManager.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
            TokenManager.ensureToken("admin", "f77FzWR3");
            try (final WListClient client = ClientManager.getInstance().getNewClient()) {
                final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list = OperateFileHelper.listFiles(client, TokenManager.getToken(),
                        new FileLocation(SpecialDriverName.RootDriver.getIdentifier(), 0),
                        20, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
                if (list != null)
                    FileContent.setList(activity, list, content);
            }
        }));
//        content.setOnItemClickListener((adapter, view, i, l) -> {
//            Toast.makeText(this, ((Map<String , Object>) adapter.getItemAtPosition(i)).get("name").toString(),
//                    Toast.LENGTH_SHORT).show();
//        });
        return content;
    }

    private static void setList(@NonNull final MainActivity activity, @NonNull final Pair.ImmutablePair<Long, ? extends List<VisibleFileInformation>> list, @NonNull final ListView content) {
        final List<Map<String, Object>> resources = new ArrayList<>();
        for (final VisibleFileInformation information: list.getSecond()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("image", R.mipmap.app_logo);
            map.put("name", FileInformationGetter.name(information));
            map.put("tip", FileInformationGetter.updateTime(information));
            resources.add(map);
        }
        final ListAdapter adapter = new SimpleAdapter(activity, resources, R.layout.file_list_cell,
                new String[] {"image", "name", "tip"},
                new int[] {R.id.file_list_image, R.id.file_list_name, R.id.file_list_tip});
        activity.runOnUiThread(() -> content.setAdapter(adapter));
    }
}
