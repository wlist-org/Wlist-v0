package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @see DownloadConfirm
 */
public final class DownloadConfirmGetter {
    public boolean acceptedRange(final @NotNull DownloadConfirm confirm) {
        return confirm.acceptedRange();
    }

    public long downloadingSize(final @NotNull DownloadConfirm confirm) {
        return confirm.downloadingSize();
    }

    public @NotNull String id(final @NotNull DownloadConfirm confirm) {
        return confirm.id();
    }

    public static final class DownloadInformationGetter {
        public @NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> parallel(final DownloadConfirm.@NotNull DownloadInformation information) {
            return information.parallel();
        }

        public @Nullable ZonedDateTime expire(final DownloadConfirm.@NotNull DownloadInformation information) {
            return information.expire();
        }

        public static @NotNull String expireString(final DownloadConfirm.@NotNull DownloadInformation information, final @NotNull DateTimeFormatter formatter, final @Nullable String inf) {
            return information.expireString(formatter, inf);
        }
    }
}
