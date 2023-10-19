package com.xuxiaocheng.WList.Server.Storage.Helpers;

import com.xuxiaocheng.HeadLibs.HeadLibs;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import io.netty.util.internal.EmptyArrays;
import org.htmlunit.BrowserVersion;
import org.htmlunit.Cache;
import org.htmlunit.HttpMethod;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebConsole;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseData;
import org.htmlunit.javascript.SilentJavaScriptErrorListener;
import org.htmlunit.util.NameValuePair;
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
    private static final @NotNull Cache SharedCache = new Cache();

    public static @NotNull WebClient newWebClient() {
        final WebClient client = new WebClient(BrowserVersion.EDGE);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.setCache(BrowserUtil.SharedCache);
        client.setWebConnection(new WebConnectionWrapper(client.getWebConnection()) {
            @Override
            public WebResponse getResponse(final WebRequest request) throws IOException {
                BrowserUtil.logger.log(HLogLevel.NETWORK, "Sending: ", request.getHttpMethod(), ' ', request.getUrl(),
                        ((Supplier<String>) () -> {
                            if (request.getHttpMethod() == HttpMethod.GET || request.getParameters().isEmpty())
                                return "";
                            final StringBuilder builder = new StringBuilder();
                            builder.append(" (Form: {");
                            for (final NameValuePair pair: request.getParameters()) {
                                builder.append(pair.getName());
                                if (HttpNetworkHelper.PrivateFormNames.contains(pair.getName()))
                                    builder.append(": ***");
                                else
                                    builder.append("=").append(pair.getValue());
                                builder.append(", ");
                            }
                            return builder.replace(builder.length() - 2, builder.length(), "})").toString();
                        }), request.getAdditionalHeader("Range") == null ? "" : (" (Range: " + request.getAdditionalHeader("Range") + ')'));
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
        if (!HeadLibs.isDebugMode()) {
            client.setIncorrectnessListener((message, origin) -> {/*Ignore*/});
            client.setCssErrorHandler(new SilentCssErrorHandler());
            client.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
            client.getWebConsole().setLogger(new SilentConsoleLogger());
        }
        return client;
    }

    public static class SilentConsoleLogger implements WebConsole.Logger {
        @Override public boolean isTraceEnabled() {return false;}@Override public void trace(final Object message) {}
        @Override public boolean isDebugEnabled() {return false;}@Override public void debug(final Object message) {}
        @Override public boolean isInfoEnabled() {return false;}@Override public void info(final Object message) {}
        @Override public boolean isWarnEnabled() {return false;}@Override public void warn(final Object message) {}
        @Override public boolean isErrorEnabled() {return false;}@Override public void error(final Object message) {}
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

    public static final @NotNull NameValuePair JSResponseHeader = new NameValuePair("Content-Type", "application/javascript");
    public static final @NotNull WebResponseData EmptyResponse = new WebResponseData(EmptyArrays.EMPTY_BYTES, 200, "OK", List.of());
    public static final @NotNull WebResponseData EmptyJSResponse = new WebResponseData(EmptyArrays.EMPTY_BYTES, 200, "OK", List.of(BrowserUtil.JSResponseHeader));
    public static @NotNull WebResponse emptyResponse(final @NotNull WebRequest request) {
        if (request.getUrl().toString().endsWith(".js"))
            return new WebResponse(BrowserUtil.EmptyJSResponse, request, 0);
        return new WebResponse(BrowserUtil.EmptyResponse, request, 0);
    }
}
