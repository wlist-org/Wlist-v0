package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class StorageConfiguration {
    public static final @NotNull DateTimeFormatter TimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    protected @NotNull String name = "provider";
    protected final @NotNull AtomicBoolean modified = new AtomicBoolean(false);

    public @NotNull String getName() {
        return this.name;
    }

    public void setName(final @NotNull String name) {
        this.name = name;
    }

    public boolean resetModified() {
        return this.modified.compareAndSet(true, false);
    }

    public void markModified() {
        this.modified.set(true);
    }

    public @NotNull OkHttpClient getHttpClient() {
        return HttpNetworkHelper.DefaultHttpClient;
    }

    public @NotNull OkHttpClient getFileClient() {
        return HttpNetworkHelper.DefaultHttpClient;
    }

    protected @NotNull ZonedDateTime createTime = MiscellaneousUtil.now();
    protected @NotNull ZonedDateTime updateTime = this.createTime;
    protected long rootDirectoryId = 0;

    public @NotNull ZonedDateTime getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(final @NotNull ZonedDateTime createTime) {
        this.createTime = createTime;
    }

    public @NotNull ZonedDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(final @NotNull ZonedDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public long getRootDirectoryId() {
        return this.rootDirectoryId;
    }

    protected long spaceAll = -1;
    protected long spaceUsed = -1;
    protected long spaceGlobalAll = -1;
    protected long spaceGlobalUsed = -1;
    protected long fileCount = -1;
    protected long fileGlobalCount = -1;

    public long getSpaceAll() {
        return this.spaceAll;
    }

    public void setSpaceAll(final long spaceAll) {
        this.spaceAll = spaceAll;
    }

    public long getSpaceUsed() {
        return this.spaceUsed;
    }

    public void setSpaceUsed(final long spaceUsed) {
        this.spaceUsed = spaceUsed;
    }

    public long getSpaceGlobalAll() {
        return this.spaceGlobalAll;
    }

    public void setSpaceGlobalAll(final long spaceGlobalAll) {
        this.spaceGlobalAll = spaceGlobalAll;
    }

    public long getSpaceGlobalUsed() {
        return this.spaceGlobalUsed;
    }

    public void setSpaceGlobalUsed(final long spaceGlobalUsed) {
        this.spaceGlobalUsed = spaceGlobalUsed;
    }

    public long getFileCount() {
        return this.fileCount;
    }

    public void setFileCount(final long fileCount) {
        this.fileCount = fileCount;
    }

    public long getFileGlobalCount() {
        return this.fileGlobalCount;
    }

    public void setFileGlobalCount(final long fileGlobalCount) {
        this.fileGlobalCount = fileGlobalCount;
    }

    protected long maxSizePerFile = Long.MAX_VALUE;
    // TODO: More policies.

    public long getMaxSizePerFile() {
        return this.maxSizePerFile;
    }

    public void setMaxSizePerFile(final long maxSizePerFile) {
        this.maxSizePerFile = maxSizePerFile;
    }

    protected @NotNull String nickname = "user";
    protected @Nullable String imageLink = null;
    protected boolean vip = false;

    public @NotNull String getNickname() {
        return this.nickname;
    }

    public void setNickname(final @NotNull String nickname) {
        this.nickname = nickname;
    }

    public @Nullable String getImageLink() {
        return this.imageLink;
    }

    public void setImageLink(final @Nullable String imageLink) {
        this.imageLink = imageLink;
    }

    public boolean isVip() {
        return this.vip;
    }

    public void setVip(final boolean vip) {
        this.vip = vip;
    }


    public void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) {
        this.createTime = YamlHelper.getConfig(config, "create_time", this.createTime,
                o -> YamlHelper.transferDateTimeFromStr(o, errors, "create_time", StorageConfiguration.TimeFormatter)).withNano(0);
        this.updateTime = YamlHelper.getConfig(config, "update_time", this.updateTime,
                o -> YamlHelper.transferDateTimeFromStr(o, errors, "update_time", StorageConfiguration.TimeFormatter)).withNano(0);
        this.rootDirectoryId = YamlHelper.getConfig(config, "root_directory_id", this.rootDirectoryId,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "root_directory_id", YamlHelper.LongMin, YamlHelper.LongMax)).longValue();

        this.spaceAll = YamlHelper.getConfig(config, "space_all", this.spaceAll,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "space_all", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();
        this.spaceUsed = YamlHelper.getConfig(config, "space_used", this.spaceUsed,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "space_used", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();
        this.spaceGlobalAll = YamlHelper.getConfig(config, "space_global_all", this.spaceGlobalAll,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "space_global_all", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();
        this.spaceGlobalUsed = YamlHelper.getConfig(config, "space_global_used", this.spaceGlobalUsed,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "space_global_used", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();
        this.fileCount = YamlHelper.getConfig(config, "file_count", this.fileCount,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "file_count", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();
        this.fileGlobalCount = YamlHelper.getConfig(config, "file_global_count", this.fileGlobalCount,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, "file_global_count", YamlHelper.MinusOne, YamlHelper.LongMax)).longValue();

        this.maxSizePerFile = YamlHelper.getConfig(config, "max_size_per_file", this.maxSizePerFile,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "max_size_per_file", BigInteger.ZERO, YamlHelper.LongMax)).longValue();

        this.nickname = YamlHelper.getConfig(config, "nickname", this.nickname,
                o -> YamlHelper.transferString(o, errors, "nickname"));
        this.imageLink = YamlHelper.getConfigNullable(config, "image_link",
                o -> YamlHelper.transferString(o, errors, "image_link"));
        this.vip = YamlHelper.getConfig(config, "vip", this.vip,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "vip")).booleanValue();


        if (this.createTime.isAfter(this.updateTime))
            errors.add(Pair.ImmutablePair.makeImmutablePair("update_time", I18NUtil.get("provider.configuration.invalid.time")));
        if (this.spaceAll >= 0 && this.spaceGlobalAll >= 0 && this.spaceAll > this.spaceGlobalAll)
            errors.add(Pair.ImmutablePair.makeImmutablePair("space_all", I18NUtil.get("provider.configuration.invalid.space_all")));
        if (this.spaceUsed >= 0 && this.spaceGlobalUsed >= 0 && this.spaceUsed > this.spaceGlobalUsed)
            errors.add(Pair.ImmutablePair.makeImmutablePair("space_used", I18NUtil.get("provider.configuration.invalid.space_used")));
        if (this.fileCount >= 0 && this.fileGlobalCount >= 0 && this.fileCount > this.fileGlobalCount)
            errors.add(Pair.ImmutablePair.makeImmutablePair("file_count", I18NUtil.get("provider.configuration.invalid.file_count")));
    }

    public @NotNull Map<@NotNull String, @NotNull Object> dump() {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("create_time", this.createTime.format(StorageConfiguration.TimeFormatter));
        config.put("update_time", this.updateTime.format(StorageConfiguration.TimeFormatter));
        config.put("root_directory_id", this.rootDirectoryId);

        config.put("space_all", this.spaceAll);
        config.put("space_used", this.spaceUsed);
        config.put("space_global_all", this.spaceGlobalAll);
        config.put("space_global_used", this.spaceGlobalUsed);
        config.put("file_count", this.fileCount);
        config.put("file_global_count", this.fileGlobalCount);

        config.put("max_size_per_file", this.maxSizePerFile);

        config.put("nickname", this.nickname);
        config.put("image_link", this.imageLink);
        config.put("vip", this.vip);
        return config;
    }

    @Override
    public @NotNull String toString() {
        return "StorageConfiguration{" +
                "name='" + this.name + '\'' +
                ", modified=" + this.modified +
                ", createTime=" + this.createTime +
                ", updateTime=" + this.updateTime +
                ", rootDirectoryId=" + this.rootDirectoryId +
                ", spaceAll=" + this.spaceAll +
                ", spaceUsed=" + this.spaceUsed +
                ", spaceGlobalAll=" + this.spaceGlobalAll +
                ", spaceGlobalUsed=" + this.spaceGlobalUsed +
                ", fileCount=" + this.fileCount +
                ", fileGlobalCount=" + this.fileGlobalCount +
                ", maxSizePerFile=" + this.maxSizePerFile +
                ", nickname='" + this.nickname + '\'' +
                ", imageLink='" + this.imageLink + '\'' +
                ", vip=" + this.vip +
                '}';
    }
}
