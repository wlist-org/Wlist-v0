package com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.HttpNetworkHelper;
import com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil;
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
                    new HttpNetworkHelper.FrequencyControlPolicy(5, 1, TimeUnit.SECONDS),
                    HttpNetworkHelper.defaultFrequencyControlPolicyPerMinute()
            )).build();

    @Override
    public @NotNull OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public @NotNull OkHttpClient getFileClient() {
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
    private boolean skipQRCode = true;
    private boolean skipUsernameChecker = false;
    private boolean skipFileNameChecker = false;

    public boolean isDirectlyLogin() {
        return this.directlyLogin;
    }

    public boolean isSkipQRCode() {
        return this.skipQRCode;
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
        super.spaceGlobalAll = Long.MAX_VALUE;
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
        this.skipQRCode = YamlHelper.getConfig(config, "skip_qr_code", this.skipQRCode,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "skip_qr_code")).booleanValue();
        this.skipUsernameChecker = YamlHelper.getConfig(config, "skip_username_checker", this.skipUsernameChecker,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "skip_username_checker")).booleanValue();
        this.skipFileNameChecker = YamlHelper.getConfig(config, "skip_file_name_checker", this.skipFileNameChecker,
                o -> YamlHelper.transferBooleanFromStr(o, errors, "skip_file_name_checker")).booleanValue();

        if (!this.skipUsernameChecker && (this.passport.isEmpty() || !ProviderUtil.PhoneNumberPattern.matcher(this.passport).matches()))
            errors.add(Pair.ImmutablePair.makeImmutablePair("passport", I18NUtil.get("provider.lanzou.configuration.invalid.passport")));
        if (!this.skipUsernameChecker && (this.password.length() < 6 || 20 < this.password.length()))
            errors.add(Pair.ImmutablePair.makeImmutablePair("password", I18NUtil.get("provider.lanzou.configuration.invalid.password")));
        if (this.tokenExpire != null && MiscellaneousUtil.now().isAfter(this.tokenExpire))
            this.token = null;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull Object> dump() {
        final Map<String, Object> config = super.dump();
        config.put("passport", this.passport);
        config.put("password", this.password);
        config.put("uid", this.uid);
        config.put("token", this.token);
        config.put("token_expire", this.tokenExpire == null ? null : StorageConfiguration.TimeFormatter.format(this.tokenExpire));
        config.put("directly_login", this.directlyLogin);
        config.put("skip_qr_code", this.skipQRCode);
        config.put("skip_username_checker", this.skipUsernameChecker);
        config.put("skip_file_name_checker", this.skipFileNameChecker);
        return config;
    }

    @Override
    public @NotNull String toString() {
        return "LanzouConfiguration{" +
                "super=" + super.toString() +
                ", passport='" + this.passport + '\'' +
                ", password: ***" +
                ", uid=" + this.uid +
                ", token='" + this.token + '\'' +
                ", tokenExpire=" + this.tokenExpire +
                ", directlyLogin=" + this.directlyLogin +
                ", skipQRCode=" + this.skipQRCode +
                ", skipUsernameChecker=" + this.skipUsernameChecker +
                ", skipFileNameChecker=" + this.skipFileNameChecker +
                '}';
    }
}
