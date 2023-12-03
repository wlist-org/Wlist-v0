package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WListAndroid.Helpers.PasswordHelper;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerBinder;
import com.xuxiaocheng.WListAndroid.Services.InternalServer.InternalServerService;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileConnectExternalServerBinding;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PartConnect extends SFragmentFilePart {
    protected PartConnect(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        final PageFileBinding page = this.fragmentContent();
        final AtomicBoolean clickable = new AtomicBoolean(true);
        page.pageFileConnectionInternalServer.setVisibility(View.VISIBLE);
        page.pageFileConnectionInternalServer.setText(R.string.page_file_connect_internal_server);
        page.pageFileConnectionInternalServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false)) return;
            page.pageFileConnectionInternalServer.setText(R.string.page_file_connect_internal_server_starting);
            this.connectInternalServer((a, s) -> Main.runOnUiThread(a, () -> page.pageFileConnectionInternalServer.setText(s)),
                    a -> Main.runOnBackgroundThread(a, this::cOnDisconnect), ConsumerE.emptyConsumer());
        });
        page.pageFileConnectionExternalServer.setVisibility(View.VISIBLE);
        page.pageFileConnectionExternalServer.setText(R.string.page_file_connect_external_server);
        page.pageFileConnectionExternalServer.setOnClickListener(v -> {
            if (!clickable.compareAndSet(true, false)) return;
            final PageFileConnectExternalServerBinding binding = PageFileConnectExternalServerBinding.inflate(this.activity().getLayoutInflater());
            final SharedPreferences preferences = this.activity().getSharedPreferences("external_server", Context.MODE_PRIVATE);
            binding.pageFileConnectExternalServerHost.setText(preferences.getString("host", ""));
            binding.pageFileConnectExternalServerPort.setText(preferences.getString("port", ""));
            binding.pageFileConnectExternalServerUsername.setText(preferences.getString("username", "admin"));
            binding.pageFileConnectExternalServerPassword.setText(preferences.getString("password", ""));
            new AlertDialog.Builder(this.activity())
                    .setTitle(R.string.page_file_connect_external_server)
                    .setView(binding.getRoot())
                    .setOnCancelListener(d -> clickable.set(true))
                    .setNegativeButton(R.string.cancel, (d, w) -> clickable.set(true))
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        final String host = ViewUtil.getText(binding.pageFileConnectExternalServerHost);
                        final String sPort = ViewUtil.getText(binding.pageFileConnectExternalServerPort);
                        final String username = ViewUtil.getText(binding.pageFileConnectExternalServerUsername);
                        final String password = ViewUtil.getText(binding.pageFileConnectExternalServerPassword);
                        final int port;
                        try {
                            port = Integer.parseInt(sPort);
                            if (port < 1 || port > 65535)
                                throw new NumberFormatException();
                        } catch (final NumberFormatException exception) {
                            Toaster.show(R.string.page_file_connect_external_server_invalid_address);
                            Main.runOnNextUiThread(this.activity(), () -> ((Dialog) d).show());
                            return;
                        }
                        Main.runOnBackgroundThread(this.activity(), () -> {
                            final InetSocketAddress address = new InetSocketAddress(host, port);
                            if (address.isUnresolved()) {
                                Toaster.show(R.string.page_file_connect_external_server_invalid_address);
                                Main.runOnUiThread(this.activity(), () -> ((Dialog) d).show());
                                return;
                            }
                            Main.runOnUiThread(this.activity(), () -> this.connectExternalServer(address, username, password, (a, s) ->
                                    Main.runOnUiThread(a, () -> page.pageFileConnectionExternalServer.setText(s)), a ->
                                    Main.runOnBackgroundThread(a, this::cOnDisconnect), a -> {
                                        Toaster.show(R.string.page_file_connect_external_server_invalid_address);
                                        Main.runOnUiThread(a, () -> ((Dialog) d).show());
                                    }, a -> preferences.edit()
                                            .putString("host", host).putString("port", sPort)
                                            .putString("username", username).putString("password", password)
                                            .apply()));
                        });
                    }).show();
        });
    }

    @Override
    public void cOnConnect() {
        super.cOnConnect();
        Main.runOnUiThread(this.activity(), () -> {
            this.fragmentContent().pageFileConnectionInternalServer.setVisibility(View.GONE);
            this.fragmentContent().pageFileConnectionExternalServer.setVisibility(View.GONE);
        });
    }

    @Override
    public void cOnDisconnect() {
        super.cOnDisconnect();
        Main.runOnUiThread(this.activity(), () -> {
            this.iOnBuildPage();
            this.disconnectInternalServer();
            this.disconnectExternalServer();
        });
    }

    private static final @NotNull HInitializer<ActivityMain> binderActivity = new HInitializer<>("BinderActivityMain");
    private static final @NotNull HInitializer<ServiceConnection> binderConnection = new HInitializer<>("BinderConnection");

    @UiThread
    private void connectInternalServer(@AnyThread final @NotNull BiConsumer<? super @NotNull ActivityMain, ? super @NotNull String> text, @AnyThread final @NotNull Consumer<? super @NotNull ActivityMain> failure, @WorkerThread final @NotNull Consumer<? super @NotNull ActivityMain> success) {
        final ActivityMain activity = this.activity();
        try {
            final Intent serverIntent = new Intent(activity, InternalServerService.class);
            final HLog logger = HLogManager.getInstance("DefaultLogger");
            if (PartConnect.binderConnection.isInitialized() || PartConnect.binderActivity.isInitialized()) {
                logger.log(HLogLevel.MISTAKE, "Internal server service is started.");
                failure.accept(activity);
                return;
            }
            logger.log(HLogLevel.LESS, "Starting internal server...");
            final ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final @NotNull IBinder iService) {
                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                        final InetSocketAddress address = InternalServerBinder.getAddress(iService);
                        if (address == null) {
                            activity.unbindService(this);
                            PartConnect.binderConnection.uninitialize();
                            PartConnect.binderActivity.uninitialize();
                            logger.log(HLogLevel.LESS, "Internal server is stopping...");
                            Toaster.show(R.string.page_file_connect_closing);
                            failure.accept(activity);
                            return;
                        }
                        logger.log(HLogLevel.FINE, "Connecting to service: ", address);
                        text.accept(activity, activity.getString(R.string.page_file_connect_connecting));
                        WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
                        logger.log(HLogLevel.LESS, "Clients initialized.");
                        final String initPassword = InternalServerBinder.getAndDeleteAdminPassword(iService);
                        final String password = PasswordHelper.updateInternalPassword(activity, IdentifierNames.UserName.Admin.getIdentifier(), initPassword);
                        logger.log(HLogLevel.INFO, "Got server password.", ParametersMap.create().add("init", initPassword != null).add("password", password));
                        text.accept(activity, activity.getString(R.string.page_file_connect_logging_in));
                        if (password == null || !TokenAssistant.login(address, IdentifierNames.UserName.Admin.getIdentifier(), password, Main.ClientExecutors)) {
                            // TODO get password from user.
                            Main.runOnUiThread(activity, () -> Toast.makeText(activity, "No password!!!", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        activity.connect(address, IdentifierNames.UserName.Admin.getIdentifier(), iService);
                        success.accept(activity);
                    }, e -> {
                        if (e != null) {
                            logger.log(HLogLevel.FAULT, "Failed to initialize wlist clients.", e);
                            Toaster.show(R.string.toast_fatal_application_initialization);
                            failure.accept(activity);
                        }
                    }, true));
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    final HLog logger = HLogManager.getInstance("DefaultLogger");
                    logger.log(HLogLevel.FAULT, "Disconnecting to service.");
                    activity.disconnect();
                    Toaster.show(R.string.page_file_connect_internal_server_disconnected);
                    failure.accept(activity);
                }
            };
            text.accept(activity, activity.getString(R.string.page_file_connect_internal_server_starting));
            PartConnect.binderActivity.initialize(activity);
            PartConnect.binderConnection.initialize(connection);
            activity.startService(serverIntent);
            activity.bindService(serverIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
        } finally {
            text.accept(activity, activity.getString(R.string.page_file_connect_internal_server));
        }
    }

    @UiThread
    private void disconnectInternalServer() {
        final ActivityMain binderActivity = PartConnect.binderActivity.uninitializeNullable();
        final ServiceConnection binderConnection = PartConnect.binderConnection.uninitializeNullable();
        if (binderConnection != null && binderActivity != null)
            binderActivity.unbindService(binderConnection);
    }

    @UiThread
    private void connectExternalServer(final @NotNull InetSocketAddress address, final @NotNull String username, final @NotNull String password, @UiThread final @NotNull BiConsumer<? super @NotNull ActivityMain, ? super @NotNull String> text, @UiThread final @NotNull Consumer<? super @NotNull ActivityMain> failure, @WorkerThread final @NotNull Consumer<? super @NotNull ActivityMain> invalid, @WorkerThread final @NotNull Consumer<? super @NotNull ActivityMain> success) {
        final ActivityMain activity = this.activity();
        text.accept(activity, activity.getString(R.string.page_file_connect_connecting));
        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
            WListClientManager.quicklyInitialize(WListClientManager.getDefault(address));
            text.accept(activity, activity.getString(R.string.page_file_connect_logging_in));
            if (!TokenAssistant.login(address, username, password, Main.ClientExecutors)) {
                invalid.accept(activity);
                return;
            }
            activity.connect(address, username, null);
            success.accept(activity);
        }, e -> {
            if (e != null) {
                HLogManager.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Failed to connect wlist clients.", e);
                Toaster.show(R.string.toast_fatal_application_initialization);
                failure.accept(activity);
            }
        }, true));
    }

    @UiThread
    private void disconnectExternalServer() {
    }

    @UiThread
    protected void tryCloseServer() {
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            try (final WListClientInterface client = this.client()) {
                OperateServerHelper.closeServer(client, this.token());
            }
        }, e -> {
            //noinspection VariableNotUsedInsideIf
            if (e != null)
                this.activity().disconnect();
        }, false));
    }
}
