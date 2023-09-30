package com.xuxiaocheng.WListClientAndroid.Helpers;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListClientAndroid.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class PermissionsHelper {
    private PermissionsHelper() {
        super();
    }

    private static volatile int code;
    private static volatile String permission;
    private static volatile Consumer<? super @NotNull Boolean> callback;
    private static final @NotNull AtomicBoolean lock = new AtomicBoolean(false);

    @MainThread
    public static void getPermission(final @NotNull Activity activity, final @NotNull String permission, final @NotNull Consumer<? super @NotNull Boolean> callback) {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            callback.accept(true);
            return;
        }
        if (!PermissionsHelper.lock.compareAndSet(false, true)) {
            Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                synchronized (PermissionsHelper.lock) {
                    while (PermissionsHelper.lock.get())
                        PermissionsHelper.lock.wait();
                }
                Main.runOnUiThread(activity, () -> PermissionsHelper.getPermission(activity, permission, callback));
            }));
            return;
        }
        PermissionsHelper.code = Math.abs(permission.hashCode());
        PermissionsHelper.permission = permission;
        PermissionsHelper.callback = callback;
        activity.startActivity(new Intent(activity, PermissionsActivity.class));
    }

    public static class PermissionsActivity extends Activity {
        @Override
        protected void onCreate(final @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // TODO: display the usage of the permission.
            ActivityCompat.requestPermissions(this, new String[]{PermissionsHelper.permission}, PermissionsHelper.code);
        }

        @Override
        public void onRequestPermissionsResult(final int requestCode, final String @NotNull [] permissions, final int @NotNull [] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            assert requestCode == PermissionsHelper.code && grantResults.length == 1 && PermissionsHelper.permission.equals(permissions[0]);
            final Consumer<? super @NotNull Boolean> callback = PermissionsHelper.callback;
            synchronized (PermissionsHelper.lock) {
                PermissionsHelper.lock.set(false);
                PermissionsHelper.lock.notify();
            }
            this.finish();
            callback.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }
}
