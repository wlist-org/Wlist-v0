package com.xuxiaocheng.WListAndroid.UIs.Fragments.User;

import android.view.LayoutInflater;
import android.widget.TextView;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class FragmentUser extends IFragment<PageUserBinding> {
    protected final @NotNull ActivityMain activity;

    public FragmentUser(final @NotNull ActivityMain activity) {
        super(activity);
        this.activity = activity;
    }

    protected @NotNull InetSocketAddress address() {
        return this.activity.address();
    }

    protected @NotNull String username() {
        return this.activity.username();
    }


    @Override
    protected @NotNull PageUserBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageUserBinding.inflate(inflater);
    }

    @Override
    public void onShow(final @NotNull PageUserBinding page) {
        final TextView close = page.pageUserCloseServer;
        final TextView disconnection = page.pageUserDisconnect;
        // TODO
        final AtomicBoolean clickable = new AtomicBoolean(true);
        close.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
                    OperateServerHelper.closeServer(client, TokenAssistant.getToken(this.address(), this.username()));
                }
            }, () -> clickable.set(true)));
        });
        disconnection.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            try {
                this.activity.close();
            } finally {
                clickable.set(true);
            }
        });
    }

    @Override
    public @NotNull String toString() {
        return "PageUser{" +
                "activity=" + this.activity +
                ", pageCache=" + this.pageCache +
                '}';
    }
}
