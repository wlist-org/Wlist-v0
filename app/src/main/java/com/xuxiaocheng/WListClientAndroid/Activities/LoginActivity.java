package com.xuxiaocheng.WListClientAndroid.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListClientAndroid.Helpers.PasswordHelper;
import com.xuxiaocheng.WListClientAndroid.Helpers.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServer.InternalServerBinder;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating LoginActivity.");
        this.setContentView(R.layout.activity_login);
        final TextView internalServer = this.findViewById(R.id.activity_login_login_internal_server);
        final AtomicBoolean clickable = new AtomicBoolean(true);
        internalServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false))
                return;
            final Intent serverIntent = new Intent(this, InternalServerService.class);
            logger.log(HLogLevel.LESS, "Starting internal server...");
            internalServer.setText(R.string.activity_login_loading_starting_internal_server);
            this.startService(serverIntent);
            this.bindService(serverIntent, new ServiceConnection() {
                private final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("InternalServerAddress");

                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    Main.runOnBackgroundThread(LoginActivity.this, HExceptionWrapper.wrapRunnable(() -> {
                        final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                        logger.log(HLogLevel.INFO, "Connecting to service: ", address);
                        Main.runOnUiThread(LoginActivity.this, () -> internalServer.setText(R.string.activity_login_loading_connecting));
                        this.address.initialize(address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                        final String password = PasswordHelper.updateInternalPassword(LoginActivity.this, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        logger.log(HLogLevel.ENHANCED, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        Main.runOnUiThread(LoginActivity.this, () -> internalServer.setText(R.string.activity_login_loading_logging_in));
                        boolean success = password != null;
                        if (success)
                            success = TokenManager.setToken(address, IdentifierNames.UserName.Admin.getIdentifier(), password);
                        if (!success) {
                            // TODO get password from user.
                            Main.runOnUiThread(LoginActivity.this, () -> Toast.makeText(LoginActivity.this, "No password!!!", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        MainActivity.start(LoginActivity.this, address);
                        LoginActivity.this.finish();
                    }, e -> {
                        Main.runOnUiThread(LoginActivity.this, () -> internalServer.setText(R.string.activity_login_login_internal_server));
                        clickable.set(true);
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e.getLocalizedMessage());
                            Main.showToast(LoginActivity.this, R.string.toast_fatal_application_initialization);
                            LoginActivity.this.unbindService(this);
                        }
                    }, false));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    Main.runOnBackgroundThread(LoginActivity.this, () -> {
                        final InetSocketAddress address = this.address.uninitializeNullable();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to service: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                    });
                }
            }, Context.BIND_AUTO_CREATE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying LoginActivity.");
    }
}
