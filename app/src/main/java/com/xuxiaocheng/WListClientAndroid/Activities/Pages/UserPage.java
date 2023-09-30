package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Activities.LoginActivity;
import com.xuxiaocheng.WListClientAndroid.Activities.MainActivity;
import com.xuxiaocheng.WListClientAndroid.Helpers.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.databinding.PageUserContentBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UserPage implements MainTab.MainTabPage {
    protected final @NotNull MainActivity activity;
    protected final @NotNull InetSocketAddress address;

    public UserPage(final @NotNull MainActivity activity, final @NotNull InetSocketAddress address) {
        super();
        this.activity = activity;
        this.address = address;
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
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
                    OperateServerHelper.closeServer(client, TokenManager.getToken(this.address));
                }
                Main.runOnUiThread(this.activity, this.activity::close);
            }, () -> clickable.set(true)));
        });
        disconnection.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            try {
                this.activity.startActivity(new Intent(this.activity, LoginActivity.class));
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
        return "UserPage{" +
                "address=" + this.address +
                ", pageCache=" + this.pageCache +
                '}';
    }
}
