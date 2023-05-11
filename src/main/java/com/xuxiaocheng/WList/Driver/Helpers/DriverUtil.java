package com.xuxiaocheng.WList.Driver.Helpers;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DriverUtil {
    private DriverUtil() {
        super();
    }

    public static final @NotNull Predicate<String> tagPredication = s -> MiscellaneousUtil.md5Pattern.matcher(s).matches();

    public static final @NotNull Pattern phoneNumberPattern = Pattern.compile("^1([38][0-9]|4[579]|5[0-3,5-9]|66|7[0135678]|9[89])\\d{8}$");
    public static final @NotNull Pattern mailAddressPattern = Pattern.compile("^\\w+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){1,2}$");

    public static @NotNull InputStream getDownloadStream(final @NotNull OkHttpClient client, final Pair.@NotNull ImmutablePair<String, String> url, final @Nullable Headers headers, final @Nullable Map<@NotNull String, @NotNull Object> body, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IOException {
        if (from >= to)
            return InputStream.nullInputStream();
        final InputStream link = DriverNetworkHelper.extraResponse(DriverNetworkHelper.sendRequestJson(client, url, headers, body)).byteStream();
        final long skip = link.skip(from);
        assert skip == from;
        return new InputStream() {
            private long pos = skip;

            @Override
            public int read() throws IOException {
                if (this.pos + 1 > to) {
                    link.close();
                    return -1;
                }
                ++this.pos;
                return link.read();
            }

            @Override
            public int read(final byte @NotNull [] b, final int off, final int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0)
                    return 0;
                if (this.pos + 1 > to) {
                    link.close();
                    return -1;
                }
                final int r = link.read(b, off, (int) Math.min(len, to - this.pos));
                this.pos += r;
                if (this.pos + 1 > to)
                    link.close();
                return r;
            }

            @Override
            public int available() {
                return (int) Math.min(to - this.pos, Integer.MAX_VALUE);
            }

            @Override
            public void close() throws IOException {
                link.close();
            }
        };
    }
}
