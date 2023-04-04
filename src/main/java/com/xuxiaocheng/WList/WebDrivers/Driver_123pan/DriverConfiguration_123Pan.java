package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DriverConfiguration_123Pan extends DriverConfiguration {
    @Override
    public @NotNull String getName() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayLocal(), 0, String.class, "123pan");
    }

    public @NotNull String getPassport() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayWeb(), 0, String.class, "");
    }

    public void setPassport(final @NotNull String passport) {
        this.arrayWeb().set(0, passport);
    }

    public int getLoginType() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayWeb(), 1, Integer.class, 0).intValue();
    }

    public void setLoginType(final int loginType) {
        this.arrayWeb().set(1, loginType);
    }

    public @NotNull String getPassword() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayWeb(), 2, String.class, "");
    }

    public void setPassword(final @NotNull String password) {
        this.arrayWeb().set(2, password);
    }

    public int getDefaultLimitPerRequestPage() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayWeb(), 3, Integer.class, 20).intValue();
    }

    public void setDefaultLimitPerPage(final int defaultLimitPerPage) {
        this.arrayWeb().set(3, defaultLimitPerPage);
    }

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
        return "DriverConfiguration_123Pan{" +
                "token='" + this.token + '\'' +
                ", tokenExpire=" + this.tokenExpire +
                ", refreshExpire=" + this.refreshExpire +
                "} " + super.toString();
    }
}
