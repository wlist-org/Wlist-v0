package com.xuxiaocheng.WListClientAndroid.Helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.MainThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WListClientAndroid.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class PermissionsHelper {
    private PermissionsHelper() {
        super();
    }

    private static volatile int code;
    private static String[] permissions;
    private static volatile Consumer<? super @NotNull Boolean> callback;
    private static final @NotNull AtomicBoolean lock = new AtomicBoolean(false);

    @MainThread
    public static void getExternalStorage(final @NotNull Activity activity, final boolean needWrite, final @NotNull Consumer<? super @NotNull Boolean> callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                callback.accept(true);
                return;
            }
            PermissionsHelper.callback = success -> {
                if (success.booleanValue())
                    callback.accept(true);
                else
                    PermissionsHelper.getExternalStorage(activity, needWrite, callback);
            }; // TODO
            activity.startActivity(new Intent(activity, StoragesActivity.class));
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            callback.accept(true);
            return;
        }
        if (!PermissionsHelper.lock.compareAndSet(false, true)) {
            Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                synchronized (PermissionsHelper.lock) {
                    while (PermissionsHelper.lock.get())
                        PermissionsHelper.lock.wait();
                }
                Main.runOnUiThread(activity, () -> PermissionsHelper.getExternalStorage(activity, needWrite, callback));
            }));
            return;
        }
        PermissionsHelper.permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsHelper.code = Math.abs(Arrays.hashCode(PermissionsHelper.permissions));
        PermissionsHelper.callback = success -> {
            if (success.booleanValue()) {
                callback.accept(true);
                return;
            }
            PermissionsHelper.getSinglePermission(activity, needWrite ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.READ_EXTERNAL_STORAGE, callback);
        };
        activity.startActivity(new Intent(activity, PermissionsActivity.class));
    }

    @MainThread
    private static void getSinglePermission(final @NotNull Activity activity, final @NotNull String permission, final @NotNull Consumer<? super @NotNull Boolean> callback) {
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
                Main.runOnUiThread(activity, () -> PermissionsHelper.getSinglePermission(activity, permission, callback));
            }));
            return;
        }
        PermissionsHelper.permissions = new String[]{permission};
        PermissionsHelper.code = Math.abs(Arrays.hashCode(PermissionsHelper.permissions));
        PermissionsHelper.callback = callback;
        activity.startActivity(new Intent(activity, PermissionsActivity.class));
    }

    public static class PermissionsActivity extends Activity {
        @Override
        protected void onCreate(final @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // TODO: display the usage of the permission.
            ActivityCompat.requestPermissions(this, PermissionsHelper.permissions, PermissionsHelper.code);
        }

        @Override
        public void onRequestPermissionsResult(final int requestCode, final String @NotNull [] permissions, final int @NotNull [] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            assert requestCode == PermissionsHelper.code;
            final Consumer<? super @NotNull Boolean> callback = PermissionsHelper.callback;
            synchronized (PermissionsHelper.lock) {
                PermissionsHelper.lock.set(false);
                PermissionsHelper.lock.notify();
            }
            this.finish();
            boolean flag = true;
            for (final int res: grantResults)
                if (res != PackageManager.PERMISSION_GRANTED) {
                    flag = false;
                    break;
                }
            callback.accept(flag);
        }
    }

    public static class StoragesActivity extends Activity {
        @Override
        protected void onCreate(final @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.startActivityForResult(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"), Math.abs("MANAGE_ALL_FILES_ACCESS_PERMISSION".hashCode()));
        }

        @Override
        protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            assert requestCode == Math.abs("MANAGE_ALL_FILES_ACCESS_PERMISSION".hashCode());
            PermissionsHelper.callback.accept(Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager());
        }
    }
}
