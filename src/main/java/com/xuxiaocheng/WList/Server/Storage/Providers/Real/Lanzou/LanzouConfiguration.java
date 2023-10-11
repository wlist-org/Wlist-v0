package com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LanzouConfiguration extends StorageConfiguration {
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

    private boolean directlyLogin = false;
    private boolean directlyDownload = false;
    private boolean skipUsernameChecker = false;
    private boolean skipFileNameChecker = false;

    public boolean isDirectlyLogin() {
        return this.directlyLogin;
    }

    public boolean isDirectlyDownload() { // TODO
        return this.directlyDownload;
    }

    public boolean isSkipUsernameChecker() {
        return this.skipUsernameChecker;
    }

    public boolean isSkipFileNameChecker() {
        return this.skipFileNameChecker;
    }

    @Override
    public void load(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) {
        super.name = "lanzou";
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
                o -> YamlHelper.transferDateTimeFromStr(o, errors, "token_expire", StorageConfiguration.TimeFormatter));
        this.directlyLogin = YamlHelper.getConfig(config, "directly_login", this.directlyLogin,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "directly_login")).booleanValue();
        this.directlyDownload = YamlHelper.getConfig(config, "directly_download", this.directlyDownload,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "directly_download")).booleanValue();
        this.skipUsernameChecker = YamlHelper.getConfig(config, "skip_username_checker", this.skipUsernameChecker,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "skip_username_checker")).booleanValue();
        this.skipFileNameChecker = YamlHelper.getConfig(config, "skip_file_name_checker", this.skipFileNameChecker,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "skip_file_name_checker")).booleanValue();
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull Object> dump() {
        final Map<String, Object> config = super.dump();
        config.put("passport", this.passport);
        config.put("password", this.password);
        config.put("uid", this.uid);
        config.put("token", this.token);
        config.put("token_expire", this.tokenExpire == null ? null : StorageConfiguration.TimeFormatter.format(this.tokenExpire));
        config.put("directlyLogin", this.directlyLogin);
        config.put("directly_download", this.directlyDownload);
        config.put("skip_username_checker", this.skipUsernameChecker);
        config.put("skip_file_name_checker", this.skipFileNameChecker);
        return config;
    }

    @Override
    public @NotNull String toString() {
        return "LanzouConfiguration{" +
                "super=" + super.toString() +
                ", passport='" + this.passport + '\'' +
                ", password: " + "*".repeat(this.password.length()) +
                ", uid=" + this.uid +
                ", token='" + this.token + '\'' +
                ", tokenExpire=" + this.tokenExpire +
                ", directlyLogin=" + this.directlyLogin +
                ", directlyDownload=" + this.directlyDownload +
                ", skipUsernameChecker=" + this.skipUsernameChecker +
                ", skipFileNameChecker=" + this.skipFileNameChecker +
                '}';
    }
}
