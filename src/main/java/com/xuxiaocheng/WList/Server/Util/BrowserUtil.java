package com.xuxiaocheng.WList.Server.Util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Supplier;

public final class BrowserUtil {
    private BrowserUtil() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("BrowserLogger");

    /**
     * @see HttpNetworkHelper#newHttpClientBuilder()
     */
    public static final @NotNull HInitializer<Supplier<@NotNull WebClient>> WebClientCore = new HInitializer<>("WebClientCore", () -> {
        final WebClient client = new WebClient(BrowserVersion.EDGE);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.setWebConnection(new WebConnectionWrapper(client.getWebConnection()) {
            @Override
            public WebResponse getResponse(final WebRequest request) throws IOException {
                BrowserUtil.logger.log(HLogLevel.NETWORK, "Sending: ", request.getHttpMethod(), ' ', request.getUrl(),
                        " Parameters: ", request.getParameters(),
                        request.getAdditionalHeader("Range") == null ? "" : (" (Range: " + request.getAdditionalHeader("Range") + ')'));
                final long time1 = System.currentTimeMillis();
                final WebResponse response;
                boolean successFlag = false;
                try {
                    response = super.getResponse(request);
                    successFlag = true;
                } finally {
                    final long time2 = System.currentTimeMillis();
                    BrowserUtil.logger.log(HLogLevel.NETWORK, "Received. Totally cost time: ", time2 - time1, "ms.",
                            successFlag ? "" : " But something went wrong.");
                }
                return response;
            }
        });
        return client;
    });

    public static @NotNull WebClient newWebClient() {
        return BrowserUtil.WebClientCore.getInstance().get();
    }
}
