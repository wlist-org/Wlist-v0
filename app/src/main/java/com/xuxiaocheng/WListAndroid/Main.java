package com.xuxiaocheng.WListAndroid;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.HeadLibs;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Operations.OperateHelper;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Operations.ServerHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Main extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HeadLibs.setDebugMode(true);
        HUncaughtExceptionHelper.disableUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey);
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Uncaught exception listened by WList Android.", ParametersMap.create().add("thread", t.getName()), e));
        final Thread.UncaughtExceptionHandler defaulter = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey);
        final Thread.UncaughtExceptionHandler killer = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey);
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey, (t, e) -> {
            if (Looper.getMainLooper().getThread() == t) {
                if (defaulter != null) defaulter.uncaughtException(t, e);
                if (killer != null) killer.uncaughtException(t, e);
            }
        }); // Kill in main thread.
        HLog.setLogTimeFLength(3);
        OperateHelper.LogOperation.set(true);
        ServerHandler.LogActive.set(false);
        ServerHandler.LogOperation.set(false);
        Log.i("HLog", "Hello WList (Android v0.1.0)!" + ParametersMap.create().add("pid", Process.myPid()));
    }

    public static final @NotNull EventExecutorGroup ClientExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("ClientExecutors"));

    private static final @NotNull EventExecutorGroup AndroidExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutors"));

    private static final @NotNull Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void showToast(final @NotNull Activity activity, @StringRes final int message) {
        activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    public static @NotNull FutureListener<? super Object> exceptionListenerWithToast(final @NotNull Activity activity) {
        return f -> {
            final Throwable cause = f.cause();
            if (cause == null) return;
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), cause.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), cause);
        };
    }

    private static @NotNull Runnable wrapRunnable(final @Nullable Activity activity, final @NotNull Runnable runnable) {
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

    public static void runOnUiThread(final @Nullable Activity activity, final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() == Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                activity.runOnUiThread(wrappedRunnable);
            else
                Main.mainHandler.post(wrappedRunnable);
    }

    public static void runOnUiThread(final @Nullable Activity activity, final @NotNull Runnable runnable, final long delay, final @NotNull TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(() -> activity.runOnUiThread(wrappedRunnable), delay, unit);
        else
            Main.mainHandler.postDelayed(wrappedRunnable, unit.toMillis(delay));
    }

    public static void runOnBackgroundThread(final @Nullable Activity activity, final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() != Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
            else
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnNewBackgroundThread(final @Nullable Activity activity, final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnBackgroundThread(final @Nullable Activity activity, final @NotNull Runnable runnable, final long delay, final @NotNull TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(MiscellaneousUtil.exceptionListener());
    }
}