package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.DrivePath;
import org.jetbrains.annotations.NotNull;

public record FileInformation_123pan(long id, @NotNull DrivePath path, int is_dir, long size, long createTime, long updateTime,
                                     @NotNull String s3key, @NotNull String etag) {
}
