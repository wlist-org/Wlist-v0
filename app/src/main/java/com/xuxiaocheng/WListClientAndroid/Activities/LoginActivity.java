package com.xuxiaocheng.WListClientAndroid.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WListClientAndroid.Client.PasswordManager;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating LoginActivity.");
        this.setContentView(R.layout.login_activity);
        final TextView internalServer = this.findViewById(R.id.login_internal_server);
        internalServer.setOnClickListener(v -> {
            final Intent serverIntent = new Intent(this, InternalServerService.class);
            logger.log(HLogLevel.LESS, "Starting internal server...");
            internalServer.setText(R.string.starting_internal_server);
            this.startService(serverIntent);
            this.bindService(serverIntent, new ServiceConnection() {
                @NonNull private final HInitializer<InetSocketAddress> internalServerAddress = new HInitializer<>("InternalServerAddress");

                @Override
                public void onServiceConnected(final ComponentName name, @NonNull final IBinder iService) {
                    Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                        logger.log(HLogLevel.INFO, "Waiting for server start completely...");
                        final InetSocketAddress address = InternalServerService.getAddress(iService);
                        logger.log(HLogLevel.INFO, "Connecting to: ", address);
                        LoginActivity.this.runOnUiThread(() -> internalServer.setText(R.string.loading_clients));
                        this.internalServerAddress.initialize(address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        PasswordManager.initialize(LoginActivity.this.getExternalFilesDir("passwords"));
                        final String initPassword = InternalServerService.getAndDeleteAdminPassword(iService);
                        if (initPassword != null)
                            PasswordManager.registerInternalPassword(UserManager.ADMIN, initPassword);
                        final String password = PasswordManager.getInternalPassword(UserManager.ADMIN);
                        logger.log(HLogLevel.ENHANCED, "Got server password.", ParametersMap.create().add("password", password));
                        if (password != null)
                            TokenManager.setToken(address, UserManager.ADMIN, password);
                        else {
                            // TODO get password from user.
                            LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this, "No password!!!", Toast.LENGTH_SHORT).show());
                        }
                        MainActivity.start(LoginActivity.this, address);
                    }, e -> {
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                            LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this.getApplicationContext(), R.string.fatal_application_initialization, Toast.LENGTH_LONG).show());
                        }
                        LoginActivity.this.finish();
                    }, true)).addListener(Main.ThrowableListenerWithToast(LoginActivity.this));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    Main.ThreadPool.submit(() -> {
                        final InetSocketAddress address = this.internalServerAddress.getInstanceNullable();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                    }).addListener(Main.ThrowableListenerWithToast(LoginActivity.this));
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
