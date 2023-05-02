package com.xuxiaocheng.WList.Server.CryptionHandler;

import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CipherForClient extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private @Nullable AesCipher cipher = null;
    private boolean allow = false;
    private final @NotNull AtomicBoolean uninitialized = new AtomicBoolean(true);

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws IOException {
        if (this.cipher == null) {
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, "WList/Cipher");
            this.allow = true;
            ctx.channel().writeAndFlush(buffer);
        }
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException, InterruptedException {
        if (this.cipher == null) {
            if (this.allow) {
                msg.retain();
                out.add(msg);
                this.allow = false;
                return;
            }
            synchronized (this.uninitialized) {
                while (this.uninitialized.get())
                    this.uninitialized.wait();
            }
        }
        this.cipher.encode(ctx, msg, out);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<Object> out) throws IOException {
        if (this.cipher == null) {
            assert this.uninitialized.get();
            final byte[] encoded = ByteBufIOUtil.readByteArray(msg);
            final RSAPublicKey key;
            try {
                key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
            } catch (final InvalidKeySpecException | NoSuchAlgorithmException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
            this.cipher = new AesCipher(key);
            synchronized (this.uninitialized) {
                this.uninitialized.set(false);
                this.uninitialized.notifyAll();
            }
            ctx.fireChannelActive();
        } else
            this.cipher.decode(ctx, msg, out);
    }

    @Override
    public @NotNull String toString() {
        return "CipherForClient{" +
                "cipher=" + this.cipher +
                "} (" + super.toString() + ')';
    }
}
