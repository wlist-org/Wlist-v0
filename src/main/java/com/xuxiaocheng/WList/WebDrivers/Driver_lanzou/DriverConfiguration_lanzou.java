package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import okhttp3.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DriverConfiguration_lanzou extends DriverConfiguration<
        DriverConfiguration_lanzou.LocalSide,
        DriverConfiguration_lanzou.WebSide,
        DriverConfiguration_lanzou.CacheSide> {
    public DriverConfiguration_lanzou() {
        super("lanzou", LocalSide::new, WebSide::new, CacheSide::new);
    }

    public static final class LocalSide extends LocalSideDriverConfiguration {
        public LocalSide() {
            super("123pan");
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
        private @Nullable String vei;
        private @Nullable LocalDateTime tokenExpire;
        private final @NotNull Map<@NotNull String, @NotNull Cookie> cookies = new ConcurrentHashMap<>();

        @Override
        protected void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> cache, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String prefix) {
            super.load(cache, errors, prefix + "cache$");
            this.uid = YamlHelper.getConfig(cache, "uid", this.uid,
                    o -> YamlHelper.transferIntegerFromStr(o, errors, prefix + "uid", null, null)).longValue();
            this.vei = YamlHelper.getConfigNullable(cache, "vei",
                    o -> YamlHelper.transferString(o, errors, prefix + "vei"));
            this.tokenExpire = YamlHelper.getConfigNullable(cache, "token_expire",
                    o -> YamlHelper.transferDateTimeFromStr(o, errors, prefix + "token_expire", DriverConfiguration.TimeFormatter));
            // TODO save cookies  Cookie.parse(httpUrl, list.get(0).toString())
        }

        @Override
        protected @NotNull Map<@NotNull String, @NotNull Object> dump() {
            final Map<String, Object> cache = super.dump();
            cache.put("uid", this.uid);
            cache.put("vei", this.vei);
            cache.put("token_expire", this.tokenExpire == null ? null : DriverConfiguration.TimeFormatter.format(this.tokenExpire));
            return cache;
        }

        public long getUid() {
            return this.uid;
        }

        public void setUid(final long uid) {
            this.uid = uid;
        }

        public @Nullable String getVei() {
            return this.vei;
        }

        public void setVei(final @Nullable String vei) {
            this.vei = vei;
        }

        public @Nullable LocalDateTime getTokenExpire() {
            return this.tokenExpire;
        }

        public void setTokenExpire(final @Nullable LocalDateTime tokenExpire) {
            this.tokenExpire = tokenExpire;
        }

        public @NotNull @UnmodifiableView List<@NotNull Cookie> getCookies() {
            return new ArrayList<>(this.cookies.values());
        }

        public void setCookies(final @NotNull @UnmodifiableView Iterable<@NotNull Cookie> cookies) {
            for (final Cookie cookie: cookies)
                this.cookies.put(cookie.name(), cookie);
        }

        @Override
        public @NotNull String toString() {
            return "DriverConfiguration_lanzou$CacheSide{" +
                    "uid=" + this.uid +
                    ", vei=" + this.vei +
                    ", tokenExpire=" + this.tokenExpire +
                    ", super=" + super.toString() +
                    '}';
        }
    }
}
