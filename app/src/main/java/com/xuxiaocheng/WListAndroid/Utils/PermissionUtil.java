package com.xuxiaocheng.WListAndroid.Utils;

import android.Manifest;
import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import com.qw.soul.permission.SoulPermission;
import com.qw.soul.permission.bean.Permission;
import com.qw.soul.permission.bean.Permissions;
import com.qw.soul.permission.callbcak.CheckRequestPermissionsListener;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

public final class PermissionUtil {
    private PermissionUtil() {
        super();
    }

    @WorkerThread
    public static void tryGetPermission(final @NotNull Activity activity, final @NotNull Permissions permissions, @StringRes final int toast) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Main.runOnUiThread(activity, () -> SoulPermission.getInstance().checkAndRequestPermissions(permissions, new CheckRequestPermissionsListener() {
            @Override
            public void onAllPermissionOk(final Permission[] allPermissions) {
                latch.countDown();
            }

            @Override
            public void onPermissionDenied(final Permission[] refusedPermissions) {
                Main.showToast(activity, toast);
                latch.countDown();
            }
        }));
        latch.await();
    }

    @WorkerThread
    public static void readPermission(final @NotNull Activity activity) throws InterruptedException {
        PermissionUtil.tryGetPermission(activity, Permissions.build(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), R.string.toast_no_read_permissions);
        PermissionUtil.tryGetPermission(activity, Permissions.build(Manifest.permission.READ_EXTERNAL_STORAGE), R.string.toast_no_read_permissions);
    }

    @WorkerThread
    public static void writePermission(final @NotNull Activity activity) throws InterruptedException {
        PermissionUtil.tryGetPermission(activity, Permissions.build(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), R.string.toast_no_write_permissions);
        PermissionUtil.tryGetPermission(activity, Permissions.build(Manifest.permission.WRITE_EXTERNAL_STORAGE), R.string.toast_no_write_permissions);
    }
}
