package com.xuxiaocheng.WListAndroid.UIs.Main.Main;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.UIs.Main.Main.File.FragmentFile;
import com.xuxiaocheng.WListAndroid.UIs.Main.Main.User.FragmentUser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PageMainAdapter extends FragmentStateAdapter {
    public enum Types {
        File,
        User,
        ;
        @Contract(pure = true)
        public static int toPosition(final @NotNull Types type) {
            return switch (type) {
                case File -> 0;
                case User -> 1;
            };
        }

        @Contract(pure = true)
        public static @NotNull Types fromPosition(final int position) {
            return switch (position) {
                case 0 -> Types.File;
                case 1 -> Types.User;
                default -> throw new IllegalArgumentException("Invalid 'PageMainAdapter' position." + ParametersMap.create().add("position", position));
            };
        }
    }

    PageMainAdapter(final @NotNull Fragment fragment) {
        super(fragment);
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return switch (Types.fromPosition(position)) {
            case File -> new FragmentFile();
            case User -> new FragmentUser();
        };
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
