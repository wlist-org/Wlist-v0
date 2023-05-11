package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

public final class DriverConfiguration_123Pan extends DriverConfiguration<
        DriverConfiguration_123Pan.LocalSide,
        DriverConfiguration_123Pan.WebSide,
        DriverConfiguration_123Pan.CacheSide> {
    public DriverConfiguration_123Pan() {
        super(LocalSide::new, WebSide::new, CacheSide::new);
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        public LocalSide() {
            super("123pan");
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_123Pan$LocalSide{" +
                    "name='" + this.name + '\'' +
                    ", priority=" + this.priority +
                    '}';
        }
    }

    public static final class WebSide extends WebSideDriverConfiguration {
        private final @NotNull LoginPart loginPart = new LoginPart();
        private final @NotNull FilePart filePart = new FilePart();

        @Override
        protected void load(@NotNull final Map<? super @NotNull String, @NotNull Object> web, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(web, errors, prefix);
            final Map<String, Object> login = YamlHelper.getConfig(web, "login", Map::of,
                    o -> YamlHelper.transferMapNode(o, errors, prefix + "login"));
            this.loginPart.load(login, errors, prefix + "login$");
            final Map<String, Object> file = YamlHelper.getConfig(web, "file", Map::of,
                    o -> YamlHelper.transferMapNode(o, errors, prefix + "file"));
            this.filePart.load(file, errors, prefix + "file$");
        }

        public @NotNull LoginPart getLoginPart() {
            return this.loginPart;
        }

        public @NotNull FilePart getFilePart() {
            return this.filePart;
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

            private void load(final @NotNull Map<? super @NotNull String, @NotNull Object> login, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
                this.passport = YamlHelper.getConfig(login, "passport", this.passport,
                        o -> YamlHelper.transferString(o, errors, prefix + "passport"));
                this.password = YamlHelper.getConfig(login, "password", this.password,
                        o -> YamlHelper.transferString(o, errors, prefix + "password"));
                this.loginType = YamlHelper.getConfig(login, "login_type", () -> Integer.toString(this.loginType),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "login_type", BigInteger.ONE, BigInteger.TWO)).intValue();
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

            private void load(final @NotNull Map<? super @NotNull String, @NotNull Object> file, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
                this.defaultLimitPerPage = YamlHelper.getConfig(file, "default_limit_per_page", () -> Integer.toString(this.defaultLimitPerPage),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "default_limit_per_page", BigInteger.ONE, BigInteger.valueOf(GlobalConfiguration.getInstance().maxLimitPerPage()))).intValue();
                this.duplicatePolicy = YamlHelper.getConfig(file, "duplicate_policy", this.duplicatePolicy::name,
                        o -> YamlHelper.transferEnumFromStr(o, errors, prefix + "duplicate_policy", DuplicatePolicy.class));
                this.orderPolicy = YamlHelper.getConfig(file, "order_policy", this.orderPolicy::name,
                        o -> YamlHelper.transferEnumFromStr(o, errors, prefix + "order_policy", OrderPolicy.class));
                this.orderDirection = YamlHelper.getConfig(file, "order_direction", this.orderDirection::name,
                        o -> YamlHelper.transferEnumFromStr(o, errors, prefix + "order_direction", OrderDirection.class));
                this.rootDirectoryId = YamlHelper.getConfig(file, "root_directory_id", () -> Long.toString(this.rootDirectoryId),
                        o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "root_directory_id", BigInteger.ZERO, null)).longValue();
            }

            public int getDefaultLimitPerPage() {
                return this.defaultLimitPerPage;
            }

            public @NotNull DuplicatePolicy getDuplicatePolicy() {
                return this.duplicatePolicy;
            }

            public @NotNull OrderPolicy getOrderPolicy() {
                return this.orderPolicy;
            }

            public @NotNull OrderDirection getOrderDirection() {
                return this.orderDirection;
            }

            public long getRootDirectoryId() {
                return this.rootDirectoryId;
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
        private @Nullable LocalDateTime tokenExpire;
        private @Nullable LocalDateTime refreshExpire;

        @Override
        protected void load(@NotNull final Map<? super @NotNull String, @NotNull Object> cache, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, @NotNull final String prefix) {
            super.load(cache, errors, prefix + "cache$");
            this.token = YamlHelper.getConfigNullable(cache, "token",
                    o -> YamlHelper.transferString(o, errors, prefix + "token"));
            this.tokenExpire = YamlHelper.getConfigNullable(cache, "token_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "token_expire", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            this.refreshExpire = YamlHelper.getConfigNullable(cache, "refresh_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "refresh_expire", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
