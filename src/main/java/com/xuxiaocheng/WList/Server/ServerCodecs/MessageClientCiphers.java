package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageClientCiphers extends MessageCiphers {
    protected final @NotNull AtomicBoolean uninitialized;

    public MessageClientCiphers(final int maxSize, final @NotNull AtomicBoolean uninitialized) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(maxSize);
        this.uninitialized = uninitialized;
        assert this.uninitialized.get();
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.uninitialized.get())
            throw new IllegalStateException("Uninitialized. Please wait.");
        super.encode(ctx, msg, out);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.uninitialized.get()) {
            if (!MessageCiphers.defaultHeader.equals(ByteBufIOUtil.readUTF(msg)))
                throw new IllegalStateException();
            final byte[] rsaModulus = ByteBufIOUtil.readByteArray(msg);
            final byte[] rsaExponent = ByteBufIOUtil.readByteArray(msg);
            final byte[] aesKey = new byte[32];
            final byte[] aesVector = new byte[16];
            msg.readBytes(aesKey).readBytes(aesVector);
            try {
                final Key key = new SecretKeySpec(aesKey, "AES");
                final AlgorithmParameterSpec vector = new IvParameterSpec(aesVector);
                final Key publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                        new BigInteger(rsaModulus), new BigInteger(rsaExponent)
                ));
                this.aesDecryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
                this.aesEncryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
                this.rsaDecryptCipher.init(Cipher.DECRYPT_MODE, publicKey);
                this.rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            } catch (final InvalidKeyException | InvalidKeySpecException |
                           InvalidAlgorithmParameterException exception) {
                throw new IllegalStateException(exception);
            } catch (final NoSuchAlgorithmException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            synchronized (this.uninitialized) {
                this.uninitialized.set(false);
                //noinspection NotifyWithoutCorrespondingWait
                this.uninitialized.notifyAll();
            }
            return;
        }
        super.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "maxSize=" + this.maxSize +
                ", uninitialized=" + this.uninitialized +
                ", super=" + super.toString() +
                '}';
    }
}
