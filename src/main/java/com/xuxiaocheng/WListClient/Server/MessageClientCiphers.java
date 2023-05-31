package com.xuxiaocheng.WListClient.Server;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.IllegalFormatFlagsException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageClientCiphers extends MessageCiphers {
    protected final @NotNull AtomicBoolean initializingStage;

    public MessageClientCiphers(final int maxSize, final @NotNull AtomicBoolean initializingStage) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(maxSize);
        this.initializingStage = initializingStage;
        assert this.initializingStage.get();
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.initializingStage.get())
            throw new IllegalStateException("Uninitialized. Please wait.");
        super.encode(ctx, msg, out);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.initializingStage.get()) {
            if (!MessageCiphers.defaultHeader.equals(ByteBufIOUtil.readUTF(msg)))
                throw new IllegalFormatFlagsException("Header");
            final byte[] rsaModulus = ByteBufIOUtil.readByteArray(msg);
            final byte[] rsaExponent = ByteBufIOUtil.readByteArray(msg);
            final byte[] aesKey = new byte[128];
            HRandomHelper.DefaultSecureRandom.nextBytes(aesKey);
            try {
                final Key rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                        new BigInteger(rsaModulus), new BigInteger(rsaExponent)
                ));
                final Key key = new SecretKeySpec(aesKey, 80, 32, "AES");
                final AlgorithmParameterSpec vector = new IvParameterSpec(aesKey, 112, 16);
                this.aesDecryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
                this.aesEncryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
                final Cipher rsaEncryptCipher = Cipher.getInstance("RSA/ECB/NoPadding");
                rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
                ByteBufIOUtil.writeByteArray(buffer, rsaEncryptCipher.doFinal(aesKey));
                ByteBufIOUtil.writeByteArray(buffer, this.aesEncryptCipher.doFinal(
                        MessageCiphers.defaultTailor.getBytes(StandardCharsets.UTF_8)));
                ctx.writeAndFlush(buffer);
            } catch (final InvalidKeyException | InvalidKeySpecException |
                           InvalidAlgorithmParameterException exception) {
                throw new IllegalStateException(exception);
            } catch (final NoSuchAlgorithmException | NoSuchPaddingException |
                           IllegalBlockSizeException | BadPaddingException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            synchronized (this.initializingStage) {
                this.initializingStage.set(false);
                //noinspection NotifyWithoutCorrespondingWait
                this.initializingStage.notifyAll();
            }
            return;
        }
        super.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "maxSize=" + this.maxSize +
                ", initializingStage=" + this.initializingStage +
                ", super=" + super.toString() +
                '}';
    }
}
