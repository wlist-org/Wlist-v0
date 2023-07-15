package com.xuxiaocheng.WListClientAndroid.UI;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.Helpers.WListClientManager;
import com.xuxiaocheng.WListClientAndroid.Helpers.WListServerManager;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.SocketAddress;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final HLog logger = HLogManager.getInstance(this, "DefaultLogger");
        this.setContentView(R.layout.login_activity);
        final Intent serverIntent = new Intent(this, WListServerManager.class);
        this.startService(serverIntent);
        this.bindService(serverIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, @NonNull final IBinder iService) {
                WListClientManager.ThreadPool.submit(HExceptionWrapper.wrapRunnable(() -> {
                    final WListServerManager.ServerBinder service = (WListServerManager.ServerBinder) iService;
                    final SocketAddress address = service.getAddress();
                    if (address == null)
                        throw new IllegalStateException("Failed to initialize WList server.");
                    logger.log(HLogLevel.INFO, "Initializing WListClients.", ParametersMap.create().add("address", address));
                    WListClientManager.initialize(new WListClientManager.ClientManagerConfig(address, 1, 2, 64));
                }, e -> {
                    if (e != null) {
                        logger.log(HLogLevel.FAULT, "Failed to initialize clients.", e);
                        // TODO Toast.
                    } else {
                        logger.log(HLogLevel.FINE, "Clients initialized.");
                        LoginActivity.this.runOnUiThread(() -> LoginActivity.this.startActivity(new Intent(LoginActivity.this, MainActivity.class)));
                    }
                    LoginActivity.this.finish();
                }));
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
