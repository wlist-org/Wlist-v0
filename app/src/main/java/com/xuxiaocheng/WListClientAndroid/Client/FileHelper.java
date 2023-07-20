package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;

import java.io.IOException;
import java.util.List;

public final class FileHelper {
    private FileHelper() {
        super();
    }

    @Nullable public static Pair.ImmutablePair<Long, List<VisibleFileInformation>> getFileList(@NonNull final FileLocation directory) throws InterruptedException, IOException, WrongStateException {
        try (final WListClient client = WListClientManager.getInstance().getNewClient()) {
            return OperateFileHelper.listFiles(client, TokenManager.getToken(), directory,
                    20, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND, false);
        }
    }
}
