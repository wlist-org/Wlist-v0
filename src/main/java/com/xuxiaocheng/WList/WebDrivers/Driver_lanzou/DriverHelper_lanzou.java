package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.OptionalNullable;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.JavaScriptUtil;
import io.netty.buffer.ByteBuf;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.rmi.ServerException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("SpellCheckingInspection")
final class DriverHelper_lanzou {
    private DriverHelper_lanzou() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("DriverLogger/lanzou", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true, HMergedStreams.getFileOutputStreamNoException(null));

    static final @NotNull DateTimeFormatter dataTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    static final @NotNull Headers headers = new Headers.Builder().set("referer", "https://up.woozooo.com/u").set("accept-language", "zh-CN")
            .set("user-agent", DriverNetworkHelper.defaultWebAgent).set("cache-control", "no-cache").build();

    private static final @NotNull @Unmodifiable Set<@NotNull String> allowSuffix = Set.of("doc","docx","zip","rar","apk","ipa","txt","exe","7z","e","z","ct","ke",
            "cetrainer","db","tar","pdf","w3x","epub","mobi","azw","azw3","osk","osz","xpa","cpk","lua","jar","dmg","ppt","pptx","xls","xlsx","mp3","iso","img",
            "gho","ttf","ttc","txf","dwg","bat","imazingapp","dll","crx","xapk","conf","deb","rp","rpm","rplib","mobileconfig","appimage","lolgezi","flac","cad",
            "hwt","accdb","ce","xmind","enc","bds","bdi","ssf","it","pkg","cfg");
    static final @NotNull Predicate<@NotNull String> filenamePredication = s -> {
        final int index = s.lastIndexOf('.');
        if (index < 0) return false;
        final String suffix = s.substring(index + 1);
        for (final String a: DriverHelper_lanzou.allowSuffix)
            if (suffix.equalsIgnoreCase(a))
                return true;
        return false;
    };

    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/mlogin.php", "POST"); // TODO use account.php
//    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> InformationURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/mydisk.php", "GET");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> TaskURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/doupload.php", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UploadURL = Pair.ImmutablePair.makeImmutablePair("https://up.woozooo.com/html5up.php", "POST");

    static @NotNull String requestHtml(final @NotNull OkHttpClient httpClient, final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url) throws IOException {
        try (final ResponseBody body = DriverNetworkHelper.extraResponseBody(DriverNetworkHelper.getWithParameters(httpClient, url, DriverHelper_lanzou.headers, null).execute())) {
            return DriverUtil.removeHtmlComments(body.string());
        }
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

    private static final @NotNull Pattern srcPattern = Pattern.compile("src=\"(/fn?[^\"]+)");
    @SuppressWarnings("unchecked")
    static @Nullable String getSingleShareFileDownloadUrl(final @NotNull DriverConfiguration_lanzou configuration, final @NotNull String domin, final @NotNull String identifier, final @Nullable String pwd) throws IOException, IllegalParametersException {
        final String sharePage = DriverHelper_lanzou.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin + '/' + identifier, "GET"));
        if (sharePage.contains("\u6587\u4EF6\u53D6\u6D88\u5206\u4EAB\u4E86") || sharePage.contains("\u6587\u4EF6\u5730\u5740\u9519\u8BEF"))
            return null;
        final ParametersMap parametersMap = ParametersMap.create().add("configuration", configuration).add("domin", domin).add("identifier", identifier);
        final List<String> javaScript;
        if (sharePage.contains("<iframe")) {
            final Matcher srcMatcher = DriverHelper_lanzou.srcPattern.matcher(sharePage);
            if (!srcMatcher.find())
                throw new WrongResponseException("No src matched.", sharePage, parametersMap);
            final String src = srcMatcher.group(1);
            final String loadingPage = DriverHelper_lanzou.requestHtml(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin + src, "GET"));
            javaScript = DriverUtil.findScripts(loadingPage);
        } else {
            parametersMap.add("pwd", pwd);
            if (pwd == null)
                throw new IllegalParametersException("Require password.", ParametersMap.create().add("domin", domin).add("identifier", identifier));
            final List<String> scripts = DriverUtil.findScripts(sharePage);
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
                throw new ServerException("Null ajax.");
            assert "post".equals(ajax.get("type"));
            ajaxUrl = (String) ajax.get("url");
            ajaxData = (Map<String, Object>) ajax.get("data");
        } catch (final JavaScriptUtil.ScriptException exception) {
            throw new IOException("Failed to run share page java scripts." + parametersMap, exception);
        }
        final FormBody.Builder builder = new FormBody.Builder();
        for (final Map.Entry<String, Object> entry: ajaxData.entrySet())
            builder.add(entry.getKey(), entry.getValue().toString());
        final JSONObject json = DriverHelper_lanzou.requestJson(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(domin + ajaxUrl, "POST"), builder);
        final int code = json.getIntValue("zt", -1);
        if (code != 1)
            throw new IllegalResponseCodeException(code, json.getString("inf"), parametersMap.add("json", json));
        final String dom = json.getString("dom");
        final String para = json.getString("url");
        if (dom == null || para == null)
            throw new WrongResponseException("Getting single shared file download url.", json, parametersMap);
        final String displayUrl = dom + "/file/" + para;
