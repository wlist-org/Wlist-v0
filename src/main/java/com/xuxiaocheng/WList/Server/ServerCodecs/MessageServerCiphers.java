package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
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
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, MessageCiphers.defaultHeader);
        ByteBufIOUtil.writeByteArray(buffer, rsaModulus);
        ByteBufIOUtil.writeByteArray(buffer, rsaExponent);
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
                final byte[] aesKey = rsaDecryptCipher.doFinal(ByteBufIOUtil.readByteArray(msg));
                final Key key = new SecretKeySpec(aesKey, 50, 32, "AES");
                final AlgorithmParameterSpec vector = new IvParameterSpec(aesKey, 82, 16);
                this.aesDecryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
                this.aesEncryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
                if (!MessageCiphers.defaultTailor.equals(new String(this.aesDecryptCipher.doFinal(ByteBufIOUtil.readByteArray(msg)), StandardCharsets.UTF_8)))
                    throw new IllegalStateException("Invalid AES key.");
            } catch (final IllegalBlockSizeException | BadPaddingException |
                           InvalidKeyException | InvalidAlgorithmParameterException exception) {
                throw new IOException(exception);
            }
            ctx.fireChannelActive();
            this.initializingStage.set(null);
            return;
        }
        super.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "maxSize=" + this.maxSize +
                ", super=" + super.toString() +
                '}';
    }
}
