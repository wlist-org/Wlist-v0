package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
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
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class MessageCiphers extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    static final @NotNull String defaultHeader = "WList/Ciphers/Initializing";
    static final @NotNull String defaultTailor = "Checking";

    public static final byte doAes = 1;
    public static final byte defaultDoAes = MessageCiphers.doAes;
    public static final byte doGZip = 1 << 1;
    public static final byte defaultDoGZip = 0;

    public static final byte defaultCipher = MessageCiphers.defaultDoAes | MessageCiphers.defaultDoGZip;

    protected final int maxSize;
    protected final @NotNull Cipher aesDecryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    protected final @NotNull Cipher aesEncryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

    protected MessageCiphers(final int maxSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super();
        this.maxSize = maxSize;
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (msg.readableBytes() > this.maxSize)
            throw new IllegalArgumentException("Too long msg. len: " + msg.readableBytes());
        final byte flags = ByteBufIOUtil.readByte(msg);
        final boolean aes = (flags & MessageCiphers.doAes) > 0;
        final boolean gzip = (flags & MessageCiphers.doGZip) > 0;
        if (!aes && !gzip) {
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByte(prefix, flags);
            ByteBufIOUtil.writeVariableLenInt(prefix, msg.readableBytes());
            out.add(ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, msg.retain()));
            return;
        }
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes() + 128);
        ByteBufIOUtil.writeByte(buf, flags);
        ByteBufIOUtil.writeVariableLenInt(buf, msg.readableBytes());
        InputStream is = new ByteBufInputStream(msg);
        if (aes)
            is = new CipherInputStream(is, this.aesEncryptCipher);
        OutputStream os = new ByteBufOutputStream(buf);
        if (gzip)
            os = new GZIPOutputStream(os);
        try (final InputStream inputStream = is; final OutputStream outputStream = os) {
            inputStream.transferTo(outputStream);
        }
        out.add(buf);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        final byte flags = ByteBufIOUtil.readByte(msg);
        final int length = ByteBufIOUtil.readVariableLenInt(msg);
        if (length > this.maxSize)
            throw new IllegalArgumentException("Too long source msg. len: " + length);
        final boolean aes = (flags & MessageCiphers.doAes) > 0;
        final boolean gzip = (flags & MessageCiphers.doGZip) > 0;
        if (!aes && !gzip) {
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer(1).writeByte(flags);
            out.add(ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, msg.retain()));
            return;
        }
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(length + 1);
        ByteBufIOUtil.writeByte(buf, flags);
        InputStream is = new ByteBufInputStream(msg);
        if (gzip)
            is = new GZIPInputStream(is);
        OutputStream os = new ByteBufOutputStream(buf);
        // TODO: Random aes vector.
        if (aes)
            os = new CipherOutputStream(os, this.aesDecryptCipher);
        try (final InputStream inputStream = is; final OutputStream outputStream = os) {
            inputStream.transferTo(outputStream);
        }
        out.add(buf);
    }

    @Override
    public @NotNull String toString() {
        return "MessageServerCiphers{" +
                "maxSize=" + this.maxSize +
                ", super=" + super.toString() +
                '}';
    }
}
