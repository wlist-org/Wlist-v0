package com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BrowserUtil;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseSharer;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseData;
import org.htmlunit.html.FrameWindow;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.WebConnectionWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("SpellCheckingInspection")
public class LanzouSharer extends AbstractIdBaseSharer<LanzouConfiguration> {
    @Override
    public @NotNull StorageTypes<LanzouConfiguration> getType() {
        return StorageTypes.Lanzou;
    }

    protected static final @NotNull String AssertHost = Objects.requireNonNull(HttpUrl.parse("https://assets.woozooo.com/")).url().getHost();
    protected static final @NotNull DateTimeFormatter dataTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

    protected @Nullable Pair.ImmutablePair<@NotNull HttpUrl, @Nullable Headers> getSingleShareFileDownloadUrl(final @NotNull HttpUrl url, final @Nullable String password) throws IOException, IllegalParametersException {
        final HtmlElement downloading;
        try (final WebClient client = BrowserUtil.newWebClient()) {
            client.setWebConnection(new WebConnectionWrapper(client.getWebConnection()) {
                @Override
                public @NotNull WebResponse getResponse(final @NotNull WebRequest request) throws IOException {
                    if (LanzouSharer.this.configuration.getInstance().isSkipQRCode()) {
                        if (!request.getUrl().getHost().equals(url.url().getHost()) && !request.getUrl().getHost().equals(LanzouSharer.AssertHost))
                            return BrowserUtil.emptyResponse(request);
                        if (request.getUrl().toString().endsWith("/qrcode.min.js"))
                            return new WebResponse(new WebResponseData("QRCode=function(a,b){};QRCode.CorrectLevel={L:1,M:0,Q:3,H:2};"
                                    .getBytes(StandardCharsets.UTF_8), 200, "OK", List.of(BrowserUtil.JSResponseHeader)), request, 0);
                    }
                    return super.getResponse(request);
                }
            });
            final HtmlPage page = client.getPage(url.url());
            try {
                page.getElementByName("description");
            } catch (final ElementNotFoundException ignore) {
                return null;
            }
            boolean onof;
            HtmlInput input = null;
            try {
                input = page.getElementByName("pwd");
                onof = true;
            } catch (final ElementNotFoundException ignore) {
                onof = false;
            }
            if (onof) {
                if (password == null)
                    throw new IllegalParametersException("Require password.", ParametersMap.create().add("url", url));
                input.setValue(password);
                boolean flag = true;
                for (final HtmlElement element: input.getParentNode().getHtmlElementDescendants())
                    if (element.hasAttribute("onclick")) {
                        element.click();
                        if (flag)
                            flag = false;
                        else
                            LanzouProvider.logger.log(HLogLevel.WARN, "Multi clickable elements after input password.", ParametersMap.create().add("url", url).add("password", password));
                    }
                if (flag)
                    throw new IllegalStateException("No clickable element after input password." + ParametersMap.create().add("url", url).add("password", password));
                BrowserUtil.waitJavaScriptCompleted(client);
                final String info = page.getHtmlElementById("info").asNormalizedText();
                if ("\u5BC6\u7801\u4E0D\u6B63\u786E".equals(info))
                    throw new IllegalParametersException("Wrong password.", ParametersMap.create().add("url", url).add("password", password));
                if (!info.isEmpty())
                    throw new IllegalStateException("Unknown info after enter password." + ParametersMap.create().add("url", url).add("password", password).add("info", info));
                downloading = page.getHtmlElementById("downajax");
            } else {
                BrowserUtil.waitJavaScriptCompleted(client, 1);
                final List<FrameWindow> frames = page.getFrames();
                if (frames.isEmpty())
                    throw new IllegalStateException("No iframe." + ParametersMap.create().add("url", url).add("password", password));
                if (frames.size() != 1)
                    throw new IllegalStateException("Multi iframes." + ParametersMap.create().add("url", url).add("password", password).add("frames", frames.size()));
                final HtmlPage frame = (HtmlPage) page.getFrames().get(0).getFrameElement().getEnclosedPage();
                downloading = frame.getHtmlElementById("tourl");
            }
        }
        String downloadUrl = "";
        for (final HtmlElement element: downloading.getElementsByTagName("a")) {
            final String current = element.getAttribute("href");
            if (downloadUrl.isEmpty())
                downloadUrl = current;
            else if (!current.isEmpty()) {
                LanzouProvider.logger.log(HLogLevel.WARN, "Multi download urls.", ParametersMap.create().add("url", url).add("password", password).add("url", downloadUrl));
                downloadUrl = current;
            }
        }
        if (downloadUrl.isEmpty())
            throw new IllegalStateException("No download url." + ParametersMap.create().add("url", url).add("password", password));
        final HttpUrl displayUrl = Objects.requireNonNull(HttpUrl.parse(downloadUrl));
        // A Provider Bug: 9/27/2023
        // In Lanzou Provider, using the HEAD method for the first download after uploading will cause the file length to be reset to zero.
        // Whether uploaded through a browser or through WList. Therefore, use the GET method to avoid this bug.
        final HttpUrl finalUrl;
        try (final Response response = HttpNetworkHelper.getWithParameters(HttpNetworkHelper.DefaultNoRedirectHttpClient, Pair.ImmutablePair.makeImmutablePair(displayUrl, "HEAD"), LanzouProvider.Headers, null).execute()) {
            if (!response.isRedirect()) // always redirect?
                return Pair.ImmutablePair.makeImmutablePair(displayUrl, response.headers());
            finalUrl = HttpNetworkHelper.extraLocationHeader(displayUrl, response.header("Location"));
            assert finalUrl != null;
        }
        try (final Response response = HttpNetworkHelper.getWithParameters(HttpNetworkHelper.DefaultNoRedirectHttpClient, Pair.ImmutablePair.makeImmutablePair(finalUrl, "GET"), LanzouProvider.Headers, null).execute()) {
            return Pair.ImmutablePair.makeImmutablePair(finalUrl, response.headers());
        }
    }

    protected Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull ZonedDateTime> testRealSizeAndData(final @NotNull HttpUrl url, final @Nullable Headers header) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final Headers headers;
        if (header == null)
            try (final Response response = HttpNetworkHelper.getWithParameters(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url, "HEAD"), LanzouProvider.Headers, null).execute()) {
                headers = response.headers();
            }
        else headers = header;
        final String sizeS = headers.get("Content-Length");
        final String dataS = headers.get("Last-Modified");
        if (sizeS == null || dataS == null)
            return null;
        try {
            return Pair.ImmutablePair.makeImmutablePair(Long.parseLong(sizeS), ZonedDateTime.parse(dataS, LanzouSharer.dataTimeFormatter));
        } catch (final NumberFormatException | DateTimeParseException ignore) {
            return null;
        }
    }

}
