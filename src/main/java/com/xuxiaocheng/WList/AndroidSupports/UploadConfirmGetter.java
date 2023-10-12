package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * @see UploadConfirm
 */
public final class UploadConfirmGetter {
    private UploadConfirmGetter() {
        super();
    }

    public static @NotNull @Unmodifiable List<@NotNull UploadChecksum> checksums(final @NotNull UploadConfirm confirm) {
        return confirm.checksums();
    }

    public static @NotNull String id(final @NotNull UploadConfirm confirm) {
        return confirm.id();
    }

    /**
     * @see UploadConfirm.UploadInformation
     */
    public static final class UploadInformationGetter {
        private UploadInformationGetter() {
            super();
        }

        public static @NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> parallel(final UploadConfirm.@NotNull UploadInformation information) {
            return information.parallel();
        }
    }
}
