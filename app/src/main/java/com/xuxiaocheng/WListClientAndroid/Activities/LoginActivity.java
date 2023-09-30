package com.xuxiaocheng.WListClientAndroid.Activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListClientAndroid.Client.PasswordManager;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {
    public static final @NotNull HInitializer<InetSocketAddress> internalServerAddress = new HInitializer<>("InternalServerAddress");

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating LoginActivity.");
        this.setContentView(R.layout.activity_login);
        final TextView internalServer = this.findViewById(R.id.activity_login_login_internal_server);
        final AtomicBoolean clickable = new AtomicBoolean(true);
        internalServer.setOnClickListener(v -> { // TODO: Rationalize code.
            if (!clickable.compareAndSet(true, false))
                return;
            final Intent serverIntent = new Intent(this, InternalServerService.class);
            logger.log(HLogLevel.LESS, "Starting internal server...");
            internalServer.setText(R.string.activity_login_loading_starting_internal_server);
            this.startService(serverIntent);
            this.bindService(serverIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    final AtomicBoolean finishActivity = new AtomicBoolean(true);
                    Main.runOnBackgroundThread(LoginActivity.this, HExceptionWrapper.wrapRunnable(() -> {
                        logger.log(HLogLevel.INFO, "Waiting for server start completely...");
                        if (LoginActivity.internalServerAddress.isInitialized() && InternalServerService.getMainStage(iService) > 1) {
                            Main.runOnNewBackgroundThread(LoginActivity.this, HExceptionWrapper.wrapRunnable(() -> {
                                LoginActivity.this.unbindService(this);
                                synchronized (LoginActivity.internalServerAddress) {
                                    while (LoginActivity.internalServerAddress.isInitialized())
                                        LoginActivity.internalServerAddress.wait();
                                }
                                LoginActivity.this.startService(serverIntent);
                                LoginActivity.this.bindService(serverIntent, this, Context.BIND_AUTO_CREATE);
                            }));
                            finishActivity.set(false);
                            return;
                        }
                        final InetSocketAddress address = InternalServerService.getAddress(iService);
                        logger.log(HLogLevel.INFO, "Connecting to service: ", address);
                        Main.runOnUiThread(LoginActivity.this, () -> internalServer.setText(R.string.activity_login_loading_connecting));
                        assert !LoginActivity.internalServerAddress.isInitialized() || LoginActivity.internalServerAddress.getInstance().equals(address);
                        LoginActivity.internalServerAddress.initializeIfNot(() -> address);
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        PasswordManager.initialize(LoginActivity.this.getExternalFilesDir("passwords"));
                        final String initPassword = InternalServerService.getAndDeleteAdminPassword(iService);
                        if (initPassword != null)
                            PasswordManager.registerInternalPassword(IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        final String password = PasswordManager.getInternalPassword(IdentifierNames.UserName.Admin.getIdentifier());
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
                        final InetSocketAddress address = LoginActivity.internalServerAddress.getInstanceNullable();
                        if (address != null) {
                            logger.log(HLogLevel.INFO, "Disconnecting to service: ", address);
                            WListClientManager.quicklyUninitialize(address);
                        }
                        synchronized (LoginActivity.internalServerAddress) {
                            LoginActivity.internalServerAddress.uninitialize();
                            LoginActivity.internalServerAddress.notifyAll();
                        }
                    });
                }
            }, Context.BIND_AUTO_CREATE);
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, "EXTERNAL_STORAGE".hashCode());
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String @NotNull [] permissions, final int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == "EXTERNAL_STORAGE".hashCode() && grantResults.length == 2 && (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED))
            Main.showToast(this, R.string.toast_no_permissions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying LoginActivity.");
    }
}
