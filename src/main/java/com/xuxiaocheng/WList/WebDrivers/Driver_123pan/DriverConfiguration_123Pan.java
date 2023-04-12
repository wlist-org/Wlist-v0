package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.Configuration.CacheSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.LocalSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.WebSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class DriverConfiguration_123Pan extends DriverConfiguration<
        DriverConfiguration_123Pan.LocalSide,
        DriverConfiguration_123Pan.WebSide,
        DriverConfiguration_123Pan.CacheSide> {
    public DriverConfiguration_123Pan() {
        super(LocalSide::new, WebSide::new, CacheSide::new);
    }

    @Override
    public @NotNull Set<Class<?>> getDumpMapClasses() {
        final Set<Class<?>> classes =  super.getDumpMapClasses();
        classes.add(WebSide.LoginPart.class);
        classes.add(WebSide.FilePart.class);
        return classes;
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
        private @NotNull LoginPart loginPart = new LoginPart();
        private @NotNull FilePart filePart = new FilePart();

        public @NotNull LoginPart getLoginPart() {
            return this.loginPart;
        }

        public void setLoginPart(final @NotNull LoginPart loginPart) {
            this.loginPart = loginPart;
        }

        public @NotNull FilePart getFilePart() {
            return this.filePart;
        }

        public void setFilePart(final @NotNull FilePart filePart) {
            this.filePart = filePart;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$WebSide{" +
                    "loginPart=" + this.loginPart +
                    ", filePart=" + this.filePart +
                    '}';
        }

        public static final class LoginPart {
            private @NotNull String passport = "";
            private @NotNull String password = "";
            private int loginType = 1;

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

            @Override
            public @NotNull String toString() {
                return "DriverConfiguration_123Pan$WebSide$LoginPart{" +
                        "passport='" + this.passport + '\'' +
                        ", password='" + this.password + '\'' +
                        ", loginType=" + this.loginType +
                        '}';
            }
        }

        public static final class FilePart {
            private int defaultLimitPerPage = 20;
            private @NotNull DuplicatePolicy duplicatePolicy = DuplicatePolicy.KEEP;
            private @NotNull OrderPolicy orderPolicy = OrderPolicy.FileName;
            private @NotNull OrderDirection orderDirection = OrderDirection.ASCEND;
            private long rootDirectoryId = 0;

            public int getDefaultLimitPerPage() {
                return this.defaultLimitPerPage;
            }

            public void setDefaultLimitPerPage(final int defaultLimitPerPage) {
                this.defaultLimitPerPage = defaultLimitPerPage;
            }

            public @NotNull DuplicatePolicy getDuplicatePolicy() {
                return this.duplicatePolicy;
            }

            public void setDuplicatePolicy(final @NotNull DuplicatePolicy duplicatePolicy) {
                this.duplicatePolicy = duplicatePolicy;
            }

            public @NotNull OrderPolicy getOrderPolicy() {
                return this.orderPolicy;
            }

            public void setOrderPolicy(final @NotNull OrderPolicy orderPolicy) {
                this.orderPolicy = orderPolicy;
            }

            public @NotNull OrderDirection getOrderDirection() {
                return this.orderDirection;
            }

            public void setOrderDirection(final @NotNull OrderDirection orderDirection) {
                this.orderDirection = orderDirection;
            }

            public long getRootDirectoryId() {
                return this.rootDirectoryId;
            }

            public void setRootDirectoryId(final long rootDirectoryId) {
                this.rootDirectoryId = rootDirectoryId;
            }

            @Override
            public @NotNull String toString() {
                return "DriverConfiguration_123Pan$WebSide$FilePart{" +
                        "defaultLimitPerPage=" + this.defaultLimitPerPage +
                        ", duplicatePolicy=" + this.duplicatePolicy +
                        ", orderPolicy=" + this.orderPolicy +
                        ", orderDirection=" + this.orderDirection +
                        ", rootDirectoryId=" + this.rootDirectoryId +
                        '}';
            }
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
