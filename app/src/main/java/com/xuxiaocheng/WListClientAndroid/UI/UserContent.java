package com.xuxiaocheng.WListClientAndroid.UI;

import android.view.View;
import androidx.annotation.NonNull;

public final class UserContent {
    private UserContent() {
        super();
    }

    @NonNull
    public static View onChange(@NonNull final MainActivity activity) {
        return new View(activity);
    }
}
