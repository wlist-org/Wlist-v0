package com.xuxiaocheng.WList.Server.ServerCodecs;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AIOStream;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class MessageCiphers extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    static final @NotNull String defaultHeader = "Ciphers v0.3.0 by xuxiaocheng";
    static final @NotNull String defaultTailor = "Verification: WList";

    public static final byte doAes = 1;
    public static final byte defaultDoAes = MessageCiphers.doAes;
    public static final byte doGZip = 1 << 1;
    public static final byte defaultDoGZip = 0;

    public static final byte defaultCipher = MessageCiphers.defaultDoAes | MessageCiphers.defaultDoGZip;

    protected final int maxSize;
    protected final @NotNull Cipher aesDecryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
    protected final @NotNull Cipher aesEncryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
    protected final byte[] keyArray = new byte[1024];
    protected int keyPosition = 0;
    protected int vectorPosition = 0;

    protected MessageCiphers(final int maxSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super();
        this.maxSize = maxSize;
    }

    protected void reinitializeAesCiphers(final boolean decrypt) {
        final Pair.ImmutablePair<byte[], Integer> keyArray = MiscellaneousUtil.getCircleBytes(this.keyArray, this.keyPosition, 16);
        final Key key = new SecretKeySpec(keyArray.getFirst(), keyArray.getSecond().intValue(), 16, "AES");
        final Pair.ImmutablePair<byte[], Integer> vectorArray = MiscellaneousUtil.getCircleBytes(this.keyArray, this.vectorPosition, 16);
        final AlgorithmParameterSpec vector = new GCMParameterSpec(128, vectorArray.getFirst(), vectorArray.getSecond().intValue(), 16);
        try {
            if (decrypt)
                this.aesDecryptCipher.init(Cipher.DECRYPT_MODE, key, vector);
            else
                this.aesEncryptCipher.init(Cipher.ENCRYPT_MODE, key, vector);
        } catch (final InvalidKeyException | InvalidAlgorithmParameterException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
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
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(buf, flags);
        ByteBufIOUtil.writeVariableLenInt(buf, msg.readableBytes());
        InputStream is = new ByteBufInputStream(msg);
        if (aes) {
            final int oldKey = this.keyPosition;
            while (this.keyPosition == oldKey)
                this.keyPosition = HRandomHelper.DefaultSecureRandom.nextInt(this.keyArray.length);
            final int oldVector = this.vectorPosition;
            while (this.vectorPosition == oldVector)
                this.vectorPosition = HRandomHelper.DefaultSecureRandom.nextInt(this.keyArray.length);
            ByteBufIOUtil.writeVariableLenInt(buf, this.keyPosition);
            ByteBufIOUtil.writeVariableLenInt(buf, this.vectorPosition);
            this.reinitializeAesCiphers(false);
            is = new CipherInputStream(is, this.aesEncryptCipher);
        }
        OutputStream os = new ByteBufOutputStream(buf);
        if (gzip)
            os = new GZIPOutputStream(os);
        try (final OutputStream outputStream = os) {
            try (final InputStream inputStream = is) {
                if (PlatformDependent.isAndroid())
                    AIOStream.transferTo(inputStream, outputStream);
                else
                    inputStream.transferTo(outputStream);
            }
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
        final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(length + 1, length + 1);
        ByteBufIOUtil.writeByte(buf, flags);
        OutputStream os = new ByteBufOutputStream(buf);
        if (aes) {
            this.keyPosition = ByteBufIOUtil.readVariableLenInt(msg);
            this.vectorPosition = ByteBufIOUtil.readVariableLenInt(msg);
            this.reinitializeAesCiphers(true);
            os = new CipherOutputStream(os, this.aesDecryptCipher);
        }
        InputStream is = new ByteBufInputStream(msg);
        if (gzip)
            is = new GZIPInputStream(is);
        try (final OutputStream outputStream = os) {
            try (final InputStream inputStream = is) {
                if (PlatformDependent.isAndroid())
                    AIOStream.transferTo(inputStream, outputStream);
                else
                    inputStream.transferTo(outputStream);
            }
        }
        out.add(buf);
    }

    @Override
    public @NotNull String toString() {
        return "MessageCiphers{" +
                "maxSize=" + this.maxSize +
                ", keyArray=" + Arrays.toString(this.keyArray) +
                ", keyPosition=" + this.keyPosition +
                ", vectorPosition=" + this.vectorPosition +
                ", super=" + super.toString() +
                '}';
    }
}
