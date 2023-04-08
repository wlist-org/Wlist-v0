package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.Configuration.CacheSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.LocalSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.WebSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DriverConfiguration_123Pan extends DriverConfiguration<
        DriverConfiguration_123Pan.LocalSide,
        DriverConfiguration_123Pan.WebSide,
        DriverConfiguration_123Pan.CacheSide> {
    public DriverConfiguration_123Pan() {
        super(LocalSide::new, WebSide::new, CacheSide::new);
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        public LocalSide() {
            super();
            super.setName("123pan");
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$LocalSide{" +
                    "name='" + this.name + '\'' +
                    ", priority=" + this.priority +
                    ", strictMode=" + this.strictMode +
                    '}';
        }
    }

    public static final class WebSide extends WebSideDriverConfiguration {
        private @NotNull String passport = "";
        private @NotNull String password = "";
        private int loginType = 1;
        private int defaultLimitPerPage = 20;

        public @NotNull String getPassport() {
            return this.passport;
        }

        public void setPassport(final @NotNull String passport) {
            this.passport = passport;
        }

        public @NotNull String getPassword() {
            return this.password;
        }

        public void setPassword(final @NotNull String password) {
            this.password = password;
        }

        public int getLoginType() {
            return this.loginType;
        }

        public void setLoginType(final int loginType) {
            this.loginType = loginType;
        }

        public int getDefaultLimitPerPage() {
            return this.defaultLimitPerPage;
        }

        public void setDefaultLimitPerPage(final int defaultLimitPerPage) {
            this.defaultLimitPerPage = defaultLimitPerPage;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$WebSide{" +
                    "passport='" + this.passport + '\'' +
                    ", password='" + this.password + '\'' +
                    ", loginType=" + this.loginType +
                    ", defaultLimitPerPage=" + this.defaultLimitPerPage +
                    '}';
        }
    }

    public static final class CacheSide extends CacheSideDriverConfiguration {
        private @Nullable String token;
        private long tokenExpire;
        private long refreshExpire;

        public @Nullable String getToken() {
            return this.token;
        }

        public long getTokenExpire() {
            return this.tokenExpire;
        }

        public long getRefreshExpire() {
            return this.refreshExpire;
        }

        public void setToken(final @Nullable String token) {
            this.token = token;
        }

        public void setTokenExpire(final long tokenExpire) {
            this.tokenExpire = tokenExpire;
        }

        public void setRefreshExpire(final long refreshExpire) {
            this.refreshExpire = refreshExpire;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$CacheSide{" +
                    "token='" + this.token + '\'' +
                    ", tokenExpire=" + this.tokenExpire +
                    ", refreshExpire=" + this.refreshExpire +
                    ", nickname='" + this.nickname + '\'' +
                    ", imageLink='" + this.imageLink + '\'' +
                    ", vip=" + this.vip +
                    ", spaceAll=" + this.spaceAll +
                    ", spaceUsed=" + this.spaceUsed +
                    ", fileCount=" + this.fileCount +
                    '}';
        }
    }
}
