package com.xuxiaocheng.WListClientAndroid.Utils;

import android.view.View;
import android.widget.ListView;
import androidx.annotation.NonNull;

public final class ViewUtil {
    private ViewUtil() {
        super();
    }

    @NonNull public static View getViewByPosition(@NonNull final ListView listView, final int position) {
        final int first = listView.getFirstVisiblePosition();
        final int last = first + listView.getChildCount() - 1;
        if (position < first || last < position)
            return listView.getAdapter().getView(position, null, listView);
        return listView.getChildAt(position - first);
    }
}
