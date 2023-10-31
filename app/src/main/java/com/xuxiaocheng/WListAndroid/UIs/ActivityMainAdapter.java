package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.File.FragmentFile;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.IFragment;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.User.FragmentUser;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.List;

public class ActivityMainAdapter extends FragmentStateAdapter {
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

    public ActivityMainAdapter(final @NotNull ActivityMain activity) {
        super(activity);
        this.activity = activity;
    }

    @WorkerThread
    public void setArguments(final @NotNull InetSocketAddress address, final @NotNull String username) {
        final Bundle bundle = new Bundle();
        BundleHelper.saveClient(address, username, bundle);
        Main.runOnUiThread(this.activity, () -> this.getAllFragments().forEach(f -> f.setArguments(bundle)));
    }

    @SuppressWarnings("unchecked")
    public @NotNull List<@NotNull IFragment<?, ?>> getAllFragments() {
        return (List<IFragment<?,?>>) (List<?>) this.activity.getSupportFragmentManager().getFragments();
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return switch (FragmentTypes.fromPosition(position)) {
            case File -> new FragmentFile();
            case User -> new FragmentUser();
        };
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
