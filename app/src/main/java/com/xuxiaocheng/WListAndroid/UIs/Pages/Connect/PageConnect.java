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
    public PageConnect(final @NotNull ActivityMain mainActivity) {
        super(mainActivity);
    }

    @Override
    protected @NotNull PageConnectBinding onCreate(final @NotNull LayoutInflater inflater) {
        return PageConnectBinding.inflate(inflater);
    }

    private static final @NotNull AtomicBoolean clickable = new AtomicBoolean(true);
    private final @NotNull HInitializer<ServiceConnection> connection = new HInitializer<>("ServiceConnection");

    @Override
    protected void onBuild(final @NotNull PageConnectBinding page) {
        page.pageConnectionInternalServer.setOnClickListener(v -> {
            if (!PageConnect.clickable.compareAndSet(true, false)) return;
            final Intent serverIntent = new Intent(this.mainActivity, InternalServerService.class);
            final HLog logger = HLogManager.getInstance("DefaultLogger");
            logger.log(HLogLevel.LESS, "Starting internal server...");
            page.pageConnectionInternalServer.setText(R.string.page_connect_internal_server_starting);
            this.mainActivity.startService(serverIntent);
            this.connection.initializeIfNot(() -> new ServiceConnection() {
                    private final @NotNull ActivityMain activity = PageConnect.this.mainActivity;
                    private final @NotNull TextView text = PageConnect.this.getPage().pageConnectionInternalServer;
                    private final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("InternalServerAddress");

                    @Override
                    public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                        final HLog logger = HLogManager.getInstance("DefaultLogger");
                        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                            final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                            if (address == null) {
                                PageConnect.this.mainActivity.unbindService(this);
                                Main.showToast(PageConnect.this.mainActivity, R.string.page_connect_closing);
                                return;
                            }
                            logger.log(HLogLevel.FINE, "Connecting to service: ", address);
                            Main.runOnUiThread(this.activity, () -> this.text.setText(R.string.page_connect_connecting));
                            this.address.initialize(address);
                            WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                            logger.log(HLogLevel.LESS, "Clients initialized.");
                            final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                            final String password = PasswordHelper.updateInternalPassword(this.activity, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                            logger.log(HLogLevel.INFO, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                            Main.runOnUiThread(this.activity, () -> this.text.setText(R.string.page_connect_logging_in));
                            if (password == null || !TokenAssistant.login(address, IdentifierNames.UserName.Admin.getIdentifier(), password, Main.ClientExecutors)) {
                                // TODO get password from user.
                                Main.runOnUiThread(this.activity, () -> Toast.makeText(this.activity, "No password!!!", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            this.activity.connect(address, IdentifierNames.UserName.Admin.getIdentifier(), iService);
                        }, e -> {
                            Main.runOnUiThread(this.activity, () -> this.text.setText(R.string.page_connect_internal_server));
                            PageConnect.clickable.set(true);
                            if (e != null) {
                                logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                                Main.showToast(this.activity, R.string.toast_fatal_application_initialization);
                            }
                        }, true));
                    }

                    @Override
                    public void onServiceDisconnected(final ComponentName name) {
                        PageConnect.clickable.set(true);
                        final HLog logger = HLogManager.getInstance("DefaultLogger");
                        Main.runOnBackgroundThread(this.activity, () -> {
                            final InetSocketAddress address = this.address.uninitializeNullable();
                            if (address != null) {
                                logger.log(HLogLevel.INFO, "Disconnecting to service: ", address);
                                WListClientManager.quicklyUninitialize(address);
                            }
                        });
                        Main.showToast(this.activity, R.string.page_connect_internal_server_disconnected);
                        this.activity.close();
                    }
                });
            this.mainActivity.bindService(serverIntent, this.connection.getInstance(),
                    Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
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
    public void onDisconnected() {
        final ServiceConnection connection = this.connection.uninitializeNullable();
        if (connection != null)
            this.mainActivity.unbindService(connection);
    }
}
