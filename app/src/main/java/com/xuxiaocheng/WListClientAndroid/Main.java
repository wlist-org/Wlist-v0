package com.xuxiaocheng.WListClientAndroid;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helper.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;

import java.util.concurrent.atomic.AtomicInteger;

public final class Main extends Application {
    @NonNull public static final EventExecutorGroup ThreadPool =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutor"));

    @NonNull public static final FutureListener<? super Object> ThrowableListener = f -> {
        if (f.cause() != null)
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
    };

    @NonNull public static FutureListener<? super Object> ThrowableListenerWithToast(@NonNull final Activity activity) {
        return f -> {
            if (f.cause() != null) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), f.cause());
                activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), f.cause().getLocalizedMessage(), Toast.LENGTH_SHORT).show());
            }
        };
    }

    @NonNull private final AtomicInteger activeActivities = new AtomicInteger(0);

    public void waitApplicationForeground() throws InterruptedException {
        synchronized (this.activeActivities) {
            while (this.activeActivities.get() <= 0)
                this.activeActivities.wait();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("HLog", "Hello WList Client (Android v0.1.0)." + ParametersMap.create().add("pid", Process.myPid()));
        this.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull final Activity activity) {
                synchronized (Main.this.activeActivities) {
                    Main.this.activeActivities.incrementAndGet();
                    Main.this.activeActivities.notifyAll();
                }
            }

            @Override
            public void onActivityResumed(@NonNull final Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull final Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull final Activity activity) {
                synchronized (Main.this.activeActivities) {
                    Main.this.activeActivities.decrementAndGet();
                    Main.this.activeActivities.notifyAll();
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull final Activity activity) {
            }
        });
    }

    @Override
    @NonNull public String toString() {
        return "Main{" +
                "activeActivities=" + this.activeActivities +
                "} " + super.toString();
    }
}
