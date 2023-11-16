package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;

public class PageTaskStateAdapter extends FragmentStateAdapter {
    public enum Types {
        Failure,
        Working,
        Success,
        ;

        public static int toPosition(final @NotNull Types type) {
            return switch (type) {
                case Failure -> 0;
                case Working -> 1;
                case Success -> 2;
            };
        }

        public static @NotNull Types fromPosition(final int position) {
            return switch (position) {
                case 0 -> Types.Failure;
                case 1 -> Types.Working;
                case 2 -> Types.Success;
                default -> throw new IllegalArgumentException("Invalid 'PageTaskStateAdapter' position." + ParametersMap.create().add("position", position));
            };
        }
    }

    protected @NotNull SPageTaskFragment fragment;

    PageTaskStateAdapter(final @NotNull SPageTaskFragment fragment) {
        super(fragment);
        this.fragment = fragment;
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return this.fragment.createStateFragment(Types.fromPosition(position));
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public @NotNull String toString() {
        return "PageTaskStateAdapter{" +
                "fragment=" + this.fragment +
                '}';
    }
}
