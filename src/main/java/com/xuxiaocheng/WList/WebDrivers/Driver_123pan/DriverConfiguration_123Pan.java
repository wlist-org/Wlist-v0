package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Utils.AndroidSupport;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

public final class DriverConfiguration_123Pan extends DriverConfiguration<
        DriverConfiguration_123Pan.LocalSide,
        DriverConfiguration_123Pan.WebSide,
        DriverConfiguration_123Pan.CacheSide> {
    public DriverConfiguration_123Pan() {
        super("123pan", LocalSide::new, WebSide::new, CacheSide::new);
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        public LocalSide() {
            super("123pan");
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$LocalSide{" +
                    "super=" + super.toString() +
                    '}';
        }
    }

    public static final class WebSide extends WebSideDriverConfiguration {
        private @NotNull String passport = "";
        private @NotNull String password = "";
        private int loginType = 1;
        private long rootDirectoryId = 0;

        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> web, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(web, errors, prefix);
            this.passport = YamlHelper.getConfig(web, "passport", this.passport,
                    o -> YamlHelper.transferString(o, errors, prefix + "passport"));
            this.password = YamlHelper.getConfig(web, "password", this.password,
                    o -> YamlHelper.transferString(o, errors, prefix + "password"));
            this.loginType = YamlHelper.getConfig(web, "login_type", this.loginType,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "login_type", BigInteger.ONE, AndroidSupport.BigIntegerTwo)).intValue();
            this.rootDirectoryId = YamlHelper.getConfig(web, "root_directory_id", this.rootDirectoryId,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "root_directory_id", BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE))).longValue();
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> web = super.dump();
            web.put("passport", this.passport);
            web.put("password", this.password);
            web.put("login_type", this.loginType);
            web.put("root_directory_id", this.rootDirectoryId);
            return web;
        }

        public @NotNull String getPassport() {
            return this.passport;
        }

        public @NotNull String getPassword() {
            return this.password;
        }

        public int getLoginType() {
            return this.loginType;
        }

        public long getRootDirectoryId() {
            return this.rootDirectoryId;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$WebSide{" +
                    "passport='" + this.passport + '\'' +
                    ", password='" + this.password + '\'' +
                    ", loginType=" + this.loginType +
                    ", rootDirectoryId=" + this.rootDirectoryId +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    public static final class CacheSide extends CacheSideDriverConfiguration {
        private @Nullable String token;
        private @Nullable LocalDateTime tokenExpire;
        private @Nullable LocalDateTime refreshExpire;

        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> cache, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(cache, errors, prefix + "cache$");
            this.token = YamlHelper.getConfigNullable(cache, "token",
                    o -> YamlHelper.transferString(o, errors, prefix + "token"));
            this.tokenExpire = YamlHelper.getConfigNullable(cache, "token_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "token_expire", DriverConfiguration.TimeFormatter));
            this.refreshExpire = YamlHelper.getConfigNullable(cache, "refresh_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "refresh_expire", DriverConfiguration.TimeFormatter));
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> cache = super.dump();
            cache.put("token", this.token);
            cache.put("token_expire", this.tokenExpire == null ? null : DriverConfiguration.TimeFormatter.format(this.tokenExpire));
            cache.put("refresh_expire", this.refreshExpire == null ? null : DriverConfiguration.TimeFormatter.format(this.refreshExpire));
            return cache;
        }

        public @Nullable String getToken() {
            return this.token;
        }

        public void setToken(final @Nullable String token) {
            this.token = token;
        }

        public @Nullable LocalDateTime getTokenExpire() {
            return this.tokenExpire;
        }

        public void setTokenExpire(final @Nullable LocalDateTime tokenExpire) {
            this.tokenExpire = tokenExpire;
        }

        public @Nullable LocalDateTime getRefreshExpire() {
            return this.refreshExpire;
        }

        public void setRefreshExpire(final @Nullable LocalDateTime refreshExpire) {
            this.refreshExpire = refreshExpire;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$CacheSide{" +
                    "token='" + this.token + '\'' +
                    ", tokenExpire=" + this.tokenExpire +
                    ", refreshExpire=" + this.refreshExpire +
                    ", super=" + super.toString() +
                    '}';
        }
    }
}
