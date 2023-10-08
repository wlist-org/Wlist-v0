package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.CheckRules.CheckRuleSet;
import com.xuxiaocheng.HeadLibs.CheckRules.StringCheckRules.ContainsCheckRule;
import com.xuxiaocheng.HeadLibs.CheckRules.StringCheckRules.LengthCheckRule;
import com.xuxiaocheng.HeadLibs.CheckRules.StringCheckRules.SuffixCheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.BiConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Ranges.IntRange;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Server.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BrowserUtil;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Response;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSpan;
import org.htmlunit.javascript.host.event.MouseEvent;
import org.htmlunit.util.Cookie;
import org.jetbrains.annotations.Contract;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"SpellCheckingInspection", "CallToSuspiciousStringMethod", "OverlyBroadThrowsClause"})
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
    protected @Nullable ZonedDateTime loginIfNot0() throws IOException, IllegalParametersException {
        final LanzouConfiguration configuration = this.getConfiguration();
        if (configuration.getToken() != null && configuration.getTokenExpire() != null && !MiscellaneousUtil.now().isAfter(configuration.getTokenExpire())) {
            this.headerWithToken = LanzouProvider.Headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getToken()).build();
            return configuration.getTokenExpire();
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
        final ZonedDateTime expires = ZonedDateTime.ofInstant(token.getExpires().toInstant(), ZoneOffset.UTC).minusSeconds(30);
        configuration.setNickname(configuration.getPassport());
        configuration.setVip(false); // TODO: nickname and vip.
        configuration.setUid(Long.parseLong(uid.getValue()));
        configuration.setToken(token.getValue());
        configuration.setTokenExpire(expires);
        configuration.markModified();
        LanzouProvider.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create().add("storage", configuration.getName()).add("passport", configuration.getPassport()).add("expires", expires));
        return expires;
    }


    protected static @NotNull String dumpOthers(final @NotNull HttpUrl url, final @Nullable String pwd) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("u", url.toString());
        if (pwd != null)
            map.put("p", pwd);
        return JSON.toJSONString(map);
    }

    protected static Pair.@NotNull ImmutablePair<@NotNull HttpUrl, @Nullable String> parseOthers(final @NotNull String others) {
        final JSONObject map = JSON.parseObject(others);
        return Pair.ImmutablePair.makeImmutablePair(Objects.requireNonNull(HttpUrl.parse(map.getString("u"))), map.getString("p"));
    }


    protected void task(final int type, final @NotNull UnaryOperator<FormBody.@NotNull Builder> request, final @NotNull Consumer<? super @NotNull Throwable> error, final @NotNull ConsumerE<? super @NotNull JSONObject> consumer) {
        final LanzouConfiguration configuration = this.getConfiguration();
        final FormBody body = request.apply(new FormBody.Builder().add("task", String.valueOf(type))).build();
        HttpNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), LanzouProvider.TaskURL, this.headerWithToken,
                Map.of("uid", String.valueOf(configuration.getUid())), body).enqueue(new Callback() {
            @Override
            public void onFailure(final @NotNull Call call, final @NotNull IOException e) {
                error.accept(e);
            }

            @Override
            public void onResponse(final @NotNull Call call, final @NotNull Response response) {
                try {
                    final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(response);
                    if (json.getIntValue("zt", 0) == 9) { // login not.
                        configuration.setToken(null);
                        configuration.setTokenExpire(null);
                        LanzouProvider.this.loginExpireTime.set(null);
                        LanzouProvider.this.loginIfNot0();
                        HttpNetworkHelper.postWithParametersAndBody(configuration.getHttpClient(), LanzouProvider.TaskURL, LanzouProvider.this.headerWithToken,
                                Map.of("uid", String.valueOf(configuration.getUid())), body).enqueue(new Callback() {
                            @Override
                            public void onFailure(final @NotNull Call call, final @NotNull IOException e) {
                                error.accept(e);
                            }

                            @Override
                            public void onResponse(final @NotNull Call call, final @NotNull Response response) {
                                try {
                                    consumer.accept(HttpNetworkHelper.extraJsonResponseBody(response));
                                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                                    error.accept(exception);
                                }
                            }
                        });
                        return;
                    }
                    consumer.accept(json);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    error.accept(exception);
                }
            }
        });
    }

    @Contract("null, _, _, _ -> fail")
    protected <T> void throwIfNull(final @Nullable T value, final @NotNull JSONObject json, final @NotNull String caller, final @NotNull UnaryOperator<@NotNull ParametersMap> parameters) throws WrongResponseException {
        if (value == null)
            throw new WrongResponseException(caller, json, parameters.apply(ParametersMap.create().add("configuration", this.getConfiguration()).add("caller", caller)));
    }

    protected void throwIfZt(final @NotNull JSONObject json, final @NotNull String caller, final @NotNull UnaryOperator<@NotNull ParametersMap> parameters) throws WrongResponseException {
        final Integer code = json.getInteger("zt");
        this.throwIfNull(code, json, caller, parameters);
        if (code.intValue() != 1) { // "1"
            String error = json.getString("info");
            if (error == null)
                error = json.getString("inf");
            if (error == null)
                error = json.getString("text");
            if (error == null)
                error = "";
            throw new IllegalResponseCodeException(code.intValue(), error, parameters.apply(ParametersMap.create()
                    .add("configuration", this.getConfiguration()).add("json", json).add("caller", caller)));
        }
    }


    protected void listAllDirectory(final long directoryId, final @NotNull Consumer<? super @NotNull Throwable> error, final @NotNull Consumer<? super @Nullable @Unmodifiable List<FileInformation>> consumer) {
        this.task(47, f -> f.add("folder_id", String.valueOf(directoryId)), error, json -> {
            if (directoryId != -1) {
                final JSONArray header = json.getJSONArray("info");
                this.throwIfNull(header, json, "listAllDirectory", p -> p.add("directoryId", directoryId));
                if (header.isEmpty()) {
                    consumer.accept(null);
                    return;
                }
            }
            final int code = json.getIntValue("zt", 1);
            if (code == 2) {
                consumer.accept(List.of());
                return;
            }
            this.throwIfZt(json, "listAllDirectory", p -> p.add("directoryId", directoryId));
            final JSONArray infos = json.getJSONArray("text");
            this.throwIfNull(infos, json, "listAllDirectory", p -> p.add("directoryId", directoryId));
            final List<FileInformation> directories = new ArrayList<>(infos.size());
            for (final JSONObject info: infos.toList(JSONObject.class)) {
                final String name = info.getString("name");
                final Long id = info.getLong("fol_id");
                try {
                    this.throwIfNull(name, json, "listAllDirectory", p -> p.add("directoryId", directoryId));
                    this.throwIfNull(id, json, "listAllDirectory", p -> p.add("directoryId", directoryId));
                } catch (final WrongResponseException exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                    continue;
                }
                directories.add(new FileInformation(id.longValue(), directoryId, name, true, -1, null, null, null));
            }
            consumer.accept(Collections.unmodifiableList(directories));
        });
    }

    protected void getFileShareUrl(final long fileId, final @NotNull Consumer<? super @NotNull Throwable> error, final @NotNull BiConsumerE<? super @Nullable HttpUrl, ? super @Nullable String> consumer) {
        this.task(22, f -> f.add("file_id", String.valueOf(fileId)), error, json -> {
            this.throwIfZt(json, "getFileShareUrl", p -> p.add("fileId", fileId));
            final JSONObject info = json.getJSONObject("info");
            this.throwIfNull(info, json, "getFileShareUrl", p -> p.add("fileId", fileId));
            final Integer onof = info.getInteger("onof");
            if (onof == null) {// not available.
                consumer.accept(null, null);
                return;
            }
            final boolean hasPwd = onof.intValue() == 1;
            final String domin = info.getString("is_newd");
            final String identifier = info.getString("f_id");
            final String taoc = Objects.requireNonNullElse(info.getString("taoc"), ""); // This field may have been discarded.
            this.throwIfNull(domin, json, "getFileShareUrl", p -> p.add("fileId", fileId));
            this.throwIfNull(identifier, json, "getFileShareUrl", p -> p.add("fileId", fileId));
            if (hasPwd) {
                final String pwd = info.getString("pwd");
                this.throwIfNull(pwd, json, "getFileShareUrl", p -> p.add("fileId", fileId));
                consumer.accept(Objects.requireNonNull(HttpUrl.parse(domin + '/' + identifier)), pwd + taoc);
            } else
                consumer.accept(Objects.requireNonNull(HttpUrl.parse(domin + '/' + identifier + taoc)), null);
        });
    }

    protected void listFilesInPage(final long directoryId, final @IntRange(minimum = 1) int page, final @NotNull Consumer<? super @NotNull Throwable> error, final @NotNull Consumer<? super Pair.@NotNull ImmutablePair<@NotNull@Unmodifiable Collection<FileInformation>, @NotNull Boolean>> consumer) {
        this.task(5, f -> f.add("folder_id", String.valueOf(directoryId)).add("pg", String.valueOf(page)), error, json -> {
            this.throwIfZt(json, "listFilesInPage", p -> p.add("directoryId", directoryId).add("page", page));
            final JSONArray infos = json.getJSONArray("text");
            this.throwIfNull(infos, json, "listFilesInPage", p -> p.add("directoryId", directoryId).add("page", page));
            final Boolean noMore = json.getInteger("info") == null ? null : json.getIntValue("info") == 0;
            if (infos.isEmpty()) {
                consumer.accept(Pair.ImmutablePair.makeImmutablePair(List.of(), noMore == null || noMore.booleanValue()));
                return;
            }
            final AtomicInteger counter = new AtomicInteger(infos.size());
            final Map<Long, FileInformation> map = new ConcurrentHashMap<>(infos.size());
            final Runnable finisher = () -> {
                if (counter.getAndDecrement() <= 1) {
                    consumer.accept(Pair.ImmutablePair.makeImmutablePair(map.values(), noMore == null ? map.isEmpty() : noMore.booleanValue()));
                }
            };
            for (final JSONObject info: infos.toList(JSONObject.class)) {
                final String name = info.getString("name");
                final Long id = info.getLong("id");
                try {
                    this.throwIfNull(name, json, "listFilesInPage", p -> p.add("directoryId", directoryId));
                    this.throwIfNull(id, json, "listFilesInPage", p -> p.add("directoryId", directoryId));
                } catch (final WrongResponseException exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                    finisher.run();
                    return;
                }
                this.getFileShareUrl(id.longValue(), e -> {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), e);
                    map.put(id, new FileInformation(id.longValue(), directoryId, name, false, 0, null, null, null));
                    finisher.run();
                }, (url, pwd) -> {
                    if (url == null) {
                        map.remove(id);
                        finisher.run();
                        return;
                    }
                    final String others = LanzouProvider.dumpOthers(url, pwd);
                    long size = 0;
                    ZonedDateTime time = null;
                    try {
                        final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
                        assert sharer != null;
                        final Pair.ImmutablePair<HttpUrl, Headers> downloadUrl = sharer.getSingleShareFileDownloadUrl(url, pwd);
                        if (downloadUrl == null) return;
                        final Pair<Long, ZonedDateTime> fixed = sharer.testRealSizeAndData(downloadUrl.getFirst(), downloadUrl.getSecond());
                        if (fixed == null) return;
                        size = fixed.getFirst().longValue();
                        time = fixed.getSecond();
                    } finally {
                        map.put(id, new FileInformation(id.longValue(), directoryId, name, false, size, time, time, others));
                        finisher.run();
                    }
                });
            }
        });
    }

    @Override
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final AtomicReference<List<FileInformation>> directories = new AtomicReference<>();
        this.listAllDirectory(directoryId, e -> {
            exception.set(e);
            countDownLatch.countDown();
        }, list -> {
            directories.set(list);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        MiscellaneousUtil.throwException(exception.get());
        if (directories.get() == null) return null;
        return ProviderUtil.wrapSuppliersInPages(page -> {
            if (page.intValue() == 0)
                return directories.get();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> e = new AtomicReference<>();
            final AtomicReference<Pair.ImmutablePair<Collection<FileInformation>, Boolean>> res = new AtomicReference<>();
            this.listFilesInPage(directoryId, page.intValue(), t -> {
                e.set(t);
                latch.countDown();
            }, p -> {
                res.set(p);
                latch.countDown();
            });
            latch.await();
            MiscellaneousUtil.throwException(e.get());
            if (res.get().getSecond().booleanValue() && res.get().getFirst().isEmpty())
                return null;
            return res.get().getFirst();
        }, this.getConfiguration().getRetry());
    }


    @Override
    protected boolean doesRequireUpdate(final @NotNull FileInformation information) {
        return !information.isDirectory() && (information.createTime() == null || information.others() == null);
    }

    @Override
    protected void update0(final @NotNull FileInformation oldInformation, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) throws Exception {
        assert this.doesRequireUpdate(oldInformation);
        this.loginIfNot();
        this.getFileShareUrl(oldInformation.id(), e -> consumer.accept(UnionPair.fail(e)), (url, pwd) -> {
            if (url == null) {
                consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
                return;
            }
            final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
            assert sharer != null;
            final Pair.ImmutablePair<HttpUrl, Headers> downloadUrl = sharer.getSingleShareFileDownloadUrl(url, pwd);
            if (downloadUrl == null) {
                consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
                return;
            }
            final Pair<Long, ZonedDateTime> fixed = sharer.testRealSizeAndData(downloadUrl.getFirst(), downloadUrl.getSecond());
            if (fixed == null) {
                consumer.accept(AbstractIdBaseProvider.UpdateNoRequired);
                return;
            }
            consumer.accept(UnionPair.ok(UnionPair.ok(new FileInformation(oldInformation.id(), oldInformation.parentId(), oldInformation.name(),
                    false, fixed.getFirst().longValue(), fixed.getSecond(), fixed.getSecond(), LanzouProvider.dumpOthers(url, pwd)))));
        });
    }


    @Override
    protected boolean doesSupportInfo(final boolean isDirectory) {
        return isDirectory;
    }

    @Override
    protected void info0(final long id, final boolean isDirectory, @NotNull final Consumer<? super UnionPair<Optional<FileInformation>, Throwable>> consumer) {
        assert this.doesSupportInfo(isDirectory);
        this.task(47, f -> f.add("folder_id", String.valueOf(id)), e -> consumer.accept(UnionPair.fail(e)), json -> {
            final Integer code = json.getInteger("zt");
            this.throwIfNull(code == null ? null : code.intValue() != 1 && code.intValue() != 2 ? null : code, json, "info0", p -> p.add("id", id).add("isDirectory", isDirectory));
            final JSONArray infos = json.getJSONArray("info");
            this.throwIfNull(infos, json, "info0", p -> p.add("id", id).add("isDirectory", isDirectory));
            if (infos.isEmpty()) {
                consumer.accept(AbstractIdBaseProvider.InfoNotExist);
                return;
            }
            final Long parent = infos.getJSONObject(Math.max(infos.size() - 2, 0)).getLong("folderid");
            final String name = infos.getJSONObject(infos.size() - 1).getString("name");
            this.throwIfNull(parent, json, "info0", p -> p.add("id", id).add("isDirectory", isDirectory));
            this.throwIfNull(name, json, "info0", p -> p.add("id", id).add("isDirectory", isDirectory));
            consumer.accept(UnionPair.ok(Optional.of(new FileInformation(id, parent.longValue(), name, isDirectory, -1, null, null, null))));
        });
    }


    @Override
    protected boolean doesSupportTrashNotEmptyDirectory() {
        return false;
    }

    @Override
    protected void trash0(final @NotNull FileInformation information, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) {
        this.task(information.isDirectory() ? 3 : 6, f -> f.add(information.isDirectory() ? "folder_id" : "file_id", String.valueOf(information.id())),
                e -> consumer.accept(UnionPair.fail(e)), json -> {
            this.throwIfZt(json, "trash0", p -> p.add("information", information));
            consumer.accept(AbstractIdBaseProvider.TrashSuccess);
        });
    }


    @Override
    protected boolean doesRequireLoginDownloading(final @NotNull FileInformation information) {
        return information.others() == null;
    }

    @Override
    protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) throws Exception {
        assert !information.isDirectory();
        final RunnableE onDelete = () -> {
            this.manager.getInstance().deleteFile(information.id(), null);
            BroadcastManager.onFileTrash(this.getLocation(information.id()), false);
            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(information.id()), false))));
        };
        final BiConsumerE<HttpUrl, String> downloader = (url, pwd) -> {
            if (url == null) {
                onDelete.run();
                return;
            }
            final LanzouSharer sharer = (LanzouSharer) StorageManager.getSharer(this.getConfiguration().getName());
            assert sharer != null;
            final ConsumerE<Pair.ImmutablePair<HttpUrl, Headers>> consume = downloadUrl -> {
                if (downloadUrl == null) {
                    onDelete.run();
                    return;
                }
                consumer.accept(UnionPair.ok(UnionPair.ok(DownloadRequirements.tryGetDownloadFromUrl(this.getConfiguration().getFileClient(),
                        downloadUrl.getFirst(), downloadUrl.getSecond(), information.size(), LanzouProvider.Headers.newBuilder(), from, to, null))));
            };
            try {
                consume.accept(sharer.getSingleShareFileDownloadUrl(url, pwd));
            } catch (final IllegalParametersException ignore) { // Wrong password.
                this.getFileShareUrl(information.id(), e -> consumer.accept(UnionPair.fail(e)), (u, p) -> {
                    if (u == null)
                        onDelete.run();
                    else
                        consume.accept(sharer.getSingleShareFileDownloadUrl(u, p));
                });
            }
        };
        if (information.others() == null) {
            this.getFileShareUrl(information.id(), e -> consumer.accept(UnionPair.fail(e)), downloader);
        } else {
            final Pair.ImmutablePair<HttpUrl, String> map = LanzouProvider.parseOthers(information.others());
            downloader.accept(map.getFirst(), map.getSecond());
        }
    }


    protected static final @NotNull Pair.ImmutablePair<@NotNull String, @NotNull String> RetryBracketPair = Pair.ImmutablePair.makeImmutablePair("\uFF08", "\uFF09");
    @Override
    protected Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> retryBracketPair() {
        return LanzouProvider.RetryBracketPair;
    }


    protected static final @NotNull CheckRule<@NotNull String> DirectoryNameChecker = new CheckRuleSet<>(new LengthCheckRule(1, 100),
            new ContainsCheckRule(Set.of("/", "\\", "*", "|", "#", "$", "%", "^", "(", ")", "?", ":", "'", "\"", "`", "=", "+"), false)
    );

    @Override
    protected @NotNull CheckRule<@NotNull String> directoryNameChecker() {
        return LanzouProvider.DirectoryNameChecker;
    }

    @Override
    protected void createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) {
        this.task(2, f -> f.add("parent_id", String.valueOf(parentId)).add("folder_name", directoryName), e -> consumer.accept(UnionPair.fail(e)), json -> {
            final ZonedDateTime now = MiscellaneousUtil.now();
            try {
                this.throwIfZt(json, "createDirectory0", p -> p.add("parentId", parentId).add("directoryName", directoryName));
            } catch (final IllegalResponseCodeException exception) {
                if (exception.getCode() == 0) {
                    consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(this.getLocation(parentId), directoryName, exception.getMeaning()))));
                    return;
                }
                throw exception;
            }
            final Long id = json.getLong("text");
            this.throwIfNull(id, json, "createDirectory0", p -> p.add("parentId", parentId).add("directoryName", directoryName));
            consumer.accept(UnionPair.ok(UnionPair.ok(new FileInformation(id.longValue(), parentId, directoryName, true, 0, now, now, null))));
        });
    }


    protected static final @NotNull CheckRule<@NotNull String> FileNameChecker = new CheckRuleSet<>(new SuffixCheckRule(Stream.of(
            "doc","docx","zip","rar","apk","ipa","txt","exe","7z","e","z","ct","ke","cetrainer","db","tar","pdf","w3x","epub","mobi","azw","azw3","osk", "osz",
                    "xpa","cpk","lua","jar","dmg","ppt","pptx","xls","xlsx","mp3","iso","img","gho","ttf","ttc","txf","dwg","bat","imazingapp","dll","crx","xapk",
                    "conf","deb","rp","rpm","rplib","mobileconfig","appimage","lolgezi","flac","cad","hwt","accdb","ce","xmind","enc","bds","bdi","ssf","it","pkg","cfg"
            ).map(s -> "." + s).collect(Collectors.toSet()), true), new LengthCheckRule(1, 100),
            new ContainsCheckRule(Set.of("/", "\\", "*", "|", "#", "$", "%", "^", "(", ")", "?", ":", "'", "\"", "`", "=", "+"), false)
    );

    @Override
    protected @NotNull CheckRule<@NotNull String> fileNameChecker() {
        return LanzouProvider.FileNameChecker;
    }

    @Override
    protected void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) {
        consumer.accept(UnionPair.ok(UnionPair.ok(new UploadRequirements(List.of(), ignore -> {
            final AtomicReference<FileInformation> information = new AtomicReference<>(null);
            final Pair.ImmutablePair<List<UploadRequirements.OrderedConsumers>, Runnable> pair = UploadRequirements.splitUploadBuffer((content, listener) -> {
                final MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("task", "1")
                        .addFormDataPart("ve", "2")
                        .addFormDataPart("folder_id_bb_n", String.valueOf(parentId))
                        .addFormDataPart("upload_file", filename, HttpNetworkHelper.createOctetStreamRequestBody(content, listener))
                        .build();
                final JSONObject json = HttpNetworkHelper.extraJsonResponseBody(HttpNetworkHelper.postWithBody(this.getConfiguration().getHttpClient(),
                        LanzouProvider.UploadURL, this.headerWithToken, body).execute());
                final ZonedDateTime now = MiscellaneousUtil.now();
                this.throwIfZt(json, "uploadFile0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size));
                final JSONArray infos = json.getJSONArray("text");
                this.throwIfNull(infos == null || infos.isEmpty() ? null : infos, json, "uploadFile0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size));
                this.throwIfNull(infos.size() > 1 ? null : infos, json, "uploadFile0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size).add("infos", infos));
                final JSONObject file = infos.getJSONObject(0);
                this.throwIfNull(file, json, "uploadFile0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size));
                final Long id = file.getLong("id");
                this.throwIfNull(id, json, "uploadFile0", p -> p.add("parentId", parentId).add("filename", filename).add("size", size));
                information.set(new FileInformation(id.longValue(), parentId, filename, false, size, now, now, null));
            }, 0, Math.toIntExact(size));
            return new UploadRequirements.UploadMethods(pair.getFirst(), c -> c.accept(UnionPair.ok(Optional.ofNullable(information.get()))), pair.getSecond());
        }, RunnableE.EmptyRunnable))));
    }


    @Override
    protected boolean doesSupportCopyDirectly(final @NotNull FileInformation information, final long parentId) {
        return false;
    }

    @Override
    protected void copyDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        consumer.accept(AbstractIdBaseProvider.CopyNotSupport);
    }


    @Override
    protected boolean doesSupportMoveDirectly(final @NotNull FileInformation information, final long parentId) {
        return !information.isDirectory();
    }

    @Override
    protected void moveDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        assert this.doesSupportMoveDirectly(information, parentId);
        this.task(20, f -> f.add("folder_id", String.valueOf(parentId)).add("file_id", String.valueOf(information.id())), e -> consumer.accept(UnionPair.fail(e)), json -> {
            final ZonedDateTime now = MiscellaneousUtil.now();
            try {
                this.throwIfZt(json, "moveDirectly0", p -> p.add("information", information));
            } catch (final IllegalResponseCodeException exception) {
                if (exception.getCode() == 0) {
                    consumer.accept(switch (exception.getMeaning()) {
                        case "\u79FB\u52A8\u5931\u8D25\uFF0C\u6587\u4EF6\u5DF2\u5728\u6B64\u76EE\u5F55" -> UnionPair.ok(Optional.of(UnionPair.ok(new FileInformation(
                                information.id(), parentId, information.name(), false, information.size(), information.createTime(), information.updateTime(), information.others()
                        ))));
                        case "\u6CA1\u6709\u627E\u5230\u6587\u4EF6" -> UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(information.id()), false))));
                        case "\u6CA1\u6709\u627E\u5230\u6587\u4EF6\u5939" -> UnionPair.ok(Optional.of(UnionPair.fail(FailureReason.byNoSuchFile(this.getLocation(parentId), true))));
                        default -> UnionPair.fail(exception);
                    });
                    return;
                }
                consumer.accept(UnionPair.fail(exception));
                return;
            }
            consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(new FileInformation(
                    information.id(), parentId, information.name(), false, information.size(), information.createTime(), now, information.others()
            )))));
        });
    }


    @Override
    protected boolean doesSupportRenameDirectly(final @NotNull FileInformation information) {
        return this.getConfiguration().isVip() || information.isDirectory();
    }

    @Override
    protected void renameDirectly0(final @NotNull FileInformation information, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        assert this.doesSupportRenameDirectly(information);
        if (information.isDirectory())
            this.task(4, f -> f.add("folder_id", String.valueOf(information.id())).add("folder_name", name), e -> consumer.accept(UnionPair.fail(e)), json -> {
                final ZonedDateTime now = MiscellaneousUtil.now();
                this.throwIfZt(json, "renameDirectly0", p -> p.add("information", information).add("name", name));
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(new FileInformation(
                        information.id(), information.parentId(), name, true, information.size(), information.createTime(), now, information.others()
                )))));
            });
        else
            this.task(46, f -> f.add("file_id", String.valueOf(information.id())).add("file_name", name).add("type", "2"), e -> consumer.accept(UnionPair.fail(e)), json -> {
                final ZonedDateTime now = MiscellaneousUtil.now();
                this.throwIfZt(json, "renameDirectly0", p -> p.add("information", information).add("name", name));
                final String real = json.getString("info");
                this.throwIfNull(real, json, "renameDirectly0", p -> p.add("information", information).add("name", name));
                consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(new FileInformation(
                        information.id(), information.parentId(), real, true, information.size(), information.createTime(), now, information.others()
                )))));
            });
    }


    @Override
    public @NotNull String toString() {
        return "LanzouProvider{" +
                "headerWithToken=" + this.headerWithToken +
                ", super=" + super.toString() +
                '}';
    }
}
