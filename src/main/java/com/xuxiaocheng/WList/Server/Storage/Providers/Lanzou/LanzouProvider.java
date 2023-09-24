package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.javascript.host.event.MouseEvent;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Server.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.Util.BrowserUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("SpellCheckingInspection")
public class LanzouProvider extends AbstractIdBaseProvider<LanzouConfiguration> {
    @Override
    public @NotNull StorageTypes<LanzouConfiguration> getType() {
        return StorageTypes.Lanzou;
    }

    protected static final @NotNull HLog logger = HLog.create("DriverLogger/lanzou");

    protected static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> LoginURL =
            Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/account.php")), "POST");
    protected static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> TaskURL =
        Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/doupload.php")), "POST");
    protected static final Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @NotNull String> UploadURL =
            Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse("https://up.woozooo.com/html5up.php")), "POST");
    protected static final @NotNull URL CookieUrl = LanzouProvider.LoginURL.getFirst().url();

    protected static final @NotNull Headers Headers = new Headers.Builder().set("referer", "https://up.woozooo.com/u").set("accept-language", "zh-CN")
            .set("user-agent", HttpNetworkHelper.defaultWebAgent).set("cache-control", "no-cache").build();
    protected @NotNull Headers headerWithToken = LanzouProvider.Headers;

    @Override
    protected void loginIfNot() throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        if (configuration.getToken() != null && configuration.getTokenExpire() != null && !MiscellaneousUtil.now().isAfter(configuration.getTokenExpire()))
            return;
        { // Quicker response.
            if (configuration.getPassport().isEmpty() || !ProviderUtil.PhoneNumberPattern.matcher(configuration.getPassport()).matches())
                throw new IllegalParametersException("Invalid passport.", ParametersMap.create().add("configuration", configuration));
            if (configuration.getPassword().length() < 6 || 20 < configuration.getPassword().length())
                throw new IllegalParametersException("Invalid password.", ParametersMap.create().add("configuration", configuration));
        }
        final Set<Cookie> cookies;
        try (final WebClient client = BrowserUtil.newWebClient()) {
            final HtmlPage page = client.getPage("https://up.woozooo.com/account.php?action=login");
            BrowserUtil.waitJavaScriptCompleted(client);
            final HtmlSpan slide = page.getHtmlElementById("nc_1_n1z");
            slide.mouseDown();
            slide.mouseMove(false, false, false, MouseEvent.BUTTON_RIGHT);
            page.<HtmlInput>getElementByName("username").setValue(configuration.getPassport());
            page.<HtmlInput>getElementByName("password").setValue(configuration.getPassword());
            final HtmlPage res = page.getHtmlElementById("s3").click();
            final String result = res.asNormalizedText(); // ((DomNode) res.getByXPath("//p").get(0)).getVisibleText()
            final Optional<String> message = Arrays.stream(result.split("\n")).dropWhile(Predicate.not("\u63D0\u793A\u4FE1\u606F"::equals)).skip(1).findFirst();
            if (message.isEmpty() || !message.get().contains("\u767B\u5F55\u6210\u529F"))
                throw new IllegalParametersException("Failed to login.", ParametersMap.create().add("configuration", configuration).add("page", result));
            cookies = client.getCookies(LanzouProvider.CookieUrl);
        }
        Cookie token = null, uid = null;
        for (final Cookie c: cookies) {
            if ("phpdisk_info".equalsIgnoreCase(c.getName()))
                token = c;
            if ("ylogin".equalsIgnoreCase(c.getName()))
                uid = c;
        }
        if (token == null || uid == null)
            throw new IllegalParametersException("No token/uid receive.", ParametersMap.create().add("configuration", configuration).add("cookies", cookies));
        this.headerWithToken = LanzouProvider.Headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getToken()).build();
        configuration.setNickname(configuration.getPassport());
        configuration.setVip(false); // TODO: nickname and vip.
        configuration.setUid(Long.parseLong(uid.getValue()));
        configuration.setToken(token.getValue());
        configuration.setTokenExpire(ZonedDateTime.ofInstant(token.getExpires().toInstant(), ZoneOffset.UTC));
        configuration.markModified();
        LanzouProvider.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create().add("storage", configuration.getName()).add("passport", configuration.getPassport()));
    }

    protected @NotNull JSONObject task(final int type, final FormBody.@NotNull Builder request, final @Nullable Integer zt) throws IOException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", String.valueOf(configuration.getUid()));
        request.add("task", String.valueOf(type));
        final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), LanzouProvider.TaskURL, this.headerWithToken, parameters, request.build()).execute());
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

    protected @NotNull @Unmodifiable Set<@NotNull FileInformation> listFilesInPage(final long directoryId, final int page) throws IOException, InterruptedException {
        final LanzouConfiguration configuration = this.configuration.getInstance();
        final FormBody.Builder builder = new FormBody.Builder()
                .add("folder_id", String.valueOf(directoryId))
                .add("pg", String.valueOf(page));
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
                String others = null;
                boolean flag = true;
                try {
                    final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(id.longValue());
                    if (shareUrl == null) return;
                    others = JSON.toJSONString(Map.of(
                            "domin", shareUrl.getA().toString(),
                            "id", shareUrl.getB(),
                            "pwd", Objects.requireNonNullElse(shareUrl.getC(), "")
                    ));
                    final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
                    assert sharer != null;
                    final Pair.ImmutablePair<HttpUrl, Headers> url = sharer.getSingleShareFileDownloadUrl(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
                    if (url == null) return;
                    final Pair<Long, ZonedDateTime> fixed = sharer.testRealSizeAndData(url.getFirst(), url.getSecond());
                    if (fixed == null) return;
                    set.add(new FileInformation(id.longValue(), directoryId, name, false, fixed.getFirst().longValue(), fixed.getSecond(), fixed.getSecond(), others));
                    flag = false;
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

    @Override
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws IOException {
        final List<FileInformation> directories = this.listAllDirectory(directoryId);
        if (directories == null) return null;
        return ProviderUtil.wrapSuppliersInPages(page -> {
            if (page.intValue() == 0)
                return directories;
            final Collection<FileInformation> list = this.listFilesInPage(directoryId, page.intValue());
            if (list.isEmpty())
                return null;
            return list;
        }, this.getConfiguration().getRetry());
    }

    @Override
    protected @NotNull UnionPair<FileInformation, Boolean> update0(@NotNull final FileInformation oldInformation) throws IOException {
        if (oldInformation.createTime() != null || oldInformation.isDirectory())
            return AbstractIdBaseProvider.UpdateNoRequired;
        final HttpUrl url;
        final String id, pwd, others;
        if (oldInformation.others() == null) {
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(oldInformation.id());
            if (shareUrl == null) return UnionPair.fail(Boolean.FALSE);
            url = shareUrl.getA();
            id = shareUrl.getB();
            pwd = shareUrl.getC();
            others = JSON.toJSONString(Map.of("domin", url.toString(), "id", id, "pwd", Objects.requireNonNullElse(pwd, "")));
        } else {
            others = oldInformation.others();
            final JSONObject json = JSON.parseObject(others);
            url = Objects.requireNonNull(HttpUrl.parse(json.getString("domin")));
            id = Objects.requireNonNull(json.getString("id"));
            final String p = Objects.requireNonNull(json.getString("pwd"));
            pwd = p.isEmpty() ? null : p;
        }
        final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
        assert sharer != null;
        try {
            final Pair.ImmutablePair<HttpUrl, Headers> downloadUrl = sharer.getSingleShareFileDownloadUrl(url, id, pwd);
            if (downloadUrl == null)
                return AbstractIdBaseProvider.UpdateNotExisted;
            final Pair<Long, ZonedDateTime> fixed = sharer.testRealSizeAndData(downloadUrl.getFirst(), downloadUrl.getSecond());
            if (fixed != null)
                return UnionPair.ok(new FileInformation(oldInformation.id(), oldInformation.parentId(), oldInformation.name(), false, fixed.getFirst().longValue(), fixed.getSecond(), fixed.getSecond(), others));
        } catch (final IllegalParametersException exception) {
            LanzouProvider.logger.log(HLogLevel.MISTAKE, exception);
        }
        return UnionPair.ok(new FileInformation(oldInformation.id(), oldInformation.parentId(), oldInformation.name(), false, oldInformation.size(), oldInformation.createTime(), oldInformation.updateTime(), others));
    }

    @Override
    protected boolean isSupportedInfo() {
        return false;
    }

    @Override
    protected boolean isSupportedNotEmptyDirectoryTrash() {
        return false;
    }

    @Override
    protected void trash0(final @NotNull FileInformation information) throws IOException {
        if (information.isDirectory()) {
            final FormBody.Builder builder = new FormBody.Builder()
                    .add("folder_id", String.valueOf(information.id()));
            final JSONObject json = this.task(3, builder, 1);
            final String message = json.getString("info");
            if (!"\u5220\u9664\u6210\u529F".equals(message))
                LanzouProvider.logger.log(HLogLevel.WARN, new WrongResponseException("Trashing directory.", message, ParametersMap.create()
                        .add("configuration", this.configuration.getInstance()).add("information", information).add("json", json)));
        } else {
            final FormBody.Builder builder = new FormBody.Builder()
                    .add("file_id", String.valueOf(information.id()));
            final JSONObject json = this.task(6, builder, 1);
            final String message = json.getString("info");
            if (!"\u5DF2\u5220\u9664".equals(message))
                LanzouProvider.logger.log(HLogLevel.WARN, new WrongResponseException("Trashing file.", message, ParametersMap.create()
                        .add("configuration", this.configuration.getInstance()).add("information", information).add("json", json)));
        }
    }

    @Override
    protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) throws Exception {
        final HttpUrl url;
        final String id, pwd;
        if (information.others() == null) {
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(information.id());
            if (shareUrl == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(location))));
                return;
            }
            url = shareUrl.getA();
            id = shareUrl.getB();
            pwd = shareUrl.getC();
        } else {
            final JSONObject json = JSON.parseObject(information.others());
            url = Objects.requireNonNull(HttpUrl.parse(json.getString("domin")));
            id = Objects.requireNonNull(json.getString("id"));
            final String p = Objects.requireNonNull(json.getString("pwd"));
            pwd = p.isEmpty() ? null : p;
        }
        final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
        assert sharer != null;
        final Pair.ImmutablePair<HttpUrl, Headers> downloadUrl = sharer.getSingleShareFileDownloadUrl(url, id, pwd);
        if (downloadUrl == null) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(location))));
            return;
        }
        consumer.accept(UnionPair.ok(UnionPair.ok(DownloadRequirements.tryGetDownloadFromUrl(this.getConfiguration().getFileClient(),
                downloadUrl.getFirst(), downloadUrl.getSecond(), information.size(), LanzouProvider.Headers.newBuilder(), from, to, null))));
    }

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

