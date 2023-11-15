package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import org.jetbrains.annotations.NotNull;

/**
 * @see VisibleFailureReason
 */
public final class FailureReasonGetter {
    private FailureReasonGetter() {
        super();
    }

    public static @NotNull FailureKind kind(final @NotNull VisibleFailureReason reason) {
        return reason.kind();
    }

    public static @NotNull FileLocation location(final @NotNull VisibleFailureReason reason) {
        return reason.location();
    }

    public static @NotNull String message(final @NotNull VisibleFailureReason reason) {
        return reason.message();
    }

    public static boolean equals(final @NotNull VisibleFailureReason a, final @NotNull VisibleFailureReason b) {
        return a.equals(b);
    }

    public static int hashCode(final @NotNull VisibleFailureReason reason) {
        return reason.hashCode();
    }

    public static @NotNull String toString(final @NotNull VisibleFailureReason reason) {
        return reason.toString();
    }
}
