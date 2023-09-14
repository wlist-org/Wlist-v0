package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class DriverConfiguration_lanzou extends ProviderConfiguration<
        DriverConfiguration_lanzou.LocalSide,
        DriverConfiguration_lanzou.WebSide,
        DriverConfiguration_lanzou.CacheSide> {
    DriverConfiguration_lanzou() {
        super("lanzou", LocalSide::new, WebSide::new, CacheSide::new);
    }

    private final @NotNull OkHttpClient httpClient = DriverNetworkHelper.newHttpClientBuilder()
            .addNetworkInterceptor(new DriverNetworkHelper.FrequencyControlInterceptor(
                    new DriverNetworkHelper.FrequencyControlPolicy(3, 1, TimeUnit.SECONDS),
                    DriverNetworkHelper.defaultFrequencyControlPolicyPerMinute())).build();

    @Override
    public @NotNull OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> local, @NotNull final Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, @NotNull final String prefix) {
            super.displayName = "lanzou";
            super.load(local, errors, prefix);
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_lanzou$LocalSide{" +
                    "super=" + super.toString() +
                    '}';
        }
    }

    public static final class WebSide extends WebSideDriverConfiguration {
        private @NotNull String passport = "";
        private @NotNull String password = "";

        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> web, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.maxSizePerFile = 100 << 20;
            super.rootDirectoryId = -1;
            super.load(web, errors, prefix);
            this.passport = YamlHelper.getConfig(web, "passport", this.passport,
                    o -> YamlHelper.transferString(o, errors, prefix + "passport"));
            this.password = YamlHelper.getConfig(web, "password", this.password,
                    o -> YamlHelper.transferString(o, errors, prefix + "password"));
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> web = super.dump();
            web.put("passport", this.passport);
            web.put("password", this.password);
            return web;
        }

        public @NotNull String getPassport() {
            return this.passport;
        }

        public @NotNull String getPassword() {
            return this.password;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_lanzou$WebSide{" +
                    "passport='" + this.passport + '\'' +
                    ", password='" + this.password + '\'' +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    public static final class CacheSide extends CacheSideDriverConfiguration {
        private long uid;
        private @Nullable String identifier;
        private @Nullable LocalDateTime tokenExpire;

        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> cache, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(cache, errors, prefix + "cache$");
            this.uid = YamlHelper.getConfig(cache, "uid", this.uid,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "uid", null, null)).longValue();
            this.identifier = YamlHelper.getConfigNullable(cache, "identifier",
                    o -> YamlHelper.transferString(o, errors, prefix + "identifier"));
            this.tokenExpire = YamlHelper.getConfigNullable(cache, "token_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "token_expire", ProviderConfiguration.TimeFormatter));
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> cache = super.dump();
            cache.put("uid", this.uid);
            cache.put("identifier", this.identifier);
            cache.put("token_expire", this.tokenExpire == null ? null : ProviderConfiguration.TimeFormatter.format(this.tokenExpire));
            return cache;
        }

        public long getUid() {
            return this.uid;
        }

        public void setUid(final long uid) {
            this.uid = uid;
        }

        public @Nullable String getIdentifier() {
            return this.identifier;
        }

        public void setIdentifier(final @Nullable String vei) {
            this.identifier = vei;
        }

        public @Nullable LocalDateTime getTokenExpire() {
            return this.tokenExpire;
        }

        public void setTokenExpire(final @Nullable LocalDateTime tokenExpire) {
            this.tokenExpire = tokenExpire;
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_lanzou$CacheSide{" +
                    "uid=" + this.uid +
                    ", identifier=" + this.identifier +
                    ", tokenExpire=" + this.tokenExpire +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @Override
    public @NotNull String toString() {
        return "DriverConfiguration_lanzou{" +
                "super=" + super.toString() +
                '}';
    }
}
