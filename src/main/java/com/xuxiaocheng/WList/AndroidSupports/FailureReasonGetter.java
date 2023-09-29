package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import org.jetbrains.annotations.NotNull;

/**
 * @see VisibleFailureReason
 */
public final class FailureReasonGetter {
    public @NotNull FailureKind kind(final @NotNull VisibleFailureReason reason) {
        return reason.kind();
    }

    public @NotNull FileLocation location(final @NotNull VisibleFailureReason reason) {
        return reason.location();
    }

    public @NotNull String message(final @NotNull VisibleFailureReason reason) {
        return reason.message();
    }
}
