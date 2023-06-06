package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.DriverTokenExpiredException;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.WList;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("DriverLogger/123pan",
            WList.InIdeaMode ? Integer.MIN_VALUE : HLogLevel.LESS.getLevel(),
            true,  WList.InIdeaMode ? null : HMergedStream.getFileOutputStreamNoException(""));

    static final @NotNull OkHttpClient httpClient = DriverNetworkHelper.httpClientBuilder
            .addNetworkInterceptor(new DriverNetworkHelper.FrequencyControlInterceptor(5, 100)).build();
    private static final @NotNull String agent = DriverNetworkHelper.defaultWebAgent + " " + DriverNetworkHelper.defaultAgent;

    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
//   private  static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> TokenDelayURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/token_delay", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> FilesInfoURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/info", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> SingleFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/download_info", "POST");
//   private  static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> BatchFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/batch_download_info", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> S3AuthPartURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_upload_object/auth", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> S3ParePartsURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_repare_upload_parts_batch", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> UploadCompleteURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_complete/v2", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> TrashFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/trash", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> RenameFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/rename", "POST");
    private static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> MoveFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/mod_pid", "POST");

    static final int InvalidArgumentResponseCode = 400;
    static final int TokenExpireResponseCode = 401;
    static final int FileAlreadyExistResponseCode = 5060;
    static final int InvalidFilenameResponseCode = 5064;
    static final int ExceedSizeLimitSingleFileResponseCode = 5054;

    @Contract(pure = true) private static int getDuplicatePolicy(final Options.@NotNull DuplicatePolicy policy) {
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
        };
    }
    @Contract(pure = true) private static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
        };
    }
    @Contract(pure = true) private static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
        };
    }

    static final @NotNull Pattern PhoneNumberPattern = Pattern.compile("^1[0-9]{10}$");
    static final @NotNull Pattern MailAddressPattern = DriverUtil.mailAddressPattern;// Pattern.compile("^\\w+@[a-zA-Z0-9]{2,10}(?:\\.[a-z]{2,3}){1,3}$");
    static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };
    static final long MaxSizePerFile = 107374182400L;
    static final int UploadPartSize = 16 << 20;

    private static @NotNull JSONObject sendRequestReceiveExtractedData(final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> url, final @Nullable String token, final @Nullable Map<@NotNull String, @NotNull Object> body, final int successCode, final @NotNull String successMessage) throws IOException {
        final Headers.Builder builder = new Headers.Builder();
        if (token != null)
            builder.add("authorization", "Bearer " + token);
        builder.add("user-agent", DriverHelper_123pan.agent);
//        builder.add("user-agent", "123pan/1.0.100");
        builder.add("platform", "web").add("app-version", "3");
        final JSONObject json = DriverNetworkHelper.sendRequestReceiveJson(DriverHelper_123pan.httpClient, url, builder.build(), body);
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != successCode || !successMessage.equals(message))
            throw new IllegalResponseCodeException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Missing response data.", json);
        return data;
    }

    // User

    private static void handleLoginData(final DriverConfiguration_123Pan.@NotNull CacheSide configurationCache, final @NotNull JSONObject data) throws WrongResponseException {
        final String token = data.getString("token");
        if (token == null)
            throw new WrongResponseException("No token in response.");
        configurationCache.setToken(token);
        final String expire = data.getString("expire");
        if (expire == null)
            throw new WrongResponseException("No expire time in response.");
        configurationCache.setTokenExpire(LocalDateTime.parse(expire, DateTimeFormatter.ISO_ZONED_DATE_TIME));
        final Long refresh = data.getLong("refresh_token_expire_time");
        if (refresh == null)
            throw new WrongResponseException("No refresh time in response.");
        configurationCache.setRefreshExpire(LocalDateTime.ofEpochSecond(refresh.longValue(), 0, ZoneOffset.ofHours(8)));
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
     * @return Notice! Not TOKEN!
     * <p> {@literal Null: }Success.
     * <p> {@literal NotNull: }Error message.
     */
    private static @Nullable String login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final int loginType = configuration.getWebSide().getLoginType();
        final boolean isPhone = switch (loginType) {
            case 1 -> true; case 2 -> false;
            default -> throw new IllegalParametersException("Unsupported login type.", loginType);
        };
        if (!(isPhone ? DriverHelper_123pan.PhoneNumberPattern : DriverHelper_123pan.MailAddressPattern).matcher(configuration.getWebSide().getPassport()).matches())
            return isPhone ? DriverUtil.InvalidPhoneNumber : DriverUtil.InvalidMailAddress;
                // isPhone ? "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u7801" : "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u90ae\u7bb1\u53f7";
        final Map<String, Object> request = new LinkedHashMap<>(4);
        request.put("type", loginType);
        request.put(isPhone ? "passport" : "mail", configuration.getWebSide().getPassport());
        request.put("password", configuration.getWebSide().getPassword());
        request.put("remember", false);
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.LoginURL, null, request, 200, "success");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.TokenExpireResponseCode)
                return exception.getMessage();
            throw exception;
        }
        DriverHelper_123pan.logger.log(HLogLevel.LESS, "Logged in for: ", configuration.getLocalSide().getName() + ", passport: " + configuration.getWebSide().getPassport());
        DriverHelper_123pan.handleLoginData(configuration.getCacheSide(), data);
        return null;
    }

    /**
     * Refresh token.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Refresh token.
     * <p> {@literal SET configuration.cacheSide.token: }New token.
     * <p> {@literal SET configuration.cacheSide.tokenExpire: }New token expire time.
     * <p> {@literal SET configuration.cacheSide.refreshExpire: }New refresh token expire time.
     * @return
     * <p> {@code true}: Failure.
     * <p> {@code false}: Success.
     */
    private static boolean refreshToken(final @NotNull DriverConfiguration_123Pan configuration) throws IOException {
        if (configuration.getCacheSide().getToken() == null) // Quick response.
            return true;
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.RefreshTokenURL, configuration.getCacheSide().getToken(), null, 200, "success");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.TokenExpireResponseCode)
                return true;
            throw exception;
        }
        DriverHelper_123pan.logger.log(HLogLevel.LESS, "Refreshed token for: ", configuration.getLocalSide().getName() + ", passport: " + configuration.getWebSide().getPassport());
        DriverHelper_123pan.handleLoginData(configuration.getCacheSide(), data);
        return false;
    }

    static @NotNull String getToken(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final LocalDateTime time = LocalDateTime.now().minusMinutes(3);
        if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getTokenExpire() == null
                || time.isAfter(configuration.getCacheSide().getTokenExpire()))
            if (configuration.getCacheSide().getToken() == null
                || configuration.getCacheSide().getRefreshExpire() == null
                || time.isAfter(configuration.getCacheSide().getRefreshExpire())
                || DriverHelper_123pan.refreshToken(configuration)) {
                final String message = DriverHelper_123pan.login(configuration);
                if (message != null)
                    throw new DriverTokenExpiredException(message);
            }
        return configuration.getCacheSide().getToken();
    }

    /**
     * Get user information.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * <p> {@literal SET configuration.cacheSide.nickname: }
     * <p> {@literal SET configuration.cacheSide.vip: }
     * <p> {@literal SET configuration.cacheSide.spaceAll: }
     * <p> {@literal SET configuration.cacheSide.spaceUsed: }
     * <p> {@literal SET configuration.cacheSide.fileCount: }
     */
    static void resetUserInformation(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UserInformationURL, token, null, 0, "ok");
        configuration.getCacheSide().setNickname(Objects.requireNonNullElse(data.getString("Nickname"), "undefined"));
        configuration.getCacheSide().setImageLink(data.getString("HeadImage"));
        configuration.getCacheSide().setVip(data.getBooleanValue("Vip", false));
        configuration.getCacheSide().setSpaceAll(data.getLongValue("SpacePermanent", 0) + data.getLongValue("SpaceTemp", 0));
        configuration.getCacheSide().setSpaceUsed(data.getLongValue("SpaceUsed", -1));
        configuration.getCacheSide().setFileCount(data.getLongValue("FileCount", -1));
    }

    // File

    /**
     * List file from directory path.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @return Notice! The return value when the directory does not exist is the same as when it's empty.
     * <p> {@literal return.first: }Total count in directory.
     * <p> {@literal return.second: }Files list.
     */
    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull FileSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final long directoryId, final @NotNull DrivePath directoryPath, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("DriveId", 0);
        request.put("Limit", limit);
        request.put("OrderBy", DriverHelper_123pan.getOrderPolicy(policy));
        request.put("OrderDirection", DriverHelper_123pan.getOrderDirection(direction));
        request.put("ParentFileId", directoryId);
        request.put("Page", page + 1);
        request.put("Trashed", false);
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.ListFilesURL, token, request, 0, "ok");
        final Long total = data.getLong("Total");
        final JSONArray infos = data.getJSONArray("InfoList");
        if (total == null || infos == null)
            throw new WrongResponseException("Listing file.", data);
        final List<FileSqlInformation> list = new ArrayList<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null)
                continue;
            final FileSqlInformation information = FileInformation_123pan.create(directoryPath, info);
            if (information != null)
                list.add(information);
        }
        return Pair.ImmutablePair.makeImmutablePair(total, list);
    }

    /**
     * Get files information.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @param idMap (id -> parentPath) map.
     * @return Notice! Not necessarily every requested ID has corresponding information.
     * <p> Files map.
     */
    static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> getFilesInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Map<@NotNull Long, ? extends @NotNull DrivePath> idMap) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        request.put("FileIdList", idMap.keySet().stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id);
            return pair;
        }).toList());
        final JSONObject data =  DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.FilesInfoURL, token, request, 0, "ok");
        final JSONArray infos = data.getJSONArray("infoList");
        if (infos == null)
            return Map.of();
        final Map<Long, FileSqlInformation> map = new HashMap<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null) continue;
            final Long id = info.getLong("FileId");
            if (id == null) continue;
            final DrivePath parentPath = idMap.get(id.longValue());
            if (parentPath == null) continue;
            final FileSqlInformation information = FileInformation_123pan.create(parentPath, info);
            if (information != null)
                map.put(id, information);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Get directly download url.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @param file
     * <p> {@literal GET file.id: }No requirement for accuracy.
     * <p> {@literal GET file.other.deserialize().s3key: }Require accuracy.
     * <p> {@literal GET file.path.name: }No requirement for accuracy.
     * <p> {@literal GET file.size: }No requirement for accuracy.
     * <p> {@literal GET file.md5: }Require accuracy.
     * @return
     * <p> {@literal Null: }Failure. Any of the above parameters that require accuracy are incorrect.
     * <p> {@literal NotNull: }Redirected download url.
     */
    static @Nullable String getFileDownloadUrl(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull FileSqlInformation file) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final FileInformation_123pan.FileInfoExtra_123pan extra = FileInformation_123pan.deserializeOther(file);
        final Map<String, Object> request = new LinkedHashMap<>(6);
        request.put("DriveId", 0);
        request.put("FileId", file.id());
        request.put("S3KeyFlag",extra.s3key());
        request.put("FileName", file.path().getName());
        request.put("Size", file.size());
        request.put("Etag", file.md5());
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.SingleFileDownloadURL, token, request, 0, "ok");
        final String url = data.getString("DownloadUrl");
        if (url == null)
            throw new WrongResponseException("Getting download url.", data);
        assert url.contains("params=");
        final int pIndex = url.indexOf("params=") + "params=".length();
        final int aIndex = url.indexOf('&', pIndex);
        final String base64 = url.substring(pIndex, aIndex < 0 ? url.length() : aIndex);
        final String decodedUrl = new String(Base64.getDecoder().decode(base64));
        assert decodedUrl.startsWith("https://download-cdn.123pan.cn/");
        final JSONObject redirectData = DriverHelper_123pan.sendRequestReceiveExtractedData(Pair.ImmutablePair.makeImmutablePair(decodedUrl, "GET"), null, null, 0, "ok");
        final String redirectUrl = redirectData.getString("redirect_url");
        if (redirectUrl == null)
            throw new WrongResponseException("Missing 'redirect_url'.", redirectData);
        try (final Response response = DriverNetworkHelper.callRequestWithParameters(DriverHelper_123pan.httpClient, Pair.ImmutablePair.makeImmutablePair(redirectUrl, "GET"),
                Headers.of(Map.of("Range", "bytes=0-0")), null).execute()) {
            if (!response.isSuccessful())
                return null;
        } catch (final IOException exception) {
            DriverHelper_123pan.logger.log(HLogLevel.MISTAKE, "Getting download url.", exception);
            return null;
        }
        return redirectUrl;
    }

    /**
     * Create a directory.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     */
    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull DrivePath directoryPath, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        final String name = directoryPath.getName();
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, directoryPath));
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("DriveId", 0);
        request.put("FileName", name);
        request.put("Type", 1);
        request.put("Size", 0);
        request.put("ParentFileId", parentId);
        request.put("NotReuse", true);
        request.put("Duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadRequestURL, token, request, 0, "ok");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName(name, directoryPath));
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode && policy == Options.DuplicatePolicy.ERROR)
                return UnionPair.fail(FailureReason.byDuplicateError("Creating directory.", directoryPath));
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(directoryPath.parent(), data.getJSONObject("Info"));
        directoryPath.child(name);
        if (information == null)
            throw new WrongResponseException("Creating directory.", data);
        return UnionPair.ok(information);
    }

    public record UploadIdentifier_123pan(long unionId, @NotNull DrivePath path, @NotNull String bucket, @NotNull String key, @NotNull String node, @NotNull String uploadId) {
    }

    /**
     * Request upload a file.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @return
     * <p> {@literal Ok.Ok: }Success. Reuse.
     * <p> {@literal Ok.Failure: }Success. Need upload the file.
     * <p> {@literal Fail: }Failure.
     */
    static @NotNull UnionPair<@NotNull UnionPair<@NotNull FileSqlInformation, @NotNull UploadIdentifier_123pan>, @NotNull FailureReason> uploadRequest(final @NotNull DriverConfiguration_123Pan configuration, final long parentId, final @NotNull DrivePath filePath, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException {
        if (size < 0)
            throw new IllegalParametersException("Negative size.", size);
        if (!MiscellaneousUtil.md5Pattern.matcher(md5).matches())
            throw new IllegalParametersException("Invalid md5.", md5);
        final String name = filePath.getName();
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, filePath));
        if (size > DriverHelper_123pan.MaxSizePerFile)
            return UnionPair.fail(FailureReason.byExceedMaxSize(size, DriverHelper_123pan.MaxSizePerFile, filePath));
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("DriveId", 0);
        request.put("FileName", name);
        request.put("Type", 0);
        request.put("Size", size);
        request.put("Etag", md5);
        request.put("ParentFileId", parentId);
        request.put("Duplicate", DriverHelper_123pan.getDuplicatePolicy(policy));
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadRequestURL, token, request, 0, "ok");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName(name, filePath));
            if (exception.getCode() == DriverHelper_123pan.ExceedSizeLimitSingleFileResponseCode)
                return UnionPair.fail(FailureReason.byExceedMaxSize(size, exception.getMeaning(), filePath));
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode && policy == Options.DuplicatePolicy.ERROR)
                return UnionPair.fail(FailureReason.byDuplicateError("Requesting upload.", filePath));
            if (exception.getCode() == DriverHelper_123pan.InvalidArgumentResponseCode && exception.getMeaning().contains("Etag"))
                throw new IllegalParametersException("Etag", md5, exception);
            throw exception;
        }
        final Boolean reuse = data.getBoolean("Reuse");
        if (reuse == null)
            throw new WrongResponseException("PreUploading file (Reuse).", data);
        if (reuse.booleanValue()) {
            final FileSqlInformation information = FileInformation_123pan.create(filePath.parent(), data.getJSONObject("Info"));
            filePath.child(name);
            if (information == null)
                throw new WrongResponseException("PreUploading file.", data);
            return UnionPair.ok(UnionPair.ok(information));
        }
        final String bucket = data.getString("Bucket");
        final String key = data.getString("Key");
        final String node = data.getString("StorageNode");
        final String uploadId = data.getString("UploadId");
        final Long unionFileId = data.getLong("FileId");
        if (bucket == null || key == null || node == null || uploadId == null || unionFileId == null)
            throw new WrongResponseException("PreUploading file.", data);
        return UnionPair.ok(UnionPair.fail(new UploadIdentifier_123pan(unionFileId.longValue(), filePath, bucket, key, node, uploadId)));
    }

    /**
     * Get upload urls.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @param uploadIdentifier {@literal GET bucket, key, node, (may uploadId)}
     * @param partCount {@code assert partCount > 0;}
     */
    static @NotNull List<@NotNull String> uploadPare(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull UploadIdentifier_123pan uploadIdentifier, final int partCount) throws IllegalParametersException, IOException {
        assert partCount > 0;
        final String token = DriverHelper_123pan.getToken(configuration);
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
                token, request, 0, "ok");
        final JSONObject urls = data.getJSONObject("presignedUrls");
        if (urls == null)
            throw new WrongResponseException("PareUploading file.", data);
        final List<String> res = new ArrayList<>(partCount);
        for (int i = 1; i <= urls.size(); ++i) {
            final String url = urls.getString(String.valueOf(i));
            if (url == null)
                throw new WrongResponseException("PareUploading file(url:" + i + ").", data);
            res.add(url);
        }
        assert res.size() == partCount;
        return res;
    }

    /**
     * Complete a file uploading process.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @param uploadIdentifier {@literal GET unionId, path, uploadId}
     * @return
     * <p> {@literal Null: }Failure. Possible error during upload process.
     * <p> {@literal NotNull: }Success.
     */
    static @Nullable FileSqlInformation uploadComplete(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull UploadIdentifier_123pan uploadIdentifier, final int partCount, final long size) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("FileId", uploadIdentifier.unionId);
        request.put("UploadId", uploadIdentifier.uploadId);
