package com.xuxiaocheng.WListAndroid.Utils;

import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.WListAndroid.R;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class PermissionUtil {
    private PermissionUtil() {
        super();
    }

    @WorkerThread
    public static void tryGetPermission(final @NotNull Activity activity, @StringRes final int toast, final @NotNull String @NotNull ... permissions) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        XXPermissions.with(activity)
                .permission(permissions)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(final @NotNull List<@NotNull String> permissions, final boolean allGranted) {
                        if (allGranted)
                            latch.countDown();
                    }

                    @Override
                    public void onDenied(final @NotNull List<@NotNull String> permissions, final boolean doNotAskAgain) {
                        Toaster.show(toast);
                        latch.countDown();
                    }
                });
        latch.await();
    }

    @WorkerThread
    public static void readPermission(final @NotNull Activity activity) throws InterruptedException {
        PermissionUtil.tryGetPermission(activity, R.string.toast_no_read_permissions, Permission.Group.STORAGE);
        PermissionUtil.tryGetPermission(activity, R.string.toast_no_read_permissions, Permission.READ_EXTERNAL_STORAGE);
    }

    @WorkerThread
    public static void writePermission(final @NotNull Activity activity) throws InterruptedException {
        PermissionUtil.tryGetPermission(activity, R.string.toast_no_write_permissions, Permission.Group.STORAGE);
        PermissionUtil.tryGetPermission(activity, R.string.toast_no_write_permissions, Permission.WRITE_EXTERNAL_STORAGE);
    }
}
