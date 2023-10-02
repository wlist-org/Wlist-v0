package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseSharer;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BrowserUtil;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.FrameWindow;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.WebConnectionWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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

    public @Nullable Pair.ImmutablePair<@NotNull HttpUrl, @Nullable Headers> getSingleShareFileDownloadUrl(final @NotNull HttpUrl domin, final @NotNull String identifier, final @Nullable String password) throws IOException, IllegalParametersException {
        final HtmlElement downloading;
        try (final WebClient client = BrowserUtil.newWebClient()) {
            client.setWebConnection(new WebConnectionWrapper(client.getWebConnection()) {
                @Override
                public @NotNull WebResponse getResponse(final @NotNull WebRequest request) throws IOException {
                    if (!request.getUrl().getHost().equals(domin.url().getHost()) && !request.getUrl().getHost().equals(LanzouSharer.AssertHost))
                        return BrowserUtil.emptyResponse(request);
                    return super.getResponse(request);
                }
            });
            final HtmlPage page = client.getPage(domin.newBuilder().addPathSegment(identifier).build().url());
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
                    throw new IllegalParametersException("Require password.", ParametersMap.create().add("domin", domin).add("identifier", identifier));
                input.setValue(password);
                boolean flag = true;
                for (final HtmlElement element: input.getParentNode().getHtmlElementDescendants())
                    if (element.hasAttribute("onclick")) {
                        element.click();
                        flag = false;
                    }
                if (flag)
                    throw new IllegalStateException("No clickable element after input password." + ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password));
                BrowserUtil.waitJavaScriptCompleted(client);
                final String info = page.getHtmlElementById("info").asNormalizedText();
                if ("\u5BC6\u7801\u4E0D\u6B63\u786E".equals(info))
                    throw new IllegalParametersException("Wrong password.", ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password));
                if (!info.isEmpty())
                    throw new IllegalStateException("Unknown info after enter password." + ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password).add("info", info));
                downloading = page.getHtmlElementById("downajax");
            } else {
                BrowserUtil.waitJavaScriptCompleted(client, 1);
                final List<FrameWindow> frames = page.getFrames();
                if (frames.size() != 1)
                    throw new IllegalStateException("Unclear iframe." + ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password).add("frames", frames.size()));
                final HtmlPage frame = (HtmlPage) page.getFrames().get(0).getFrameElement().getEnclosedPage();
                downloading = frame.getHtmlElementById("tourl");
            }
        }
        String url = "";
        for (final HtmlElement element: downloading.getElementsByTagName("a")) {
            final String current = element.getAttribute("href");
            if (url.isEmpty())
                url = current;
            else if (!current.isEmpty()) {
                LanzouProvider.logger.log(HLogLevel.WARN, "Multi download urls.", ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password).add("url", url));
                url = current;
            }
        }
        if (url.isEmpty())
            throw new IllegalStateException("No download url." + ParametersMap.create().add("domin", domin).add("identifier", identifier).add("password", password));
        final HttpUrl displayUrl = Objects.requireNonNull(HttpUrl.parse(url));
        // A Provider Bug: 9/27/2023
        // In Lanzou Provider, using the HEAD method for the first download after uploading will cause the file length to be reset to zero.
        // Whether uploaded through a browser or through WList. Therefore, always use the GET method to avoid this bug.
        try (final Response response = HttpNetworkHelper.getWithParameters(this.getConfiguration().getFileClient(), Pair.ImmutablePair.makeImmutablePair(displayUrl, "GET"), LanzouProvider.Headers, null).execute()) {
            return Pair.ImmutablePair.makeImmutablePair(displayUrl, response.headers());
        }
        // The code following does not work.
//        try (final Response response = HttpNetworkHelper.getWithParameters(HttpNetworkHelper.DefaultNoRedirectHttpClient, Pair.ImmutablePair.makeImmutablePair(displayUrl, "GET"), LanzouProvider.Headers, null).execute()) {
//            if (response.isRedirect()) {
//                final String finalUrl = response.header("Location");
//                assert finalUrl != null;
//                return Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse(finalUrl)), null);
//            }
//            return Pair.ImmutablePair.makeImmutablePair(displayUrl, response.headers());
//        }
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
