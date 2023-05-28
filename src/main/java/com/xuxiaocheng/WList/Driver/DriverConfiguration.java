package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class DriverConfiguration<L extends DriverConfiguration.LocalSideDriverConfiguration, W extends DriverConfiguration.WebSideDriverConfiguration, C extends DriverConfiguration.CacheSideDriverConfiguration> {
    public static final @NotNull DateTimeFormatter TokenExpireTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    protected final @NotNull L localSide;
    protected final @NotNull W webSide;
    protected final @NotNull C cacheSide;

    protected DriverConfiguration(@NotNull final Supplier<? extends L> local, final @NotNull Supplier<? extends W> web, final @NotNull Supplier<? extends C> cache) {
        super();
        this.localSide = local.get();
        this.webSide = web.get();
        this.cacheSide = cache.get();
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

    public void load(final @NotNull Map<? super @NotNull String, @NotNull Object> config, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) {
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
                "localSide=" + this.localSide +
                ", webSide=" + this.webSide +
                ", cacheSide=" + this.cacheSide +
                '}';
    }

    public abstract static class LocalSideDriverConfiguration {
        protected @NotNull String name;
        protected @NotNull BigInteger priority = BigInteger.ZERO;

        protected LocalSideDriverConfiguration() {
            super();
            this.name = "Driver";
        }

        protected LocalSideDriverConfiguration(final @NotNull String defaultName) {
            super();
            this.name = defaultName;
        }

        protected void load(final @NotNull Map<? super @NotNull String, @NotNull Object> local, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.name = YamlHelper.getConfig(local, "name", this.name,
                    o -> YamlHelper.transferString(o, errors, prefix + "name"));
            this.priority = YamlHelper.getConfig(local, "priority", this.priority::toString,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "priority", null, null));
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> local = new LinkedHashMap<>();
            local.put("name", this.name);
            local.put("priority", this.priority);
            return local;
        }

        public @NotNull String getName() {
            return this.name;
        }

        public @NotNull BigInteger getPriority() {
            return this.priority;
        }

        @Override
        public @NotNull String toString() {
            return "LocalSideDriverConfiguration{" +
                    "name='" + this.name + '\'' +
                    ", priority=" + this.priority +
                    '}';
        }
    }

    public static class WebSideDriverConfiguration {
        protected int defaultLimitPerPage = 50;

        protected void load(final @NotNull Map<? super @NotNull String, @NotNull Object> web, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.defaultLimitPerPage = YamlHelper.getConfig(web, "default_limit_per_page", () -> Integer.toString(this.defaultLimitPerPage),
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "default_limit_per_page", BigInteger.ONE, BigInteger.valueOf(GlobalConfiguration.getInstance().maxLimitPerPage()))).intValue();
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> web = new LinkedHashMap<>();
            web.put("default_limit_per_page", this.defaultLimitPerPage);
            return web;
        }

        public int getDefaultLimitPerPage() {
            return this.defaultLimitPerPage;
        }

        @Override
        public @NotNull String toString() {
            return "WebSideDriverConfiguration{" +
                    "defaultLimitPerPage=" + this.defaultLimitPerPage +
                    "}";
        }
    }

    public abstract static class CacheSideDriverConfiguration {
        protected @NotNull String nickname = "";
        protected @Nullable String imageLink = null;
        protected boolean vip = false;
        protected long spaceAll = 0;
        protected long spaceUsed = -1;
        protected long fileCount = -1;

        protected void load(final @NotNull Map<? super @NotNull String, @NotNull Object> cache, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            this.nickname = YamlHelper.getConfig(cache, "nickname", this.nickname,
                    o -> YamlHelper.transferString(o, errors, prefix + "nickname"));
            this.imageLink = YamlHelper.getConfigNullable(cache, "image_link",
                    o -> YamlHelper.transferString(o, errors, prefix + "image_link"));
            this.vip = YamlHelper.getConfig(cache, "vip", () -> Boolean.toString(this.vip),
                    o -> YamlHelper.transferBooleanFromStr(o, errors, prefix + "vip")).booleanValue();
            this.spaceAll = YamlHelper.getConfig(cache, "space_all", () -> Long.toString(this.spaceAll),
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "space_all", BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE))).longValue();
            this.spaceUsed = YamlHelper.getConfig(cache, "space_used", () -> Long.toString(this.spaceUsed),
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "space_used", BigInteger.valueOf(-1), BigInteger.valueOf(Long.MAX_VALUE))).longValue();
            this.fileCount = YamlHelper.getConfig(cache, "file_count", () -> Long.toString(this.fileCount),
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "file_count", BigInteger.valueOf(-1), BigInteger.valueOf(Long.MAX_VALUE))).longValue();
        }

        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> cache = new LinkedHashMap<>();
            cache.put("nickname", this.nickname);
            cache.put("image_link", this.imageLink);
            cache.put("vip", this.vip);
            cache.put("space_all", this.spaceAll);
            cache.put("space_used", this.spaceUsed);
            cache.put("file_count", this.fileCount);
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

        public long getFileCount() {
            return this.fileCount;
        }

        public void setFileCount(final long fileCount) {
            this.fileCount = fileCount;
        }

        @Override
        public @NotNull String toString() {
            return "CacheSideDriverConfiguration{" +
                    "nickname='" + this.nickname + '\'' +
                    ", imageLink='" + this.imageLink + '\'' +
                    ", vip=" + this.vip +
                    ", spaceAll=" + this.spaceAll + " Byte" +
                    ", spaceUsed=" + this.spaceUsed + " Byte" +
                    ", fileCount=" + this.fileCount +
                    '}';
        }
    }
}
