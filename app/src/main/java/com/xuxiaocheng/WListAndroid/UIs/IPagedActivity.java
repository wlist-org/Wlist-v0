package com.xuxiaocheng.WListAndroid.UIs;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.databinding.ActivityBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

public abstract class IPagedActivity extends IActivity<ActivityBinding> {
    private final @NotNull IPage<?> mainPage;
    private final @Nullable String mainTag;

    protected IPagedActivity(final @NotNull IPage<?> page, final @Nullable String tag) {
        super();
        this.mainPage = page;
        this.mainTag = tag;
    }

    @Override
    protected @NotNull ActivityBinding iOnInflater() {
        return ActivityBinding.inflate(this.getLayoutInflater());
    }

    @Override
    protected void iOnBuildActivity(final @NotNull ActivityBinding content, final boolean isFirstTime) {
        super.iOnBuildActivity(content, isFirstTime);
        if (isFirstTime)
            this.getSupportFragmentManager().beginTransaction()
                    .add(this.content().activity.getId(), this.mainPage, this.mainTag)
                    .commitNow();
    }

    protected @Nullable ZonedDateTime lastPushPopTime = null;

    @Override
    protected boolean iOnBackPressed() {
        if (this.lastPushPopTime != null && Duration.between(this.lastPushPopTime, ZonedDateTime.now()).toMillis() < this.getResources().getInteger(R.integer.anim_slide_duration))
            return true; // Lock on anim.
        final IPage<?> page = this.currentPage();
        if (page != null && page.onBackPressed()) return true;
        return this.popNow() || super.iOnBackPressed();
    }

    @SuppressWarnings("unchecked")
    public <P extends IPage<?>> @Nullable P currentPage() {
        return (P) this.getSupportFragmentManager().findFragmentById(this.content().activity.getId());
    }

    @SuppressWarnings("unchecked")
    public <P extends IPage<?>> @Nullable P pageByTag(final @NotNull String tag) {
        return (P) this.getSupportFragmentManager().findFragmentByTag(tag);
    }

    @SuppressWarnings("unchecked")
    public @NotNull List<? extends @NotNull IPage<?>> existingPages() {
        return (List<IPage<?>>) (List<?>) this.getSupportFragmentManager().getFragments();
    }

    @AnyThread
    public void push(final @NotNull IPage<?> page, final @Nullable String tag) {
        this.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out, R.anim.slide_left_in, R.anim.slide_right_out)
                .add(this.content().activity.getId(), page, tag)
                .addToBackStack(null).commit();
        this.lastPushPopTime = ZonedDateTime.now();
    }

    @AnyThread
    public void pop() {
        this.getSupportFragmentManager().popBackStack();
        this.lastPushPopTime = ZonedDateTime.now();
    }

    @UiThread
    public boolean popNow() {
        final boolean popped = this.getSupportFragmentManager().popBackStackImmediate();
        if (popped)
            this.lastPushPopTime = ZonedDateTime.now();
        return popped;
    }

    @Override
    public @NotNull String toString() {
        return "IPagedActivity{" +
                "lastPushPopTime=" + this.lastPushPopTime +
                ", super=" + super.toString() +
                '}';
    }
}
