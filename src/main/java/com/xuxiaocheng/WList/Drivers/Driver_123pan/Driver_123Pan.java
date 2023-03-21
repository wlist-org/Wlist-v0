package com.xuxiaocheng.WList.Drivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Drivers.HttpUtil;
import com.xuxiaocheng.WList.Internal.Drives.DrivePath;
import com.xuxiaocheng.WList.Internal.Drives.Driver;
import com.xuxiaocheng.WList.Internal.Drives.Exceptions.WrongResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Driver_123Pan implements Driver<DriverConfiguration_123Pan> {
    private static final @NotNull HLog logger = HLog.getInstance("DefaultLogger");

    @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    public @NotNull DriverConfiguration_123Pan login(final @Nullable DriverConfiguration_123Pan info) throws IOException {
        final DriverConfiguration_123Pan config = Objects.requireNonNullElseGet(info, DriverConfiguration_123Pan::new);
        final long time = System.currentTimeMillis();
        if (config.tokenExpire >= time && config.token != null) {
            this.configuration = config;
            return config;
        }
        final JSONObject data;
        if (config.refreshExpire >= time && config.token != null) {
            data = DriverUtil_123pan.postHttpData(DriverUtil_123pan.RefreshTokenURL, config.token, null);
            Driver_123Pan.logger.log(HLogLevel.DEBUG, "Refresh token: ", data);
        } else {
            final Map<String, String> property = new HashMap<>(1);
            property.put("Content-Type", "application/json;charset=utf-8");
            final JSONObject request = new JSONObject(4);
            request.put("passport", config.passport);
            request.put("password", config.password);
            request.put("remember", false);
            request.put("type", config.passport.contains("@") ? 2 : 1);
            final JSONObject json = HttpUtil.sendJsonHttp((HttpURLConnection) DriverUtil_123pan.LoginURL.openConnection(), "POST", property, request);
            final int code = json.getIntValue("code", -1);
            final String message = json.getString("message");
            if (code != 200 || !"success".equals(message))
                throw new WrongResponseException(code, message);
            data = json.getJSONObject("data");
            Driver_123Pan.logger.log(HLogLevel.DEBUG, "Login in: ", data);
        }
        config.token = data.getString("token");
        if (config.token == null)
            throw new WrongResponseException("No token response.");
        //noinspection SpellCheckingInspection
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final StringBuilder t = new StringBuilder(data.getString("expire"));
        try {
            config.tokenExpire = dateFormat.parse(t.deleteCharAt(t.lastIndexOf(":")).toString()).getTime();
        } catch (final ParseException exception) {
            throw new WrongResponseException("Invalid expire time.", exception);
        }
        config.refreshExpire = data.getLongValue("refresh_token_expire_time") * 1000;
        this.configuration = config;
        return config;
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull String>> list(@NotNull final DrivePath path, final int page, final int limit) {
        final long id = DriverUtil_123pan.getDirectoryId(path, true, this);
// TODO list
        return Pair.ImmutablePair.makeImmutablePair(0L, List.of());
    }

    @Override
    public @Nullable Long size(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String download(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String mkdirs(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String upload(@NotNull final DrivePath path, @NotNull final InputStream file) {
        return null;
    }

    @Override
    public void delete(@NotNull final DrivePath path) {

    }

    @Override
    public void rmdir(@NotNull final DrivePath path) {

    }

    @Override
    public @Nullable String copy(@NotNull final DrivePath source, @NotNull final DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String move(@NotNull final DrivePath source, @NotNull final DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String rename(@NotNull final DrivePath source, @NotNull final String name) {
        return null;
    }
}
