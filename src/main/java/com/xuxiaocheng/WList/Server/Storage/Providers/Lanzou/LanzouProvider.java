package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Server.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.Util.JavaScriptUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("SpellCheckingInspection")
public class LanzouProvider extends AbstractIdBaseProvider<LanzouConfiguration> {
    @Override
    public @NotNull ProviderTypes<LanzouConfiguration> getType() {
        return ProviderTypes.Lanzou;
    }

    private static final @NotNull HLog logger = HLog.create("DriverLogger/lanzou");

    static final @NotNull DateTimeFormatter dataTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    static final @NotNull Headers headers = new Headers.Builder().set("referer", "https://up.woozooo.com/u").set("accept-language", "zh-CN")
            .set("user-agent", HttpNetworkHelper.defaultWebAgent).set("cache-control", "no-cache").build();

    static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> LoginURL =
            Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/mlogin.php")), "POST"); // TODO use account.php
//    static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> InformationURL =
//    Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/mydisk.php")), "GET");
    static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> TaskURL =
        Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/doupload.php")), "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> UploadURL =
            Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/html5up.php")), "POST");

    static @NotNull String requestHtml(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url) throws IOException {
        try (final ResponseBody body = HttpNetworkHelper.extraResponseBody(HttpNetworkHelper.getWithParameters(httpClient, url, LanzouProvider.headers, null).execute())) {
            return ProviderUtil.removeHtmlComments(body.string());
        }
    }
    static @NotNull JSONObject requestJson(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> url, final FormBody.@NotNull Builder request) throws IOException {
        return HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithBody(httpClient, url, LanzouProvider.headers, request.build()).execute());
    }

    @Override
    protected void loginIfNot() throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        if (configuration.getToken() != null && configuration.getTokenExpire() != null && !MiscellaneousUtil.now().isAfter(configuration.getTokenExpire()))
            return;
        if (configuration.getPassport().isEmpty() || configuration.getPassword().isEmpty()) // Quicker response.
            throw new IllegalResponseCodeException(0, "\u5BC6\u7801\u4E0D\u5BF9", ParametersMap.create().add("process", "login").add("configuration", configuration));
        final FormBody.Builder builder = new FormBody.Builder()
                .add("task", "3")
                .add("uid", configuration.getPassport())
                .add("pwd", configuration.getPassword());
        final List<Cookie> cookies;
        final JSONObject json;
        try (final Response response = HttpNetworkHelper.postWithBody(configuration.getHttpClient(), LanzouProvider.LoginURL, LanzouProvider.headers, builder.build()).execute()) {
            cookies = Cookie.parseAll(response.request().url(), response.headers());
            json = HttpNetworkHelper.extraJsonResponseBody(response);
        }
        final int code = json.getIntValue("zt", -1);
        final String info = json.getString("info");
        final Long id = json.getLong("id");
        Cookie token = null;
        for (final Cookie cookie: cookies)
            if ("phpdisk_info".equals(cookie.name())) {
                token = cookie;
                break;
            }
        if (code != 1 || !"\u6210\u529f\u767b\u5f55".equals(info) || token == null)
            throw new IllegalResponseCodeException(code, info, ParametersMap.create().add("process", "login").add("configuration", configuration).add("json", json).add("cookies", cookies));
        configuration.setName(configuration.getPassport()); // TODO: if vip.
        configuration.setUid(id.longValue());
        configuration.setToken(token.value());
        configuration.setTokenExpire(ZonedDateTime.ofInstant(Instant.ofEpochMilli(token.expiresAt()), ZoneOffset.UTC));
        configuration.markModified();
        LanzouProvider.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create().add("storage", configuration.getName()).add("passport", configuration.getPassport()));

    }

    protected @NotNull JSONObject task(final int type, final FormBody.@NotNull Builder request, final @Nullable Integer zt) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", String.valueOf(configuration.getUid()));
        request.add("task", String.valueOf(type));
        final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), LanzouProvider.TaskURL,
                LanzouProvider.headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getToken() + "; ").build(), parameters, request.build()).execute());
        if (zt != null) {
            final Integer code = json.getInteger("zt");
            if (code == null || code.intValue() != zt.intValue())
                throw new IllegalResponseCodeException(code == null ? -1 : code.intValue(), json.getString("info") == null ? json.getString("text") : json.getString("info"),
                        ParametersMap.create().add("configuration", configuration).add("requireZt", zt).add("json", json));
        }
        return json;
    }

    protected @Nullable @Unmodifiable List<@NotNull FileInformation> listAllDirectory(final long directoryId) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final FormBody.Builder filesBuilder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId));
        final JSONObject json = this.task(47, filesBuilder, null);
        final Integer code = json.getInteger("zt");
        if (code == null || (code.intValue() != 1 && code.intValue() != 2))
            throw new IllegalResponseCodeException(code == null ? -1 : code.intValue(), json.getString("info") == null ? json.getString("text") : json.getString("info"),
                    ParametersMap.create().add("configuration", configuration).add("directoryId", directoryId).add("requireZt", "1 || 2").add("json", json));
        if (directoryId != -1) {
            final JSONArray directoryInfo = json.getJSONArray("info");
            if (directoryInfo == null)
                throw new WrongResponseException("Listing directories.", json, ParametersMap.create()
                        .add("configuration", configuration).add("directoryId", directoryId));
            if (directoryInfo.isEmpty())
                return null;
            //noinspection SpellCheckingInspection
            assert directoryInfo.size() == 1 && directoryInfo.getJSONObject(0) != null && directoryInfo.getJSONObject(0).getIntValue("folderid", -1) == directoryId;
        }
        final JSONArray filesInfos = json.getJSONArray("text");
        if (filesInfos == null)
            throw new WrongResponseException("Listing directories.", json, ParametersMap.create().add("configuration", configuration).add("directoryId", directoryId));
        final List<FileInformation> list = new ArrayList<>(filesInfos.size());
        for (final JSONObject info: filesInfos.toList(JSONObject.class)) {
            final String name = info.getString("name");
            final Long id = info.getLong("fol_id");
            if (name == null || id == null) continue;
            list.add(new FileInformation(id.longValue(), directoryId, name, true, -1, null, null, null));
        }
        return list;
    }

    protected @NotNull @Unmodifiable Set<@NotNull FileInformation> listFilesInPage(final long directoryId, final int page) throws IOException, InterruptedException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final FormBody.Builder builder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId))
                .add("pg", String.valueOf(page + 1));
        final JSONObject files = this.task(5, builder, 1);
        final JSONArray infos = files.getJSONArray("text");
        if (infos == null)
            throw new WrongResponseException("Listing files.", files, ParametersMap.create().add("configuration", configuration).add("directoryId", directoryId).add("page", page));
        if (infos.isEmpty()) return Set.of();
        final Set<FileInformation> set = ConcurrentHashMap.newKeySet();
        try {
            HMultiRunHelper.runConsumers(WListServer.IOExecutors, infos.toList(JSONObject.class), HExceptionWrapper.wrapConsumer(info -> {
                final String name = info.getString("name");
                final Long id = info.getLong("id");
                if (name == null || id == null) return;
                final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(id.longValue());
                if (shareUrl == null) return;
                final String others = JSON.toJSONString(Map.of(
                        "domin", shareUrl.getA().toString(),
                        "identifier", shareUrl.getB(),
                        "pwd", Objects.requireNonNullElse(shareUrl.getC(), "")
                ));
                boolean flag = true;
                try {
                    final Pair.ImmutablePair<HttpUrl, Headers> downloadUrl = this.getSingleShareFileDownloadUrl(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
                    if (downloadUrl != null) {
                        final Pair<Long, ZonedDateTime> fixed = this.testRealSizeAndData(downloadUrl);
                        if (fixed != null) {
                            set.add(new FileInformation(id.longValue(), directoryId, name, false, fixed.getFirst().longValue(), fixed.getSecond(), fixed.getSecond(), others));
                            flag = false;
                            return;
                        }
                    }
                } finally {
                    if (flag)
                        set.add(new FileInformation(id.longValue(), directoryId, name, false, 0, null, null, others));
                }
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class);
        }
        return Collections.unmodifiableSet(set);
    }

    protected @Nullable Triad.ImmutableTriad<@NotNull HttpUrl, @NotNull String, @Nullable String> getFileShareUrl(final long fileId) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final FormBody.Builder sharerBuilder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId));
        final JSONObject json = this.task(22, sharerBuilder, 1);
        final JSONObject info = json.getJSONObject("info");
        if (info == null)
            throw new WrongResponseException("Getting share url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        final HttpUrl domin = HttpUrl.parse(info.getString("is_newd"));
        final String identifier = info.getString("f_id");
        final Boolean hasPwd = info.getInteger("onof") == null ? null : info.getIntValue("onof") == 1;
        final String pwd = info.getString("pwd");
        if (domin == null || identifier == null || hasPwd == null || (hasPwd.booleanValue() && pwd == null))
            throw new WrongResponseException("Getting download url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        return Triad.ImmutableTriad.makeImmutableTriad(domin, identifier, hasPwd.booleanValue() ? pwd : null);
    }

    private static final @NotNull Pattern srcPattern = Pattern.compile("src=\"(/fn?[^\"]+)");
    protected @Nullable Pair.ImmutablePair<@NotNull HttpUrl, @Nullable Headers> getSingleShareFileDownloadUrl(final @NotNull HttpUrl domin, final @NotNull String identifier, final @Nullable String pwd) throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final String sharePage = LanzouProvider.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin.newBuilder().addPathSegment(identifier).build(), "GET"));
        if (sharePage.contains("\u6587\u4EF6\u53D6\u6D88\u5206\u4EAB\u4E86") || sharePage.contains("\u6587\u4EF6\u5730\u5740\u9519\u8BEF"))
            return null;
        final ParametersMap parametersMap = ParametersMap.create().add("configuration", configuration).add("domin", domin).add("identifier", identifier);
        final List<String> javaScript;
        if (sharePage.contains("<iframe")) {
            final Matcher srcMatcher = LanzouProvider.srcPattern.matcher(sharePage);
            if (!srcMatcher.find())
                throw new WrongResponseException("No src matched.", sharePage, parametersMap);
            final String src = srcMatcher.group(1);
            final String loadingPage = LanzouProvider.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin.newBuilder().addPathSegment(src).build(), "GET"));
            javaScript = ProviderUtil.findScripts(loadingPage);
        } else {
            parametersMap.add("pwd", pwd);
            if (pwd == null)
                throw new IllegalParametersException("Require password.", ParametersMap.create().add("domin", domin).add("identifier", identifier));
            final List<String> scripts = ProviderUtil.findScripts(sharePage);
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
        final String ajaxUrl;
        final Map<String, Object> ajaxData;
        try {
            final Map<String, Object> ajax = JavaScriptUtil.extraOnlyAjaxData(javaScript);
            if (ajax == null)
                throw new IOException("Null ajax.");
            assert "post".equals(ajax.get("type"));
            ajaxUrl = (String) ajax.get("url");
            ajaxData = (Map<String, Object>) ajax.get("data");
        } catch (final JavaScriptUtil.ScriptException exception) {
            throw new IOException("Failed to run share page java scripts." + parametersMap, exception);
        }
        final FormBody.Builder builder = new FormBody.Builder();
        for (final Map.Entry<String, Object> entry: ajaxData.entrySet())
            builder.add(entry.getKey(), entry.getValue().toString());
        final JSONObject json = LanzouProvider.requestJson(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin.newBuilder().addPathSegment(ajaxUrl).build(), "POST"), builder);
        final int code = json.getIntValue("zt", -1);
        if (code != 1)
            throw new IllegalResponseCodeException(code, json.getString("inf"), parametersMap.add("json", json));
        final HttpUrl dom = HttpUrl.parse(json.getString("dom"));
        final String para = json.getString("url");
        if (dom == null || para == null)
            throw new WrongResponseException("Getting single shared file download url.", json, parametersMap);
        final HttpUrl displayUrl = dom.newBuilder().addPathSegment("file").addPathSegment("?" + para).build();
        final String redirectPage;
        try (final Response response = HttpNetworkHelper.getWithParameters(HttpNetworkHelper.DefaultNoRedirectHttpClient, Pair.ImmutablePair.makeImmutablePair(displayUrl, "GET"), LanzouProvider.headers, null).execute()) {
            if (response.code() == 302) {
                final String finalUrl = response.header("Location");
                assert finalUrl != null;
                return Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse(finalUrl)), null);
            }
            redirectPage = ProviderUtil.removeHtmlComments(HttpNetworkHelper.extraResponseBody(response).string());
        }
        // TODO: el
        LanzouProvider.logger.log(HLogLevel.WARN, "Find el: " + redirectPage);
        return Pair.ImmutablePair.makeImmutablePair(displayUrl, null);
    }

    protected Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull ZonedDateTime> testRealSizeAndData(final @NotNull Pair.ImmutablePair<@NotNull HttpUrl, @Nullable Headers> downloadUrl) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final Headers headers;
        if (downloadUrl.getSecond() == null)
            try (final Response response = HttpNetworkHelper.getWithParameters(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(downloadUrl.getFirst(), "HEAD"), LanzouProvider.headers, null).execute()) {
                headers = response.headers();
            }
        else headers = downloadUrl.getSecond();
        final String sizeS = headers.get("Content-Length");
        final String dataS = headers.get("Last-Modified");
        if (sizeS == null || dataS == null)
            return null;
        final long size;
        final ZonedDateTime time;
        try {
            size = Long.parseLong(sizeS);
            time = ZonedDateTime.parse(dataS, LanzouProvider.dataTimeFormatter);
        } catch (final NumberFormatException | DateTimeParseException ignore) {
            return null;
        }
        return Pair.ImmutablePair.makeImmutablePair(size, time);
    }

    @Override
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws IOException {
        final List<FileInformation> directories = this.listAllDirectory(directoryId);
        if (directories == null) return null;
        return ProviderUtil.wrapSuppliersInPages(page -> {
            if (page.intValue() == 0)
                return directories;
            final Collection<FileInformation> list = this.listFilesInPage(directoryId, page.intValue() - 1);
            if (list.isEmpty())
                return null;
            return list;
        }, HExceptionWrapper.wrapConsumer(e -> {
            if (e != null)
                throw e;
        })).getFirst();
    }

    @Override
    protected @NotNull UnionPair<FileInformation, Boolean> update0(@NotNull final FileInformation oldInformation) throws Exception {
        return UnionPair.fail(Boolean.TRUE);
    }

