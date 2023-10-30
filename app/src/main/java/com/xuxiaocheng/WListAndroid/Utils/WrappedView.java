package com.xuxiaocheng.WListAndroid.Utils;

import android.content.Context;
import android.view.View;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;

public final class WrappedView extends ConstraintLayout {
    public WrappedView(final @NotNull Context context, final boolean setParams) {
        super(context);
        this.setParams = setParams;
    }

    private final boolean setParams;
    private final @NotNull HInitializer<View> view = new HInitializer<>("WrappedView");

    public static final LayoutParams MatchConstraintParentParams = new LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.MATCH_CONSTRAINT); static {
        WrappedView.MatchConstraintParentParams.bottomToBottom = LayoutParams.PARENT_ID;
        WrappedView.MatchConstraintParentParams.leftToLeft = LayoutParams.PARENT_ID;
        WrappedView.MatchConstraintParentParams.rightToRight = LayoutParams.PARENT_ID;
        WrappedView.MatchConstraintParentParams.topToTop = LayoutParams.PARENT_ID;
    }

    @UiThread
    public void setView(final @NotNull View view) {
        this.removeAllViews();
        this.view.reinitialize(view);
        if (this.setParams && view.getLayoutParams() != null)
            this.setLayoutParams(view.getLayoutParams());
        this.addView(view, WrappedView.MatchConstraintParentParams);
    }

    @UiThread
    public void removeView() {
        this.removeAllViews();
        this.view.uninitializeNullable();
    }

    @AnyThread
    public @NotNull View getView() {
        return this.view.getInstance();
    }

    @Override
    public @NotNull String toString() {
        return "WrappedView{" +
                "view=" + this.view +
                '}';
    }
}
