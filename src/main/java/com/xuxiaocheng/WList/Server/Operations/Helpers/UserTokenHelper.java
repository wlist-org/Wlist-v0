package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Payload;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Server.Databases.Constant.ConstantManager;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class UserTokenHelper {
    private UserTokenHelper() {
        super();
    }

    private static final @NotNull Algorithm sign = Algorithm.HMAC512(HExceptionWrapper.wrapSupplier(() -> ConstantManager.get("TokenHMAC",
            () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 128, HRandomHelper.AnyWords), "initialize")).get());
    private static final JWTCreator.Builder builder = JWT.create().withIssuer("WList");
    private static final JWTVerifier verifier = JWT.require(UserTokenHelper.sign).withIssuer("WList").build();
    private static final @NotNull String constPrefix = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9."; // {"typ":"JWT","alg":"HS512"}

    public static @NotNull String encodeToken(final long id, final @NotNull LocalDateTime modifyTime) {
        return UserTokenHelper.builder.withAudience(Long.toString(id, Character.MAX_RADIX))
                .withJWTId(String.valueOf(modifyTime.toEpochSecond(ZoneOffset.UTC)))
                .withSubject(String.valueOf(modifyTime.getNano()))
                .withExpiresAt(LocalDateTime.now().plusSeconds(ServerConfiguration.get().tokenExpireTime()).toInstant(ZoneOffset.UTC))
                .sign(UserTokenHelper.sign).substring(UserTokenHelper.constPrefix.length());
    }

    public static @Nullable UserInformation decodeToken(final @NotNull String token) throws SQLException {
        final long id;
        final LocalDateTime modifyTime;
        try {
            final Payload payload = UserTokenHelper.verifier.verify(UserTokenHelper.constPrefix + token);
            if (LocalDateTime.now().isAfter(LocalDateTime.ofInstant(payload.getExpiresAtAsInstant(), ZoneOffset.UTC)))
                return null;
            id = Long.valueOf(payload.getAudience().get(0), Character.MAX_RADIX).longValue();
            modifyTime = LocalDateTime.ofEpochSecond(Integer.valueOf(payload.getId()).intValue(),
                            Integer.valueOf(payload.getSubject()).intValue(), ZoneOffset.UTC);
        } catch (final JWTVerificationException | NumberFormatException ignore) {
            return null;
        }
        final UserInformation user = UserManager.selectUser(id, null);
        if (user == null || !user.modifyTime().equals(modifyTime))
            return null;
        return user;
    }
}
