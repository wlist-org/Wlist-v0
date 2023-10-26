package com.xuxiaocheng.WListAndroid.Utils;

import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import com.qw.soul.permission.SoulPermission;
import com.qw.soul.permission.bean.Permission;
import com.qw.soul.permission.bean.Permissions;
import com.qw.soul.permission.callbcak.CheckRequestPermissionsListener;
import com.xuxiaocheng.WListAndroid.Main;
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
}
