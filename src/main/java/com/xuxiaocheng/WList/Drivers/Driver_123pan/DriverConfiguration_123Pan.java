package com.xuxiaocheng.WList.Drivers.Driver_123pan;

import com.xuxiaocheng.WList.Internal.Drives.DriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DriverConfiguration_123Pan implements DriverConfiguration {
    protected @NotNull String passport = "";
    protected @NotNull String password = "";
    protected @Nullable String token;
    protected long tokenExpire;
    protected long refreshExpire;
    protected int defaultLimitPerPage = 20;

    public @NotNull String getPassport() {
        return this.passport;
    }

    public @NotNull String getPassword() {
        return this.password;
    }

    public @Nullable String getToken() {
        return this.token;
    }

    public long getTokenExpire() {
        return this.tokenExpire;
    }

    public long getRefreshExpire() {
        return this.refreshExpire;
    }

    public int getDefaultLimitPerPage() {
        return this.defaultLimitPerPage;
    }

    public void setPassport(final @NotNull String passport) {
        this.passport = passport;
    }

    public void setPassword(final @NotNull String password) {
        this.password = password;
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

    public void setDefaultLimitPerPage(final int defaultLimitPerPage) {
        this.defaultLimitPerPage = defaultLimitPerPage;
    }

    @Override
    public @NotNull String toString() {
        return "DriverConfiguration_123Pan{" +
                "passport='" + this.passport + '\'' +
                ", password='" + this.password + '\'' +
                ", token='" + this.token + '\'' +
                ", tokenExpire=" + this.tokenExpire +
                ", refreshExpire=" + this.refreshExpire +
                ", defaultLimitPerPage=" + this.defaultLimitPerPage +
                '}';
    }
}
