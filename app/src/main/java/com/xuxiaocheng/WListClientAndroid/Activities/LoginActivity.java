package com.xuxiaocheng.WListClientAndroid.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WListClient.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Client.PasswordManager;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {
    @NonNull private static final HInitializer<InetSocketAddress> internalServerAddress = new HInitializer<>("InternalServerAddress");

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating LoginActivity.");
        this.setContentView(R.layout.activity_login);
        final TextView internalServer = this.findViewById(R.id.activity_login_login_internal_server);
        final View exit = this.findViewById(R.id.activity_login_exit);
        internalServer.setOnClickListener(v -> {
            final Intent serverIntent = new Intent(this, InternalServerService.class);
            logger.log(HLogLevel.LESS, "Starting internal server...");
            internalServer.setText(R.string.activity_login_loading_starting_internal_server);
            this.startService(serverIntent);
            this.bindService(serverIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, @NonNull final IBinder iService) {
                    final AtomicBoolean finishActivity = new AtomicBoolean(true);
                    Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                        logger.log(HLogLevel.INFO, "Waiting for server start completely...");
                        if (LoginActivity.internalServerAddress.isInitialized() && InternalServerService.getMainStage(iService) > 1) {
                            Main.AndroidExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                                LoginActivity.this.unbindService(this);
                                synchronized (LoginActivity.internalServerAddress) {
                                    while (LoginActivity.internalServerAddress.isInitialized())
                                        LoginActivity.internalServerAddress.wait();
                                }
                                LoginActivity.this.startService(serverIntent);
                                LoginActivity.this.bindService(serverIntent, this, Context.BIND_AUTO_CREATE);
                            })).addListener(Main.exceptionListenerWithToast(LoginActivity.this));
                            finishActivity.set(false);
                            return;
                        }
                        final InetSocketAddress address = InternalServerService.getAddress(iService);
                        logger.log(HLogLevel.INFO, "Connecting to: ", address);
                        LoginActivity.this.runOnUiThread(() -> internalServer.setText(R.string.activity_login_loading_connecting));
                        assert !LoginActivity.internalServerAddress.isInitialized() || LoginActivity.internalServerAddress.getInstance().equals(address);
                        LoginActivity.internalServerAddress.initializeIfNot(() -> address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        PasswordManager.initialize(LoginActivity.this.getExternalFilesDir("passwords"));
                        final String initPassword = InternalServerService.getAndDeleteAdminPassword(iService);
                        if (initPassword != null)
                            PasswordManager.registerInternalPassword(UserManager.ADMIN, initPassword);
                        final String password = PasswordManager.getInternalPassword(UserManager.ADMIN);
                        logger.log(HLogLevel.ENHANCED, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        LoginActivity.this.runOnUiThread(() -> internalServer.setText(R.string.activity_login_loading_logging_in));
                        boolean success = password != null;
                        if (success)
                            success = TokenManager.setToken(address, UserManager.ADMIN, password);
                        if (success) {
                            // TODO get password from user.
                            LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this, "No password!!!", Toast.LENGTH_SHORT).show());
                        }
                        MainActivity.start(LoginActivity.this, address);
                        LoginActivity.this.finish();
                    }, e -> {
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e.getLocalizedMessage());
                            LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this.getApplicationContext(), R.string.toast_fatal_application_initialization, Toast.LENGTH_LONG).show());
                        }
                    }, false)).addListener(Main.exceptionListenerWithToast(LoginActivity.this));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    Main.AndroidExecutors.submit(() -> {
                        final InetSocketAddress address = LoginActivity.internalServerAddress.uninitialize();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                        synchronized (LoginActivity.internalServerAddress) {
                            LoginActivity.internalServerAddress.uninitialize(); // assert == null;
                            LoginActivity.internalServerAddress.notifyAll();
                        }
                    }).addListener(Main.exceptionListenerWithToast(LoginActivity.this));
                }
            }, Context.BIND_AUTO_CREATE);
        });
        exit.setOnClickListener(v -> this.finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying LoginActivity.");
    }
}