//        request.put("bucket", uploadIdentifier.bucket);
//        request.put("StorageNode", uploadIdentifier.node);
//        request.put("key", uploadIdentifier.key);
//        request.put("isMultipart", partCount != 1);
//        request.put("fileSize", size);
        final String name = uploadIdentifier.path.getName();
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.UploadCompleteURL, token, request, 0, "ok");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == 500) // \u4e0a\u4f20\u6587\u4ef6\u5927\u5c0f\u65e0\u6548
                return null;
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(uploadIdentifier.path.parent(), data.getJSONObject("file_info"));
        uploadIdentifier.path.child(name);
        if (information == null)
            throw new WrongResponseException("CompleteUploading file.", data);
        return information;
    }

    /**
     * Trash files.
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @return Successful ids.
     * <p> Other ids are more likely to no longer exist, so this method return value can be ignored.
     */
    @SuppressWarnings("UnusedReturnValue")
    static @NotNull @UnmodifiableView Set<@NotNull Long> trashFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(3);
        request.put("Operation", true);
        request.put("DriveId", 0);
        request.put("FileTrashInfoList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        }).toList());
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.TrashFileURL, token, request, 0, "ok");
        final JSONArray infos = data.getJSONArray("InfoList");
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
     * Rename file. (Not support DuplicatePolicy. {@code ERROR})
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @return
     * <p> {@literal Ok: }Success.
     * <p> {@literal Fail.NIL: }Duplicate filename. Should be handled by the caller.
     * <p> {@literal Fail.InvalidName: }Failure. Invalid filename.
     */
    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_123Pan configuration, final long id, final @NotNull DrivePath path) throws IllegalParametersException, IOException {
        final String name = path.getName();
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, path));
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(4);
        request.put("DriveId", 0);
        request.put("FileId", id);
        request.put("FileName", name);
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.RenameFileURL, token, request, 0, "ok");
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName(name, path));
            if (exception.getCode() == DriverHelper_123pan.FileAlreadyExistResponseCode)
                return UnionPair.fail(FailureReason.NIL); // Handling by the caller.
            throw exception;
        }
        final FileSqlInformation information = FileInformation_123pan.create(path.parent(), data.getJSONObject("Info"));
        path.child(name);
        if (information == null)
            throw new WrongResponseException("Renaming file.", data);
        return UnionPair.ok(information);
    }

    /**
     * Move files. (Not support DuplicatePolicy. {@code KEEP})
     * @param configuration
     * <p> {@literal GET configuration.cacheSide.token: }Token. (May refresh. {@link DriverHelper_123pan#getToken(DriverConfiguration_123Pan)})
     * @return Successful ids map.
     */
    static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> moveFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList, final long parentId, final @NotNull DrivePath parentPath) throws IllegalParametersException, IOException {
        final String token = DriverHelper_123pan.getToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(2);
        request.put("ParentFileId", parentId);
        request.put("FileIdList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        }).toList());
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.MoveFilesURL, token, request, 0, "ok");
        final JSONArray infos = data.getJSONArray("InfoList");
        if (infos == null)
            return Map.of();
        final Map<Long, FileSqlInformation> map = new HashMap<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            final FileSqlInformation information = FileInformation_123pan.create(parentPath, info);
            if (information != null)
                map.put(information.id(), information);
        }
        return Collections.unmodifiableMap(map);
    }
}
