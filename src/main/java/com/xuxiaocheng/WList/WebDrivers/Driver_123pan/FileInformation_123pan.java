package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class FileInformation_123pan {
    private FileInformation_123pan() {
        super();
    }

    public static @NotNull String serializeOther(final @NotNull FileInfoExtra_123pan extra) {
        if (extra.type == 0 || extra.type == 1)
            return 'S' + extra.s3key;
        final JSONObject json = new JSONObject(3);
        json.put("type", extra.type);
        json.put("s3key", extra.s3key);
        return 'J' + json.toJSONString();
    }

    public static @NotNull FileInfoExtra_123pan deserializeOther(final @NotNull FileInformation information) throws IllegalParametersException {
        final String others = information.others();
        if (others == null || others.isEmpty())
            throw new IllegalParametersException("No extra part.", information);
        if (others.charAt(0) == 'S')
            return new FileInfoExtra_123pan(information.is_dir() ? 1 : 0, others.substring(1));
        if (others.charAt(0) == 'J') {
            final JSONObject json = JSON.parseObject(others.substring(1));
            final Integer type = json.getInteger("type");
            if (type == null)
                throw new IllegalParametersException("No type.", information);
            final String s3key = json.getString("s3key");
            if (s3key == null)
                throw new IllegalParametersException("No s3key.", information);
            return new FileInfoExtra_123pan(type.intValue(), s3key);
        }
        throw new IllegalParametersException("Invalid information.", information);
    }

    static @Nullable FileInformation create(final @NotNull DrivePath parentPath, final @Nullable JSONObject info) {
        if (info == null)
            return null;
        try {
            DriverHelper_123pan.checkForFileNecessaryInfo(info);
            return new FileInformation(info.getLongValue("FileId"),
                    parentPath.getChild(info.getString("FileName")),
                    info.getIntValue("Type") == 1, info.getLongValue("Size"),
                    LocalDateTime.parse(info.getString("CreateAt"), DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    LocalDateTime.parse(info.getString("UpdateAt"), DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    info.getString("Etag"), FileInformation_123pan.serializeOther(
                            new FileInfoExtra_123pan(info.getIntValue("Type"), info.getString("S3KeyFlag"))));
        } catch (final WrongResponseException | DateTimeParseException ignore) {
            return null;
        }
    }

    public record FileInfoExtra_123pan(int type, @NotNull String s3key) {
    }
}
