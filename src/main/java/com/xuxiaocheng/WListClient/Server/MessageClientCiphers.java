package com.xuxiaocheng.WListClient.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
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
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
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
            final String header = ByteBufIOUtil.readUTF(msg);
            if (!MessageCiphers.defaultHeader.equals(header))
                throw new IllegalFormatFlagsException("Invalid header." + ParametersMap.create().add("excepted", MessageCiphers.defaultHeader).add("real", header));
            final Cipher rsaEncryptCipher;
            try {
                final byte[] rsaModulus = ByteBufIOUtil.readByteArray(msg);
                final byte[] rsaExponent = ByteBufIOUtil.readByteArray(msg);
                final Key rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                        new BigInteger(rsaModulus), new BigInteger(rsaExponent)
                ));
                rsaEncryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            } catch (final InvalidKeySpecException exception) {
                throw new IllegalStateException(exception);
            } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            final byte[] tempKey = new byte[this.keyArray.length >> 1];
            msg.readBytes(tempKey);
            for (int i = 0; i < this.keyArray.length >> 1; ++i)
                this.keyArray[i << 1] = tempKey[i];
            HRandomHelper.DefaultSecureRandom.nextBytes(tempKey);
            for (int i = 0; i < this.keyArray.length >> 1; ++i)
                this.keyArray[(i << 1) + 1] = tempKey[i];
            this.reinitializeAesCiphers(false);
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufIOUtil.writeByteArray(buffer, rsaEncryptCipher.doFinal(tempKey, 0, 117));
                ByteBufIOUtil.writeByteArray(buffer, rsaEncryptCipher.doFinal(tempKey, 117, 117));
                ByteBufIOUtil.writeByteArray(buffer, rsaEncryptCipher.doFinal(tempKey, 117 << 1, 117));
                ByteBufIOUtil.writeByteArray(buffer, this.aesEncryptCipher.doFinal(tempKey, 117 * 3, tempKey.length - 117 * 3));
                this.vectorPosition += this.keyArray.length >> 1;
                this.reinitializeAesCiphers(false);
                ByteBufIOUtil.writeByteArray(buffer, this.aesEncryptCipher.doFinal(MessageCiphers.defaultTailor.getBytes(StandardCharsets.UTF_8)));
            } catch (final IllegalBlockSizeException | BadPaddingException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            ctx.writeAndFlush(buffer);
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
        return "MessageClientCiphers{" +
                "super=" + super.toString() +
                '}';
    }
}
