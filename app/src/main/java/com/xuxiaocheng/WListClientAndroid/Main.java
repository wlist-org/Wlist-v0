package com.xuxiaocheng.WListClientAndroid;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Main extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("HLog", "Hello WList (Android v0.1.0)!" + ParametersMap.create().add("pid", Process.myPid()));
    }

    @NonNull private static final EventExecutorGroup AndroidExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutors"));

    @NonNull private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void showToast(@NonNull final Activity activity, @StringRes final int message) {
        activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @NonNull public static FutureListener<? super Object> exceptionListenerWithToast(@NonNull final Activity activity) {
        return f -> {
            final Throwable cause = f.cause();
            if (cause == null) return;
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), cause.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), cause);
        };
    }

    @NonNull private static Runnable wrapRunnable(@Nullable final Activity activity, @NonNull final Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable throwable) {
                if (activity != null) {
                    final String message = Objects.requireNonNullElse(throwable.getLocalizedMessage(), "");
                    activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), message.length() < 8 ? throwable.toString() : message, Toast.LENGTH_SHORT).show());
                }
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
            }
        };
    }

    public static void runOnUiThread(@Nullable final Activity activity, @NonNull final Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() == Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                activity.runOnUiThread(wrappedRunnable);
            else
                Main.mainHandler.post(wrappedRunnable);
    }

    public static void runOnUiThread(@Nullable final Activity activity, @NonNull final Runnable runnable, final long delay, @NonNull final TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(() -> activity.runOnUiThread(wrappedRunnable), delay, unit);
        else
            Main.mainHandler.postDelayed(wrappedRunnable, unit.toMillis(delay));
    }

    public static void runOnBackgroundThread(@Nullable final Activity activity, @NonNull final Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() != Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
            else
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnNewBackgroundThread(@Nullable final Activity activity, @NonNull final Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnBackgroundThread(@Nullable final Activity activity, @NonNull final Runnable runnable, final long delay, @NonNull final TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(MiscellaneousUtil.exceptionListener());
    }
}
