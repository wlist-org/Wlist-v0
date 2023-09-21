package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderConfiguration;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LanzouConfiguration extends ProviderConfiguration {
    private final @NotNull OkHttpClient httpClient = HttpNetworkHelper.newHttpClientBuilder()
            .addNetworkInterceptor(new HttpNetworkHelper.FrequencyControlInterceptor(
                    new HttpNetworkHelper.FrequencyControlPolicy(3, 1, TimeUnit.SECONDS),
                    HttpNetworkHelper.defaultFrequencyControlPolicyPerMinute()
            )).build();

    @Override
    public @NotNull OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    private @NotNull String passport = "";
    private @NotNull String password = "";
    private long uid;
    private @Nullable String token;
    private @Nullable ZonedDateTime tokenExpire;

    public @NotNull String getPassport() {
        return this.passport;
    }

    public @NotNull String getPassword() {
        return this.password;
    }

    public long getUid() {
        return this.uid;
    }

    public void setUid(final long uid) {
        this.uid = uid;
    }

    public @Nullable String getToken() {
        return this.token;
    }

    public void setToken(final @Nullable String token) {
        this.token = token;
    }

    public @Nullable ZonedDateTime getTokenExpire() {
        return this.tokenExpire;
    }

    public void setTokenExpire(final @Nullable ZonedDateTime tokenExpire) {
        this.tokenExpire = tokenExpire;
    }

    @Override
    public void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) {
        super.name = "lanzou";
        super.displayName = "Lanzou";
        super.maxSizePerFile = 100 << 20;
        super.rootDirectoryId = -1;
        super.load(config, errors);
        this.passport = YamlHelper.getConfig(config, "passport", this.passport,
                o -> YamlHelper.transferString(o, errors, "passport"));
        this.password = YamlHelper.getConfig(config, "password", this.password,
                o -> YamlHelper.transferString(o, errors, "password"));
        this.uid = YamlHelper.getConfig(config, "uid", this.uid,
                o -> YamlHelper.transferIntegerFromStr(o, errors, "uid", YamlHelper.LongMin, YamlHelper.LongMax)).longValue();
        this.token = YamlHelper.getConfigNullable(config, "token",
                o -> YamlHelper.transferString(o, errors, "token"));
        this.tokenExpire = YamlHelper.getConfigNullable(config, "token_expire",
                o -> YamlHelper.transferDateTimeFromStr(o, errors, "token_expire", ProviderConfiguration.TimeFormatter));
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull Object> dump() {
        final Map<String, Object> config = super.dump();
        config.put("passport", this.passport);
        config.put("password", this.password);
        config.put("uid", this.uid);
        config.put("token", this.token);
        config.put("token_expire", this.tokenExpire == null ? null : ProviderConfiguration.TimeFormatter.format(this.tokenExpire));
        return config;
    }

    @Override
    public @NotNull String toString() {
        return "LanzouConfiguration{" +
                "super=" + super.toString() +
                ", passport='" + this.passport + '\'' +
                ", password='" + this.password + '\'' +
                ", uid=" + this.uid +
                ", token='" + this.token + '\'' +
                ", tokenExpire=" + this.tokenExpire +
                '}';
    }
}
