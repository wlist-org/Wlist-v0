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
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Server.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BrowserUtil;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WList.Server.WListServer;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSpan;
import org.htmlunit.javascript.host.event.MouseEvent;
import org.htmlunit.util.Cookie;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings({"SpellCheckingInspection", "CallToSuspiciousStringMethod"})
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

    protected static final @NotNull Headers Headers = new Headers.Builder().set("user-agent", HttpNetworkHelper.DefaultWebAgent).set("referer", "https://up.woozooo.com/u").set("accept-language", "zh-CN").build();
    protected @NotNull Headers headerWithToken = LanzouProvider.Headers;

    @Override
    protected void loginIfNot() throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        if (configuration.getToken() != null && configuration.getTokenExpire() != null && !MiscellaneousUtil.now().isAfter(configuration.getTokenExpire())) {
            this.headerWithToken = LanzouProvider.Headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getToken()).build();
            return;
        }
        { // Quicker response.
            if (configuration.getPassport().isEmpty() || !ProviderUtil.PhoneNumberPattern.matcher(configuration.getPassport()).matches())
                throw new IllegalParametersException(I18NUtil.get("server.provider.invalid_passport"), ParametersMap.create().add("configuration", configuration));
            if (configuration.getPassword().length() < 6 || 20 < configuration.getPassword().length())
                throw new IllegalParametersException(I18NUtil.get("server.provider.invalid_password"), ParametersMap.create().add("configuration", configuration));
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
            boolean flag = true;
            for (final Iterator<String> iterator = Arrays.stream(result.split("\n")).iterator(); iterator.hasNext();) {
                if ("\u63D0\u793A\u4FE1\u606F".equals(iterator.next())) {
                    if (iterator.hasNext() && iterator.next().contains("\u767B\u5F55\u6210\u529F"))
                        flag = false;
                    break;
                }
            }
            if (flag)
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
        this.headerWithToken = LanzouProvider.Headers.newBuilder().set("cookie", "phpdisk_info=" + token.getValue()).build();
        configuration.setNickname(configuration.getPassport());
        configuration.setVip(false); // TODO: nickname and vip.
        configuration.setUid(Long.parseLong(uid.getValue()));
        configuration.setToken(token.getValue());
        configuration.setTokenExpire(ZonedDateTime.ofInstant(token.getExpires().toInstant(), ZoneOffset.UTC));
        configuration.markModified();
        LanzouProvider.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create().add("storage", configuration.getName()).add("passport", configuration.getPassport()));
    }

    protected static @NotNull String dumpOthers(final @NotNull HttpUrl domin, final @NotNull String identifier, final @Nullable String pwd) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("d", domin.toString());
        map.put("i", identifier);
        if (pwd != null)
            map.put("p", pwd);
        return JSON.toJSONString(map);
    }

    protected static Triad.@NotNull ImmutableTriad<@NotNull HttpUrl, @NotNull String, @Nullable String> parseOthers(final @NotNull String others) {
        final JSONObject map = JSON.parseObject(others);
        return Triad.ImmutableTriad.makeImmutableTriad(Objects.requireNonNull(HttpUrl.parse(map.getString("d"))), map.getString("i"), map.getString("p"));
    }

    protected @NotNull JSONObject task(final int type, final @NotNull Consumer<? super FormBody.@NotNull Builder> request, final @Nullable Integer zt, final boolean loginFlag) throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("uid", String.valueOf(configuration.getUid()));
        final FormBody.Builder builder = new FormBody.Builder();
        builder.add("task", String.valueOf(type));
        request.accept(builder);
        final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), LanzouProvider.TaskURL, this.headerWithToken, parameters, builder.build()).execute());
        if (!loginFlag && json.getIntValue("zt", 0) == 9) { // login not.
            this.getConfiguration().setToken(null);
            this.getConfiguration().setTokenExpire(null);
            this.loginIfNot();
            return this.task(type, request, zt, true);
        }
        if (zt != null) {
            final Integer code = json.getInteger("zt");
            if (code == null || code.intValue() != zt.intValue())
                throw new IllegalResponseCodeException(code == null ? -1 : code.intValue(), json.getString("info") == null ? json.getString("text") : json.getString("info"),
                        ParametersMap.create().add("configuration", configuration).add("requireZt", zt).add("json", json));
        }
        return json;
    }

    protected @Nullable @Unmodifiable List<@NotNull FileInformation> listAllDirectory(final long directoryId) throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        final JSONObject json = this.task(47, f -> f.add("folder_id", String.valueOf(directoryId)), null, false);
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

    protected @Nullable Triad.ImmutableTriad<@NotNull HttpUrl, @NotNull String, @Nullable String> getFileShareUrl(final long fileId) throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        final JSONObject json = this.task(22, f -> f.add("file_id", String.valueOf(fileId)), 1, false);
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

    protected @NotNull @Unmodifiable Set<@NotNull FileInformation> listFilesInPage(final long directoryId, final int page) throws IOException, InterruptedException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        final JSONObject files = this.task(5, f -> f.add("folder_id", String.valueOf(directoryId))
                .add("pg", String.valueOf(page)), 1, false);
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
                    others = LanzouProvider.dumpOthers(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
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
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws IOException, IllegalParametersException {
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
    protected boolean doesRequireUpdate(@NotNull final FileInformation information) {
        return !information.isDirectory() && (information.createTime() == null || information.others() == null);
    }

    @Override
    protected void update0(@NotNull final FileInformation oldInformation, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) throws IOException, IllegalParametersException {
        final HttpUrl url;
        final String identifier;
        final String pwd;
        String others;
        if (oldInformation.others() == null) {
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(oldInformation.id());
            if (shareUrl == null) {
                consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
                return;
            }
            url = shareUrl.getA();
            identifier = shareUrl.getB();
            pwd = shareUrl.getC();
            others = LanzouProvider.dumpOthers(url, identifier, pwd);
        } else {
            others = oldInformation.others();
            final Triad.ImmutableTriad<HttpUrl, String, String> map = LanzouProvider.parseOthers(others);
            url = map.getA();
            identifier = map.getB();
            pwd = map.getC();
        }
        final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
        assert sharer != null;
        Pair.ImmutablePair<HttpUrl, Headers> downloadUrl;
        try {
            downloadUrl = sharer.getSingleShareFileDownloadUrl(url, identifier, pwd);
        } catch (final IllegalParametersException exception) { // Wrong password.
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(oldInformation.id());
            if (shareUrl == null) {
                consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
                return;
            }
            others = LanzouProvider.dumpOthers(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
            downloadUrl = sharer.getSingleShareFileDownloadUrl(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
        }
        if (downloadUrl == null) {
            consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
            return;
        }
        final Pair<Long, ZonedDateTime> fixed = sharer.testRealSizeAndData(downloadUrl.getFirst(), downloadUrl.getSecond());
        if (fixed != null) {
            consumer.accept(UnionPair.ok(UnionPair.ok(new FileInformation(oldInformation.id(), oldInformation.parentId(), oldInformation.name(), false, fixed.getFirst().longValue(), fixed.getSecond(), fixed.getSecond(), others))));
            return;
        }
        consumer.accept(UnionPair.ok(UnionPair.ok(new FileInformation(oldInformation.id(), oldInformation.parentId(), oldInformation.name(), false, oldInformation.size(), oldInformation.createTime(), oldInformation.updateTime(), others))));
    }

    @Override
    protected void trash0(final @NotNull FileInformation information, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws IOException, IllegalParametersException {
        if (information.isDirectory()) {
            final JSONObject json = this.task(3, f -> f.add("folder_id", String.valueOf(information.id())), 1, false);
            final String message = json.getString("info");
            if (!"\u5220\u9664\u6210\u529F".equals(message))
                LanzouProvider.logger.log(HLogLevel.WARN, new WrongResponseException("Trashing directory.", message, ParametersMap.create()
                        .add("configuration", this.getConfiguration()).add("information", information).add("json", json)));
        } else {
            final JSONObject json = this.task(6, f -> f.add("file_id", String.valueOf(information.id())), 1, false);
            final String message = json.getString("info");
            if (!"\u5DF2\u5220\u9664".equals(message))
                LanzouProvider.logger.log(HLogLevel.WARN, new WrongResponseException("Trashing file.", message, ParametersMap.create()
                        .add("configuration", this.getConfiguration()).add("information", information).add("json", json)));
        }
        consumer.accept(AbstractIdBaseProvider.TrashSuccess); // TODO: failed on trash not empty directory.
    }

    @Override
    protected boolean doesRequireLoginDownloading(final @NotNull FileInformation information) {
        return information.others() == null;
    }

    @Override
    protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) throws IOException, IllegalParametersException {
        final HttpUrl url;
        final String identifier;
        final String pwd;
        if (information.others() == null) {
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(information.id());
            if (shareUrl == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(information.id()), false))));
                return;
            }
            url = shareUrl.getA();
            identifier = shareUrl.getB();
            pwd = shareUrl.getC();
        } else {
            final Triad.ImmutableTriad<HttpUrl, String, String> map = LanzouProvider.parseOthers(information.others());
            url = map.getA();
            identifier = map.getB();
            pwd = map.getC();
        }
        final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
        assert sharer != null;
        Pair.ImmutablePair<HttpUrl, Headers> downloadUrl;
        try {
            downloadUrl = sharer.getSingleShareFileDownloadUrl(url, identifier, pwd);
        } catch (final IllegalParametersException ignore) { // Wrong password.
            final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(information.id());
            if (shareUrl == null) {
                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(information.id()), false))));
                return;
            }
            downloadUrl = sharer.getSingleShareFileDownloadUrl(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
        }
        if (downloadUrl == null) {
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(information.id()), false))));
            return;
        }
        consumer.accept(UnionPair.ok(UnionPair.ok(DownloadRequirements.tryGetDownloadFromUrl(this.getConfiguration().getFileClient(),
                downloadUrl.getFirst(), downloadUrl.getSecond(), information.size(), LanzouProvider.Headers.newBuilder(), from, to, null))));
    }

