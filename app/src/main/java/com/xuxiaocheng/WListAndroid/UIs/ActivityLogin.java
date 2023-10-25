package com.xuxiaocheng.WListAndroid.UIs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityLoginBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityLogin extends AppCompatActivity {

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityLogin.");
        final ActivityLoginBinding activity = ActivityLoginBinding.inflate(this.getLayoutInflater());
        this.setContentView(activity.getRoot());
        final AtomicBoolean clickable = new AtomicBoolean(true);
        activity.activityLoginLoginInternalServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            final Intent serverIntent = new Intent(this, InternalServerService.class);
            logger.log(HLogLevel.LESS, "Starting internal server...");
            activity.activityLoginLoginInternalServer.setText(R.string.activity_login_loading_starting_internal_server);
            this.startService(serverIntent);
            this.bindService(serverIntent, new ServiceConnection() {
                private final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("InternalServerAddress");

                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    Main.runOnBackgroundThread(ActivityLogin.this, HExceptionWrapper.wrapRunnable(() -> {
                        final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                        logger.log(HLogLevel.FINE, "Connecting to service: ", address);
                        Main.runOnUiThread(ActivityLogin.this, () -> activity.activityLoginLoginInternalServer.setText(R.string.activity_login_loading_connecting));
                        this.address.initialize(address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                        final String password = PasswordHelper.updateInternalPassword(ActivityLogin.this, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        logger.log(HLogLevel.INFO, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        Main.runOnUiThread(ActivityLogin.this, () -> activity.activityLoginLoginInternalServer.setText(R.string.activity_login_loading_logging_in));
                        if (password == null || !TokenAssistant.login(address, IdentifierNames.UserName.Admin.getIdentifier(), password, Main.ClientExecutors)) {
                            // TODO get password from user.
                            Main.runOnUiThread(ActivityLogin.this, () -> Toast.makeText(ActivityLogin.this, "No password!!!", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        ActivityMain.start(ActivityLogin.this, address, IdentifierNames.UserName.Admin.getIdentifier(), true);
                        ActivityLogin.this.finish();
                    }, e -> {
                        Main.runOnUiThread(ActivityLogin.this, () -> activity.activityLoginLoginInternalServer.setText(R.string.activity_login_login_internal_server));
                        clickable.set(true);
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                            Main.showToast(ActivityLogin.this, R.string.toast_fatal_application_initialization);
                            ActivityLogin.this.unbindService(this);
                        }
                    }, true));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    Main.runOnBackgroundThread(ActivityLogin.this, () -> {
                        final InetSocketAddress address = this.address.uninitializeNullable();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to service: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                    });
                    final Intent intent = new Intent(ActivityLogin.this, ActivityLogin.class);
                    ActivityLogin.this.startActivity(intent);
                    ActivityLogin.this.finish();
                }
            }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
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
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying ActivityLogin.");
    }
}