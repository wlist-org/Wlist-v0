package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListContentBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class SPageTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E> extends CFragment<PageTaskListContentBinding> {
    @FunctionalInterface
    public interface Inflater<V extends ViewBinding> {
        V inflate(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup group, final boolean attachRoot);
    }

    protected final @NotNull Inflater<V> inflater;
    protected final @NotNull String callbackName = this.getClass().getName();

    protected SPageTaskStateFragment(final @NotNull Inflater<V> inflater) {
        super();
        this.inflater = inflater;
    }

    @Override
    protected @NotNull PageTaskListContentBinding iOnInflater() {
        return PageTaskListContentBinding.inflate(this.getLayoutInflater());
    }

    public @NotNull SPageTaskFragment fragment() {
        return (SPageTaskFragment) super.requireParentFragment();
    }

    @Override
    public @NotNull PageTask page() {
        return (PageTask) super.requireParentFragment().requireParentFragment();
    }

    @Override
    public @NotNull ActivityMain activity() {
        return (ActivityMain) super.activity();
    }

    @Override
    protected void iOnBuildPage(final @NotNull PageTaskListContentBinding page, final boolean isFirstTime) {
        super.iOnBuildPage(page, isFirstTime);
        final EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull SPageTaskStateFragmentVewHolder createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition) {
                //noinspection ReturnOfInnerClass
                return new SPageTaskStateFragmentVewHolder(SPageTaskStateFragment.this.inflater.inflate(SPageTaskStateFragment.this.getLayoutInflater(), parent, false));
            }

            @Override
            protected void recycleDataViewHolder(final @NotNull SPageTaskStateFragmentVewHolder holder) {
                super.recycleDataViewHolder(holder);
                SPageTaskStateFragment.this.onUnbind(holder.cell);
            }
        };
        page.pageTaskListContentList.setLayoutManager(new LinearLayoutManager(this.activity()));
        page.pageTaskListContentList.setHasFixedSize(true);
        page.pageTaskListContentList.setAdapter(adapter);
        Main.runOnBackgroundThread(this.activity(), () -> this.sOnBuildPage(adapter));
    }

    @WorkerThread
    protected abstract void sOnBuildPage(final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter);

    @UiThread
    protected abstract void onBind(final @NotNull V cell, final @NotNull T task, final @NotNull E data);

    @UiThread
    protected void onUnbind(final @NotNull V cell) {
    }

    public static class AdapterUpdateCallback<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask, E> implements AbstractTasksManager.UpdateCallback<T, E> {
        protected final @NotNull Supplier<@NotNull Activity> activitySupplier;
        protected final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter;
        protected final @NotNull TextView textView;
        @StringRes protected final int textId;

        public AdapterUpdateCallback(final @NotNull Supplier<@NotNull Activity> activitySupplier, final @NotNull EnhancedRecyclerViewAdapter<Map.Entry<T, E>, SPageTaskStateFragment<V, T, E>.SPageTaskStateFragmentVewHolder> adapter, final @NotNull TextView textView, @StringRes final int textId) {
            super();
            this.activitySupplier = activitySupplier;
            this.adapter = adapter;
            this.textView = textView;
            this.textId = textId;
        }

        @Override
        public void onAdded(final @NotNull T task, final @NotNull E extra) {
            Main.runOnUiThread(this.activitySupplier.get(), () -> {
                this.adapter.addData(new AbstractMap.SimpleImmutableEntry<>(task, extra));
                this.textView.setText(MessageFormat.format(this.textView.getContext().getString(this.textId), this.adapter.dataSize()));
            });
        }

        @Override
        public void onRemoved(final @NotNull T task, final @NotNull E extra) {
            Main.runOnUiThread(this.activitySupplier.get(), () -> {
                final int index = this.adapter.getData().indexOf(new AbstractMap.SimpleImmutableEntry<>(task, extra));
                if (index >= 0) {
                    this.adapter.removeData(index);
                    this.textView.setText(MessageFormat.format(this.textView.getContext().getString(this.textId), this.adapter.dataSize()));
                }
            });
        }

        @Override
        public @NotNull String toString() {
            return "AdapterUpdateCallback{" +
                    "adapter=" + this.adapter +
                    '}';
        }
    }

    public class SPageTaskStateFragmentVewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<View, Map.Entry<T, E>> {
        protected final @NotNull V cell;

        protected SPageTaskStateFragmentVewHolder(final @NotNull V cell) {
            super(cell.getRoot());
            this.cell = cell;
        }

        @Override
        public void onBind(final Map.@NotNull Entry<@NotNull T, @NotNull E> data) {
            SPageTaskStateFragment.this.onBind(this.cell, data.getKey(), data.getValue());
        }

        @Override
        public @NotNull String toString() {
            return "SPageTaskStateFragmentVewHolder{" +
                    "cell=" + this.cell +
                    '}';
        }
    }

    @Override
    public @NotNull String toString() {
        return "SPageTaskStateFragment{" +
                "inflater=" + this.inflater +
                ", contentCache=" + this.contentCache.isInitialized() +
                '}';
    }
}
