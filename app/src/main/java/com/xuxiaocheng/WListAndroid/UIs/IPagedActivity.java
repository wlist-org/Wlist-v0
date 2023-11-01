package com.xuxiaocheng.WListAndroid.UIs;

import androidx.fragment.app.FragmentTransaction;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Pages.IPage;
import com.xuxiaocheng.WListAndroid.databinding.ActivityBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;

public abstract class IPagedActivity extends IActivity<ActivityBinding> {
    @Override
    protected @NotNull ActivityBinding iOnInflater() {
        return ActivityBinding.inflate(this.getLayoutInflater());
    }

    @SuppressWarnings("unchecked")
    public <P extends IPage<?>> @Nullable P currentPage() {
        return (P) this.getSupportFragmentManager().findFragmentById(this.content().activity.getId());
    }

    @SuppressWarnings("unchecked")
    public <P extends IPage<?>> @Nullable P pageByTag(final @NotNull String tag) {
        return (P) this.getSupportFragmentManager().findFragmentByTag(tag);
    }

    protected @Nullable ZonedDateTime lastPushPopTime = null;

    public void push(final @NotNull IPage<?> page, final @Nullable String tag, final boolean allowPop) {
        final FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out, R.anim.slide_left_in, R.anim.slide_right_out)
                .add(this.content().activity.getId(), page, tag);
        if (allowPop)
            transaction.addToBackStack(null).commit();
        else
            transaction.commitNow();
        this.lastPushPopTime = ZonedDateTime.now();
    }

    public boolean pop() {
        final boolean popped = this.getSupportFragmentManager().popBackStackImmediate();
        if (popped)
            this.lastPushPopTime = ZonedDateTime.now();
        return popped;
    }

    @Override
    protected boolean iOnBackPressed() {
        if (this.lastPushPopTime != null && Duration.between(this.lastPushPopTime, ZonedDateTime.now()).toMillis() < this.getResources().getInteger(R.integer.anim_slide_duration))
            return true; // Lock on anim.
        final IPage<?> page = this.currentPage();
        if (page != null && page.onBackPressed()) return true;
        return this.pop() || super.iOnBackPressed();
    }

    @Override
    public @NotNull String toString() {
        return "IPagedActivity{" +
                "lastPushPopTime=" + this.lastPushPopTime +
                ", super=" + super.toString() +
                '}';
    }
}
