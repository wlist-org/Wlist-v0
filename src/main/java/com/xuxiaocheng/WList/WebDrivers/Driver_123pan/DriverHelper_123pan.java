package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    static final @NotNull Pattern etagPattern = Pattern.compile("^[a-z0-9]{32}$");
    static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch: s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };

    static final @NotNull Pair.ImmutablePair<String, String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> FilesInfoURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/info", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> SingleFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/download_info", "POST");
//    static final @NotNull Pair.ImmutablePair<String, String> BatchFileDownloadURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/batch_download_info", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> S3PareURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/s3_repare_upload_parts_batch", "POST");

    private static final @NotNull DuplicatePolicy defaultDuplicatePolicy = DuplicatePolicy.KEEP;
    private static final @NotNull OrderPolicy defaultOrderPolicy = OrderPolicy.FileName;
    private static final @NotNull OrderDirection defaultOrderDirection = OrderDirection.ASCEND;
    @Contract(pure = true) static int getDuplicatePolicy(final @Nullable DuplicatePolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
            default -> DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        };
    }
    @Contract(pure = true) static @NotNull String getOrderPolicy(final @Nullable OrderPolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderPolicy(DriverHelper_123pan.defaultOrderPolicy);
        return switch (policy) {
            case FileName -> "file_name";
            case Size -> "size";
            case CreateTime -> "fileId";
            case UpdateTime -> "update_at";
//            default -> DriverHelper_123pan.getOrderPolicy(DriverHelper_123pan.defaultOrderPolicy);
        };
    }
    @Contract(pure = true) static @NotNull String getOrderDirection(final @Nullable OrderDirection policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
//            default -> DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        };
    }

    static @NotNull JSONObject extractResponseData(final @NotNull JSONObject json, final int successCode, final @NotNull String successMessage) throws WrongResponseException {
        final int code = json.getIntValue("code", -1);
        final String message = json.getString("message");
        if (code != successCode || !successMessage.equals(message))
            throw new WrongResponseException(code, message);
        final JSONObject data = json.getJSONObject("data");
        if (data == null)
            throw new WrongResponseException("Null response data.", json);
        return data;
    }

    static void checkForFileNecessaryInfo(final @NotNull JSONObject info) throws WrongResponseException {
        if (info.getLong("FileId") == null)
            throw new WrongResponseException("Abnormal data/info of 'FileId'.", info);
        if (info.getString("FileName") == null)
            throw new WrongResponseException("Abnormal data/info of 'FileName'.", info);
        if (info.getInteger("Type") == null)
            throw new WrongResponseException("Abnormal data/info of 'Type'.", info);
        if (info.getLongValue("Size", -1) < 0)
            throw new WrongResponseException("Abnormal data/info of 'Size'.", info);
        if (info.getString("S3KeyFlag") == null)
            throw new WrongResponseException("Abnormal data/info of 'S3KeyFlag'.", info);
        if (info.getString("CreateAt") == null)
            throw new WrongResponseException("Abnormal data/info of 'CreateAt'.", info);
        if (info.getString("UpdateAt") == null)
            throw new WrongResponseException("Abnormal data/info of 'UpdateAt'.", info);
        final String etag = info.getString("Etag");
        if (etag == null || (!etag.isEmpty() && !DriverHelper_123pan.etagPattern.matcher(etag).matches()))
            throw new WrongResponseException("Abnormal data of 'Etag'.", info);
    }

    static @NotNull JSONObject buildFileIdList(final @NotNull Collection<@NotNull Long> idList) {
        final Collection<Object> list = new JSONArray(idList.size());
        for (final Long id: idList) {
            final Map<String, Object> pair = new JSONObject(1);
            pair.put("fileId", id.longValue());
            list.add(pair);
        }
        final JSONObject res = new JSONObject(1);
        res.put("fileIdList", list);
        return res;
    }
}
