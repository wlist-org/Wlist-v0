package com.xuxiaocheng.WList.Server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Payload;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
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

    private static final @NotNull Algorithm sign = Algorithm.HMAC512("Server:SIGN***#WListServer%CreateBy@XXC#***TokenHMAC512&SIGN");
    private static final JWTVerifier verifier = JWT.require(UserTokenHelper.sign).withIssuer("WList").build();

    public static @NotNull String encodeToken(final @NotNull String username, final @NotNull LocalDateTime modifyTime) {
        return JWT.create().withAudience(username)
                .withJWTId(String.valueOf(modifyTime.toEpochSecond(ZoneOffset.UTC)))
                .withSubject(String.valueOf(modifyTime.getNano() / 1000))
                .withIssuer("WList")
                .withExpiresAt(LocalDateTime.now().plusSeconds(GlobalConfiguration.getInstance().getToken_expire_time()).toInstant(ZoneOffset.UTC))
                .sign(UserTokenHelper.sign);
    }

    private static @Nullable Pair.ImmutablePair<@NotNull String, @NotNull LocalDateTime> decodeToken(final @NotNull String token) {
        //noinspection OverlyBroadCatchBlock
        try {
            final Payload payload = UserTokenHelper.verifier.verify(token);
            return Pair.ImmutablePair.makeImmutablePair(payload.getAudience().get(0),
                    LocalDateTime.ofEpochSecond(Integer.valueOf(payload.getId()).intValue(),
                            Integer.valueOf(payload.getSubject()).intValue() * 1000, ZoneOffset.UTC));
        } catch (final RuntimeException ignore) {
            return null;
        }
    }

    @Deprecated
    public static @NotNull String generateToken(final @NotNull String username) throws SQLException, IllegalParametersException {
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(username);
        if (user == null)
            throw new IllegalParametersException("username", username);
        return UserTokenHelper.encodeToken(username, user.getC());
    }

    public static @Nullable Pair<@NotNull String, @NotNull SortedSet<Operation.@NotNull Permission>> resolveToken(final @NotNull String token) throws SQLException {
        final Pair<String, LocalDateTime> pair = UserTokenHelper.decodeToken(token);
        if (pair == null)
            return null;
        final Triad<String, SortedSet<Operation.Permission>, LocalDateTime> user = UserSqlHelper.selectUser(pair.getFirst());
        if (user == null || !user.getC().equals(pair.getSecond()))
            return null;
        return Pair.ImmutablePair.makeImmutablePair(pair.getFirst(), user.getB());
    }
}
