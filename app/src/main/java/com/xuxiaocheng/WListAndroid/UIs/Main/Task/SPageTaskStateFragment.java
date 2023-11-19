package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.CFragment;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListAndroid.databinding.PageTaskListContentBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SPageTaskStateFragment<V extends ViewBinding, T extends AbstractTasksManager.AbstractTask> extends CFragment<PageTaskListContentBinding> {
    @FunctionalInterface
    public interface Inflater<V extends ViewBinding> {
        V inflate(final @NotNull LayoutInflater inflater, final @Nullable ViewGroup group, final boolean attachRoot);
    }

    protected final @NotNull Inflater<V> inflater;

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
        final EnhancedRecyclerViewAdapter<T, SPageTaskStateFragmentVewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull SPageTaskStateFragmentVewHolder createDataViewHolder(final @NotNull ViewGroup parent, final int realPosition) {
                //noinspection ReturnOfInnerClass
                return new SPageTaskStateFragmentVewHolder(SPageTaskStateFragment.this.inflater.inflate(SPageTaskStateFragment.this.getLayoutInflater(), parent, false));
            }
        };
        page.pageTaskListContentList.setLayoutManager(new LinearLayoutManager(this.activity()));
        page.pageTaskListContentList.setHasFixedSize(true);
        page.pageTaskListContentList.setAdapter(adapter);
        final AtomicBoolean firstTime = new AtomicBoolean(true);
        Main.runOnBackgroundThread(this.activity(), new Runnable() {
            @Override
            public void run() {
                final Activity activity = SPageTaskStateFragment.this.getActivity();
                if (activity == null) return;
                try {
                    SPageTaskStateFragment.this.sOnUpdate(adapter, firstTime.getAndSet(false));
                } catch (final InterruptedException exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                } finally {
                    Main.runOnBackgroundThread(activity, this, 500, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    @WorkerThread
    protected abstract void sOnUpdate(final @NotNull EnhancedRecyclerViewAdapter<? super T, ?> adapter, final boolean isFirstTime) throws InterruptedException;

    @UiThread
    protected abstract void onBind(final @NotNull V cell, final @NotNull T data);

    protected class SPageTaskStateFragmentVewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<View, T> {
        protected final @NotNull V cell;

        protected SPageTaskStateFragmentVewHolder(final @NotNull V cell) {
            super(cell.getRoot());
            this.cell = cell;
        }

        @Override
        public void onBind(final @NotNull T data) {
            SPageTaskStateFragment.this.onBind(this.cell, data);
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
