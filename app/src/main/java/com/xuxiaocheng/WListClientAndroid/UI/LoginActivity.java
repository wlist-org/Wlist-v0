package com.xuxiaocheng.WListClientAndroid.UI;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.Helpers.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Service.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final HLog logger = HLogManager.getInstance(this, "DefaultLogger");
        this.setContentView(R.layout.login_activity);
        final Intent serverIntent = new Intent(this, InternalServerService.class);
        logger.log(HLogLevel.LESS, "Starting internal server...");
        this.startService(serverIntent);
        this.bindService(serverIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, @NonNull final IBinder iService) {
                if (!iService.isBinderAlive()) {
                    logger.log(HLogLevel.WARN, "Dead iServer.");
                    return;
                }
                WListClientManager.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                    logger.log(HLogLevel.INFO, "Waiting for server start completely...");
                    final SocketAddress[] address = new SocketAddress[1];
                    InternalServerService.sendTransact(iService, InternalServerService.TransactOperate.GetAddress, null, p -> {
                        if (p.readInt() != 0)
                            throw new IllegalStateException("Failed to get internal server address.");
                        final String hostname = p.readString();
                        final int port = p.readInt();
                        address[0] = new InetSocketAddress(hostname, port);
                    });
                    logger.log(HLogLevel.INFO, "Connecting to: ", address[0]);
                    WListClientManager.initialize(new WListClientManager.ClientManagerConfig(address[0], 1, 2, 64));
                    logger.log(HLogLevel.FINE, "Clients initialized.");
                    LoginActivity.this.runOnUiThread(() -> LoginActivity.this.startActivity(new Intent(LoginActivity.this, MainActivity.class)));
                }, e -> {
                    if (e != null) {
                        logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                        LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this.getApplicationContext(), R.string.fatal_application_initialization, Toast.LENGTH_LONG).show());
                    }
                    LoginActivity.this.finish();
                }, true));
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
