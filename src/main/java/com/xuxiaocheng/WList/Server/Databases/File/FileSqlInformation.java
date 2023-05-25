package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
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
 * @param md5 File md5.
 * @param others Something extra for driver.
 */
public record FileSqlInformation(long id, @NotNull DrivePath path, boolean is_dir, long size,
                                 @Nullable LocalDateTime createTime, @Nullable LocalDateTime updateTime,
                                 @NotNull String md5, @Nullable String others) {
}
