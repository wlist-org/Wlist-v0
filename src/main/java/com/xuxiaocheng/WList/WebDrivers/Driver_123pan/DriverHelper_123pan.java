package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DriverHelper_123pan {
    private DriverHelper_123pan() {
        super();
    }

    static final @NotNull Pattern etagPattern = Pattern.compile("^[a-z0-9]{32}$");
    static final @NotNull Pattern tokenPattern = Pattern.compile("^([a-z]|[A-Z]|[0-9]){36}\\.([a-z]|[A-Z]|[0-9]){139}\\.([a-z]|[A-Z]|[0-9]|_|-){43}$");
    static final @NotNull Predicate<String> filenamePredication = (s) -> {
        if (s.length() >= 128)
            return false;
        for (char ch : s.toCharArray())
            if ("\"\\/:*?|><".indexOf(ch) != -1)
                return false;
        return true;
    };

    static final @NotNull Pair.ImmutablePair<String, String> UploadRequestURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/upload_request", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> ListFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/list/new", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> RefreshTokenURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/refresh_token", "POST");
    static final @NotNull Pair.ImmutablePair<String, String> UserInformationURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/info", "GET");
    static final @NotNull Pair.ImmutablePair<String, String> LoginURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/user/sign_in", "POST");

    // "1970-01-01 08:00:00"
    static long parseServerTime(final @Nullable String time) throws IllegalParametersException {
        if (time == null)
            throw new IllegalParametersException(new NullPointerException("time"));
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(time).getTime();
        } catch (final ParseException exception) {
            throw new IllegalParametersException("Invalid time format.", time);
        }
    }

    // "0001-01-01T00:00:00+00:00"
    static long parseServerTimeWithZone(final @Nullable String time) throws IllegalParametersException {
        if (time == null)
            throw new IllegalParametersException(new NullPointerException("time"));
        final int index = time.lastIndexOf(':');
        if (index != 22)
            throw new IllegalParametersException("Invalid time format.", time);
        //noinspection SpellCheckingInspection
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final StringBuilder t = new StringBuilder(time);
        try {
            return dateFormat.parse(t.deleteCharAt(t.lastIndexOf(":")).toString()).getTime();
        } catch (final ParseException exception) {
            throw new IllegalParametersException("Invalid time format.", time);
        }
    }

    private static final @NotNull DuplicatePolicy defaultDuplicatePolicy = DuplicatePolicy.KEEP;
    private static final @NotNull OrderPolicy defaultOrderPolicy = OrderPolicy.FileName;
    private static final @NotNull OrderDirection defaultOrderDirection = OrderDirection.ASCEND;

    static int getDuplicatePolicy(final @Nullable DuplicatePolicy policy) {
        if (policy == null)
            return DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        return switch (policy) {
            case ERROR -> 0;
            case OVER -> 2;
            case KEEP -> 1;
            default -> DriverHelper_123pan.getDuplicatePolicy(DriverHelper_123pan.defaultDuplicatePolicy);
        };
    }

    static @NotNull String getOrderPolicy(final @Nullable OrderPolicy policy) {
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

    static @NotNull String getOrderDirection(final @Nullable OrderDirection policy) {
        if (policy == null)
            return DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        return switch (policy) {
            case ASCEND -> "asc";
            case DESCEND -> "desc";
//            default -> DriverHelper_123pan.getOrderDirection(DriverHelper_123pan.defaultOrderDirection);
        };
    }

    static @Nullable FileInformation_123pan createFileInfo(final @NotNull DrivePath parentPath, final @NotNull JSONObject info) {
        try {
            DriverUtil_123pan.strictCheckForFileNecessaryInfo(info);
            return new FileInformation_123pan(info.getLongValue("FileId"),
                    parentPath.getChild(info.getString("FileName")),
                    info.getIntValue("Type"), info.getLongValue("Size"),
                    DriverHelper_123pan.parseServerTimeWithZone(info.getString("CreateAt")),
                    DriverHelper_123pan.parseServerTimeWithZone(info.getString("UpdateAt")),
                    info.getString("S3KeyFlag"), info.getString("Etag"));
        } catch (final WrongResponseException | IllegalParametersException ignore) {
            return null;
        }
    }

    static boolean isDirectory(final @NotNull FileInformation_123pan info) {
        return info.is_dir() == 1;
    }
}
