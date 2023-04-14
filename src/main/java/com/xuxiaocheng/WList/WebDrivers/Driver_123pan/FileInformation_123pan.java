package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.DrivePath;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public record FileInformation_123pan(long id, @NotNull DrivePath path, int is_dir, long size, LocalDateTime createTime, LocalDateTime updateTime,
                                     @NotNull String s3key, @NotNull String etag) {
}
