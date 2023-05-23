package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;

public class MessageServerCiphers extends MessageCiphers {
    protected final byte @NotNull [] rsaModulus;
    protected final byte @NotNull [] rsaExponent;
    protected final byte @NotNull [] aesKey = new byte[32];
    protected final byte @NotNull [] aesVector = new byte[16];

    protected final @NotNull Cipher rsaEncryptCipher = Cipher.getInstance("RSA");

    public MessageServerCiphers(final int maxSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(maxSize);
        final KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
        rsaGenerator.initialize(1024, (SecureRandom) HRandomHelper.DefaultSecureRandom);
        final KeyPair rsaKeys = rsaGenerator.generateKeyPair();
        this.rsaModulus = ((RSAKey) rsaKeys.getPublic()).getModulus().toByteArray();
        this.rsaExponent = ((RSAPublicKey) rsaKeys.getPublic()).getPublicExponent().toByteArray();
        HRandomHelper.DefaultSecureRandom.nextBytes(this.aesKey);
        HRandomHelper.DefaultSecureRandom.nextBytes(this.aesVector);
        try {
            final Key key = new SecretKeySpec(this.aesKey, "AES");
            final AlgorithmParameterSpec vector = new IvParameterSpec(this.aesVector);
            this.aesDecryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
            this.aesEncryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
            this.rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, rsaKeys.getPrivate());
        } catch (final InvalidKeyException | InvalidAlgorithmParameterException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, MessageCiphers.defaultHeader);
        ByteBufIOUtil.writeByteArray(buffer, this.rsaModulus);
        ByteBufIOUtil.writeByteArray(buffer, this.rsaExponent);
        try (final OutputStream stream = new CipherOutputStream(new ByteBufOutputStream(buffer), this.rsaEncryptCipher)) {
            stream.write(this.aesKey);
            stream.write(this.aesVector);
        }
        ctx.writeAndFlush(buffer);
        ctx.fireChannelActive();
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "maxSize=" + this.maxSize +
                ", super=" + super.toString() +
                '}';
    }
}
