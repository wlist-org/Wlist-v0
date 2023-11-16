package com.xuxiaocheng.WListAndroid.UIs.Main.Task;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;

public class PageTaskAdapter extends FragmentStateAdapter {
    public enum Types {
        Download,
        Upload,
        Trash,
        Copy,
        Move,
        Rename,
        ;

        public static int toPosition(final @NotNull Types type) {
            return switch (type) {
                case Download -> 0;
                case Upload -> 1;
                case Trash -> 2;
                case Copy -> 3;
                case Move -> 4;
                case Rename -> 5;
            };
        }

        public static @NotNull Types fromPosition(final int position) {
            return switch (position) {
                case 0 -> Types.Download;
                case 1 -> Types.Upload;
                case 2 -> Types.Trash;
                case 3 -> Types.Copy;
                case 4 -> Types.Move;
                case 5 -> Types.Rename;
                default -> throw new IllegalArgumentException("Invalid 'PageTaskAdapter' position." + ParametersMap.create().add("position", position));
            };
        }
    }

    PageTaskAdapter(final @NotNull Fragment fragment) {
        super(fragment);
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return switch (Types.fromPosition(position)) {
            case Download -> new TaskDownload();
            case Upload -> new TaskUpload();
            case Trash -> new TaskTrash();
            case Copy -> new TaskCopy();
            case Move -> new TaskMove();
            case Rename -> new TaskRename();
        };
    }

    @Override
    public int getItemCount() {
        return 6;
    }
}