//    protected static final @NotNull Pair.ImmutablePair<@NotNull String, @NotNull String> RetryBracketPair = Pair.ImmutablePair.makeImmutablePair("\uFF08", "\uFF09");
//    @Override
//    protected Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> retryBracketPair() {
//        return LanzouProvider.RetryBracketPair;
//    }
//
//    protected static final @NotNull CheckRule<@NotNull String> DirectoryNameChecker = new CheckRuleSet<>(new LengthCheckRule(1, 100),
//            new ContainsCheckRule(Set.of("/", "\\", "*", "|", "#", "$", "%", "^", "(", ")", "?", ":", "'", "\"", "`", "=", "+"), false)
//    );
//
//    @Override
//    protected @NotNull CheckRule<@NotNull String> directoryNameChecker() {
//        return LanzouProvider.DirectoryNameChecker;
//    }
//
//    @Override
//    protected void createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws IOException, IllegalParametersException {
//        final JSONObject json;
//        final ZonedDateTime now;
//        try {
//            json = this.task(2, f -> f.add("parent_id", String.valueOf(parentId)).add("folder_name", directoryName), 1, false);
//            now = MiscellaneousUtil.now();
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0) {
//                consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, directoryName, exception.getMeaning()))));
//                return;
//            }
//            throw exception;
//        }
//        final Long id = json.getLong("text");
//        final String message = json.getString("info");
//        if (id == null)
//            throw new WrongResponseException("Creating directories.", message, ParametersMap.create().add("configuration", this.getConfiguration())
//                    .add("directoryName", directoryName).add("parentId", parentId).add("json", json));
//        consumer.accept(UnionPair.ok(UnionPair.ok(new FileInformation(id.longValue(), parentId, directoryName, true, 0, now, now, null))));
//    }
//
//    protected static final @NotNull CheckRule<@NotNull String> FileNameChecker = new CheckRuleSet<>(new SuffixCheckRule(Stream.of(
//            "doc","docx","zip","rar","apk","ipa","txt","exe","7z","e","z","ct","ke","cetrainer","db","tar","pdf","w3x","epub","mobi","azw","azw3","osk", "osz",
//                    "xpa","cpk","lua","jar","dmg","ppt","pptx","xls","xlsx","mp3","iso","img","gho","ttf","ttc","txf","dwg","bat","imazingapp","dll","crx","xapk",
//                    "conf","deb","rp","rpm","rplib","mobileconfig","appimage","lolgezi","flac","cad","hwt","accdb","ce","xmind","enc","bds","bdi","ssf","it","pkg","cfg"
//            ).map(s -> "." + s).collect(Collectors.toSet()), true), new LengthCheckRule(1, 100),
//            new ContainsCheckRule(Set.of("/", "\\", "*", "|", "#", "$", "%", "^", "(", ")", "?", ":", "'", "\"", "`", "=", "+"), false)
//    );
//
//    @Override
//    protected @NotNull CheckRule<@NotNull String> fileNameChecker() {
//        return LanzouProvider.FileNameChecker;
//    }
//
//    @Override
//    protected void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) {
//        consumer.accept(UnionPair.ok(UnionPair.ok(new UploadRequirements(List.of(), ignore -> {
//            final AtomicReference<FileInformation> information = new AtomicReference<>(null);
//            final Pair.ImmutablePair<List<UploadRequirements.OrderedConsumers>, Runnable> pair = UploadRequirements.splitUploadBuffer((content, listener) -> {
//                final MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                        .addFormDataPart("task", "1")
//                        .addFormDataPart("ve", "2")
//                        .addFormDataPart("folder_id_bb_n", String.valueOf(parentId))
//                        .addFormDataPart("upload_file", filename, HttpNetworkHelper.createOctetStreamRequestBody(content, listener))
//                        .build();
//                final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithBody(this.getConfiguration().getHttpClient(),
//                        LanzouProvider.UploadURL, this.headerWithToken, body).execute());
//                final ZonedDateTime now = MiscellaneousUtil.now();
//                final int code = json.getIntValue("zt", -1);
//                if (code != 1)
//                    throw new IllegalResponseCodeException(code, json.getString("info") == null ? json.getString("text") : json.getString("info"),
//                            ParametersMap.create().add("configuration", this.getConfiguration()).add("requireZt", 1).add("json", json));
//                final String message = json.getString("info");
//                final JSONArray array = json.getJSONArray("text");
//                if (!"\u4E0A\u4F20\u6210\u529F".equals(message) || array == null || array.isEmpty())
//                    throw new WrongResponseException("Uploading file.", message, ParametersMap.create().add("configuration", this.getConfiguration())
//                            .add("parentId", parentId).add("filename", filename).add("json", json));
//                final JSONObject info = array.getJSONObject(0);
//                if (info == null || info.getLong("id") == null)
//                    throw new WrongResponseException("Uploading file.", message, ParametersMap.create().add("configuration", this.getConfiguration())
//                            .add("parentId", parentId).add("filename", filename).add("json", json));
//                final long id = info.getLongValue("id");
//                information.set(new FileInformation(id, parentId, filename, false, size, now, now, null));
//                final Triad.ImmutableTriad<HttpUrl, String, String> shareUrl = this.getFileShareUrl(id);
//                if (shareUrl != null) {
//                    final String others = LanzouProvider.dumpOthers(shareUrl.getA(), shareUrl.getB(), shareUrl.getC());
//                    information.set(new FileInformation(id, parentId, filename, false, size, now, now, others));
//                }
//            }, 0, Math.toIntExact(size));
//            return new UploadRequirements.UploadMethods(pair.getFirst(), c -> c.accept(UnionPair.ok(Optional.ofNullable(information.get()))), pair.getSecond());
//        }, RunnableE.EmptyRunnable))));
//    }
//
//    @Override
//    protected boolean isSupportedCopyFileDirectly(final @NotNull FileInformation information, final long parentId) {
//        return false;
//    }
//
//    @Override
//    protected void copyFileDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) {
//        consumer.accept(ProviderInterface.CopyNotSupported);
//    }
//
//    @Override
//    protected boolean isSupportedMoveDirectly(final @NotNull FileInformation information, final long parentId) {
//        return !information.isDirectory();
//    }
//
//    @Override
//    protected void moveDirectly0(final @NotNull FileInformation information, final long parentId, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) {
//        consumer.accept(ProviderInterface.MoveNotSupported); // TODO
//    }

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
