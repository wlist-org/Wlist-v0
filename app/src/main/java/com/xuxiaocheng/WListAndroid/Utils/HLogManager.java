package com.xuxiaocheng.WListAndroid.Utils;

import android.content.Context;
import android.util.Log;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HLogManager {
    private HLogManager() {
        super();
    }

    public enum ProcessType {
        Activity,
        Server,
    }

    public static final @NotNull AtomicBoolean initialized = new AtomicBoolean(false);
    public static boolean initialize(final @NotNull Context context, final @NotNull ProcessType type) {
        if (!HLogManager.initialized.compareAndSet(false, true))
            return true;
        try {
            HMergedStreams.initializeDefaultFileOutputStream(new File(context.getApplicationContext().getExternalCacheDir(), "logs/" +
                    ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss", Locale.getDefault())) + '.' + type + ".log"));
        } catch (final IOException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        final OutputStream fileOutputStream = HMergedStreams.getFileOutputStreamNoException(null);
        HLog.LoggerCreateCore.reinitialize(name -> HLog.createInstance(name, HLogLevel.VERBOSE.getLevel(), true, false, new OutputStream() {
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
                if (message.contains("[VERBOSE]") || message.contains("[DEBUG]"))
                    priority = Log.VERBOSE;
                if (message.contains("[LESS]") || message.contains("[NETWORK]"))
                    priority = Log.DEBUG;
                if (message.contains("[FINE]") || message.contains("[INFO]") || message.contains("[ENHANCED]"))
                    priority = Log.INFO;
                if (message.contains("[MISTAKE]") || message.contains("[WARN]"))
                    priority = Log.WARN;
                if (message.contains("[ERROR]"))
                    priority = Log.ERROR;
                if (message.contains("[FAULT]") || message.contains("[BUG]"))
                    priority = Log.ASSERT;
                Log.println(priority, "HLog-WList", message);
                this.lastPriority = priority;
            }
        }, fileOutputStream));
        return false;
    }

    public static @NotNull HLog getInstance(final @NotNull String logger) {
        return HLog.getInstance(logger) == HLog.DefaultLogger ? HLog.create(logger) : HLog.getInstance(logger);
    }
}
