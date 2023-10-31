package com.xuxiaocheng.WListAndroid.UIs.Fragments.File.Task;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.UIs.Fragments.File.FragmentFile;
import org.jetbrains.annotations.NotNull;

public class PageTaskAdapter extends FragmentStateAdapter {
    public enum TaskTypes {
        Download,
        Upload,
        Trash,
        Copy,
        Move,
        Rename,
        ;

        public static int toPosition(final PageTaskAdapter.@NotNull TaskTypes type) {
            return switch (type) {
                case Download -> 0;
                case Upload -> 1;
                case Trash -> 2;
                case Copy -> 3;
                case Move -> 4;
                case Rename -> 5;
            };
        }

        public static PageTaskAdapter.@NotNull TaskTypes fromPosition(final int position) {
            return switch (position) {
                case 0 -> TaskTypes.Download;
                case 1 -> TaskTypes.Upload;
                case 2 -> TaskTypes.Trash;
                case 3 -> TaskTypes.Copy;
                case 4 -> TaskTypes.Move;
                case 5 -> TaskTypes.Rename;
                default -> throw new IllegalArgumentException("Invalid fragment position." + ParametersMap.create().add("position", position));
            };
        }
    }

    protected final @NotNull FragmentFile fragment;

    public PageTaskAdapter(final @NotNull FragmentFile fragment) {
        super(fragment);
        this.fragment = fragment;
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return switch (TaskTypes.fromPosition(position)) {
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

    @Override
    public @NotNull String toString() {
        return "PageTaskAdapter{" +
                "fragment=" + this.fragment +
                '}';
    }
}
