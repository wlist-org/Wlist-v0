package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Server.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseSharer;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Util.JavaScriptUtil;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("SpellCheckingInspection")
public class LanzouSharer extends AbstractIdBaseSharer<LanzouConfiguration> {
    @Override
    public @NotNull StorageTypes<LanzouConfiguration> getType() {
        return StorageTypes.Lanzou;
    }

    protected static final @NotNull DateTimeFormatter dataTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final @NotNull Iterable<@NotNull Pattern> HtmlCommentsTags = List.of(Pattern.compile("<!--.*?-->"));
    protected static @NotNull String removeHtmlComments(final @Nullable String html) {
        if (html == null)
            return "";
        String res = html;
        for (final Pattern pattern: LanzouSharer.HtmlCommentsTags)
            res = pattern.matcher(res).replaceAll("");
        return res;
    }

    private static final @NotNull String scriptStartTag = "<script type=\"text/javascript\">";
    private static final @NotNull String scriptEndTag = "</script>";
    protected static @NotNull List<@NotNull String> findScripts(final @NotNull String html) {
        final List<String> scripts = new ArrayList<>();
        int index = 0;
        while (true) {
            index = html.indexOf(LanzouSharer.scriptStartTag, index);
            if (index == -1) break;
            final int endIndex = html.indexOf(LanzouSharer.scriptEndTag, index);
            if (endIndex == -1) break;
            scripts.add(html.substring(index + LanzouSharer.scriptStartTag.length(), endIndex));
            index = endIndex;
        }
        return scripts;
    }

    static @NotNull String requestHtml(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url) throws IOException {
        try (final ResponseBody body = HttpNetworkHelper.extraResponseBody(HttpNetworkHelper.getWithParameters(httpClient, url, LanzouProvider.Headers, null).execute())) {
            return LanzouSharer.removeHtmlComments(body.string());
        }
    }

    static @NotNull JSONObject requestJson(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final FormBody.@NotNull Builder request) throws IOException {
        return HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithBody(httpClient, url, LanzouProvider.Headers, request.build()).execute());
    }

    private static final @NotNull Pattern srcPattern = Pattern.compile("src=\"/(fn?[^\"]+)");
    @SuppressWarnings("unchecked")
    protected @Nullable Pair.ImmutablePair<@NotNull HttpUrl, @Nullable Headers> getSingleShareFileDownloadUrl(final @NotNull HttpUrl domin, final @NotNull String id, final @Nullable String pwd) throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final String sharePage = LanzouSharer.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin.newBuilder().addPathSegment(id).build(), "GET"));
        if (sharePage.contains("\u6587\u4EF6\u53D6\u6D88\u5206\u4EAB\u4E86") || sharePage.contains("\u6587\u4EF6\u5730\u5740\u9519\u8BEF"))
            return null;
        final ParametersMap parametersMap = ParametersMap.create().add("configuration", configuration).add("domin", domin).add("id", id);
        final List<String> javaScript;
        if (sharePage.contains("<iframe")) {
            final Matcher srcMatcher = LanzouSharer.srcPattern.matcher(sharePage);
            if (!srcMatcher.find())
                throw new WrongResponseException("No src matched.", sharePage, parametersMap);
            final String src = srcMatcher.group(1);
            final HttpUrl url = Objects.requireNonNull(HttpUrl.parse(domin + src));
            final String loadingPage = LanzouSharer.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url, "GET"));
            javaScript = LanzouSharer.findScripts(loadingPage);
        } else {
            parametersMap.add("pwd", pwd);
            if (pwd == null)
                throw new IllegalParametersException("Require password.", ParametersMap.create().add("domin", domin).add("id", id));
            final List<String> scripts = LanzouSharer.findScripts(sharePage);
            javaScript = new ArrayList<>(scripts.size());
            for (final String script: scripts) {
                //noinspection ReassignedVariable,NonConstantStringShouldBeStringBuffer
                String js = script.replace("document.getElementById('pwd').value", String.format("'%s'", pwd));
                if (js.contains("$(document).keyup(")) {
                    js = js.replace("$(document)", "$$()");
                    js = "function $$(){return{keyup:function(f){f({keyCode:13});}};}" + js;
                }
                final int endIndex = js.indexOf("document.getElementById('rpt')");
                if (endIndex > 0)
                    js = js.substring(0, endIndex);
                javaScript.add(js);
            }
        }
        final Map<String, Object> ajaxData;
        try {
            final Map<String, Object> ajax = JavaScriptUtil.extraOnlyAjaxData(javaScript);
            if (ajax == null)
                throw new IOException("Null ajax.");
            assert "post".equals(ajax.get("type"));
            assert "/ajaxm.php".equals(ajax.get("url"));
            ajaxData = (Map<String, Object>) ajax.get("data");
        } catch (final JavaScriptUtil.ScriptException exception) {
            throw new IOException("Failed to run share page java scripts." + parametersMap, exception);
        }
        final FormBody.Builder builder = new FormBody.Builder();
        for (final Map.Entry<String, Object> entry: ajaxData.entrySet())
            builder.add(entry.getKey(), entry.getValue().toString());
        final JSONObject json = LanzouSharer.requestJson(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin.newBuilder().addPathSegment("ajaxm.php").build(), "POST"), builder);
        final int code = json.getIntValue("zt", -1);
        if (code != 1)
            throw new IllegalResponseCodeException(code, json.getString("inf"), parametersMap.add("json", json));
        final HttpUrl dom = HttpUrl.parse(json.getString("dom"));
        final String para = json.getString("url");
        if (dom == null || para == null)
            return null;
        //noinspection StringConcatenationMissingWhitespace
        final HttpUrl displayUrl = Objects.requireNonNull(HttpUrl.parse(dom + "file/" + para));
        // A Provider Bug: 9/27/2023
        // In Lanzou Provider, using the HEAD method for the first download after uploading will cause the file length to be reset to zero.
        // Whether uploaded through a browser or through WList. Therefore, always use the GET method to avoid this bug.
        try (final Response response = HttpNetworkHelper.getWithParameters(this.getConfiguration().getFileClient(), Pair.ImmutablePair.makeImmutablePair(displayUrl, "GET"), LanzouProvider.Headers, null).execute()) {
            return Pair.ImmutablePair.makeImmutablePair(displayUrl, response.headers());
        }
//        return Pair.ImmutablePair.makeImmutablePair(displayUrl, null);
//        try (final Response response = HttpNetworkHelper.getWithParameters(HttpNetworkHelper.DefaultNoRedirectHttpClient, Pair.ImmutablePair.makeImmutablePair(displayUrl, "HEAD"), LanzouProvider.Headers, null).execute()) {
//            if (response.isRedirect()) {
//                final String finalUrl = response.header("Location");
//                assert finalUrl != null;
//                return Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse(finalUrl)), null);
//            }
//            return Pair.ImmutablePair.makeImmutablePair(displayUrl, response.headers()); // TODO: Stable? el?
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
