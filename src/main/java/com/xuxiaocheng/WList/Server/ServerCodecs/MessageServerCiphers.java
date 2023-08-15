package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MessageServerCiphers extends MessageCiphers {
    protected final @NotNull AtomicReference<@Nullable Cipher> initializingStage =
            new AtomicReference<>(Cipher.getInstance("RSA/ECB/PKCS1Padding"));

    public MessageServerCiphers(final int maxSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(maxSize);
    }

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws IOException {
        final byte[] rsaModulus;
        final byte[] rsaExponent;
        final Cipher rsaDecryptCipher = this.initializingStage.get();
        if (rsaDecryptCipher == null)
            throw new IllegalStateException("Initialized.");
        try {
            final KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
            rsaGenerator.initialize(2048, HRandomHelper.DefaultSecureRandom);
            final KeyPair rsaKeys = rsaGenerator.generateKeyPair();
            rsaModulus = ((RSAKey) rsaKeys.getPublic()).getModulus().toByteArray();
            rsaExponent = ((RSAPublicKey) rsaKeys.getPublic()).getPublicExponent().toByteArray();
            rsaDecryptCipher.init(Cipher.DECRYPT_MODE, rsaKeys.getPrivate());
        } catch (final NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        final byte[] tempKey = new byte[this.keyArray.length >> 1];
        HRandomHelper.DefaultSecureRandom.nextBytes(tempKey);
        for (int i = 0; i < this.keyArray.length >> 1; ++i)
            this.keyArray[i << 1] = tempKey[i];
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, MessageCiphers.defaultHeader);
        ByteBufIOUtil.writeByteArray(buffer, rsaModulus);
        ByteBufIOUtil.writeByteArray(buffer, rsaExponent);
        buffer.writeBytes(tempKey);
        ctx.writeAndFlush(buffer);
    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) {
        if (this.initializingStage.get() == null)
            ctx.fireChannelInactive();
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final Cipher rsaDecryptCipher = this.initializingStage.get();
        if (rsaDecryptCipher != null) {
            try {
                for (int i = 0; i < 3; ++i) {
                    final byte[] tempKey = rsaDecryptCipher.doFinal(ByteBufIOUtil.readByteArray(msg));
                    if (tempKey.length != 117)
                        throw new IllegalStateException("Illegal key array length.");
                    for (int j = 0; j < 117; ++j)
                        this.keyArray[((j + 117 * i) << 1) + 1] = tempKey[j];
                }
                this.reinitializeAesCiphers(true);
                final byte[] tempKey = this.aesDecryptCipher.doFinal(ByteBufIOUtil.readByteArray(msg));
                if (tempKey.length != (this.keyArray.length >> 1) - 117 * 3)
                    throw new IllegalStateException();
                for (int i = 0; i < tempKey.length; ++i)
                    this.keyArray[((i + 117 * 3) << 1) + 1] = tempKey[i];
                this.vectorPosition += this.keyArray.length >> 1;
                this.reinitializeAesCiphers(true);
                final String tailor = new String(this.aesDecryptCipher.doFinal(ByteBufIOUtil.readByteArray(msg)), StandardCharsets.UTF_8);
                if (!MessageCiphers.defaultTailor.equals(tailor))
                    throw new IllegalStateException("Invalid tailor." + ParametersMap.create().add("expected", MessageCiphers.defaultTailor).add("real", tailor));
            } catch (final IllegalBlockSizeException | BadPaddingException exception) {
                throw new IOException(exception);
            }
            this.initializingStage.set(null);
            ctx.fireChannelActive();
            return;
        }
        super.decode(ctx, msg, out);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (this.initializingStage.get() == null)
            ctx.fireExceptionCaught(cause);
        else {
            HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Something wrong when codec at " + ctx.channel().remoteAddress(), ": ", cause.getLocalizedMessage());
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), cause.getCause());
        }
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "super=" + super.toString() +
                '}';
    }
}
