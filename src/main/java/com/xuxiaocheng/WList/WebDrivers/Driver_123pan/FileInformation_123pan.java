package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
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

    public static @NotNull FileInfoExtra_123pan deserializeOther(final @NotNull FileSqlInformation information) throws IllegalParametersException {
        final String others = information.others();
        if (others == null || others.isEmpty())
            throw new IllegalParametersException("No extra part.", ParametersMap.create().add("information", information));
        if (others.charAt(0) == 'S')
            return new FileInfoExtra_123pan(information.isDirectory() ? 1 : 0, others.substring(1));
        if (others.charAt(0) == 'J') {
            final JSONObject json = JSON.parseObject(others.substring(1));
            final Integer type = json.getInteger("type");
            if (type == null)
                throw new IllegalParametersException("No type.", ParametersMap.create().add("information", information));
            final String s3key = json.getString("s3key");
            if (s3key == null)
                throw new IllegalParametersException("No s3key.", ParametersMap.create().add("information", information));
            return new FileInfoExtra_123pan(type.intValue(), s3key);
        }
        throw new IllegalParametersException("Invalid extra part.", ParametersMap.create().add("information", information));
    }

    static @Nullable FileSqlInformation create(final @NotNull String driver, final @Nullable JSONObject info) {
        if (info == null)
            return null;
        final Long id = info.getLong("FileId");
        final Long parent = info.getLong("ParentFileId");
        final String name = info.getString("FileName");
        final Integer type = info.getInteger("Type");
        final Long size = info.getLong("Size");
        final String create = info.getString("CreateAt");
        final String update = info.getString("UpdateAt");
        final String flag = info.getString("S3KeyFlag");
        final String etag = info.getString("Etag");
        if (id == null || name == null || type == null || size == null || size.longValue() < 0 || create == null || update == null || flag == null
                || etag == null || (!etag.isEmpty() && !HMessageDigestHelper.MD5.pattern.matcher(etag).matches()))
            return null;
        try {
            return new FileSqlInformation(new FileLocation(driver, id.longValue()), parent.longValue(),
                    name, type.intValue() == 0 ? FileSqlInterface.FileSqlType.RegularFile : FileSqlInterface.FileSqlType.Directory, size.longValue(),
                    LocalDateTime.parse(create, DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    LocalDateTime.parse(update, DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    etag, FileInformation_123pan.serializeOther(new FileInfoExtra_123pan(
                            type.intValue(), flag)));
        } catch (final DateTimeParseException ignore) {
            return null;
        }
    }

    static @Nullable TrashedSqlInformation createTrashed(final @NotNull String driver, final @Nullable JSONObject info) {
        if (info == null)
            return null;
        final Long id = info.getLong("FileId");
        final String name = info.getString("FileName");
        final Integer type = info.getInteger("Type");
        final Long size = info.getLong("Size");
        final String create = info.getString("CreateAt");
        final String trashed = info.getString("TrashedAt");
        final String expire = info.getString("ExpireTime");
        final String flag = info.getString("S3KeyFlag");
        final String etag = info.getString("Etag");
        if (id == null || name == null || type == null || size == null || size.longValue() < 0
                || create == null || trashed == null || expire == null || flag == null
                || etag == null || (!etag.isEmpty() && !HMessageDigestHelper.MD5.pattern.matcher(etag).matches()))
            return null;
        try {
            return new TrashedSqlInformation(new FileLocation(driver, id.longValue()), name, type.intValue() == 1, size.longValue(),
                    LocalDateTime.parse(create, DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    LocalDateTime.parse(trashed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    LocalDateTime.parse(expire, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    etag, FileInformation_123pan.serializeOther(new FileInfoExtra_123pan(
                    type.intValue(), flag)));
        } catch (final DateTimeParseException ignore) {
            return null;
        }
    }

    public record FileInfoExtra_123pan(int type, @NotNull String s3key) {
    }
}
