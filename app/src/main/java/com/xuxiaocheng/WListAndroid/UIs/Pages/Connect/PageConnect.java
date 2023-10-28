package com.xuxiaocheng.WListAndroid.UIs.Pages.Connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListAndroid.Helpers.PasswordHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerBinder;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.IFragment;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.PageConnectBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageConnect extends IFragment<PageConnectBinding> {
    @Override
    protected @NotNull PageConnectBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageConnectBinding.inflate(inflater);
    }

    private static final @NotNull AtomicBoolean clickable = new AtomicBoolean(true);
    private static final @NotNull HInitializer<ActivityMain> binderActivity = new HInitializer<>("BinderActivityMain");
    private static final @NotNull HInitializer<ServiceConnection> connection = new HInitializer<>("BinderConnection");

    @Override
    protected void onBuild(final @NotNull PageConnectBinding page) {
        page.pageConnectionInternalServer.setOnClickListener(v -> {
            if (PageConnect.connection.isInitialized() || !PageConnect.clickable.compareAndSet(true, false)) return;
            final ActivityMain activity = this.activity();
            final Intent serverIntent = new Intent(activity, InternalServerService.class);
            final HLog logger = HLogManager.getInstance("DefaultLogger");
            logger.log(HLogLevel.LESS, "Starting internal server...");
            final TextView text = page.pageConnectionInternalServer;
            final ServiceConnection connection = new ServiceConnection() {
                private final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("InternalServerAddress");

                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                        final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                        if (address == null) {
                            activity.unbindService(this);
                            PageConnect.connection.uninitializeNullable();
                            logger.log(HLogLevel.LESS, "Internal server is stopping...");
                            Main.showToast(activity, R.string.page_connect_closing);
                            return;
                        }
                        final HLog logger = HLogManager.getInstance("DefaultLogger");
                        logger.log(HLogLevel.FINE, "Connecting to service: ", address);
                        Main.runOnUiThread(activity, () -> text.setText(R.string.page_connect_connecting));
                        this.address.initialize(address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                        final String password = PasswordHelper.updateInternalPassword(activity, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        logger.log(HLogLevel.INFO, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        Main.runOnUiThread(activity, () -> text.setText(R.string.page_connect_logging_in));
                        if (password == null || !TokenAssistant.login(address, IdentifierNames.UserName.Admin.getIdentifier(), password, Main.ClientExecutors)) {
                            // TODO get password from user.
                            Main.runOnUiThread(activity, () -> Toast.makeText(activity, "No password!!!", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        activity.connect(address, IdentifierNames.UserName.Admin.getIdentifier(), iService);
                    }, e -> {
                        Main.runOnUiThread(activity, () -> text.setText(R.string.page_connect_internal_server));
                        PageConnect.clickable.set(true);
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                            Main.showToast(activity, R.string.toast_fatal_application_initialization);
                        }
                    }, true));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    PageConnect.clickable.set(true);
                    final HLog logger = HLogManager.getInstance("DefaultLogger");
                    logger.log(HLogLevel.FAULT, "Disconnecting to service.");
                    Main.runOnBackgroundThread(activity, () -> {
                        final InetSocketAddress address = this.address.uninitializeNullable();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to service: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                    });
                    Main.showToast(activity, R.string.page_connect_internal_server_disconnected);
                    activity.disconnect();
                }
            };
            text.setText(R.string.page_connect_internal_server_starting);
            activity.startService(serverIntent);
            PageConnect.connection.initialize(connection);
            PageConnect.binderActivity.reinitialize(activity);
            activity.bindService(serverIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
        });
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
    }

    @Override
    public void onDisconnected(final @NotNull ActivityMain activity) {
        final ActivityMain binderActivity = PageConnect.binderActivity.uninitializeNullable();
        final ServiceConnection connection = PageConnect.connection.uninitializeNullable();
        if (connection != null && binderActivity != null)
            binderActivity.unbindService(connection);
    }

    @Override
    public @NotNull String toString() {
        return "PageConnect{" +
                "super=" + super.toString() +
                '}';
    }
}
