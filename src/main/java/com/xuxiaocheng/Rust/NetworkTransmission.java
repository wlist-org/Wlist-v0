package com.xuxiaocheng.Rust;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.Buffer;
import java.nio.ByteBuffer;

@SuppressWarnings("NativeMethod")
public final class NetworkTransmission {
    private NetworkTransmission() {
        super();
    }

    private static native void initialize();
    static {
        NativeUtil.load("network_transmission");
        NetworkTransmission.initialize();
    }

    public static void load() { // Just call and load native library.
    }

    private static native String getCipherVersion();
    public static final @NotNull String CipherVersion = NetworkTransmission.getCipherVersion();
    public static final int FileTransferBufferSize = 4 << 20;
    public static final int MaxSizePerPacket = (1 << 10) + NetworkTransmission.FileTransferBufferSize;

    @SuppressWarnings("ClassHasNoToStringMethod")
    public static final class RsaPrivateKey {
        private final ByteBuffer[] keys;
        private RsaPrivateKey(final ByteBuffer[] keys) {
            super();
            this.keys = keys;
        }
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public static final class AesKeyPair {
        private final ByteBuffer key;
        private ByteBuffer nonce;
        private AesKeyPair(final ByteBuffer key, final ByteBuffer nonce) {
            super();
            this.key = key;
            this.nonce = nonce;
        }
    }

    private static native Object[] clientStart0();
    public static Pair.@NotNull ImmutablePair<@NotNull RsaPrivateKey, @NotNull ByteBuf> clientStart() {
        final Object[] array = NetworkTransmission.clientStart0();
        assert array.length == 2; // (key, request)
        assert array[1] instanceof ByteBuffer;
        return Pair.ImmutablePair.makeImmutablePair(new RsaPrivateKey((ByteBuffer[]) array[0]), Unpooled.wrappedBuffer((ByteBuffer) array[1]));
    }

    private static native ByteBuffer[] serverStart0(final @NotNull ByteBuffer request, final @NotNull String application);
    public static Pair.@NotNull ImmutablePair<@NotNull ByteBuf, @Nullable AesKeyPair> serverStart(final @NotNull ByteBuf request, final @NotNull String application) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(request);
        final ByteBuffer[] array;
        try {
            array = NetworkTransmission.serverStart0(directByteBuffer.getFirst(), application);
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        assert array.length == 3; // (response, key, value)
        final ByteBuf response = Unpooled.wrappedBuffer(array[0]);
        if (array[1] == null || array[2] == null)
            return Pair.ImmutablePair.makeImmutablePair(response, null);
        assert array[1].isDirect() && array[2].isDirect();
        return Pair.ImmutablePair.makeImmutablePair(response, new AesKeyPair(array[1], array[2]));
    }

    private static native Object[] clientCheck0(final @NotNull ByteBuffer[] key, final @NotNull ByteBuffer response, final @NotNull String application);
    public static @Nullable UnionPair<AesKeyPair, UnionPair<String, String>> clientCheck(final NetworkTransmission.@NotNull RsaPrivateKey key, final @NotNull ByteBuf response, final @NotNull String application) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(response);
        final Object[] array;
        try {
            array = NetworkTransmission.clientCheck0(key.keys, directByteBuffer.getFirst(), application);
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        if (array == null)
            return null;
        assert array.length == 2; // (key, nonce) / (application, null) / (null, version)
        if (array[0] == null) {
            assert array[1] instanceof String;
            return UnionPair.fail(UnionPair.fail((String) array[1]));
        }
        if (array[1] == null) {
            assert array[0] instanceof String;
            return UnionPair.fail(UnionPair.ok((String) array[0]));
        }
        assert array[0] instanceof ByteBuffer && ((Buffer) array[0]).isDirect();
        assert array[1] instanceof ByteBuffer && ((Buffer) array[1]).isDirect();
        return UnionPair.ok(new AesKeyPair((ByteBuffer) array[0], (ByteBuffer) array[1]));
    }


    private static native @Nullable ByteBuffer clientEncrypt0(final @NotNull ByteBuffer key, final @NotNull ByteBuffer nonce, final @NotNull ByteBuffer message);
    public static @Nullable ByteBuf clientEncrypt(final @NotNull AesKeyPair key, final @NotNull ByteBuf message) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(message);
        final ByteBuffer encrypted;
        try {
            encrypted = NetworkTransmission.clientEncrypt0(key.key, key.nonce, directByteBuffer.getFirst());
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        if (encrypted == null)
            return null;
        return Unpooled.wrappedBuffer(encrypted);
    }

    private static native @Nullable ByteBuffer serverDecrypt0(final @NotNull ByteBuffer key, final @NotNull ByteBuffer nonce, final @NotNull ByteBuffer message);
    public static @Nullable ByteBuf serverDecrypt(final @NotNull AesKeyPair key, final @NotNull ByteBuf message) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(message);
        final ByteBuffer decrypted;
        try {
            decrypted = NetworkTransmission.serverDecrypt0(key.key, key.nonce, directByteBuffer.getFirst());
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        if (decrypted == null)
            return null;
        return Unpooled.wrappedBuffer(decrypted);
    }

    private static native @Nullable ByteBuffer[] serverEncrypt0(final @NotNull ByteBuffer key, final @NotNull ByteBuffer nonce, final @NotNull ByteBuffer message);
    public static @Nullable ByteBuf serverEncrypt(final @NotNull AesKeyPair key, final @NotNull ByteBuf message) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(message);
        final ByteBuffer[] encrypted;
        try {
            encrypted = NetworkTransmission.serverEncrypt0(key.key, key.nonce, directByteBuffer.getFirst());
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        if (encrypted == null)
            return null;
        assert encrypted.length == 2; // (response, new_nonce)
        assert encrypted[1].isDirect();
        key.nonce = encrypted[1];
        return Unpooled.wrappedBuffer(encrypted[0]);
    }

    private static native @Nullable ByteBuffer[] clientDecrypt0(final @NotNull ByteBuffer key, final @NotNull ByteBuffer nonce, final @NotNull ByteBuffer message);
    public static @Nullable ByteBuf clientDecrypt(final @NotNull AesKeyPair key, final @NotNull ByteBuf message) {
        final Pair.ImmutablePair<ByteBuffer, ByteBuf> directByteBuffer = NetworkTransmission.toDirectByteBuffer(message);
        final ByteBuffer[] encrypted;
        try {
            encrypted = NetworkTransmission.clientDecrypt0(key.key, key.nonce, directByteBuffer.getFirst());
        } finally {
            if (directByteBuffer.getSecond() != null)
                directByteBuffer.getSecond().release();
        }
        if (encrypted == null)
            return null;
        assert encrypted.length == 2; // (response, new_nonce)
        assert encrypted[1].isDirect();
        key.nonce = encrypted[1];
        return Unpooled.wrappedBuffer(encrypted[0]);
    }


    private static Pair.@NotNull ImmutablePair<@NotNull ByteBuffer, @Nullable ByteBuf> toDirectByteBuffer(final @NotNull ByteBuf buffer) {
        if (buffer.nioBufferCount() == 1 && buffer.nioBuffer().isDirect())
            return Pair.ImmutablePair.makeImmutablePair(buffer.nioBuffer(), null);
        final ByteBuf tmp = ByteBufAllocator.DEFAULT.directBuffer(buffer.readableBytes(), buffer.readableBytes());
        tmp.writeBytes(buffer);
        return Pair.ImmutablePair.makeImmutablePair(tmp.nioBuffer(), tmp);
    }
}
