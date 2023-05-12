package com.xuxiaocheng.WListClient.Utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.List;

public class AesCipher extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    protected final @NotNull Cipher decryptCipher;
    protected final @NotNull Cipher encryptCipher;

    public AesCipher(final BigInteger keySeed, final BigInteger vectorSeed) {
        super();
        final byte[] keys = new byte[32];
        MiscellaneousUtil.generateRandomByteArray(keySeed, keys);
        final Key key = new SecretKeySpec(keys, "AES");
        final byte[] vectors = new byte[16];
        MiscellaneousUtil.generateRandomByteArray(vectorSeed, vectors);
        final AlgorithmParameterSpec vector = new IvParameterSpec(vectors);
        try {
            this.decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.decryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
            this.encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                       InvalidAlgorithmParameterException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes() + 128);
        try (final InputStream inputStream = new CipherInputStream(new ByteBufInputStream(msg), this.encryptCipher)) {
            try (final OutputStream outputStream = new ByteBufOutputStream(buffer)) {
                inputStream.transferTo(outputStream);
            }
        }
        out.add(buffer);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes());
        try (final InputStream inputStream = new ByteBufInputStream(msg)) {
            try (final OutputStream outputStream = new CipherOutputStream(new ByteBufOutputStream(buffer), this.decryptCipher)) {
                inputStream.transferTo(outputStream);
            }
        }
        out.add(buffer);
    }

    @Override
    public @NotNull String toString() {
        return "AesCipher{" + super.toString() + '}';
    }
}
