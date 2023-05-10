package com.xuxiaocheng.WList.Server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Payload;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.SortedSet;

public final class UserTokenHelper {
    private UserTokenHelper() {
        super();
    }

    private static final @NotNull Algorithm sign = Algorithm.HMAC512("User: Server:SIGN***#WListServer%CreateBy@XXC#***TokenHMAC512&SIGN");
    private static final JWTCreator.Builder builder = JWT.create().withIssuer("WList");
    private static final JWTVerifier verifier = JWT.require(UserTokenHelper.sign).withIssuer("WList").build();

    public static @NotNull String encodeToken(final @NotNull String username, final @NotNull LocalDateTime modifyTime) {
        return UserTokenHelper.builder.withAudience(username)
                .withJWTId(String.valueOf(modifyTime.toEpochSecond(ZoneOffset.UTC)))
                .withSubject(String.valueOf(modifyTime.getNano()))
                .withExpiresAt(LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().tokenExpireTime()).toInstant(ZoneOffset.UTC))
                .sign(UserTokenHelper.sign);
    }

    public static Triad.@Nullable ImmutableTriad<@NotNull String, @NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> decodeToken(final @NotNull String token) throws SQLException {
        final Pair.ImmutablePair<String, LocalDateTime> pair;
        //noinspection OverlyBroadCatchBlock
        try {
            final Payload payload = UserTokenHelper.verifier.verify(token);
            if (LocalDateTime.now().isAfter(LocalDateTime.ofInstant(payload.getExpiresAtAsInstant(), ZoneOffset.UTC)))
                return null;
            pair = Pair.ImmutablePair.makeImmutablePair(payload.getAudience().get(0),
                    LocalDateTime.ofEpochSecond(Integer.valueOf(payload.getId()).intValue(),
                            Integer.valueOf(payload.getSubject()).intValue(), ZoneOffset.UTC));
        } catch (final RuntimeException ignore) {
            return null;
        }
        final Triad.ImmutableTriad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(pair.getFirst());
        if (user == null || !user.getC().equals(pair.getSecond()))
            return null;
        return Triad.ImmutableTriad.makeImmutableTriad(pair.getFirst(), user.getA(),  user.getB());
    }
}
