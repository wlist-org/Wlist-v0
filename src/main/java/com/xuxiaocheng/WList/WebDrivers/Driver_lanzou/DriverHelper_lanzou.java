package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"SameParameterValue", "SpellCheckingInspection"})
final class DriverHelper_lanzou {
    private DriverHelper_lanzou() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("DriverLogger/lanzou", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true, HMergedStreams.getFileOutputStreamNoException(null));

    static final @NotNull DateTimeFormatter dataTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    static final @NotNull Headers headers = new Headers.Builder().set("referer", "https://up.woozooo.com/").set("accept-language", "zh-CN")
            .set("user-agent", DriverNetworkHelper.defaultAgent).set("cache-control", "no-cache").build();

    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/mlogin.php", "POST"); // TODO use account.php
//    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> InformationURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/mydisk.php", "GET");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> TaskURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/doupload.php", "POST");

    static @NotNull ResponseBody request(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @NotNull Map<@NotNull String, @NotNull String> request) throws IOException {
        return DriverNetworkHelper.extraResponseBody(DriverNetworkHelper.getWithParameters(httpClient, url, DriverHelper_lanzou.headers, request).execute());
    }
    static @NotNull ResponseBody request(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final FormBody.@NotNull Builder request) throws IOException {
        return DriverNetworkHelper.extraResponseBody(DriverNetworkHelper.postWithBody(httpClient, url, DriverHelper_lanzou.headers, request.build()).execute());
    }
    static @NotNull JSONObject requestJson(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @NotNull Map<@NotNull String, @NotNull String> request) throws IOException {
        return DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.getWithParameters(httpClient, url, DriverHelper_lanzou.headers, request).execute());
    }
    static @NotNull JSONObject requestJson(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final FormBody.@NotNull Builder request) throws IOException {
        return DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.postWithBody(httpClient, url, DriverHelper_lanzou.headers, request.build()).execute());
    }

    static void login(final @NotNull DriverConfiguration_lanzou configuration) throws IOException {
        if (configuration.getWebSide().getPassport().isEmpty() || configuration.getWebSide().getPassword().isEmpty())
            throw new IllegalResponseCodeException(0, "\u5BC6\u7801\u4E0D\u5BF9", ParametersMap.create()
                    .add("process", "login").add("configuration", configuration));
        final FormBody.Builder builder = new FormBody.Builder()
                .add("task", "3")
                .add("uid", configuration.getWebSide().getPassport())
                .add("pwd", configuration.getWebSide().getPassword());
        final List<Cookie> cookies;
        final JSONObject json;
        try (final Response response = DriverNetworkHelper.postWithBody(configuration.getHttpClient(), DriverHelper_lanzou.LoginURL, DriverHelper_lanzou.headers, builder.build()).execute()) {
            cookies = Cookie.parseAll(response.request().url(), response.headers()); // expires=Sat, 12-Aug-2023 06:42:01 GMT;
            json = DriverNetworkHelper.extraJsonResponseBody(response);
        }
        final int code = json.getIntValue("zt", -1);
        final String info = json.getString("info");
        final Long id = json.getLong("id");
        Cookie c = null;
        for (final Cookie cookie: cookies)
            if ("phpdisk_info".equals(cookie.name())) {
                c = cookie;
                break;
            }
        if (code != 1 || !"\u6210\u529f\u767b\u5f55".equals(info) || c == null)
            throw new IllegalResponseCodeException(code, info, ParametersMap.create()
                    .add("process", "login").add("configuration", configuration).add("json", json).add("cookies", cookies));
        final String identifier = c.value();
        final LocalDateTime expire = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.expiresAt()), ZoneOffset.UTC);
        configuration.getCacheSide().setUid(id.longValue());
        configuration.getCacheSide().setIdentifier(identifier);
        configuration.getCacheSide().setTokenExpire(expire);
        configuration.getCacheSide().setModified(true);
        DriverHelper_lanzou.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create()
                .add("driver", configuration.getName()).add("passport", configuration.getWebSide().getPassport()));
    }

    static void ensureLoggedIn(final @NotNull DriverConfiguration_lanzou configuration) throws IOException {
        if (configuration.getCacheSide().getIdentifier() == null || configuration.getCacheSide().getTokenExpire() == null
                || LocalDateTime.now().isAfter(configuration.getCacheSide().getTokenExpire()))
            DriverHelper_lanzou.login(configuration);
    }

    static @NotNull JSONObject task(final @NotNull DriverConfiguration_lanzou configuration, final int type, final FormBody.@NotNull Builder request, final @Nullable Integer zt) throws IOException {
        DriverManager_lanzou.ensureLoggedIn(configuration);
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", String.valueOf(configuration.getCacheSide().getUid()));
        request.add("task", String.valueOf(type));
        final JSONObject json = DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), DriverHelper_lanzou.TaskURL,
                DriverHelper_lanzou.headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getCacheSide().getIdentifier() + "; ").build(), parameters, request.build()).execute());
        if (zt != null) {
            final Integer code = json.getInteger("zt");
            if (code == null || code.intValue() != zt.intValue())
                throw new IllegalResponseCodeException(code == null ? -1 : code.intValue(), json.getString("info") == null ? json.getString("text") : json.getString("info"),
                        ParametersMap.create().add("configuration", configuration).add("requireZt", zt).add("json", json));
        }
        return json;
    }

    private static final @NotNull Pattern signPattern = Pattern.compile("&sign=([^'&]+)");
    @SuppressWarnings("SpellCheckingInspection")
    static @Nullable String getSingleShareFileDownloadUrl(final @NotNull DriverConfiguration_lanzou configuration, final @NotNull String domin, final @NotNull String identifier, final @NotNull String pwd) throws IOException {
        final String message;
        try (final ResponseBody body = DriverHelper_lanzou.request(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin + identifier, "GET"), Map.of())) {
            message = DriverUtil.removeHtmlComments(body.string());
        }
        if (message.contains("\u6587\u4EF6\u53D6\u6D88\u5206\u4EAB\u4E86"))
            return null;
        final Matcher signMatcher = DriverHelper_lanzou.signPattern.matcher(message);
        if (!signMatcher.find())
            throw new WrongResponseException("No sign matched.", message, ParametersMap.create().add("configuration", configuration).add("domin", domin).add("identifier", identifier).add("pwd", pwd));
        final String sign = signMatcher.group().substring("&sign=".length());
        final FormBody.Builder builder = new FormBody.Builder()
                .add("action", "downprocess")
                .add("sign", sign)
                .add("p", pwd);
        final JSONObject json;
        try (final ResponseBody body = DriverHelper_lanzou.request(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin + "/ajaxm.php", "POST"), builder)) {
            try (final InputStream stream = body.byteStream()) {
                json = JSON.parseObject(stream);
            }
        }
        final Integer code = json.getInteger("zt");
        if (code == null || code.intValue() != 1)
            throw new IllegalResponseCodeException(code == null ? -1 : code.intValue(), json.getString("info"), ParametersMap.create().add("configuration", configuration).add("json", json));
        final String dom = json.getString("dom");
        final String para = json.getString("url");
        if (dom == null || para == null)
            throw new WrongResponseException("Getting single shared file download url.", json, ParametersMap.create().add("configuration", configuration).add("domin", domin).add("identifier", identifier).add("pwd", pwd));
        return dom + "/file/" + para;
    }

    @SuppressWarnings("SpellCheckingInspection")
    static @Nullable String getFileDownloadUrl(final @NotNull DriverConfiguration_lanzou configuration, final long fileId) throws IOException {
        final FormBody.Builder sharerBuilder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId));
        final JSONObject json = DriverHelper_lanzou.task(configuration, 22, sharerBuilder, 1);
        final JSONObject info = json.getJSONObject("info");
        if (info == null)
            throw new WrongResponseException("Getting download url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        final String domin = info.getString("is_newd");
        final String identifier = info.getString("f_id");
        final String password = info.getString("pwd");
        if (domin == null || identifier == null || password == null)
            throw new WrongResponseException("Getting download url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        return DriverHelper_lanzou.getSingleShareFileDownloadUrl(configuration, domin + "/", identifier, password);
    }

    static @Nullable List<@NotNull FileSqlInformation> listAllDirectory(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId) throws IOException {
        final FormBody.Builder filesBuilder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId));
        final JSONObject json = DriverHelper_lanzou.task(configuration, 47, filesBuilder, null);
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
            throw new WrongResponseException("Listing directories.", json, ParametersMap.create()
                    .add("configuration", configuration).add("directoryId", directoryId));
        final List<FileSqlInformation> list = new ArrayList<>(filesInfos.size());
        for (final JSONObject info: filesInfos.toList(JSONObject.class)) {
            final String name = info.getString("name");
            final Long id = info.getLong("fol_id");
            if (name == null || id == null) continue;
            list.add(new FileSqlInformation(new FileLocation(configuration.getName(), id.longValue()),
                    directoryId, name, FileSqlInterface.FileSqlType.Directory, -1, null, null, "", null));
        }
        return list;
    }

    static @NotNull List<@NotNull FileSqlInformation> listAllFiles(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId) throws IOException, InterruptedException {
        final Collection<CountDownLatch> latches = new ArrayList<>();
        final Map<Integer, Triad.ImmutableTriad<String, Long, Headers>> filesMap = new ConcurrentHashMap<>();
        final Collection<CompletableFuture<?>> futures = new LinkedList<>();
        int total = 0;
        int page = 0;
        while (true) {
            final FormBody.Builder filesBuilder = new FormBody.Builder()
                    .add("folder_id", String.valueOf(directoryId))
                    .add("pg", String.valueOf(++page));
            final JSONObject files = DriverHelper_lanzou.task(configuration, 5, filesBuilder, 1);
            final Integer filesTotal = files.getInteger("info");
            final JSONArray filesInfos = files.getJSONArray("text");
            if (filesTotal == null || filesInfos == null)
                throw new WrongResponseException("Listing files.", files, ParametersMap.create()
                        .add("configuration", configuration).add("directoryId", directoryId).add("page", page));
            if (filesTotal.intValue() <= 0)
                break;
            final CountDownLatch filesLatch = new CountDownLatch(filesInfos.size());
            latches.add(filesLatch);
            for (final JSONObject info: filesInfos.toList(JSONObject.class)) {
                final int k = total++;
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        final String name = info.getString("name");
                        final Long id = info.getLong("id");
                        if (name == null || id == null) return;
                        final String url = DriverHelper_lanzou.getFileDownloadUrl(configuration, id.longValue());
                        if (url == null) return;
                        try (final Response response = DriverNetworkHelper.getWithParameters(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url, "HEAD"), DriverHelper_lanzou.headers, null).execute()) {
                            filesMap.put(k, Triad.ImmutableTriad.makeImmutableTriad(name, id, response.headers()));
                        }
                    } catch (final IOException exception) {
                        HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                    } finally {
                        filesLatch.countDown();
                    }
                }, WListServer.IOExecutors));
            }
        }
        try {
            for (final CountDownLatch latch: latches)
                latch.await();
        } catch (final InterruptedException exception) {
            futures.forEach(f -> f.cancel(false));
            throw exception;
        }
        final List<FileSqlInformation> list = new ArrayList<>(filesMap.size());
        for (int i = 0; i < total; ++i) {
            final Triad.ImmutableTriad<String, Long, Headers> headers = filesMap.get(i);
            if (headers == null)
                continue;
            final String sizeS = headers.getC().get("Content-Length");
            final String dataS = headers.getC().get("Last-Modified");
            if (sizeS == null || dataS == null)
                continue;
            final long size;
            final LocalDateTime time;
            try {
                size = Long.parseLong(sizeS);
                time = LocalDateTime.parse(dataS, DriverHelper_lanzou.dataTimeFormatter);
            } catch (final NumberFormatException | DateTimeParseException ignore) {
                continue;
            }
            list.add(new FileSqlInformation(new FileLocation(configuration.getName(), headers.getB().longValue()),
                    directoryId, headers.getA(), FileSqlInterface.FileSqlType.RegularFile, size, time, time, "", null));
        }
        return list;
    }

}
