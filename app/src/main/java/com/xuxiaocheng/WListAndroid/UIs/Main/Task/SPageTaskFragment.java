package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.WorkerThread;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SPageTaskFragment extends CFragment<PageTaskListBinding> {
    protected final PageTaskAdapter.@NotNull Types type;

    protected SPageTaskFragment(final PageTaskAdapter.@NotNull Types type) {
        super();
        this.type = type;
    }

    protected abstract @NotNull SPageTaskStateFragment<?, ?, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type);

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
        final int type = savedInstanceState != null ? savedInstanceState.getInt("w:page_task:" + this.type.name() + ":current_state", -1) : -1;
        this.currentState.set(type == -1 ? this.getSuggestedChoice() : PageTaskStateAdapter.Types.fromPosition(type));
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        outState.putInt("w:page_task:" + this.type.name() + ":current_type", PageTaskStateAdapter.Types.toPosition(this.currentState.get()));
    }

    protected PageTaskStateAdapter.@NotNull Types getSuggestedChoice() {
        final AbstractTasksManager<?, ?, ?, ?> manager = AbstractTasksManager.managers.getInstanceNullable(this.type);
        if (manager != null) {
            final int failure = manager.getFailureTasks().size();
            final int working = manager.getWorkingTasks().size() + manager.getPendingTasks().size();
            final int success = manager.getSuccessTasks().size();
            if (working != 0) return PageTaskStateAdapter.Types.Working;
            if (failure != 0) return PageTaskStateAdapter.Types.Failure;
            if (success != 0) return PageTaskStateAdapter.Types.Success;
        }
        return PageTaskStateAdapter.Types.Working;
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
        page.pageTaskListStatesFailure.setText(MessageFormat.format(page.getRoot().getContext().getString(R.string.page_task_failure), 0));
        page.pageTaskListStatesWorking.setText(MessageFormat.format(page.getRoot().getContext().getString(R.string.page_task_working), 0));
        page.pageTaskListStatesSuccess.setText(MessageFormat.format(page.getRoot().getContext().getString(R.string.page_task_success), 0));
        page.pageTaskListContent.setAdapter(new PageTaskStateAdapter(this));
        page.pageTaskListContent.setOffscreenPageLimit(3);
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

    protected abstract static class FailureTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E> extends SPageTaskStateFragment<V, T, E> {
        protected FailureTaskStateFragment(final @NotNull Inflater<@NotNull V> inflater) {
            super(inflater);
        }

        @WorkerThread
        protected abstract @NotNull AbstractTasksManager<T, ?, ?, E> getManager();

        @Override
        protected void sOnBuildPage(final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter) {
            final AbstractTasksManager<T, ?, ?, E> manager = this.getManager();
            Main.runOnUiThread(this.activity(), () -> {
                adapter.addDataRange(manager.getFailureTasks().entrySet());
                this.fragment().content().pageTaskListStatesFailure.setText(MessageFormat.format(this.requireContext().getString(R.string.page_task_failure), adapter.dataSize()));
            });
            manager.getFailureTasksCallbacks().registerNamedForce(this.callbackName, new AdapterUpdateCallback<>(this::activity, adapter, this.fragment().content().pageTaskListStatesFailure, R.string.page_task_failure));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Main.runOnBackgroundThread(this.activity(), () -> this.getManager().getFailureTasksCallbacks().unregisterNamed(this.callbackName));
        }
    }

    protected abstract static class WorkingTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E> extends SPageTaskStateFragment<V, T, E> {
        protected WorkingTaskStateFragment(final @NotNull Inflater<@NotNull V> inflater) {
            super(inflater);
        }

        @WorkerThread
        protected abstract @NotNull AbstractTasksManager<T, E, ?, ?> getManager();

        @Override
        protected void sOnBuildPage(final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter) {
            final AbstractTasksManager<T, E, ?, ?> manager = this.getManager();
            Main.runOnUiThread(this.activity(), () -> {
                adapter.addDataRange(manager.getWorkingTasks().entrySet());
                adapter.addDataRange(manager.getPendingTasks().entrySet());
                this.fragment().content().pageTaskListStatesWorking.setText(MessageFormat.format(this.requireContext().getString(R.string.page_task_working), adapter.dataSize()));
            });
            manager.getWorkingTasksCallbacks().registerNamedForce(this.callbackName, new AdapterUpdateCallback<>(this::activity, adapter, this.fragment().content().pageTaskListStatesWorking, R.string.page_task_working) {
                @Override
                public void onAdded(final @NotNull T task, final @NotNull E extra) {
                    Main.runOnUiThread(this.activitySupplier.get(), () -> {
                        int size = 0;
                        for (final Map.Entry<T, E> t: this.adapter.getData())
                            if (manager.getWorkingTasks().containsKey(t.getKey()))
                                ++size;
                            else
                                break;
                        this.adapter.addData(size, new AbstractMap.SimpleImmutableEntry<>(task, extra));
                        this.textView.setText(MessageFormat.format(this.textView.getContext().getString(this.textId), this.adapter.dataSize()));
                    });
                }
            });
            manager.getPendingTasksCallbacks().registerNamedForce(this.callbackName, new AdapterUpdateCallback<>(this::activity, adapter, this.fragment().content().pageTaskListStatesWorking, R.string.page_task_working));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Main.runOnBackgroundThread(this.activity(), () -> {
                final AbstractTasksManager<T, E, ?, ?> manager = this.getManager();
                manager.getWorkingTasksCallbacks().unregisterNamed(this.callbackName);
                manager.getPendingTasksCallbacks().unregisterNamed(this.callbackName);
            });
        }
    }

    protected abstract static class SuccessTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E> extends SPageTaskStateFragment<V, T, E> {
        protected SuccessTaskStateFragment(final @NotNull Inflater<@NotNull V> inflater) {
            super(inflater);
        }

        @WorkerThread
        protected abstract @NotNull AbstractTasksManager<T, ?, E, ?> getManager();

        @Override
        protected void sOnBuildPage(final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter) {
            final AbstractTasksManager<T, ?, E, ?> manager = this.getManager();
            Main.runOnUiThread(this.activity(), () -> {
                adapter.addDataRange(manager.getSuccessTasks().entrySet());
                this.fragment().content().pageTaskListStatesSuccess.setText(MessageFormat.format(this.requireContext().getString(R.string.page_task_success), adapter.dataSize()));
            });
            manager.getSuccessTasksCallbacks().registerNamedForce(this.callbackName, new AdapterUpdateCallback<>(this::activity, adapter, this.fragment().content().pageTaskListStatesSuccess, R.string.page_task_success));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Main.runOnBackgroundThread(this.activity(), () -> this.getManager().getSuccessTasksCallbacks().unregisterNamed(this.callbackName));
        }
    }

    @Override
    public @NotNull String toString() {
        return "SPageTaskFragment{" +
                "currentState=" + this.currentState +
                ", super=" + super.toString() +
                '}';
    }
}
