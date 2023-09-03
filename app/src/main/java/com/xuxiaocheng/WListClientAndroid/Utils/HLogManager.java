package com.xuxiaocheng.WListClientAndroid.Utils;

import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import com.xuxiaocheng.WListClient.Client.OperationHelpers.OperateHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HLogManager {
    private HLogManager() {
        super();
    }

    @NonNull private static final Collection<String> loggers = Set.of("DefaultLogger", "NetworkLogger", "ServerLogger", "ClientLogger", "DriverLogger", "DriverLogger/lanzou");
    static {
        HLog.setLogTimeFLength(3);
        for (final String name: HLogManager.loggers)
            HLogManager.buildInstance(name, "DefaultLogger".equals(name) || "ClientLogger".equals(name) ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1);
        OperateHelper.logOperation.set(false);
        HUncaughtExceptionHelper.disableUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey); // Application Killer
        HUncaughtExceptionHelper.setUncaughtExceptionListener(HUncaughtExceptionHelper.ListenerKey, (t, e) ->
                HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Uncaught exception listened by WList Android.", ParametersMap.create().add("thread", t.getName()).add("pid", Process.myPid()), e));
        final Thread.UncaughtExceptionHandler defaulter = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.DefaultKey);
        final Thread.UncaughtExceptionHandler killer = HUncaughtExceptionHelper.getUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey);
        HUncaughtExceptionHelper.replaceUncaughtExceptionListener(HUncaughtExceptionHelper.KillerKey, (t, e) -> {
            if (Looper.getMainLooper().getThread() == t) {
                if (defaulter != null) defaulter.uncaughtException(t, e);
                if (killer != null) killer.uncaughtException(t, e);
            }
        });
    }

    @NonNull public static final AtomicBoolean initialized = new AtomicBoolean(false);
    public static void initialize(@NonNull final Context context, @NonNull final String processName) {
        if (!HLogManager.initialized.compareAndSet(false, true))
            return;
        try {
            HMergedStreams.initializeDefaultFileOutputStream(new File(context.getApplicationContext().getExternalCacheDir(), "logs/" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss")) + '.' + processName + ".log"));
        } catch (final IOException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        final OutputStream fileOutputStream = HMergedStreams.getFileOutputStreamNoException(null);
        for (final String loggerName: HLogManager.loggers)
            HLog.getInstance(loggerName).getStreams().add(fileOutputStream);
    }

    @NonNull public static HLog getInstance(@NonNull final String name) {
        final HLog instance = HLogManager.loggers.contains(name) ? HLog.getInstance(name) : HLog.getInstance("DefaultLogger");
        if (!HLogManager.loggers.contains(name))
            instance.log(HLogLevel.BUG, "Getting unexpected HLog.", ParametersMap.create().add("name", name));
        return instance;
    }

    public static void buildInstance(@NonNull final String name, final int level) {
        HLog.createInstance(name, level, true, false, new OutputStream() {
            private final ByteArrayOutputStream cache = new ByteArrayOutputStream(256);
            private int lastPriority = Log.INFO;

            @Override
            public void write(final int i) {
                if (i != '\n') {
                    this.cache.write(i);
                    return;
                }
                final String message = this.cache.toString();
                this.cache.reset();
                int priority = this.lastPriority;
                if (message.contains("[VERBOSE]") || message.contains("[LESS]"))
                    priority = Log.VERBOSE;
                if (message.contains("[DEBUG]") || message.contains("[NETWORK]"))
                    priority = Log.DEBUG;
                if (message.contains("[FINE]") || message.contains("[INFO]") || message.contains("[ENHANCED]"))
                    priority = Log.INFO;
                if (message.contains("[MISTAKE]") || message.contains("[WARN]"))
                    priority = Log.WARN;
                if (message.contains("[ERROR]"))
                    priority = Log.ERROR;
                if (message.contains("[FAULT]") || message.contains("[BUG]"))
                    priority = Log.ASSERT;
                Log.println(priority, "HLog", message);
                this.lastPriority = priority;
            }
        });
    }
}
