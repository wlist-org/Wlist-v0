package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.File.FragmentFile;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.User.FragmentUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FragmentsAdapter extends FragmentStateAdapter {
    public enum FragmentTypes {
        File,
        User,
        ;
        public static int toPosition(final @NotNull FragmentTypes type) {
            return switch (type) {
                case File -> 0;
                case User -> 1;
            };
        }

        public static @NotNull FragmentTypes fromPosition(final int position) {
            return switch (position) {
                case 0 -> FragmentTypes.File;
                case 1 -> FragmentTypes.User;
                default -> throw new IllegalArgumentException("Invalid fragment position." + ParametersMap.create().add("position", position));
            };
        }
    }

    protected final @NotNull ActivityMain activity;
    protected final @NotNull FragmentFile file;
    protected final @NotNull FragmentUser user;

    public FragmentsAdapter(final @NotNull ActivityMain activity) {
        super(activity);
        this.activity = activity;
        this.file = new FragmentFile();
        this.user = new FragmentUser();
    }

    @WorkerThread
    public void setArguments(final @NotNull InetSocketAddress address, final @NotNull String username) {
        final Bundle bundle = new Bundle();
        BundleHelper.saveClient(address, username, bundle);
        Main.runOnUiThread(this.activity, () -> this.file.setArguments(bundle));
    }

    public @NotNull List<@NotNull IFragment<?, ?>> getAllFragments() {
        return List.of(this.file, this.user);
    }

    public @NotNull IFragment<?, ?> getFragment(final @NotNull FragmentTypes type) {
        return switch (type) {
            case File -> this.file;
            case User -> this.user;
        };
    }

    @UiThread
    public void notifyConnectStateChanged(final boolean connected, final @Nullable FragmentTypes current) {
        this.notifyItemRangeChanged(0, this.getItemCount());
        if (this.activity.getContent().activityMainContent.getAdapter() == this)
            Main.runOnUiThread(this.activity, () -> {
                if (this.activity.getContent().activityMainContent.getAdapter() == this) {
                    this.activity.getContent().activityMainContent.setAdapter(this); // Fix cache when dragging.
                    final int position = FragmentTypes.toPosition(Objects.requireNonNullElse(current, FragmentTypes.File));
                    this.activity.getContent().activityMainContent.setCurrentItem(position, false);
                }
            }, 100, TimeUnit.MILLISECONDS);
    }

    public @NotNull IFragment<?, ?> getFragment(final int position) {
        return this.getFragment(FragmentTypes.fromPosition(position));
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return this.getFragment(position);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @Override
    public @NotNull String toString() {
        return "FragmentsAdapter{" +
                "activity=" + this.activity +
                '}';
    }
}
