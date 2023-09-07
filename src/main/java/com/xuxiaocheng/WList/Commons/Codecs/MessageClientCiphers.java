package com.xuxiaocheng.WList.Commons.Codecs;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MessageClientCiphers extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private static final @NotNull String application = "WList@operation=0.2";

    protected NetworkTransmission.RsaPrivateKey rsaPrivateKey;
    protected NetworkTransmission.AesKeyPair aesKeyPair;
    protected final @NotNull AtomicBoolean initialized;
    protected final @NotNull AtomicReference<Throwable> error;

    public MessageClientCiphers(final @NotNull AtomicBoolean initialized, final @NotNull AtomicReference<@Nullable Throwable> error) {
        super();
        this.initialized = initialized;
        assert !this.initialized.get();
        this.error = error;
        assert this.error.get() == null;
    }

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) {
        final Pair.ImmutablePair<NetworkTransmission.RsaPrivateKey, ByteBuf> pair = NetworkTransmission.clientStart();
        this.rsaPrivateKey = pair.getFirst();
        ctx.writeAndFlush(pair.getSecond());
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<@NotNull Object> out) {
        if (!this.initialized.get())
            throw new IllegalStateException("Uninitialized. Please wait.");
        assert this.aesKeyPair != null;
        final ByteBuf encrypted = NetworkTransmission.clientEncrypt(this.aesKeyPair, msg);
        if (encrypted == null)
            throw new IllegalStateException("Something went wrong when client encrypted message.");
        HLog.getInstance("ClientLogger").log(HLogLevel.VERBOSE, "Write.",
                ParametersMap.create().add("length", msg.readableBytes()).add("network", encrypted.readableBytes()));
        out.add(encrypted);
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf msg, final @NotNull List<@NotNull Object> out) {
        if (!this.initialized.get()) {
            try {
                assert this.rsaPrivateKey != null;
                final UnionPair<NetworkTransmission.AesKeyPair, UnionPair<String, String>> pair = NetworkTransmission.clientCheck(this.rsaPrivateKey, msg, MessageClientCiphers.application);
                if (pair == null)
                    throw new IllegalTargetServerException();
                if (pair.isFailure())
                    if (pair.getE().isFailure())
                        throw new IllegalServerVersionException(NetworkTransmission.CipherVersion, pair.getE().getE());
                    else
                        throw new IllegalServerApplicationException(MessageClientCiphers.application, pair.getE().getT());
                this.rsaPrivateKey = null;
                this.aesKeyPair = pair.getT();
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
                this.error.set(throwable);
                ctx.close();
            }
            synchronized (this.initialized) {
                this.initialized.set(true);
                //noinspection NotifyWithoutCorrespondingWait
                this.initialized.notifyAll();
            }
            if (this.error.get() != null)
                throw new RuntimeException("Illegal target server.", this.error.get());
            return;
        }
        assert this.aesKeyPair != null;
        final ByteBuf decrypted = NetworkTransmission.clientDecrypt(this.aesKeyPair, msg);
        if (decrypted == null)
            throw new IllegalStateException("Something went wrong when client decrypted message.");
        HLog.getInstance("ClientLogger").log(HLogLevel.VERBOSE, "Read.",
                ParametersMap.create().add("length", decrypted.readableBytes()).add("network", msg.readableBytes()));
        out.add(decrypted);
    }

    @Override
    public @NotNull String toString() {
        return "MessageClientCiphers{" +
                "rsaPrivateKey=" + this.rsaPrivateKey +
                ", aesKeyPair=" + this.aesKeyPair +
                ", initialized=" + this.initialized +
                ", error=" + this.error +
                '}';
    }

    public static class IllegalTargetServerException extends Exception {
        @Serial
        private static final long serialVersionUID = -8608659258952829017L;

        public IllegalTargetServerException() {
            super("Illegal target server.");
        }

        public IllegalTargetServerException(final @NotNull String message) {
            super(message);
        }
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public static class IllegalServerVersionException extends IllegalTargetServerException {
        @Serial
        private static final long serialVersionUID = -6809923542159147535L;

        protected final @NotNull String excepted;
        protected final @NotNull String received;

        public IllegalServerVersionException(final @NotNull String excepted, final @NotNull String received) {
            super("Illegal server cipher version." + ParametersMap.create().add("excepted", excepted).add("received", received));
            this.excepted = excepted;
            this.received = received;
        }
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public static class IllegalServerApplicationException extends IllegalTargetServerException {
        @Serial
        private static final long serialVersionUID = 1933643231812966622L;

        protected final @NotNull String excepted;
        protected final @NotNull String received;

        public IllegalServerApplicationException(final @NotNull String excepted, final @NotNull String received) {
            super("Illegal server application." + ParametersMap.create().add("excepted", excepted).add("received", received));
            this.excepted = excepted;
            this.received = received;
        }
    }
}
