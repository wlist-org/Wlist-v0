package com.xuxiaocheng.WListAndroid.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

@SuppressLint("ViewConstructor")
public final class StackWrappedView extends ConstraintLayout {
    public StackWrappedView(final @NotNull Context context, final long time, final boolean setParams) {
        super(context);
        this.time = time;
        this.setParams = setParams;
    }

    private final long time;
    private final boolean setParams;
    private final @NotNull Deque<View> view = new ArrayDeque<>();

    @UiThread
    public void push(final @NotNull View view) {
        if (this.setParams && view.getLayoutParams() != null)
            this.setLayoutParams(view.getLayoutParams());
        this.addView(view, WrappedView.MatchConstraintParentParams);
        final Animation animation = new AlphaAnimation(0.0F, 1.0F);
        animation.setDuration(this.time);
        view.startAnimation(animation);
        this.view.push(view);
    }

    @UiThread
    public void pop() {
        final View view = this.view.pop();
        final Animation animation = new AlphaAnimation(1.0F, 0.0F);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final @NotNull Animation animation) {
            }

            @Override
            public void onAnimationEnd(final @NotNull Animation animation) {
                StackWrappedView.this.removeView(view);
            }

            @Override
            public void onAnimationRepeat(final @NotNull Animation animation) {
            }
        });
        animation.setDuration(this.time);
        view.startAnimation(animation);
    }

    @Override
    public @NotNull String toString() {
        return "WrappedView{" +
                "view=" + this.view +
                '}';
    }
}
