package com.xuxiaocheng.WListTest;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.StaticLoader;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouSharer;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.Util.IdsHelper;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class TempTest {
    private static final boolean initializeServer = false;
    private static final @NotNull SupplierE<@Nullable Object> _main = () -> {
        final LanzouSharer sharer = new LanzouSharer();
        sharer.initialize(new LanzouConfiguration());
        return sharer.getSingleShareFileDownloadUrl(Objects.requireNonNull(HttpUrl.parse("https://wwsz.lanzouw.com/")), "iEPc8148jq5e", "b0iq");
//        final String domin = ;
//        try (final WebClient client = BrowserUtil.newWebClient()) {
//            client.setWebConnection(new WebConnectionWrapper(client.getWebConnection()) {
//                @Override
//                public @NotNull WebResponse getResponse(final @NotNull WebRequest request) throws IOException {
//                    final String url = request.getUrl().toString();
//                    if (url.startsWith(domin) || url.startsWith("https://assets.woozooo.com/"))
//                        return super.getResponse(request);
//                    return new WebResponse(new WebResponseData("(function(){})()".getBytes(StandardCharsets.UTF_8), 200, "OK",
//                            List.of(new NameValuePair("Content-Type", "application/javascript"))), request, 0);
//                }
//            });
//            final HtmlPage page = client.getPage(domin + "iEPc8148jq5e");
//            BrowserUtil.waitJavaScriptCompleted(client);
//            final HtmlInput input = page.getElementByName("pwd");
//            input.setValue("b0iq");
//            input.focus();
//            page.pressAccessKey('\n');
//            page.executeJavaScript("var a = 1;");
//        }
//        return null;
    };

    private static final @NotNull File runtimeDirectory = new File("./run");
    @BeforeAll
    public static void initialize() throws IOException, SQLException {
        StaticLoader.load();
        if (TempTest.initializeServer) {
            ServerConfiguration.Location.initialize(new File(TempTest.runtimeDirectory, "server.yaml"));
            ServerConfiguration.parseFromFile();
            final File file = new File(TempTest.runtimeDirectory, "data.db");
            final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
            ConstantManager.quicklyInitialize(database, "initialize");
            UserGroupManager.quicklyInitialize(database, "initialize");
            UserManager.quicklyInitialize(database, "initialize");
            StorageManager.initialize(new File(TempTest.runtimeDirectory, "configs"),
                    new File(TempTest.runtimeDirectory, "caches"));
        }
    }

    @Test
    public void tempTest() throws Exception {
        try {
            if (TempTest.initializeServer) {
                final Map<String, Exception> failures = StorageManager.getFailedStoragesAPI();
                if (!failures.isEmpty()) {
                    HLog.DefaultLogger.log(HLogLevel.FAULT, failures);
                    return;
                }
            }
            final Object obj = TempTest._main.get();
            if (obj != null)
                HLog.DefaultLogger.log(HLogLevel.DEBUG, obj);
        } finally {
            if (TempTest.initializeServer)
                for (final Map.Entry<String, ProviderInterface<?>> provider: StorageManager.getAllProviders().entrySet())
                    try {
                        StorageManager.dumpConfigurationIfModified(provider.getValue().getConfiguration());
                    } catch (final IOException exception) {
                        HLog.DefaultLogger.log(HLogLevel.ERROR, "Failed to dump provider configuration.", ParametersMap.create().add("name", provider.getKey()), exception);
                    }
            HLog.DefaultLogger.log(HLogLevel.FINE, "Shutting down all executors.");
            WListServer.CodecExecutors.shutdownGracefully();
            WListServer.ServerExecutors.shutdownGracefully();
            WListServer.IOExecutors.shutdownGracefully();
            BackgroundTaskManager.BackgroundExecutors.shutdownGracefully();
            HttpNetworkHelper.CountDownExecutors.shutdownGracefully();
            IdsHelper.CleanerExecutors.shutdownGracefully();
        }
    }
}
