package com.xuxiaocheng.WListAndroid.UIs;

import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.ClientConfigurationSupporter;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WListAndroid.Helpers.BundleHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Pages.PageMain;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.databinding.ActivityBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityMain extends IPagedActivity {
    protected final @NotNull HInitializer<InetSocketAddress> address = new HInitializer<>("ActivityMainAddress");
    protected final @NotNull HInitializer<String> username = new HInitializer<>("ActivityMainUsername");
    protected final @NotNull HInitializer<IBinder> binder = new HInitializer<>("ActivityMainServerBinder");
    protected final @NotNull AtomicBoolean realConnected = new AtomicBoolean(false);
    public boolean isConnected() {
        return this.address.isInitialized() && this.username.isInitialized();
    }
    public @NotNull InetSocketAddress address() {
        return this.address.getInstance();
    }
    public @NotNull String username() {
        return this.username.getInstance();
    }
    public @NotNull WListClientInterface client() throws IOException {
        return WListClientManager.quicklyGetClient(this.address());
    }
    public @NotNull String token() {
        return TokenAssistant.getToken(this.address(), this.username());
    }

    @Override
    protected void iOnRestoreInstanceState(final @Nullable Bundle savedInstanceState) {
        super.iOnRestoreInstanceState(savedInstanceState);
        this.address.uninitializeNullable();
        this.username.uninitializeNullable();
        this.binder.uninitializeNullable();
        if (savedInstanceState != null) {
            BundleHelper.restoreClient(savedInstanceState, "wlist:activity_main:client", this.address, this.username, null);
            final IBinder binder = savedInstanceState.getBinder("wlist:activity_main:binder");
            if (binder != null)
                this.binder.reinitialize(binder);
        }
    }

    @Override
    protected void iOnSaveInstanceState(final @NotNull Bundle outState) {
        super.iOnSaveInstanceState(outState);
        BundleHelper.saveClient(this.address, this.username, outState, "wlist:activity_main:client", null);
        final IBinder binder = this.binder.getInstanceNullable();
        if (binder != null)
            outState.putBinder("wlist:activity_main:binder", binder);
    }

    @Override
    protected void iOnBuildActivity(final @NotNull ActivityBinding content, final boolean isFirstTime) {
        super.iOnBuildActivity(content, isFirstTime);
        if (isFirstTime)
            this.push(new PageMain(), "main", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.isConnected() != this.realConnected.get() && this.isConnected())
            Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> this.connect(this.address.getInstance(), this.username.getInstance(), this.binder.getInstanceNullable())));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.isConnected())
            if (WListClientManager.instances.isNotInitialized(this.address.getInstance())) {
                Main.showToast(this, R.string.activity_main_server_closed);
                Main.runOnBackgroundThread(this, this::disconnect);
            } else this.addWListCloseListener(this.address.getInstance());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.isConnected())
            WListClientManager.removeAllListeners(this.address.getInstance());
    }

    @AnyThread
    private void addWListCloseListener(final @NotNull InetSocketAddress address) {
        WListClientManager.addListener(address, i -> {
            if (!i.booleanValue()) {
                Main.showToast(this, R.string.activity_main_server_closed);
                this.disconnect();
                WListClientManager.removeAllListeners(address);
            }
        });
    }

    @WorkerThread
    public void connect(final @NotNull InetSocketAddress address, final @NotNull String username, final @Nullable IBinder binder) throws IOException {
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Connecting.", ParametersMap.create()
                .add("address", address).add("username", username).add("binder", binder != null));
        this.realConnected.set(true);
        this.address.reinitialize(address);
        this.username.reinitialize(username);
        this.binder.reinitializeNullable(binder);
        this.addWListCloseListener(address);
        BroadcastAssistant.start(address);
        BroadcastAssistant.get(address).ServerClose.register(id -> this.disconnect());
        ClientConfigurationSupporter.quicklySetLocation(new File(this.getExternalFilesDir("client"), "client.yaml"));
    }

    @WorkerThread
    public void disconnect() {
        this.realConnected.set(false);
        final InetSocketAddress address = this.address.uninitializeNullable();
        final String username = this.username.uninitializeNullable();
        final boolean binder = this.binder.uninitializeNullable() != null;
        if (address == null || username == null) {
            HLogManager.getInstance("DefaultLogger").log(HLogLevel.MISTAKE, "Disconnect twice.", ParametersMap.create()
                    .add("address", address).add("username", username).add("binder", binder));
            return;
        }
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Disconnecting.", ParametersMap.create()
                .add("address", address).add("username", username).add("binder", binder));
        WListClientManager.removeAllListeners(address);
        BroadcastAssistant.stop(address);
        TokenAssistant.removeToken(address, username);
        WListClientManager.quicklyUninitialize(address);
    }

    @Override
    public @NotNull String toString() {
        return "ActivityMain{" +
                "address=" + this.address +
                ", username=" + this.username +
                ", binder=" + this.binder +
                ", realConnected=" + this.realConnected +
                ", super=" + super.toString() +
                '}';
    }
}
