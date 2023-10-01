package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record UploadChecksum(long start, long end, @NotNull String algorithm) {
    public static @NotNull UploadChecksum parse(final @NotNull ByteBuf buffer) throws IOException {
        final long start = ByteBufIOUtil.readVariableLenLong(buffer);
        final long end = ByteBufIOUtil.readVariableLenLong(buffer);
        final String algorithm = ByteBufIOUtil.readUTF(buffer);
        return new UploadChecksum(start, end, algorithm);
    }

    @Contract("_ -> param1")
    public @NotNull ByteBuf dump(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.start);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.end);
        ByteBufIOUtil.writeUTF(buffer, this.algorithm);
        return buffer;
    }

    private static final @NotNull HMultiInitializers<@NotNull String, HMessageDigestHelper.@NotNull MessageDigestAlgorithm> algorithms = new HMultiInitializers<>("ChecksumAlgorithms");

    public static final @NotNull String MD2 = "md2";
    public static final @NotNull String MD5 = "md5";
    public static final @NotNull String SHA1 = "sha1";
    public static final @NotNull String SHA224 = "sha224";
    public static final @NotNull String SHA256 = "sha256";
    public static final @NotNull String SHA384 = "sha384";
    public static final @NotNull String SHA512 = "sha512";
    public static final @NotNull String CRC32 = "crc32b";
    public static final @NotNull String ADLER32 = "adler32";
    static {
        UploadChecksum.registerAlgorithm(UploadChecksum.MD2, HMessageDigestHelper.MD2);
        UploadChecksum.registerAlgorithm(UploadChecksum.MD5, HMessageDigestHelper.MD5);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA1, HMessageDigestHelper.SHA1);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA224, HMessageDigestHelper.SHA224);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA256, HMessageDigestHelper.SHA256);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA384, HMessageDigestHelper.SHA384);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA512, HMessageDigestHelper.SHA512);
        UploadChecksum.registerAlgorithm(UploadChecksum.CRC32, HMessageDigestHelper.CRC32);
        UploadChecksum.registerAlgorithm(UploadChecksum.ADLER32, HMessageDigestHelper.ADLER32);
    }

    public static void requireRegisteredAlgorithm(final @NotNull String algorithm) {
        UploadChecksum.algorithms.requireInitialized(algorithm, null);
    }

    public static void registerAlgorithm(final @NotNull String algorithm, final HMessageDigestHelper.@NotNull MessageDigestAlgorithm pattern) {
        UploadChecksum.algorithms.initialize(algorithm, pattern);
    }

    public static HMessageDigestHelper.@NotNull MessageDigestAlgorithm getAlgorithm(final @NotNull String algorithm) {
        return UploadChecksum.algorithms.getInstance(algorithm);
    }
}
