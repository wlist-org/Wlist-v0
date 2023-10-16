package com.xuxiaocheng.WListAndroid.UIs;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.databinding.PageUserContentBinding;
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
        final PageUserContentBinding page = PageUserContentBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.set(page.getRoot());
        final TextView close = page.pageUserContentCloseServer;
        final TextView disconnection = page.pageUserContentDisconnect;
        // TODO
        final AtomicBoolean clickable = new AtomicBoolean(true);
        close.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address())) {
                    OperateServerHelper.closeServer(client, TokenAssistant.getToken(this.address(), this.username()));
                }
                Main.runOnUiThread(this.activity, this.activity::close);
            }, () -> clickable.set(true)));
        });
        disconnection.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            try {
                this.activity.startActivity(new Intent(this.activity, ActivityLogin.class));
                this.activity.finish();
            } finally {
                clickable.set(true);
            }
        });
        return page.getRoot();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public @NotNull String toString() {
        return "PageUser{" +
                "activity=" + this.activity +
                ", pageCache=" + this.pageCache +
                '}';
    }
}