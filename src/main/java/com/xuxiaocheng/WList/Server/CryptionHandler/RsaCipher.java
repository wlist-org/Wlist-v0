package com.xuxiaocheng.WList.Server.CryptionHandler;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.List;

class RsaCipher extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    protected final @NotNull Cipher decryptCipher;
    protected final @NotNull Cipher encryptCipher;

    protected RsaCipher(final @NotNull Key key) {
        super();
        try {
            this.decryptCipher = Cipher.getInstance("RSA");
            this.decryptCipher.init(Cipher.DECRYPT_MODE, key);
            this.encryptCipher = Cipher.getInstance("RSA");
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes() >> 1);
        try (final InputStream inputStream = new ByteBufInputStream(msg)) {
            try (final OutputStream outputStream = new CipherOutputStream(new ByteBufOutputStream(buffer), this.decryptCipher)) {
                inputStream.transferTo(outputStream);
            }
        }
        out.add(buffer);
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes() << 1);
        try (final OutputStream outputStream = new ByteBufOutputStream(buffer)) {
            try (final InputStream inputStream = new CipherInputStream(new ByteBufInputStream(msg), this.encryptCipher)) {
                inputStream.transferTo(outputStream);
            }
        }
        out.add(buffer);
    }

    @Override
    public @NotNull String toString() {
        return "RsaCipher{" + super.toString() + '}';
    }
}
