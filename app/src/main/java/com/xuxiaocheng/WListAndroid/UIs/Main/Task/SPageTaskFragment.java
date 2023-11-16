package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public abstract class SPageTaskFragment extends CFragment<PageTaskListBinding> {
    protected abstract @NotNull SPageTaskStateFragment createStateFragment(final PageTaskStateAdapter.@NotNull Types type);

    @Override
    protected @NotNull PageTaskListBinding iOnInflater() {
        return PageTaskListBinding.inflate(this.getLayoutInflater());
    }

    protected final @NotNull AtomicReference<PageTaskStateAdapter.Types> currentState = new AtomicReference<>();
    protected PageTaskStateAdapter.@NotNull Types currentState() {
        return this.currentState.get();
    }

    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        final int type = savedInstanceState != null ? savedInstanceState.getInt("w:page_task:" + this.getClass().getSimpleName() + ":current_state", -1) : -1;
        this.currentState.set(type == -1 ? this.getSuggestedChoice() : PageTaskStateAdapter.Types.fromPosition(type));
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        outState.putInt("w:page_task:" + this.getClass().getSimpleName() + ":current_type", PageTaskStateAdapter.Types.toPosition(this.currentState.get()));
    }

    private PageTaskStateAdapter.@NotNull Types getSuggestedChoice() {
        return PageTaskStateAdapter.Types.Working; // TODO
    }

    @Override
    protected void iOnBuildPage(final @NotNull PageTaskListBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        final PageTask.ChooserButtonGroup failure = new PageTask.ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskListStatesFailure);
        final PageTask.ChooserButtonGroup working = new PageTask.ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskListStatesWorking);
        final PageTask.ChooserButtonGroup success = new PageTask.ChooserButtonGroup(page.getRoot().getContext(), page.pageTaskListStatesSuccess);
        page.pageTaskListStatesFailure.setOnClickListener(v -> page.pageTaskListContent.setCurrentItem(PageTaskStateAdapter.Types.toPosition(PageTaskStateAdapter.Types.Failure)));
        page.pageTaskListStatesWorking.setOnClickListener(v -> page.pageTaskListContent.setCurrentItem(PageTaskStateAdapter.Types.toPosition(PageTaskStateAdapter.Types.Working)));
        page.pageTaskListStatesSuccess.setOnClickListener(v -> page.pageTaskListContent.setCurrentItem(PageTaskStateAdapter.Types.toPosition(PageTaskStateAdapter.Types.Success)));
        page.pageTaskListContent.setAdapter(new PageTaskStateAdapter(this));
        page.pageTaskListContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                final PageTaskStateAdapter.Types current = PageTaskStateAdapter.Types.fromPosition(position);
                SPageTaskFragment.this.currentState.set(current);
                switch (current) {
                    case Failure -> {
                        if (failure.isClicked()) return;
                        failure.click();
                        working.release();
                        success.release();
                    }
                    case Working -> {
                        if (working.isClicked()) return;
                        failure.release();
                        working.click();
                        success.release();
                    }
                    case Success -> {
                        if (success.isClicked()) return;
                        failure.release();
                        working.release();
                        success.click();
                    }
                }
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                final PageTaskStateAdapter.Types type = PageTaskStateAdapter.Types.fromPosition(position);
                final View current = switch (type) {
                    case Failure -> page.pageTaskListStatesFailure;
                    case Working -> page.pageTaskListStatesWorking;
                    case Success -> page.pageTaskListStatesSuccess;
                };
                page.pageTaskListStateHint.setX(current.getX() + positionOffset * page.pageTaskListStateHint.getWidth() + ((ViewGroup.MarginLayoutParams) page.pageTaskListStateHint.getLayoutParams()).leftMargin);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.content().pageTaskListContent.setCurrentItem(PageTaskStateAdapter.Types.toPosition(this.currentState.get()), false);
    }

    protected abstract static class FailureTaskStateFragment extends SPageTaskStateFragment {
    }

    protected abstract static class WorkingTaskStateFragment extends SPageTaskStateFragment {
    }

    protected abstract static class SuccessTaskStateFragment extends SPageTaskStateFragment {
    }

    @Override
    public @NotNull String toString() {
        return "SPageTaskFragment{" +
                "currentState=" + this.currentState +
                ", super=" + super.toString() +
                '}';
    }
}
