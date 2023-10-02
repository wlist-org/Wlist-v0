package com.xuxiaocheng.WList.Server.Util;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import io.netty.util.internal.EmptyArrays;
import org.htmlunit.BrowserVersion;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseData;
import org.htmlunit.util.WebConnectionWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
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
                        request.getHttpMethod() == HttpMethod.GET || request.getParameters().isEmpty() ? "" : " Parameters: " + request.getParameters(),
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

    public static void waitJavaScriptCompleted(final @NotNull WebClient client) {
        while (true)
            if (client.waitForBackgroundJavaScript(5000) == 0)
                break;
    }

    public static void waitJavaScriptCompleted(final @NotNull WebClient client, final int left) {
        if (left < 0)
            return;
        while (true)
            if (client.waitForBackgroundJavaScript(500) <= left)
                break;
    }

    public static @NotNull WebResponse emptyResponse(final @NotNull WebRequest request) {
        return new WebResponse(new WebResponseData(EmptyArrays.EMPTY_BYTES, 200, "OK", List.of()), request, 0);
    }
}
