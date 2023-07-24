package com.xuxiaocheng.WListClientAndroid.Activities;

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
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.Client.AddressManager;
import com.xuxiaocheng.WListClientAndroid.Client.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Services.InternalServerService;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, "Activities");
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating LoginActivity.");
        this.setContentView(R.layout.login_activity);

//        Main.ThreadPool.submit(() -> {
            final Intent serverIntent = new Intent(this, InternalServerService.class);
//            HExceptionWrapper.wrapRunnable(() -> ((Main) this.getApplication()).waitApplicationForeground()).run();
            logger.log(HLogLevel.LESS, "Starting internal server...");
//            this.runOnUiThread(() -> {
                this.startService(serverIntent);
                this.bindService(serverIntent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(final ComponentName name, @NonNull final IBinder iService) {
                        Main.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                            logger.log(HLogLevel.INFO, "Waiting for server start completely...");
                            final SocketAddress[] address = new SocketAddress[1];
                            InternalServerService.sendTransact(iService, InternalServerService.TransactOperate.GetAddress, null, p -> {
                                final int success = p.readInt();
                                if (success != 0)
                                    throw new IllegalStateException("Failed to get internal server address." + ParametersMap.create().add("code", success));
                                final String hostname = p.readString();
                                final int port = p.readInt();
                                address[0] = new InetSocketAddress(hostname, port);
                            });
                            logger.log(HLogLevel.INFO, "Connecting to: ", address[0]);
                            AddressManager.internalServerAddress.initialize(address[0]);
                            WListClientManager.quicklyInitialize(WListClientManager.getDefault(address[0]));
                            logger.log(HLogLevel.LESS, "Clients initialized.");
                            LoginActivity.this.runOnUiThread(() -> LoginActivity.this.startActivity(new Intent(LoginActivity.this, MainActivity.class)));
                        }, e -> {
                            if (e != null) {
                                logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                                LoginActivity.this.runOnUiThread(() -> Toast.makeText(LoginActivity.this.getApplicationContext(), R.string.fatal_application_initialization, Toast.LENGTH_LONG).show());
                            }
                            LoginActivity.this.finish();
                        }, true)).addListener(Main.ThrowableListener);
                    }

                    @Override
                    public void onServiceDisconnected(final ComponentName name) {
                    }
                }, Context.BIND_AUTO_CREATE);
//            });
//        }).addListener(Main.ThrowableListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying LoginActivity.");
    }
}
