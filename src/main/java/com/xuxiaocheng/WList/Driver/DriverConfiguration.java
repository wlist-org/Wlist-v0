package com.xuxiaocheng.WList.Driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.function.Supplier;

public abstract class DriverConfiguration<L extends DriverConfiguration.LocalSideDriverConfiguration, W extends DriverConfiguration.WebSideDriverConfiguration, C extends DriverConfiguration.CacheSideDriverConfiguration> {
    protected @NotNull L localSide;
    protected @NotNull W webSide;
    protected @NotNull C cacheSide;

    protected DriverConfiguration(@NotNull final Supplier<? extends L> local, final @NotNull Supplier<? extends W> web, final @NotNull Supplier<? extends C> cache) {
        super();
        this.localSide = local.get();
        this.webSide = web.get();
        this.cacheSide = cache.get();
    }

    public @NotNull L getLocalSide() {
        return this.localSide;
    }

    public void setLocalSide(@NotNull final L localSide) {
        this.localSide = localSide;
    }

    public @NotNull W getWebSide() {
        return this.webSide;
    }

    public void setWebSide(@NotNull final W webSide) {
        this.webSide = webSide;
    }

    public @NotNull C getCacheSide() {
        return this.cacheSide;
    }

    public void setCacheSide(@NotNull final C cacheSide) {
        this.cacheSide = cacheSide;
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
        protected @NotNull String name = "Driver";
        protected @NotNull BigInteger priority = BigInteger.ZERO;

        public @NotNull String getName() {
            return this.name;
        }

        public void setName(final @NotNull String name) {
            this.name = name;
        }

        public @NotNull BigInteger getPriority() {
            return this.priority;
        }

        public void setPriority(final @NotNull BigInteger priority) {
            this.priority = priority;
        }

        @Override
        public @NotNull String toString() {
            return "LocalSideDriverConfiguration{" +
                    "name='" + this.name + '\'' +
                    ", priority=" + this.priority +
                    '}';
        }
    }

    public abstract static class WebSideDriverConfiguration {
        @Override
        public @NotNull String toString() {
            return "WebSideDriverConfiguration{}";
        }
    }

    public abstract static class CacheSideDriverConfiguration {
        protected @NotNull String nickname = "";
        protected @Nullable String imageLink = null;
        protected boolean vip = false;
        protected long spaceAll = 0;
        protected long spaceUsed = 0;
        protected long fileCount = -1;

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