//    static @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull LanzouConfiguration configuration, final String name, final long parentId) throws IOException {
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("parent_id", String.valueOf(parentId))
//                .add("folder_name", name);
//        final JSONObject json;
//        final ZonedDateTime now;
//        try {
//            json = DriverHelper_lanzou.task(configuration, 2, builder, 1);
//            now = MiscellaneousUtil.now();
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0 && "\u540D\u79F0\u542B\u6709\u7279\u6B8A\u5B57\u7B26".equals(exception.getMeaning()))
//                return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), name));
//            throw exception;
//        }
//        final String message = json.getString("info");
//        final Long id = json.getLong("text");
//        if (id == null || !"\u521B\u5EFA\u6210\u529F".equals(message))
//            throw new WrongResponseException("Creating directories.", message, ParametersMap.create()
//                    .add("configuration", configuration).add("name", name).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(new FileInformation(new FileLocation(configuration.getName(), id.longValue()), parentId, name,
//                FileSqlInterface.FileSqlType.EmptyDirectory, 0, now, now, "", null));
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> uploadFile(final @NotNull LanzouConfiguration configuration, final String name, final long parentId, final @NotNull ByteBuf content, final @NotNull String md5) throws IOException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
//        final int size = content.readableBytes();
//        if (size > configuration.getMaxSizePerFile())
//            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, configuration.getMaxSizePerFile(), new FileLocation(configuration.getName(), parentId), name));
//        DriverManager_lanzou.ensureLoggedIn(configuration);
//        final MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                .addFormDataPart("task", "1")
//                .addFormDataPart("ve", "2")
//                .addFormDataPart("folder_id_bb_n", String.valueOf(parentId))
//                .addFormDataPart("upload_file", name, DriverNetworkHelper.createOctetStreamRequestBody(content))
//                .build();
//        final JSONObject json = DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.postWithBody(configuration.getHttpClient(), DriverHelper_lanzou.UploadURL,
//                DriverHelper_lanzou.headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getIdentifier() + "; ").build(), body).execute());
//        final ZonedDateTime now = MiscellaneousUtil.now();
//        final int code = json.getIntValue("zt", -1);
//        if (code != 1)
//            throw new IllegalResponseCodeException(code, json.getString("info") == null ? json.getString("text") : json.getString("info"),
//                    ParametersMap.create().add("configuration", configuration).add("requireZt", 1).add("json", json));
//        final String message = json.getString("info");
//        final JSONArray array = json.getJSONArray("text");
//        if (!"\u4E0A\u4F20\u6210\u529F".equals(message) || array == null || array.isEmpty())
//            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
//                    .add("name", name).add("parentId", parentId).add("json", json));
//        final JSONObject info = array.getJSONObject(0);
//        if (info == null || info.getLong("id") == null)
//            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
//                    .add("name", name).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(new FileInformation(new FileLocation(configuration.getName(), info.getLongValue("id")),
//                parentId, name, FileSqlInterface.FileSqlType.RegularFile, size, now, now, md5, null));
//    }
//
//    static @Nullable UnionPair<ZonedDateTime, FailureReason> moveFile(final @NotNull LanzouConfiguration configuration, final long fileId, final long parentId) throws IOException {
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("file_id", String.valueOf(fileId))
//                .add("folder_id", String.valueOf(parentId));
//        final JSONObject json;
//        final ZonedDateTime now;
//        try {
//            if (parentId == 0) throw new IllegalResponseCodeException(0, "\u6CA1\u6709\u627E\u5230\u6587\u4EF6", ParametersMap.create());
//            json = DriverHelper_lanzou.task(configuration, 20, builder, 1);
//            now = MiscellaneousUtil.now();
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0) {
//                if ("\u79FB\u52A8\u5931\u8D25\uFF0C\u6587\u4EF6\u5DF2\u5728\u6B64\u76EE\u5F55".equals(exception.getMeaning()))
//                    return null;
//                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6".equals(exception.getMeaning()))
//                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (source).", new FileLocation(configuration.getName(), fileId)));
//                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6\u5939".equals(exception.getMeaning()))
//                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (target).", new FileLocation(configuration.getName(), parentId)));
//            }
//            throw exception;
//        }
//        final String message = json.getString("info");
//        if (!"\u79FB\u52A8\u6210\u529F".equals(message))
//            throw new WrongResponseException("Moving.", message, ParametersMap.create()
//                    .add("configuration", configuration).add("fileId", fileId).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(now);
//    }
//
//    static @Nullable OptionalNullable<FailureReason> renameFile(final @NotNull LanzouConfiguration configuration, final long fileId, final @NotNull String name) throws IOException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return OptionalNullable.of(FailureReason.byInvalidName("Renaming.", new FileLocation(configuration.getName(), fileId), name));
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("file_id", String.valueOf(fileId))
//                .add("file_name", name)
//                .add("type", "2");
//        final JSONObject json;
//        try {
//            json = DriverHelper_lanzou.task(configuration, 46, builder, 1);
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0 && "\u6B64\u529F\u80FD\u4EC5\u4F1A\u5458\u4F7F\u7528\uFF0C\u8BF7\u5148\u5F00\u901A\u4F1A\u5458".equals(exception.getMeaning()))
//                return null;
//            throw exception;
//        }
//        // TODO: Unchecked.
//HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Driver lanzou record: Running in vip mode (rename): ", json);
//        return OptionalNullable.empty();
//    }

    @Override
    public @NotNull String toString() {
        return "LanzouProvider{" +
                "headerWithToken=" + this.headerWithToken +
                ", super=" + super.toString() +
                '}';
    }
}
