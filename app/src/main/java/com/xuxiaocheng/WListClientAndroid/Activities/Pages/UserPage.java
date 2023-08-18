package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Activities.LoginActivity;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.databinding.UserListContentBinding;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UserPage implements MainTab.MainTabPage {
    @NonNull protected final Activity activity;
    @NonNull protected final InetSocketAddress address;

    public UserPage(@NonNull final Activity activity, @NonNull final InetSocketAddress address) {
        super();
        this.activity = activity;
        this.address = address;
    }

    @NonNull private final AtomicReference<ConstraintLayout> pageCache = new AtomicReference<>();
    @NonNull public View onChange() {
        final ConstraintLayout cache = this.pageCache.get();
        if (cache != null) return cache;
        final ConstraintLayout page = UserListContentBinding.inflate(this.activity.getLayoutInflater()).getRoot();
        final TextView close = (TextView) page.getViewById(R.id.user_content_close_server);
        final TextView disconnection = (TextView) page.getViewById(R.id.user_content_disconnect);
        // TODO
        final AtomicBoolean closed = new AtomicBoolean(false);
        close.setOnClickListener(v -> {
            if (!closed.compareAndSet(false, true))
                return;
            Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                final boolean success;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
                    success = OperateServerHelper.closeServer(client, TokenManager.getToken());
                }
                if (success) {
                    this.activity.runOnUiThread(() -> this.activity.startActivity(new Intent(this.activity, LoginActivity.class)));
                    this.activity.finish();
                } else
                    closed.set(false);
            })).addListener(Main.exceptionListenerWithToast(this.activity));
        });
        disconnection.setOnClickListener(v -> {
            if (!closed.compareAndSet(false, true))
                return;
            this.activity.startActivity(new Intent(this.activity, LoginActivity.class));
            this.activity.finish();
        });
        this.pageCache.set(page);
        return page;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
