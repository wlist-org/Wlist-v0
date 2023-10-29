package com.xuxiaocheng.WListAndroid.UIs.Fragments.File;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListAndroid.Helpers.PasswordHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerBinder;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragmentPart;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class PartConnect extends IFragmentPart<PageFileBinding, FragmentFile> {
    protected PartConnect(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void onConnected(final @NotNull ActivityMain activity) {
        super.onConnected(activity);
        Main.runOnUiThread(activity, () -> this.page().pageFileConnectionInternalServer.setVisibility(View.GONE));
    }

    @Override
    protected void onDisconnected(final @NotNull ActivityMain activity) {
        super.onDisconnected(activity);
        Main.runOnUiThread(activity, () -> {
            this.page().pageFileConnectionInternalServer.setVisibility(View.VISIBLE);
            this.disconnectInternalServer();
            this.disconnectExternalServer();
        });
    }

    @Override
    protected void onBuild(@NotNull final PageFileBinding page) {
        super.onBuild(page);
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageFileConnectionInternalServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false)) return;
            this.connectInternalServer((a, s) -> Main.runOnUiThread(a, () -> page.pageFileConnectionInternalServer.setText(s)), a -> {
                clickable.set(true);
                Main.runOnUiThread(a, () -> page.pageFileConnectionInternalServer.setText(R.string.page_connect_internal_server));
            });
        });
    }

    private static final @NotNull HInitializer<ActivityMain> binderActivity = new HInitializer<>("BinderActivityMain");
    private static final @NotNull HInitializer<ServiceConnection> binderConnection = new HInitializer<>("BinderConnection");

    @UiThread
    private void connectInternalServer(@AnyThread final @NotNull BiConsumer<? super @NotNull ActivityMain, ? super @NotNull String> text, @AnyThread final @NotNull Consumer<? super @NotNull ActivityMain> failure) {
        final ActivityMain activity = this.activity();
        try {
            final Intent serverIntent = new Intent(activity, InternalServerService.class);
            final HLog logger = HLogManager.getInstance("DefaultLogger");
            if (PartConnect.binderConnection.isInitialized() || PartConnect.binderActivity.isInitialized()) {
                logger.log(HLogLevel.MISTAKE, "Internal server service is started.");
                failure.accept(activity);
                return;
            }
            logger.log(HLogLevel.LESS, "Starting internal server...");
            final ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                        final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                        if (address == null) {
                            activity.unbindService(this);
                            PartConnect.binderConnection.uninitialize();
                            PartConnect.binderActivity.uninitialize();
                            logger.log(HLogLevel.LESS, "Internal server is stopping...");
                            Main.showToast(activity, R.string.page_connect_closing);
                            Main.runOnUiThread(activity, () -> failure.accept(activity));
                            return;
                        }
                        logger.log(HLogLevel.FINE, "Connecting to service: ", address);
                        text.accept(activity, activity.getString(R.string.page_connect_connecting));
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                        final String password = PasswordHelper.updateInternalPassword(activity, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        logger.log(HLogLevel.INFO, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        text.accept(activity, activity.getString(R.string.page_connect_logging_in));
                        if (password == null || !TokenAssistant.login(address, IdentifierNames.UserName.Admin.getIdentifier(), password, Main.ClientExecutors)) {
                            // TODO get password from user.
                            Main.runOnUiThread(activity, () -> Toast.makeText(activity, "No password!!!", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        activity.connect(address, IdentifierNames.UserName.Admin.getIdentifier(), iService);
                    }, e -> {
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                            Main.showToast(activity, R.string.toast_fatal_application_initialization);
                            Main.runOnUiThread(activity, () -> failure.accept(activity));
                        }
                    }, true));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    final HLog logger = HLogManager.getInstance("DefaultLogger");
                    logger.log(HLogLevel.FAULT, "Disconnecting to service.");
                    activity.disconnect();
                    Main.showToast(activity, R.string.page_connect_internal_server_disconnected);
                    failure.accept(activity);
                }
            };
            text.accept(activity, activity.getString(R.string.page_connect_internal_server_starting));
            PartConnect.binderActivity.initialize(activity);
            PartConnect.binderConnection.initialize(connection);
            activity.startService(serverIntent);
            activity.bindService(serverIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
        } finally {
            text.accept(activity, activity.getString(R.string.page_connect_internal_server));
        }
    }

    @UiThread
    private void disconnectInternalServer() {
        final ActivityMain binderActivity = PartConnect.binderActivity.uninitializeNullable();
        final ServiceConnection binderConnection = PartConnect.binderConnection.uninitializeNullable();
        if (binderConnection != null && binderActivity != null)
            binderActivity.unbindService(binderConnection);
    }

    @UiThread
    private void connectExternalServer(@UiThread final @NotNull BiConsumer<@NotNull ActivityMain, @NotNull String> text, @UiThread final @NotNull Consumer<@NotNull ActivityMain> failure, final @NotNull InetSocketAddress address) {
//        activity.activityLoginIcon.setOnClickListener(v -> { // TODO
//            if (!clickable.compareAndSet(true, false)) return;
//            Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
//                final InetSocketAddress address = new InetSocketAddress("192.168.1.9", 5212);
////                final InetSocketAddress address = new InetSocketAddress(ViewUtil.getText(activity.activityLoginPassport), Integer.parseInt(ViewUtil.getText(activity.activityLoginPassword)));
//                WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
//                if (!TokenAssistant.login(address, "admin", "Eb7aFkA2", Main.ClientExecutors))
//                    return;
//                ActivityMain.start(this, address, "admin", false);
//            }, () -> clickable.set(true)));
//        });
        Main.runOnBackgroundThread(this.activity(), () -> {throw new UnsupportedOperationException("WIP");});
    }

    @UiThread
    private void disconnectExternalServer() {
    }

    @UiThread
    protected void tryDisconnect() {
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            try (final WListClientInterface client = this.client()) {
                OperateServerHelper.closeServer(client, this.token());
            }
        }, e -> {
            //noinspection VariableNotUsedInsideIf
            if (e != null)
                this.activity().disconnect();
        }, false));
    }
}
