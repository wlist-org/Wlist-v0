package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.Annotations.Range.IntRange;
import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.DriverTokenExpiredException;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Utils.AndroidSupport;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("DriverLogger/123pan", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, true, HMergedStream.getFileOutputStreamNoException(null));

    static final @NotNull OkHttpClient httpClient = DriverNetworkHelper.newHttpClientBuilder()
            .addNetworkInterceptor(new DriverNetworkHelper.FrequencyControlInterceptor(5, 100)).build();
    static final @NotNull OkHttpClient fileClient = DriverNetworkHelper.newHttpClientBuilder()
            .writeTimeout(3, TimeUnit.MINUTES).build();
    static final @NotNull String agent = DriverNetworkHelper.defaultWebAgent + " " + DriverNetworkHelper.defaultAgent;

    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> LogoutURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/logout", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> FilesInfoURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/info", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> SingleFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/download_info", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> S3AuthPartURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_upload_object/auth", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> S3ParePartsURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_repare_upload_parts_batch", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UploadCompleteURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_complete/v2", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> TrashFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/trash", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> RenameFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/rename", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> MoveFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/mod_pid", "POST");

    static final int InvalidArgumentResponseCode = 400;
    static final int TokenExpireResponseCode = 401;
    static final int ServerErrorResponseCode = 401;
    static final int FileAlreadyExistResponseCode = 5060;
    static final int ExceedSizeLimitSingleFileResponseCode = 5054;
    static final int InvalidFilenameResponseCode = 5064;

    @Contract(pure = true) static int getDuplicatePolicy(final Options.@NotNull DuplicatePolicy policy) {
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
        };
    }
    @Contract(pure = true) static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
        };
    }
    @Contract(pure = true) static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
        };
    }

    static final @NotNull Pattern PhoneNumberPattern = Pattern.compile("^1[0-9]{10}$");
    static final @NotNull Pattern MailAddressPattern = DriverUtil.mailAddressPattern;// Pattern.compile("^\\w+@[a-zA-Z0-9]{2,10}(?:\\.[a-z]{2,3}){1,3}$");
    static final @NotNull Predicate<String> filenamePredication = s -> {
        if (s.length() >= 128)
            return false;
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };
    static final int UploadPartSize = 16 << 20; // const

    private static final int[] ConstantArray = new int[256]; static {
        for (int i = 0; i < 256; ++i) {
            int k = i;
            for (int j = 0; j < 8; ++j)
                k = (1 & k) == 1 ? 0xedb88320 ^ k >>> 0x1 : k >>> 0x1;
            DriverHelper_123pan.ConstantArray[i] = k;
        }
    }
    private static @NotNull String getVerifyString(final @NotNull String source) {
        int k = -1;
        for (final int ch: source.toCharArray())
            k = k >>> 0x8 ^ DriverHelper_123pan.ConstantArray[0xff & (k ^ ch)];
        return Integer.toUnsignedString(~k);
    }

    private static final char[] ObscureArray = new char[] {'a', 'd', 'e', 'f', 'g', 'h', 'l', 'm', 'y', 'i', 'j', 'n', 'o', 'p', 'k', 'q', 'r', 's', 't', 'u', 'b', 'c', 'v', 'w', 's', 'z'};
    @SuppressWarnings("SameParameterValue")
    private static Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> generateDyKey(final @NotNull String url, final @NotNull String platform, final int appVersion) {
        final int random = HRandomHelper.DefaultSecureRandom.nextInt(0x989680);
        final LocalDateTime now = LocalDateTime.now();
        final long time = now.toEpochSecond(ZoneOffset.ofHours(8));
        // TODO getServerTime ;serverTime = response['data']['data']['timestamp']
        // time = serverTime && getAbsMinuteDuration(time, serverTime) >= 20 ? serverTime : time,
        // ;getAbsMinuteDuration: (a, b) => Math.abs(a - b) / 60;
        final StringBuilder builder = new StringBuilder();
        for (final char ch: now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")).toCharArray())
            //noinspection CharUsedInArithmeticContext
            builder.append(DriverHelper_123pan.ObscureArray[ch - '0']);
        final String timeVerifier = DriverHelper_123pan.getVerifyString(builder.toString());
        final String mergeVerifier = DriverHelper_123pan.getVerifyString(String.format("%d|%d|%s|%s|%d|%s", time, random, url, platform, appVersion, timeVerifier));
        return Pair.ImmutablePair.makeImmutablePair(timeVerifier, String.format("%d-%d-%s", time, random, mergeVerifier));
    }

    static @NotNull JSONObject sendRequestReceiveExtractedData(final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable DriverConfiguration_123Pan configuration, final @Nullable Map<@NotNull String, @NotNull Object> body, final boolean loginFlag) throws IllegalParametersException, IOException {
        final Headers.Builder builder = new Headers.Builder();
        if (configuration != null)
            builder.add("authorization", "Bearer " + configuration.getCacheSide().getToken());
        builder.add("user-agent", DriverHelper_123pan.agent);
        builder.add("platform", "web").add("app-version", "3");
        builder.set("cache-control", "no-cache");
        final Pair.ImmutablePair<String, String> authKey = DriverHelper_123pan.generateDyKey(new URL(url.getFirst()).getPath(), "web", 3);
        final Pair.ImmutablePair<String, String> realUrl = Pair.ImmutablePair.makeImmutablePair(String.format("%s?%s=%s", url.getFirst(), authKey.getFirst(), authKey.getSecond()), url.getSecond());
        JSONObject json = DriverNetworkHelper.sendRequestReceiveJson(DriverHelper_123pan.httpClient, realUrl, builder.build(), body);
        if (json.getIntValue("code", -1) == DriverHelper_123pan.TokenExpireResponseCode && !loginFlag && configuration != null) {
            DriverHelper_123pan.forceGetToken(configuration);
            json = DriverNetworkHelper.sendRequestReceiveJson(DriverHelper_123pan.httpClient, realUrl,
                    builder.set("authorization", "Bearer " + configuration.getCacheSide().getToken()).build(), body);
        }
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != (loginFlag ? 200 : 0) || !(loginFlag ? "success" : "ok").equals(message))
            throw new IllegalResponseCodeException(code, message, ParametersMap.create()
                    .add("realUrl", realUrl).add("configuration", configuration).add("body", body).add("json", json));
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Missing response data.", json, ParametersMap.create()
                    .add("url", url).add("configuration", configuration).add("body", body).add("json", json));
        return data;
    }

    // User

    private static void handleLoginData(final DriverConfiguration_123Pan.@NotNull CacheSide configurationCache, final @NotNull JSONObject data) throws WrongResponseException {
        final String token = data.getString("token");
        if (token == null)
            throw new WrongResponseException("No token in response.", data, ParametersMap.create().add("configurationCache", configurationCache));
        configurationCache.setToken(token);
        final String expire = data.getString("expire");
        if (expire == null)
            throw new WrongResponseException("No expire time in response.", data, ParametersMap.create().add("configurationCache", configurationCache));
        configurationCache.setTokenExpire(LocalDateTime.parse(expire, DateTimeFormatter.ISO_ZONED_DATE_TIME));
        final Long refresh = data.getLong("refresh_token_expire_time");
        if (refresh == null)
            throw new WrongResponseException("No refresh time in response.", data, ParametersMap.create().add("configurationCache", configurationCache));
        configurationCache.setRefreshExpire(LocalDateTime.ofEpochSecond(refresh.longValue(), 0, ZoneOffset.ofHours(8)));
        configurationCache.setModified(true);
    }

    /**
     * Log in.
     * @param configuration
     * <p> {@literal GET configuration.webSide.loginType: }Either 1 (phone number) or 2 (email address).
     * <p> {@literal GET configuration.webSide.passport: }Login passport.
     * <p> {@literal GET configuration.webSide.password: }Login password.
     * <p> {@literal SET configuration.cacheSide.token: }Token.
     * <p> {@literal SET configuration.cacheSide.tokenExpire: }Token expire time.
     * <p> {@literal SET configuration.cacheSide.refreshExpire: }Refresh token expire time.
     */
    private static void login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final int loginType = configuration.getWebSide().getLoginType();
        final boolean isPhone = switch (loginType) {
            case 1 -> true; case 2 -> false;
            default -> throw new IllegalParametersException("Unsupported login type.", ParametersMap.create().add("type", loginType));
        };
        if (!(isPhone ? DriverHelper_123pan.PhoneNumberPattern : DriverHelper_123pan.MailAddressPattern).matcher(configuration.getWebSide().getPassport()).matches())
            throw new DriverTokenExpiredException(isPhone ? DriverUtil.InvalidPhoneNumber : DriverUtil.InvalidMailAddress, ParametersMap.create().add("passport", configuration.getWebSide().getPassport()));
                // isPhone ? "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801" : "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u53f7";
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("type", loginType);
        request.put(isPhone ? "passport" : "mail", configuration.getWebSide().getPassport());
        request.put("password", configuration.getWebSide().getPassword());
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.LoginURL, null, request, true);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.TokenExpireResponseCode)
                throw new DriverTokenExpiredException(exception.getMeaning(), ParametersMap.create().add("passport", configuration.getWebSide().getPassport()));
            throw exception;
        }
        DriverHelper_123pan.handleLoginData(configuration.getCacheSide(), data);
        DriverHelper_123pan.logger.log(HLogLevel.LESS, "Logged in.", ParametersMap.create()
                .add("driver", configuration.getName()).add("passport", configuration.getWebSide().getPassport()));
    }

    /**
     * Refresh token.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Refresh token.
     * <p> {@literal SET configuration.cacheSide.token: }New token.
     * <p> {@literal SET configuration.cacheSide.tokenExpire: }New token expire time.
     * <p> {@literal SET configuration.cacheSide.refreshExpire: }New refresh token expire time.
     * @return Notice!
     * <p> {@code true}: Failure.
     * <p> {@code false}: Success.
     */
    private static boolean refreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        if (configuration.getCacheSide().getToken() == null) // Quick response.
            return true;
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.RefreshTokenURL, configuration, null, true);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.TokenExpireResponseCode)
                return true;
            throw exception;
        }
        DriverHelper_123pan.handleLoginData(configuration.getCacheSide(), data);
        DriverHelper_123pan.logger.log(HLogLevel.LESS, "Refreshed token.", ParametersMap.create()
                        .add("driver", configuration.getName()).add("passport", configuration.getWebSide().getPassport()));
        return false;
    }

    static void forceGetToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now().minusMinutes(3);
        synchronized (configuration.getCacheSide()) {
            if (configuration.getCacheSide().getToken() == null
                    || configuration.getCacheSide().getRefreshExpire() == null
                    || time.isAfter(configuration.getCacheSide().getRefreshExpire())
                    || DriverHelper_123pan.refreshToken(configuration)) {
                DriverHelper_123pan.login(configuration);
            }
        }
    }

    /**
     * @see DriverHelper_123pan#refreshToken(DriverConfiguration_123Pan)
     * @see DriverHelper_123pan#login(DriverConfiguration_123Pan)
     */
    static void ensureToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now().minusMinutes(3);
        synchronized (configuration.getCacheSide()) {
            if (configuration.getCacheSide().getToken() == null
                    || configuration.getCacheSide().getTokenExpire() == null
                    || time.isAfter(configuration.getCacheSide().getTokenExpire()))
                DriverHelper_123pan.forceGetToken(configuration);
        }
    }

    /**
     * Get user information.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * <p> {@literal SET configuration.cacheSide.nickname: }
     * <p> {@literal SET configuration.cacheSide.imageLink: }
     * <p> {@literal SET configuration.cacheSide.vip: }
     * <p> {@literal SET configuration.cacheSide.fileCount: }
     * <p> {@literal SET configuration.webSide.spaceAll: }
     * <p> {@literal SET configuration.webSide.spaceUsed: }
     * <p> {@literal SET configuration.webSide.maxSizePerFile: }
     */
    static void resetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UserInformationURL, configuration, null, false);
        configuration.getCacheSide().setNickname(Objects.requireNonNullElse(data.getString("Nickname"), "undefined"));
        configuration.getCacheSide().setImageLink(data.getString("HeadImage"));
        configuration.getCacheSide().setVip(data.getBooleanValue("Vip", false));
        configuration.getCacheSide().setFileCount(data.getLongValue("FileCount", -1));
        configuration.getWebSide().setSpaceAll(data.getLongValue("SpacePermanent", 0) + data.getLongValue("SpaceTemp", 0));
        configuration.getWebSide().setSpaceUsed(data.getLongValue("SpaceUsed", -1));
        configuration.getWebSide().setMaxSizePerFile(configuration.getCacheSide().isVip() ? 1L << 40 /*1TB*/ : 100L << 30 /*100GB*/);
        configuration.getCacheSide().setModified(true);
    }

    /**
     * Logout.
     * @param configuration
     * <p> {@literal SET configuration.cacheSide.token: null}
     * <p> {@literal SET configuration.cacheSide.tokenExpire: null}
     * <p> {@literal SET configuration.cacheSide.refreshExpire: null}
     */
    static void logout(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        try {
            DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.LogoutURL, configuration, null, false);
            // {"code":200,"message":"请重新登录！"}
            throw new RuntimeException("Unreachable!");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() != 200 || !"\u8bf7\u91cd\u65b0\u767b\u5f55\uff01".equals(exception.getMeaning()))
                throw exception;
        }
        configuration.getCacheSide().setToken(null);
        configuration.getCacheSide().setTokenExpire(null);
        configuration.getCacheSide().setRefreshExpire(null);
        configuration.getCacheSide().setModified(true);
        DriverHelper_123pan.logger.log(HLogLevel.LESS, "Logged out.", ParametersMap.create()
                .add("driver", configuration.getName()).add("passport", configuration.getWebSide().getPassport()));
    }

    // File

    private static @NotNull @UnmodifiableView Map<@NotNull Long, @Nullable FileSqlInformation> transferInformationMap(final @NotNull String driver, final @Nullable JSONArray infos) {
        if (infos == null)
            return Map.of();
        final Map<Long, FileSqlInformation> map = new HashMap<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            final FileSqlInformation information = FileInformation_123pan.create(driver, info);
            if (information != null)
                map.put(information.id(), information);
        }
        return Collections.unmodifiableMap(map);
    }

    static @NotNull @UnmodifiableView Set<@NotNull Long> transferInformationSet(final @Nullable JSONArray infos) {
        if (infos == null)
            return Set.of();
        final Set<Long> set = new HashSet<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null)
                continue;
            final Long id = info.getLong("FileId");
            if (id != null)
                set.add(id);
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * List file from directory path.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @return Notice! The return value when the directory does not exist is the same as when it's empty.
     * <p> {@literal return.first: }Total count in directory.
     * <p> {@literal return.second: }Files list.
     */
    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @IntRange(minimum = 1) int limit, final @IntRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("DriveId", 0);
        request.put("Limit", limit);
        request.put("OrderBy", DriverHelper_123pan.getOrderPolicy(policy));
        request.put("OrderDirection", DriverHelper_123pan.getOrderDirection(direction));
        request.put("ParentFileId", directoryId);
        request.put("Page", page + 1);
        request.put("Trashed", false);
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.ListFilesURL, configuration, request, false);
        final Long total = data.getLong("Total");
        final JSONArray infos = data.getJSONArray("InfoList");
        if (total == null || infos == null)
            throw new WrongResponseException("Listing file.", data, ParametersMap.create()
                    .add("configuration", configuration).add("directoryId", directoryId).add("limit", limit).add("page", page).add("policy", policy).add("direction", direction));
        final List<FileSqlInformation> list = new ArrayList<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            final FileSqlInformation information = FileInformation_123pan.create(configuration.getName(), info);
            if (information != null)
                list.add(information);
        }
        return Pair.ImmutablePair.makeImmutablePair(total, list);
    }

    /**
     * Get files information.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @param idList Ids to get information.
     * @return Notice! Not necessarily every requested ID has corresponding information.
     * <p> Files map.
     */
    static @NotNull @UnmodifiableView Map<@NotNull Long, @Nullable FileSqlInformation> getFilesInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        if (idList.isEmpty())
            return Map.of();
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        request.put("FileIdList", AndroidSupport.streamToList(idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id);
            return pair;
        })));
        final JSONObject data =  DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.FilesInfoURL, configuration, request, false);
        final JSONArray infos = data.getJSONArray("infoList");
        return DriverHelper_123pan.transferInformationMap(configuration.getName(), infos);
    }

    /**
     * Get directly download url.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @param file
     * <p> {@literal GET file.id: }No requirement for accuracy.
     * <p> {@literal GET file.other.deserialize().s3key: }Require accuracy.
     * <p> {@literal GET file.name: }No requirement for accuracy.
     * <p> {@literal GET file.size: }No requirement for accuracy.
     * <p> {@literal GET file.md5: }Require accuracy.
     */
    static @Nullable String getFileDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull FileSqlInformation file) throws IllegalParametersException, IOException {
        assert !file.isDirectory();
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeOther(file);
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("DriveId", 0);
        request.put("FileId", file.id());
        request.put("S3KeyFlag",extra.s3key());
        request.put("FileName", file.name());
        request.put("Size", file.size());
        request.put("Etag", file.md5());
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.SingleFileDownloadURL, configuration, request, false);
        final String url = data.getString("DownloadUrl");
        if (url == null || !url.contains("params="))
            return null;
        final int pIndex = url.indexOf("params=") + "params=".length();
        final int aIndex = url.indexOf('&', pIndex);
        final String base64 = url.substring(pIndex, aIndex < 0 ? url.length() : aIndex);
        final String decodedUrl = new String(Base64.getDecoder().decode(base64));
        if (!decodedUrl.startsWith("https://download-cdn.123pan.cn/") || !decodedUrl.contains("auto_redirect")) {
            DriverHelper_123pan.logger.log(HLogLevel.MISTAKE, "Something went wrong when getting download url! (Unresolvable! Please report this to the developer!)",
                    ParametersMap.create().add("file", file).add("decodedUrl", decodedUrl));
            throw new WrongResponseException("Invalid file download url.", data, ParametersMap.create()
                    .add("configuration", configuration).add("file", file).add("url", url).add("decodedUrl", decodedUrl));
        }
        return decodedUrl.replace("auto_redirect=0", "auto_redirect=1");
    }

    /**
     * Create a directory.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     */
    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        if (!DriverHelper_123pan.filenamePredication.test(directoryName))
            return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), directoryName));
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("DriveId", 0);
        request.put("FileName", directoryName);
        request.put("Type", 1);
        request.put("Size", 0);
        request.put("ParentFileId", parentId);
        request.put("NotReuse", true);
        request.put("Duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadRequestURL, configuration, request, false);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName("Creating directory callback.", new FileLocation(configuration.getName(), parentId), directoryName));
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode && policy == Options.DuplicatePolicy.ERROR)
                return UnionPair.fail(FailureReason.byDuplicateError("Creating directory callback.", new FileLocation(configuration.getName(), parentId), directoryName));
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(configuration.getName(), data.getJSONObject("Info"));
        if (information == null)
            throw new WrongResponseException("Creating directory.", data, ParametersMap.create()
                    .add("configuration", configuration).add("parentId", parentId).add("directoryName", directoryName).add("policy", policy));
        return UnionPair.ok(information);
    }

    public record UploadIdentifier_123pan(long unionId, @NotNull String bucket, @NotNull String key, @NotNull String node, @NotNull String uploadId) {
    }

    /**
     * Request upload a file.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @return
     * <p> {@literal Ok.Ok: }Success. Reuse.
     * <p> {@literal Ok.Failure: }Success. But need upload the file.
     * <p> {@literal Fail: }Failure.
     */
    static @NotNull UnionPair<@NotNull UnionPair<@NotNull FileSqlInformation, @NotNull UploadIdentifier_123pan>, @NotNull FailureReason> uploadRequest(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull CharSequence md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        if (!MiscellaneousUtil.md5Pattern.matcher(md5).matches()) // Unreachable!
            throw new IllegalParametersException("Invalid md5.", ParametersMap.create().add("md5", md5));
        if (!DriverHelper_123pan.filenamePredication.test(filename))
            return UnionPair.fail(FailureReason.byInvalidName("Uploading request.", new FileLocation(configuration.getName(), parentId), filename));
        if (size > configuration.getWebSide().getMaxSizePerFile())
            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading request.", size, configuration.getWebSide().getMaxSizePerFile(), new FileLocation(configuration.getName(), parentId), filename));
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("DriveId", 0);
        request.put("FileName", filename);
        request.put("Type", 0);
        request.put("Size", size);
        request.put("Etag", md5);
        request.put("ParentFileId", parentId);
        request.put("Duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadRequestURL, configuration, request, false);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidArgumentResponseCode && exception.getMeaning().contains("Etag"))
                throw new IllegalParametersException("Uploading request callback Etag.", ParametersMap.create().add("md5", md5), exception);
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode && policy == Options.DuplicatePolicy.ERROR)
                return UnionPair.fail(FailureReason.byDuplicateError("Uploading request callback.", new FileLocation(configuration.getName(), parentId), filename));
            if (exception.getCode() == DriverHelper_123pan.ExceedSizeLimitSingleFileResponseCode)
                return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading request callback.", size, exception.getMeaning(), new FileLocation(configuration.getName(), parentId), filename));
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName("Uploading request callback.", new FileLocation(configuration.getName(), parentId), filename));
            throw exception;
        }
        final Boolean reuse = data.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("Uploading request. Missing 'reuse'.", data, ParametersMap.create()
                    .add("configuration", configuration).add("parentId", parentId).add("filename", filename).add("size", size).add("md5", md5).add("policy", policy));
        if (reuse.booleanValue()) {
            final FileSqlInformation information = FileInformation_123pan.create(configuration.getName(), data.getJSONObject("Info"));
            if (information == null)
                throw new WrongResponseException("Uploading request reused.", data, ParametersMap.create()
                        .add("configuration", configuration).add("parentId", parentId).add("filename", filename).add("size", size).add("md5", md5).add("policy", policy));
            return UnionPair.ok(UnionPair.ok(information));
        }
        final String bucket = data.getString("Bucket");
        final String key = data.getString("Key");
        final String node = data.getString("StorageNode");
        final String uploadId = data.getString("UploadId");
        final Long unionFileId = data.getLong("FileId");
        if (bucket == null || key == null || node == null || uploadId == null || unionFileId == null)
            throw new WrongResponseException("Uploading request.", data, ParametersMap.create()
                    .add("configuration", configuration).add("parentId", parentId).add("filename", filename).add("size", size).add("md5", md5).add("policy", policy));
        return UnionPair.ok(UnionPair.fail(new UploadIdentifier_123pan(unionFileId.longValue(), bucket, key, node, uploadId)));
    }

    /**
     * Get upload urls.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @param uploadIdentifier {@literal GET bucket, key, node, (may uploadId)}
     */
    static @NotNull List<@NotNull String> uploadPare(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull UploadIdentifier_123pan uploadIdentifier, final @IntRange(minimum = 1) int partCount) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(partCount == 1 ? 3 : 6);
        request.put("Bucket", uploadIdentifier.bucket);
        request.put("Key", uploadIdentifier.key);
        request.put("StorageNode", uploadIdentifier.node);
        if (partCount > 1) {
            request.put("UploadId", uploadIdentifier.uploadId);
            request.put("partNumberStart", 1);
            request.put("partNumberEnd", partCount + 1);
        }
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(partCount == 1 ? DriverHelper_123pan.S3AuthPartURL : DriverHelper_123pan.S3ParePartsURL,
                configuration, request, false);
        final JSONObject urls = data.getJSONObject("presignedUrls");
        if (urls == null)
            throw new WrongResponseException("Uploading pare. Missing 'presignedUrls'.", data, ParametersMap.create()
                    .add("configuration", configuration).add("uploadIdentifier", uploadIdentifier).add("partCount", partCount));
        final List<String> res = new ArrayList<>(partCount);
        for (int i = 1; i <= urls.size(); ++i) {
            final String url = urls.getString(String.valueOf(i));
            if (url == null)
                throw new WrongResponseException("Uploading pare. Missing 'url' (" + i + ").", data, ParametersMap.create()
                        .add("configuration", configuration).add("uploadIdentifier", uploadIdentifier).add("partCount", partCount));
            res.add(url);
        }
        assert res.size() == partCount;
        return res;
    }

    /**
     * Complete a file uploading process.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @param uploadIdentifier {@literal GET unionId, path, uploadId}
     * @return
     * <p> {@literal Null: }Failure. Possible error during upload process.
     * <p> {@literal NotNull: }Success.
     */
    static @Nullable FileSqlInformation uploadComplete(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull UploadIdentifier_123pan uploadIdentifier, final int partCount) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(partCount == 1 ? 2 : 3);
        request.put("FileId", uploadIdentifier.unionId);
        request.put("UploadId", uploadIdentifier.uploadId);
        if (partCount > 1)
            request.put("isMultipart", true);
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadCompleteURL, configuration, request, false);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.ServerErrorResponseCode) // \u4e0a\u4f20\u6587\u4ef6\u5927\u5c0f\u65e0\u6548 or \u6ca1\u6709\u627e\u5230\u4e0a\u4f20\u7684\u6587\u4ef6
                return null;
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(configuration.getName(), data.getJSONObject("file_info"));
        if (information == null)
            throw new WrongResponseException("Uploading complete.", data, ParametersMap.create()
                    .add("configuration", configuration).add("uploadIdentifier", uploadIdentifier).add("partCount", partCount));
        return information;
    }

    /**
     * Trash files.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @return Successful ids.
     * <p> Other ids are more likely to no longer exist, so this method return value can be ignored.
     */
    @SuppressWarnings("UnusedReturnValue")
    static @NotNull @UnmodifiableView Set<@NotNull Long> trashFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList, final boolean operate) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("Operation", operate);
        request.put("DriveId", 0);
        request.put("FileTrashInfoList", AndroidSupport.streamToList(idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        })));
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.TrashFileURL, configuration, request, false);
        final JSONArray infos = data.getJSONArray("InfoList");
        return DriverHelper_123pan.transferInformationSet(infos);
    }

    /**
     * Rename file. (Not support DuplicatePolicy. Only {@code ERROR})
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     */
    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_123Pan configuration, final long sourceId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy ignoredPolicy) throws IllegalParametersException, IOException {
        if (!DriverHelper_123pan.filenamePredication.test(filename))
            return UnionPair.fail(FailureReason.byInvalidName("Renaming file.", new FileLocation(configuration.getName(), sourceId), filename));
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("DriveId", 0);
        request.put("FileId", sourceId);
        request.put("FileName", filename);
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.RenameFileURL, configuration, request, false);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode)
                return UnionPair.fail(FailureReason.byDuplicateError("Renaming request callback.", new FileLocation(configuration.getName(), sourceId), filename));
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName("Renaming file callback.", new FileLocation(configuration.getName(), sourceId), filename));
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(configuration.getName(), data.getJSONObject("Info"));
        if (information == null)
            throw new WrongResponseException("Renaming file.", data, ParametersMap.create()
                    .add("configuration", configuration).add("source", sourceId).add("name", filename));
        return UnionPair.ok(information);
    }

    /**
     * Move files. (Not support DuplicatePolicy. Only {@code KEEP})
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#ensureToken(DriverConfiguration_123Pan)})
     * @return Successful ids map.
     */
    static @NotNull @UnmodifiableView Map<@NotNull Long, @Nullable FileSqlInformation> moveFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList, final long parentId, final Options.@NotNull DuplicatePolicy ignoredPolicy) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(2);
        request.put("ParentFileId", parentId);
        request.put("FileIdList", AndroidSupport.streamToList(idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        })));
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.MoveFilesURL, configuration, request, false);
        final JSONArray infos = data.getJSONArray("Info");
        return DriverHelper_123pan.transferInformationMap(configuration.getName(), infos);
    }
}
