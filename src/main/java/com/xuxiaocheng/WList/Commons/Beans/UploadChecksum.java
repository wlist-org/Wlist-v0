package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.regex.Pattern;

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

    private static final @NotNull HMultiInitializers<@NotNull String, @NotNull Pattern> algorithms = new HMultiInitializers<>("ChecksumAlgorithms");

    public static final @NotNull String MD5 = "md5";
    public static final @NotNull String SHA256 = "sha256";
    static {
        UploadChecksum.registerAlgorithm(UploadChecksum.MD5, HMessageDigestHelper.MD5.pattern);
        UploadChecksum.registerAlgorithm(UploadChecksum.SHA256, HMessageDigestHelper.SHA256.pattern);
    }

    public static void requireRegisteredAlgorithm(final @NotNull String algorithm) {
        UploadChecksum.algorithms.requireInitialized(algorithm, null);
    }

    public static void registerAlgorithm(final @NotNull String algorithm, final @NotNull Pattern pattern) {
        UploadChecksum.algorithms.initialize(algorithm, pattern);
    }

    public static @NotNull Pattern getAlgorithmPattern(final @NotNull String algorithm) {
        return UploadChecksum.algorithms.getInstance(algorithm);
    }
}