//        try (final Response response = DriverNetworkHelper.getWithParameters(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(displayUrl, "GET"),
//                DriverHelper_lanzou.headers.newBuilder().add("Range", "bytes=0-0").build(), null).execute()) {
//
//        }
        return displayUrl;
    }

    static @Nullable String getFileDownloadUrl(final @NotNull DriverConfiguration_lanzou configuration, final long fileId) throws IOException {
        // TODO: get by FileSalInformation.others
        final FormBody.Builder sharerBuilder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId));
        final JSONObject json = DriverHelper_lanzou.task(configuration, 22, sharerBuilder, 1);
        final JSONObject info = json.getJSONObject("info");
        if (info == null)
            throw new WrongResponseException("Getting download url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        final String domin = info.getString("is_newd");
        final String identifier = info.getString("f_id");
        final Boolean hasPwd = info.getInteger("onof") == null ? null : info.getIntValue("onof") == 1;
        final String pwd = info.getString("pwd");
        if (domin == null || identifier == null || hasPwd == null || (hasPwd.booleanValue() && pwd == null))
            throw new WrongResponseException("Getting download url.", json, ParametersMap.create().add("configuration", configuration).add("fileId", fileId));
        try {
            return DriverHelper_lanzou.getSingleShareFileDownloadUrl(configuration, domin, identifier, hasPwd.booleanValue() ? pwd : null);
        } catch (final IllegalParametersException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull LocalDateTime> testRealSizeAndData(final @NotNull DriverConfiguration_lanzou configuration, final @NotNull String downloadUrl) throws IOException {
        final Headers headers = DriverNetworkHelper.getRealHeader(configuration.getFileClient(), downloadUrl, DriverHelper_lanzou.headers, null);
        final String sizeS = headers.get("Content-Length");
        final String dataS = headers.get("Last-Modified");
        if (sizeS == null || dataS == null)
            return null;
        final long size;
        final LocalDateTime time;
        try {
            size = Long.parseLong(sizeS);
            time = LocalDateTime.parse(dataS, DriverHelper_lanzou.dataTimeFormatter);
        } catch (final NumberFormatException | DateTimeParseException ignore) {
            return null;
        }
        return Pair.ImmutablePair.makeImmutablePair(size, time);
    }

    static @Nullable @UnmodifiableView List<@NotNull FileSqlInformation> listAllDirectory(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId) throws IOException {
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

    static @NotNull @UnmodifiableView Set<@NotNull FileSqlInformation> listFilesInPage(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final int page) throws IOException, InterruptedException {
        final AtomicBoolean interrupttedFlag = new AtomicBoolean(false);
        final FormBody.Builder builder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId))
                .add("pg", String.valueOf(page + 1));
        final JSONObject files = DriverHelper_lanzou.task(configuration, 5, builder, 1);
        final JSONArray infos = files.getJSONArray("text");
        if (infos == null)
            throw new WrongResponseException("Listing files.", files, ParametersMap.create()
                    .add("configuration", configuration).add("directoryId", directoryId).add("page", page));
        if (infos.isEmpty())
            return Set.of();
        final Set<FileSqlInformation> set = ConcurrentHashMap.newKeySet();
        final AtomicReference<Throwable> throwable = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            final String name = info.getString("name");
            final Long id = info.getLong("id");
            if (name == null || id == null) continue;
            CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                try {
                    final String url = DriverHelper_lanzou.getFileDownloadUrl(configuration, id.longValue());
                    if (url == null || interrupttedFlag.get()) return;
                    final Pair<Long, LocalDateTime> fixed = DriverHelper_lanzou.testRealSizeAndData(configuration, url);
                    if (fixed == null || interrupttedFlag.get()) return;
                    set.add(new FileSqlInformation(new FileLocation(configuration.getName(), id.longValue()),
                            directoryId, name, FileSqlInterface.FileSqlType.RegularFile, fixed.getFirst().longValue(),
                            fixed.getSecond(), fixed.getSecond(), "", null));
                } finally {
                    latch.countDown();
                }
            }), WListServer.IOExecutors).exceptionally(t -> {
                interrupttedFlag.set(true);
                if (!throwable.compareAndSet(null, t))
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), t);
                return null;
            });
        }
        try {
            latch.await();
        } catch (final InterruptedException exception) {
            interrupttedFlag.set(true);
            throw exception;
        }
        if (throwable.get() != null)
            throw new IOException(HExceptionWrapper.unwrapException(throwable.get(), IOException.class));
        return set;
    }

    static void trashFile(final @NotNull DriverConfiguration_lanzou configuration, final long fileId) throws IOException {
        final FormBody.Builder builder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId));
        final JSONObject json = DriverHelper_lanzou.task(configuration, 6, builder, 1);
        final String message = json.getString("info");
        if (!"\u5DF2\u5220\u9664".equals(message))
            throw new WrongResponseException("Trashing file.", message, ParametersMap.create()
                    .add("configuration", configuration).add("fileId", fileId).add("json", json));
    }

    static void trashDirectories(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId) throws IOException {
        final FormBody.Builder builder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId));
        final JSONObject json = DriverHelper_lanzou.task(configuration, 3, builder, 1);
        final String message = json.getString("info");
        if (!"\u5220\u9664\u6210\u529F".equals(message))
            throw new WrongResponseException("Trashing directory.", message, ParametersMap.create()
                    .add("configuration", configuration).add("directoryId", directoryId).add("json", json));
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_lanzou configuration, final String name, final long parentId) throws IOException {
        final FormBody.Builder builder = new FormBody.Builder()
                .add("parent_id", String.valueOf(parentId))
                .add("folder_name", name);
        final JSONObject json;
        final LocalDateTime now;
        try {
            json = DriverHelper_lanzou.task(configuration, 2, builder, 1);
            now = LocalDateTime.now();
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == 0 && "\u540D\u79F0\u542B\u6709\u7279\u6B8A\u5B57\u7B26".equals(exception.getMeaning()))
                return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), name));
            throw exception;
        }
        final String message = json.getString("info");
        final Long id = json.getLong("text");
        if (id == null || !"\u521B\u5EFA\u6210\u529F".equals(message))
            throw new WrongResponseException("Creating directories.", message, ParametersMap.create()
                    .add("configuration", configuration).add("name", name).add("parentId", parentId).add("json", json));
        return UnionPair.ok(new FileSqlInformation(new FileLocation(configuration.getName(), id.longValue()), parentId, name,
                FileSqlInterface.FileSqlType.EmptyDirectory, 0, now, now, "", null));
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> uploadFile(final @NotNull DriverConfiguration_lanzou configuration, final String name, final long parentId, final @NotNull ByteBuf content, final @NotNull String md5) throws IOException {
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
        final int size = content.readableBytes();
        if (size > configuration.getWebSide().getMaxSizePerFile())
            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, configuration.getWebSide().getMaxSizePerFile(),  new FileLocation(configuration.getName(), parentId), name));
        DriverManager_lanzou.ensureLoggedIn(configuration);
        final MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("task", "1")
                .addFormDataPart("ve", "2")
                .addFormDataPart("folder_id_bb_n", String.valueOf(parentId))
                .addFormDataPart("upload_file", name, DriverNetworkHelper.createOctetStreamRequestBody(content))
                .build();
        final JSONObject json = DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.postWithBody(configuration.getHttpClient(), DriverHelper_lanzou.UploadURL,
                DriverHelper_lanzou.headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getCacheSide().getIdentifier() + "; ").build(), body).execute());
        final LocalDateTime now = LocalDateTime.now();
        final int code = json.getIntValue("zt", -1);
        if (code != 1)
            throw new IllegalResponseCodeException(code, json.getString("info") == null ? json.getString("text") : json.getString("info"),
                    ParametersMap.create().add("configuration", configuration).add("requireZt", 1).add("json", json));
        final String message = json.getString("info");
        final JSONArray array = json.getJSONArray("text");
        if (!"\u4E0A\u4F20\u6210\u529F".equals(message) || array == null || array.isEmpty())
            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
                    .add("name", name).add("parentId", parentId).add("json", json));
        final JSONObject info = array.getJSONObject(0);
        if (info == null || info.getLong("id") == null)
            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
                    .add("name", name).add("parentId", parentId).add("json", json));
        return UnionPair.ok(new FileSqlInformation(new FileLocation(configuration.getName(), info.getLongValue("id")),
                parentId, name, FileSqlInterface.FileSqlType.RegularFile, size, now, now, md5, null));
    }

    static @Nullable UnionPair<@NotNull LocalDateTime, @NotNull FailureReason> moveFile(final @NotNull DriverConfiguration_lanzou configuration, final long fileId, final long parentId) throws IOException {
        final FormBody.Builder builder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId))
                .add("folder_id", String.valueOf(parentId));
        final JSONObject json;
        final LocalDateTime now;
        try {
            if (parentId == 0) throw new IllegalResponseCodeException(0, "\u6CA1\u6709\u627E\u5230\u6587\u4EF6", ParametersMap.create());
            json = DriverHelper_lanzou.task(configuration, 20, builder, 1);
            now = LocalDateTime.now();
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == 0) {
                if ("\u79FB\u52A8\u5931\u8D25\uFF0C\u6587\u4EF6\u5DF2\u5728\u6B64\u76EE\u5F55".equals(exception.getMeaning()))
                    return null;
                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6".equals(exception.getMeaning()))
                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (source).", new FileLocation(configuration.getName(), fileId)));
                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6\u5939".equals(exception.getMeaning()))
                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (target).", new FileLocation(configuration.getName(), parentId)));
            }
            throw exception;
        }
        final String message = json.getString("info");
        if (!"\u79FB\u52A8\u6210\u529F".equals(message))
            throw new WrongResponseException("Moving.", message, ParametersMap.create()
                    .add("configuration", configuration).add("fileId", fileId).add("parentId", parentId).add("json", json));
        return UnionPair.ok(now);
    }

    static @Nullable OptionalNullable<FailureReason> renameFile(final @NotNull DriverConfiguration_lanzou configuration, final long fileId, final @NotNull String name) throws IOException {
        if (!DriverHelper_lanzou.filenamePredication.test(name))
            return OptionalNullable.of(FailureReason.byInvalidName("Renaming.", new FileLocation(configuration.getName(), fileId), name));
        final FormBody.Builder builder = new FormBody.Builder()
                .add("file_id", String.valueOf(fileId))
                .add("file_name", name)
                .add("type", "2");
        final JSONObject json;
        try {
            json = DriverHelper_lanzou.task(configuration, 46, builder, 1);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == 0 && "\u6B64\u529F\u80FD\u4EC5\u4F1A\u5458\u4F7F\u7528\uFF0C\u8BF7\u5148\u5F00\u901A\u4F1A\u5458".equals(exception.getMeaning()))
                return null;
            throw exception;
        }
        // TODO: Unchecked.
HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Driver lanzou record: Running in vip mode (rename): ", json);
        return OptionalNullable.empty();
    }
}
