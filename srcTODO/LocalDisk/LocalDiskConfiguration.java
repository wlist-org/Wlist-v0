package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public final class LocalDiskConfiguration extends DriverConfiguration<LocalDiskConfiguration.LocalSide, LocalDiskConfiguration.WebSide, LocalDiskConfiguration.CacheSide> {
    public LocalDiskConfiguration() {
        super(LocalSide::new, WebSide::new, CacheSide::new);
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        public LocalSide() {
            super("Local Disk");
        }

        @Override
        public @NotNull String toString() {
            return "LocalDiskConfiguration$LocalSide{" +
                    "name='" + this.name + '\'' +
                    ", priority=" + this.priority +
                    '}';
        }
    }

    public static final class WebSide extends WebSideDriverConfiguration {
        private @NotNull File rootDirectoryPath = new File(System.getProperty("user.dir"), "disk");
        private long maxSpaceUse = Long.MAX_VALUE;

        @Override
        protected void load(@NotNull final Map<? super @NotNull String, @NotNull Object> web, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(web, errors, prefix);
            this.rootDirectoryPath = new File(YamlHelper.<String>getConfig(web, "root_directory_path", this.rootDirectoryPath.getAbsolutePath(),
                    o -> YamlHelper.transferString(o, errors, prefix + "root_directory_path")));
            this.maxSpaceUse = YamlHelper.getConfig(web, "max_space_use", () -> Long.toString(this.maxSpaceUse),
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "max_space_use", BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE))).longValue();
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> web = super.dump();
            web.put("root_directory_path", this.rootDirectoryPath.getAbsolutePath());
            web.put("max_space_use", this.maxSpaceUse);
            return web;
        }

        public @NotNull File getRootDirectoryPath() {
            return this.rootDirectoryPath;
        }

        public long getMaxSpaceUse() {
            return this.maxSpaceUse;
        }

        @Override
        public @NotNull String toString() {
            return "LocalDiskConfiguration$WebSide{" +
                    "rootDirectoryPath=" + this.rootDirectoryPath +
                    ", maxSpaceUse=" + this.maxSpaceUse +
                    ", defaultLimitPerPage=" + this.defaultLimitPerPage +
                    "}";
        }
    }

    public static final class CacheSide extends CacheSideDriverConfiguration {
        @Override
        public @NotNull String toString() {
            return "LocalDiskConfiguration$CacheSide{" +
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
