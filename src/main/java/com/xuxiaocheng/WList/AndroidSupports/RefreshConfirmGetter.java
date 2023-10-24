package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.RefreshConfirm;
import org.jetbrains.annotations.NotNull;

/**
 * @see RefreshConfirm
 */
public final class RefreshConfirmGetter {
    private RefreshConfirmGetter() {
        super();
    }

    public static @NotNull String id(final @NotNull RefreshConfirm confirm) {
        return confirm.id();
    }
}