//    @Override
//    protected void delete0(final @NotNull FileInformation information) throws IOException {
//        if (information.isDirectory()) {
//            final FormBody.Builder builder = new FormBody.Builder()
//                    .add("folder_id", String.valueOf(information.id()));
//            final JSONObject json = this.task(3, builder, 1);
//            final String message = json.getString("info");
//            if (!"\u5220\u9664\u6210\u529F".equals(message))
//                throw new WrongResponseException("Trashing directory.", message, ParametersMap.create()
//                        .add("configuration", this.configuration.getInstance()).add("information", information).add("json", json));
//        } else {
//            final FormBody.Builder builder = new FormBody.Builder()
//                    .add("file_id", String.valueOf(information.id()));
//            final JSONObject json = this.task(6, builder, 1);
//            final String message = json.getString("info");
//            if (!"\u5DF2\u5220\u9664".equals(message))
//                throw new WrongResponseException("Trashing file.", message, ParametersMap.create()
//                        .add("configuration", this.configuration.getInstance()).add("information", information).add("json", json));
//        }
//    }
//
//    @Override
//    protected @NotNull UnionPair<DownloadRequirements, FailureReason> download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull FileLocation location) throws Exception {
//        return null;
//    }
//
//    protected static @NotNull CheckRule<@NotNull String> nameChecker = new CheckRuleSet<>(
//        new SuffixCheckRule(Set.of("doc","docx","zip","rar","apk","ipa","txt","exe","7z","e","z","ct","ke","cetrainer","db","tar","pdf","w3x","epub",
//                "mobi","azw","azw3","osk","osz","xpa","cpk","lua","jar","dmg","ppt","pptx","xls","xlsx","mp3","iso","img","gho","ttf","ttc","txf","dwg","bat",
//                "imazingapp","dll","crx","xapk","conf","deb","rp","rpm","rplib","mobileconfig","appimage","lolgezi","flac","cad","hwt","accdb","ce","xmind",
//                "enc","bds","bdi","ssf","it","pkg","cfg"))
//        //TODO
//    );
//
//    @Override
//    protected @NotNull CheckRule<@NotNull String> nameChecker() {
//        return LanzouProvider.nameChecker;
//    }
//
//    @Override
//    protected @NotNull UnionPair<FileInformation, FailureReason> createDirectory0(final long parentId, @NotNull final String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull FileLocation parentLocation) throws Exception {
//        throw new UnsupportedOperationException();
//    }
}
