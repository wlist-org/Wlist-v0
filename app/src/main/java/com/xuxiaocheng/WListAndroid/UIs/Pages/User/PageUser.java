package com.xuxiaocheng.WListAndroid.UIs.Pages.User;

import android.view.LayoutInflater;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class PageUser extends IFragment<PageUserBinding> {
    @Override
    protected @NotNull PageUserBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageUserBinding.inflate(inflater);
    }

    @Override
    public void onBuild(final @NotNull ActivityMain activity, final @NotNull PageUserBinding page) {
        // TODO
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageUserCloseServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                try (final WListClientInterface client = this.client(activity)) {
                    OperateServerHelper.closeServer(client, this.token(activity));
                }
            }, () -> clickable.set(true)));
        });
        page.pageUserDisconnect.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            try {
                activity.disconnect();
            } finally {
                clickable.set(true);
            }
        });
    }

    @Override
    public @NotNull String toString() {
        return "PageUser{" +
                "super=" + super.toString() +
                '}';
    }
}
