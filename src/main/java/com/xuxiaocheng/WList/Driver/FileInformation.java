package com.xuxiaocheng.WList.Driver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * A record class for file in web side.
 * @param id File id. Primary key.
 * @param path Full path. Union.
 * @param is_dir If true this is a directory else is a regular file.
 * @param size File size. 0 means a directory, -1 means unknown.
 * @param createTime File first create time. Null means unknown.
 * @param updateTime File the latest update time. Null means unknown.
 * @param others Something extra for driver.
 */
public record FileInformation(long id, @NotNull DrivePath path, boolean is_dir, long size,
                              @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime, @Nullable String others) {
    public static @NotNull JSONObject getJson(final @NotNull FileInformation information) throws IllegalParametersException {
        final String json = information.others();
        if (json == null)
            throw new IllegalParametersException(new NullPointerException("information.others(), " + information));
        return JSON.parseObject(json);
    }
}
