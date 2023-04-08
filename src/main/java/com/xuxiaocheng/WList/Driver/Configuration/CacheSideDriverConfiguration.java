package com.xuxiaocheng.WList.Driver.Configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CacheSideDriverConfiguration {
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
