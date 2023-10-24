package com.xuxiaocheng.WListAndroid;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.HeadLibs;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Assistants.BroadcastAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateHelper;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Operations.ServerHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HeadLibs.setDebugMode(true);
        HUncaughtExceptionHelper.disableUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey);
        final Pattern ips = Pattern.compile("/((\\[?(?<ipv6>(([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:)(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))]?)|(?<ipv4>((2[0-4]\\d|25[0-5])|[0-1]? \\d{0,2})(\\.((2[0-4]\\d|25[0-5])|[0-1]? \\d{0,2})){3})):(?<port>(6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])|[0-5]?\\d{0,4})$");
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) -> {
            try {
                if (e instanceof IOException && e.getMessage() != null) {
                    final Matcher matcher = ips.matcher(e.getMessage());
                    if (matcher.find()) {
                        final String host = Objects.requireNonNullElseGet(matcher.group("ipv6"), () -> matcher.group("ipv4"));
                        final int port = Integer.parseInt(Objects.requireNonNull(matcher.group("port")));
                        final SocketAddress address = new InetSocketAddress(host, port);
                        final WListClientManager manager = WListClientManager.instances.getInstanceNullable(address);
                        if (manager == null) return;
                        try {
                            manager.getClient().close();
                        } catch (final ConnectException | RuntimeException exception) {
                            return;
                        }
                    }
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                e.addSuppressed(exception);
            }
            HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Uncaught exception listened by WList Android.", ParametersMap.create().add("thread", t.getName()), e);
        });
        final Thread.UncaughtExceptionHandler defaulter = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey);
        final Thread.UncaughtExceptionHandler killer = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey);
        HUncaughtExceptionHelper.replaceUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey, (t, e) -> {
            if (Looper.getMainLooper().getThread() == t) {
                if (defaulter != null) defaulter.uncaughtException(t, e);
                if (killer != null) killer.uncaughtException(t, e);
            }
        }); // Kill in main thread.
        HLog.setLogTimeFLength(3);
        OperateHelper.LogOperation.set(false);
        BroadcastAssistant.LogBroadcastEvent.set(false);
        ServerHandler.LogActive.set(false);
        ServerHandler.LogOperation.set(false);
        Log.i("HLog-WList", "Hello WList (Android v0.1.1)!" + ParametersMap.create().add("pid", Process.myPid()));
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

    public static void runOnUiThread(final @Nullable Activity activity, @UiThread final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() == Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                activity.runOnUiThread(wrappedRunnable);
            else
                Main.mainHandler.post(wrappedRunnable);
    }

    public static void runOnNextUiThread(final @Nullable Activity activity, @UiThread final @NotNull Runnable runnable) {
        Main.AndroidExecutors.submit(() -> Main.runOnUiThread(activity, runnable));
    }

    public static void runOnUiThread(final @Nullable Activity activity, @UiThread final @NotNull Runnable runnable, final long delay, final @NotNull TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(() -> activity.runOnUiThread(wrappedRunnable), delay, unit);
        else
            Main.mainHandler.postDelayed(wrappedRunnable, unit.toMillis(delay));
    }

    public static void runOnBackgroundThread(final @Nullable Activity activity, @WorkerThread final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (Main.mainHandler.getLooper().getThread() != Thread.currentThread())
            wrappedRunnable.run();
        else
            if (activity != null)
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
            else
                Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnNextBackgroundThread(final @Nullable Activity activity, @WorkerThread final @NotNull Runnable runnable) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.submit(wrappedRunnable).addListener(MiscellaneousUtil.exceptionListener());
    }

    public static void runOnBackgroundThread(final @Nullable Activity activity, @WorkerThread final @NotNull Runnable runnable, final long delay, final @NotNull TimeUnit unit) {
        final Runnable wrappedRunnable = Main.wrapRunnable(activity, runnable);
        if (activity != null)
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(Main.exceptionListenerWithToast(activity));
        else
            Main.AndroidExecutors.schedule(wrappedRunnable, delay, unit).addListener(MiscellaneousUtil.exceptionListener());
    }
}
