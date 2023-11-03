package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.File.FragmentFile;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.IPageMainFragment;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.Fragments.User.FragmentUser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public PageMainAdapter(final @NotNull Fragment fragment) {
        super(fragment);
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return switch (Types.fromPosition(position)) {
            case File -> new FragmentFile();
            case User -> new FragmentUser();
        };
    }

    @SuppressWarnings("unchecked")
    public static <F extends IPageMainFragment<?>> @Nullable F getFragment(final @NotNull FragmentManager manager, final @NotNull Types type) {
        //noinspection StringConcatenationMissingWhitespace
        return (F) manager.findFragmentByTag("f" + Types.toPosition(type));
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
