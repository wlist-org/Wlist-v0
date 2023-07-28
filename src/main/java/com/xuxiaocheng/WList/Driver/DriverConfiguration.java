package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class DriverConfiguration<L extends DriverConfiguration.LocalSideDriverConfiguration, W extends DriverConfiguration.WebSideDriverConfiguration, C extends DriverConfiguration.CacheSideDriverConfiguration> {
    public static final @NotNull DateTimeFormatter TimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    protected @NotNull String name;
    protected final @NotNull L localSide;
    protected final @NotNull W webSide;
    protected final @NotNull C cacheSide;

    protected DriverConfiguration(final @NotNull String name, final @NotNull Supplier<? extends L> local, final @NotNull Supplier<? extends W> web, final @NotNull Supplier<? extends C> cache) {
        super();
        this.name = name;
        this.localSide = local.get();
        this.webSide = web.get();
        this.cacheSide = cache.get();
    }

    public @NotNull String getName() {
        return this.name;
    }

    public void setName(final @NotNull String name) {
        this.name = name;
    }

    public @NotNull L getLocalSide() {
        return this.localSide;
    }

    public @NotNull W getWebSide() {
        return this.webSide;
    }

    public @NotNull C getCacheSide() {
        return this.cacheSide;
    }

    public void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) {
        this.name = YamlHelper.getConfig(config, "name", "Driver",
                o -> YamlHelper.transferString(o, errors, "nickname"));
        final Map<String, Object> local = YamlHelper.getConfig(config, "local", Map::of,
                o -> YamlHelper.transferMapNode(o, errors, "local"));
        this.localSide.load(local, errors, "local$");
        final Map<String, Object> web = YamlHelper.getConfig(config, "web", Map::of,
                o -> YamlHelper.transferMapNode(o, errors, "web"));
        this.webSide.load(web, errors, "web$");
        final Map<String, Object> cache = YamlHelper.getConfig(config, "cache", Map::of,
                o -> YamlHelper.transferMapNode(o, errors, "cache"));
        this.cacheSide.load(cache, errors, "cache$");
    }

    public @NotNull Map<@NotNull String, @NotNull Object> dump() {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("local", this.localSide.dump());
        config.put("web", this.webSide.dump());
        config.put("cache", this.cacheSide.dump());
        return config;
    }

    @Override
    public @NotNull String toString() {
        return "DriverConfiguration{" +
                "name='" + this.name + '\'' +
                ", localSide=" + this.localSide +
                ", webSide=" + this.webSide +
                ", cacheSide=" + this.cacheSide +
                '}';
    }

    public abstract static class LocalSideDriverConfiguration {
        protected @NotNull String displayName;
        protected @NotNull LocalDateTime createTime = LocalDateTime.now();
        // TODO modify update time.
        protected @NotNull LocalDateTime updateTime = LocalDateTime.now();

        protected LocalSideDriverConfiguration(final @NotNull String displayName) {
            super();
            this.displayName = displayName;
        }

        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> local, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.displayName = YamlHelper.getConfig(local, "display_name", this.displayName,
                    o -> YamlHelper.transferString(o, errors, prefix + "priority"));
            this.createTime = YamlHelper.getConfig(local, "create_time", this.createTime,
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "priority", DriverConfiguration.TimeFormatter));
            this.updateTime = YamlHelper.getConfig(local, "update_time", this.updateTime,
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "priority", DriverConfiguration.TimeFormatter));
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> local = new LinkedHashMap<>();
            local.put("display_name", this.displayName);
            local.put("create_time", this.createTime.format(DriverConfiguration.TimeFormatter));
            local.put("update_time", this.updateTime.format(DriverConfiguration.TimeFormatter));
            return local;
        }

        public @NotNull String getDisplayName() {
            return this.displayName;
        }

        public @NotNull LocalDateTime getCreateTime() {
            return this.createTime;
        }

        public @NotNull LocalDateTime getUpdateTime() {
            return this.updateTime;
        }

        @Override
        public @NotNull String toString() {
            return "LocalSideDriverConfiguration{" +
                    "displayName='" + this.displayName + '\'' +
                    ", createTime=" + this.createTime +
                    ", updateTime=" + this.updateTime +
                    '}';
        }
    }

    public static class WebSideDriverConfiguration {
        protected long spaceAll = 0;
        protected long spaceUsed = -1;
        protected long maxSizePerFile = Long.MAX_VALUE;

        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> web, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.spaceAll = YamlHelper.getConfig(web, "space_all", this.spaceAll,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "space_all", BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE))).longValue();
            this.spaceUsed = YamlHelper.getConfig(web, "space_used", this.spaceUsed,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "space_used", BigInteger.valueOf(-1), BigInteger.valueOf(Long.MAX_VALUE))).longValue();
            this.maxSizePerFile = YamlHelper.getConfig(web, "max_size_per_file", this.maxSizePerFile,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "max_size_per_file", BigInteger.valueOf(-1), BigInteger.valueOf(Long.MAX_VALUE))).longValue();
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> web = new LinkedHashMap<>();
            web.put("space_all", this.spaceAll);
            web.put("space_used", this.spaceUsed);
            web.put("max_size_per_file", this.maxSizePerFile);
            return web;
        }

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

        public long getMaxSizePerFile() {
            return this.maxSizePerFile;
        }

        public void setMaxSizePerFile(final long maxSizePerFile) {
            this.maxSizePerFile = maxSizePerFile;
        }

        @Override
        public @NotNull String toString() {
            return "WebSideDriverConfiguration{" +
                    "spaceAll=" + this.spaceAll + " Byte" +
                    ", spaceUsed=" + this.spaceUsed + " Byte" +
                    ", maxSizePerFile=" + this.maxSizePerFile + " Byte" +
                    '}';
        }
    }

    public abstract static class CacheSideDriverConfiguration {
        protected AtomicBoolean modified = new AtomicBoolean(false);
        protected @NotNull String nickname = "";
        protected @Nullable String imageLink = null;
        protected boolean vip = false; // TODO vipLevel
        protected long fileCount = -1;
        protected @Nullable LocalDateTime lastFileCacheBuildTime = null;
        protected @Nullable LocalDateTime lastFileIndexBuildTime = null;
        protected @Nullable LocalDateTime lastTrashIndexBuildTime = null;

        public boolean resetModified() {
            return this.modified.compareAndSet(true, false);
        }

        public boolean isModified() {
            return this.modified.get();
        }

        public void setModified(final boolean modified) {
            this.modified.set(modified);
        }

        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> cache, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.nickname = YamlHelper.getConfig(cache, "nickname", this.nickname,
                    o -> YamlHelper.transferString(o, errors, prefix + "nickname"));
            this.imageLink = YamlHelper.getConfigNullable(cache, "image_link",
                    o -> YamlHelper.transferString(o, errors, prefix + "image_link"));
            this.vip = YamlHelper.getConfig(cache, "vip", this.vip,
                    o -> YamlHelper.transferBooleanFromStr(o, errors, prefix + "vip")).booleanValue();
            this.fileCount = YamlHelper.getConfig(cache, "file_count", this.fileCount,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "file_count", BigInteger.valueOf(-1), BigInteger.valueOf(Long.MAX_VALUE))).longValue();
            this.lastFileCacheBuildTime = YamlHelper.getConfigNullable(cache, "last_file_cache_build_time",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "last_file_cache_build_time", DriverConfiguration.TimeFormatter));
            this.lastFileIndexBuildTime = YamlHelper.getConfigNullable(cache, "last_file_index_build_time",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "last_file_index_build_time", DriverConfiguration.TimeFormatter));
            this.lastTrashIndexBuildTime = YamlHelper.getConfigNullable(cache, "last_trash_index_build_time",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "last_trash_index_build_time", DriverConfiguration.TimeFormatter));
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> cache = new LinkedHashMap<>();
            cache.put("nickname", this.nickname);
            cache.put("image_link", this.imageLink);
            cache.put("vip", this.vip);
            cache.put("file_count", this.fileCount);
            cache.put("last_file_cache_build_time", this.lastFileCacheBuildTime == null ? null : this.lastFileCacheBuildTime.format(DriverConfiguration.TimeFormatter));
            cache.put("last_file_index_build_time", this.lastFileIndexBuildTime == null ? null : this.lastFileIndexBuildTime.format(DriverConfiguration.TimeFormatter));
            cache.put("last_trash_index_build_time", this.lastTrashIndexBuildTime == null ? null : this.lastTrashIndexBuildTime.format(DriverConfiguration.TimeFormatter));
            return cache;
        }

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

        public long getFileCount() {
            return this.fileCount;
        }

        public void setFileCount(final long fileCount) {
            this.fileCount = fileCount;
        }

        public @Nullable LocalDateTime getLastFileCacheBuildTime() {
            return this.lastFileCacheBuildTime;
        }

        public void setLastFileCacheBuildTime(final @Nullable LocalDateTime lastFileCacheBuildTime) {
            this.lastFileCacheBuildTime = lastFileCacheBuildTime;
        }

        public @Nullable LocalDateTime getLastFileIndexBuildTime() {
            return this.lastFileIndexBuildTime;
        }

        public void setLastFileIndexBuildTime(final @Nullable LocalDateTime lastFileIndexBuildTime) {
            this.lastFileIndexBuildTime = lastFileIndexBuildTime;
        }

        public @Nullable LocalDateTime getLastTrashIndexBuildTime() {
            return this.lastTrashIndexBuildTime;
        }

        public void setLastTrashIndexBuildTime(final @Nullable LocalDateTime lastTrashIndexBuildTime) {
            this.lastTrashIndexBuildTime = lastTrashIndexBuildTime;
        }

        @Override
        public @NotNull String toString() {
            return "CacheSideDriverConfiguration{" +
                    "nickname='" + this.nickname + '\'' +
                    ", imageLink='" + this.imageLink + '\'' +
                    ", vip=" + this.vip +
                    ", fileCount=" + this.fileCount +
                    ", lastFileIndexBuildTime=" + this.lastFileIndexBuildTime +
                    ", lastTrashIndexBuildTime=" + this.lastTrashIndexBuildTime +
                    '}';
        }
    }
}
