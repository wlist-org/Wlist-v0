package com.xuxiaocheng.WListAndroid.UIs;

import android.view.View;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.databinding.PageUserBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PageUser implements ActivityMainChooser.MainPage {
    protected final @NotNull ActivityMain activity;

    public PageUser(final @NotNull ActivityMain activity) {
        super();
        this.activity = activity;
    }

    protected @NotNull InetSocketAddress address() {
        return this.activity.address.getInstance();
    }

    protected @NotNull String username() {
        return this.activity.username.getInstance();
    }


    private final @NotNull AtomicReference<ConstraintLayout> pageCache = new AtomicReference<>();
    @Override
    public @NotNull View onShow() {
        final ConstraintLayout cache = this.pageCache.get();
        if (cache != null) return cache;
        final PageUserBinding page = PageUserBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.set(page.getRoot());
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
        return page.getRoot();
    }

    @Override
    public @NotNull String toString() {
        return "PageUser{" +
                "activity=" + this.activity +
                ", pageCache=" + this.pageCache +
                '}';
    }
}
