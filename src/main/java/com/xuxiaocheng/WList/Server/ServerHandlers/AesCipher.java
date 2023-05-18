package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AesCipher extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    public static final byte doAes = 1;
    public static final byte defaultDoAes = AesCipher.doAes;
    public static final byte doGZip = 1 << 1;
    public static final byte defaultDoGZip = 0;

    public static final byte defaultCipher = AesCipher.defaultDoAes | AesCipher.defaultDoGZip;

    protected final @NotNull Cipher decryptCipher;
    protected final @NotNull Cipher encryptCipher;
    protected final int maxSize;

    public AesCipher(final BigInteger keySeed, final BigInteger vectorSeed, final int maxSize) {
        super();
        this.maxSize = maxSize;
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
        if (msg.readableBytes() > this.maxSize)
            throw new IllegalArgumentException("Too long msg. len: " + msg.readableBytes());
        final byte flags = ByteBufIOUtil.readByte(msg);
        final boolean aes = (flags & AesCipher.doAes) > 0;
        final boolean gzip = (flags & AesCipher.doGZip) > 0;
        if (!aes && !gzip) {
            final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByte(prefix, flags);
            ByteBufIOUtil.writeVariable2LenInt(prefix, msg.readableBytes());
            out.add(ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, msg.retain()));
            return;
        }
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(msg.readableBytes() + 128);
        ByteBufIOUtil.writeByte(buf, flags);
        ByteBufIOUtil.writeVariable2LenInt(buf, msg.readableBytes());
        InputStream is = new ByteBufInputStream(msg);
        if (aes)
            is = new CipherInputStream(is, this.encryptCipher);
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
        final byte flags = ByteBufIOUtil.readByte(msg); // FIXME: no exception thrown.
        final int length = ByteBufIOUtil.readVariable2LenInt(msg);
        if (length > this.maxSize)
            throw new IllegalArgumentException("Too long source msg. len: " + length);
        final boolean aes = (flags & AesCipher.doAes) > 0;
        final boolean gzip = (flags & AesCipher.doGZip) > 0;
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(length + 1);
        ByteBufIOUtil.writeByte(buf, flags);
        InputStream is = new ByteBufInputStream(msg);
        if (gzip)
            is = new GZIPInputStream(is);
        OutputStream os = new ByteBufOutputStream(buf);
        if (aes)
            os = new CipherOutputStream(os, this.decryptCipher);
        try (final InputStream inputStream = is; final OutputStream outputStream = os) {
            inputStream.transferTo(outputStream);
        }
        out.add(buf);
    }

    @Override
    public @NotNull String toString() {
        return "AesCipher{" +
                "maxSize=" + this.maxSize +
                ", super=" + super.toString() +
                '}';
    }
}
