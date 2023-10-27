package com.xuxiaocheng.WListAndroid.UIs;

import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WListAndroid.UIs.Pages.Connect.PageConnect;
import com.xuxiaocheng.WListAndroid.UIs.Pages.File.PageFile;
import com.xuxiaocheng.WListAndroid.UIs.Pages.Trans.PageTrans;
import com.xuxiaocheng.WListAndroid.UIs.Pages.User.PageUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class FragmentsAdapter extends FragmentStateAdapter {
    public enum FragmentTypes {
        File,
        User,
        Trans,
        ;
        public static int toPosition(final @NotNull FragmentTypes type) {
            return switch (type) {
                case File -> 1;
                case User -> 2;
                case Trans -> 0;
            };
        }

        public static @NotNull FragmentTypes fromPosition(final int position) {
            return switch (position) {
                case 0 -> FragmentTypes.Trans;
                case 1 -> FragmentTypes.File;
                case 2 -> FragmentTypes.User;
                default -> {throw new IllegalArgumentException("Invalid fragment position." + ParametersMap.create().add("position", position));}
            };
        }
    }

    protected final @NotNull ActivityMain activity;
    protected final @NotNull PageFile fragmentFileInstance;
    protected final @NotNull PageConnect fragmentConnectFileInstance;
    protected final @NotNull PageUser fragmentUserInstance;
    protected final @NotNull PageConnect fragmentConnectUserInstance;
    protected final @NotNull PageTrans fragmentTransInstance;
    protected final @NotNull PageConnect fragmentConnectTransInstance;

    public FragmentsAdapter(final @NotNull ActivityMain activity) {
        super(activity);
        this.activity = activity;
        this.fragmentFileInstance = new PageFile();
        this.fragmentConnectFileInstance = new PageConnect();
        this.fragmentUserInstance = new PageUser();
        this.fragmentConnectUserInstance = new PageConnect();
        this.fragmentTransInstance = new PageTrans();
        this.fragmentConnectTransInstance = new PageConnect();
    }

    public @NotNull List<@NotNull IFragment<?>> getAllFragments() {
        return List.of(this.fragmentFileInstance, this.fragmentUserInstance, this.fragmentTransInstance,
                this.fragmentConnectFileInstance, this.fragmentConnectUserInstance, this.fragmentConnectTransInstance);
    }

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public @NotNull IFragment<?> getFragment(final @NotNull FragmentTypes type) {
        return this.connected.get() ? switch (type) {
            case File -> this.fragmentFileInstance;
            case User -> this.fragmentUserInstance;
            case Trans -> this.fragmentTransInstance;
        } : switch (type) {
            case File -> this.fragmentConnectFileInstance;
            case User -> this.fragmentConnectUserInstance;
            case Trans -> this.fragmentConnectTransInstance;
        };
    }

    @UiThread
    public void notifyConnected(final boolean connected, final @Nullable FragmentTypes current) {
        if (!this.connected.compareAndSet(!connected, connected)) return;
        this.notifyItemRangeChanged(0, this.getItemCount());
        this.activity.getContent().activityMainContent.setAdapter(this);
        this.activity.getContent().activityMainContent.setCurrentItem(FragmentTypes.toPosition(Objects.requireNonNullElse(current, FragmentTypes.File)), false);
    }

    public @NotNull IFragment<?> getFragment(final int position) {
        return this.getFragment(FragmentTypes.fromPosition(position));
    }

    @Override
    public @NotNull Fragment createFragment(final int position) {
        return this.getFragment(position);
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public @NotNull String toString() {
        return "FragmentsAdapter{" +
                "activity=" + this.activity +
                ", fragmentFileInstance=" + this.fragmentFileInstance +
                ", fragmentConnectFileInstance=" + this.fragmentConnectFileInstance +
                ", fragmentUserInstance=" + this.fragmentUserInstance +
                ", fragmentConnectUserInstance=" + this.fragmentConnectUserInstance +
                ", fragmentTransInstance=" + this.fragmentTransInstance +
                ", fragmentConnectTransInstance=" + this.fragmentConnectTransInstance +
                '}';
    }
}