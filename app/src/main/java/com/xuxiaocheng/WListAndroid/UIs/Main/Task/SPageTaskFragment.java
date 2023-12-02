package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.widget.ViewPager2;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListSimpleWorkingCellBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SPageTaskFragment extends CFragment<PageTaskListBinding> {
    protected final PageTaskAdapter.@NotNull Types type;
    protected final @NotNull AtomicReference<PageTaskStateAdapter.Types> currentState;
    protected PageTaskStateAdapter.@NotNull Types currentState() {
        return this.currentState.get();
    }

    protected SPageTaskFragment(final PageTaskAdapter.@NotNull Types type, final @NotNull AtomicReference<PageTaskStateAdapter.Types> currentState) {
        super();
        this.type = type;
        this.currentState = currentState;
    }

    protected abstract @NotNull SPageTaskStateFragment<?, ?, ?> createStateFragment(final PageTaskStateAdapter.@NotNull Types type);

    @Override
    protected @NotNull PageTaskListBinding iOnInflater() {
        return PageTaskListBinding.inflate(this.getLayoutInflater());
    }
    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle arguments, final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(arguments, savedInstanceState);
        if (this.currentState.get() == null) {
            final int type = savedInstanceState != null ? savedInstanceState.getInt("w:page_task:" + this.type.name() + ":current_state", -1) : -1;
            this.currentState.set(type == -1 ? this.getSuggestedChoice() : PageTaskStateAdapter.Types.fromPosition(type));
        }
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

    protected abstract static class FailureTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E extends AbstractTasksManager.AbstractExtraFailure> extends SPageTaskStateFragment<V, T, E> {
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

    protected abstract static class WorkingTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E extends AbstractTasksManager.AbstractExtraWorking> extends SPageTaskStateFragment<V, T, E> {
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

    protected abstract static class SuccessTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E extends AbstractTasksManager.AbstractExtraSuccess> extends SPageTaskStateFragment<V, T, E> {
        protected SuccessTaskStateFragment(final @NotNull Inflater<@NotNull V> inflater) {
            super(inflater);
        }

        @WorkerThread
        protected abstract @NotNull AbstractTasksManager<T, ?, E, ?> getManager();

//        @UiThread
//        protected abstract void removeAll();
//
//        @Override
//        protected void iOnBuildPage(final @NotNull PageTaskListContentBinding page, final boolean isFirstTime) {
//            super.iOnBuildPage(page, isFirstTime);
//            page.pageTaskListContentRetryAll.setVisibility(View.GONE);
//            page.pageTaskListContentRemoveAll.setOnClickListener(v -> this.removeAll());
//        }

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

    protected abstract static class SimpleWorkingTaskStateFragment<V extends SimpleWorkingTaskStateFragment.WrappedPageTaskListSimpleWorkingCellBinding<E>, T extends AbstractTasksManager.AbstractTask, E extends AbstractTasksManager.AbstractSimpleExtraWorking> extends WorkingTaskStateFragment<V, T, E> {
        protected static class WrappedPageTaskListSimpleWorkingCellBinding<E> implements ViewBinding {
            protected final @NotNull PageTaskListSimpleWorkingCellBinding cell;
            protected @Nullable E working;
            protected long lastW = 0;
            protected @Nullable LocalDateTime lastT = null;

            protected WrappedPageTaskListSimpleWorkingCellBinding(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup group, final boolean attachRoot) {
                super();
                this.cell = PageTaskListSimpleWorkingCellBinding.inflate(inflater, group, attachRoot);
            }

            @Override
            public @NotNull View getRoot() {
                return this.cell.getRoot();
            }

            @Override
            public @NotNull String toString() {
                return "WrappedPageTaskListSimpleWorkingCellBinding{" +
                        "cell=" + this.cell +
                        ", working=" + this.working +
                        '}';
            }
        }

        protected SimpleWorkingTaskStateFragment(final @NotNull Inflater<@NotNull V> inflater) {
            super(inflater);
        }

        @Override
        protected void onBind(final @NotNull V cell, final @NotNull T task, final @NotNull E data) {
            ViewUtil.setFileImage(cell.cell.pageTaskListSimpleWorkingCellImage, false, task.getFilename());
            cell.cell.pageTaskListSimpleWorkingCellName.setText(task.getFilename());
            cell.working = data;
            cell.cell.pageTaskListSimpleWorkingCellProgress.setMin(0);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setMax(1000);
            if (data.isStarted())
                this.resetProgress(cell, false);
            else
                this.onPending(cell, false);
            data.getUpdateCallbacks().registerNamedForce(this.getClass().getName(), () -> Main.runOnUiThread(this.activity(), () ->
                    this.resetProgress(cell, true)));
        }

        @UiThread
        protected void onPending(final @NotNull V cell, final boolean animate) {
            final Context context = cell.getRoot().getContext();
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 0));
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(false);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setProgress(0, false);
            cell.cell.pageTaskListSimpleWorkingCellProgress.setSecondaryProgress(0);
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(R.string.page_task_waiting);
            cell.cell.pageTaskListSimpleWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time), context.getString(R.string.unknown)));
        }

        @UiThread
        protected void onPreparing(final @NotNull V cell, final boolean animate) {
            final Context context = cell.getRoot().getContext();
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 0));
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(true);
            cell.cell.pageTaskListSimpleWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time), context.getString(R.string.unknown)));
        }

        @UiThread
        protected void onFinishing(final @NotNull V cell, final boolean animate) {
            final Context context = cell.getRoot().getContext();
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), 1));
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(true);
            cell.cell.pageTaskListSimpleWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time), "0s"));
        }

        @UiThread
        protected void onWorking(final @NotNull V cell, final boolean animate, final Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long> m) {
            final Context context = cell.getRoot().getContext();
            final float percent = 1.0f * m.getFirst().longValue() / m.getSecond().longValue();
            cell.cell.pageTaskListSimpleWorkingCellProcessText.setText(MessageFormat.format(context.getString(R.string.page_task_process), percent));
            cell.cell.pageTaskListSimpleWorkingCellProgress.setIndeterminate(false);
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListSimpleWorkingCellProgress.setProgress((int) (percent * 1000), animate);
            final String unknown = context.getString(R.string.unknown);
            final LocalDateTime now = LocalDateTime.now();
            final long interval = cell.lastT == null ? 1000 : Duration.between(cell.lastT, now).toMillis();
            final float speed = 1.0f * (m.getFirst().longValue() - cell.lastW) / (interval == 0 ? 1000 : interval);
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListSimpleWorkingCellSize.setText(MessageFormat.format(context.getString(R.string.page_task_size_speed),
                    ViewUtil.formatSize(m.getFirst().longValue(), unknown),
                    ViewUtil.formatSize(m.getSecond().longValue(), unknown),
                    ViewUtil.formatSize((long) (speed * 1000), unknown)));
            final float time = speed == 0 ? -1 : (m.getSecond().longValue() - m.getFirst().longValue()) / speed;
            //noinspection NumericCastThatLosesPrecision
            cell.cell.pageTaskListSimpleWorkingCellTime.setText(MessageFormat.format(context.getString(R.string.page_task_time),
                    ViewUtil.formatDuration(Duration.of((long) time, ChronoUnit.MILLIS), unknown)));
            cell.lastW = m.getFirst().longValue();
            cell.lastT = now;
        }

        @UiThread
        protected void resetProgress(final @NotNull V cell, final boolean animate) {
            assert cell.working != null;
            final InstantaneousProgressState readProgress = cell.working.getState();
            if (readProgress == null) {
                this.onPreparing(cell, animate);
                return;
            }
            final Pair.ImmutablePair<Long, Long> m = InstantaneousProgressStateGetter.merge(readProgress);
            if (m.getFirst().longValue() == m.getSecond().longValue()) {
                this.onFinishing(cell, animate);
                return;
            }
            this.onWorking(cell, animate, m);
        }

        @Override
        protected void onUnbind(final @NotNull V cell) {
            super.onUnbind(cell);
            if (cell.working != null)
                cell.working.getUpdateCallbacks().unregisterNamed(this.getClass().getName());
            cell.working = null;
            cell.lastW = 0;
            cell.lastT = null;
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
