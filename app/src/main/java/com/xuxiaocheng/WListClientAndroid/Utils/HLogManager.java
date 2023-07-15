package com.xuxiaocheng.WListClientAndroid.Utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class HLogManager {
    private HLogManager() {
        super();
    }

    static {
        HLogManager.buildInstance("DefaultLogger", Integer.MIN_VALUE);
        HLogManager.buildInstance("NetworkLogger", Integer.MIN_VALUE);
        HLogManager.buildInstance("ServerLogger", Integer.MIN_VALUE);
        HLogManager.buildInstance("ClientLogger", Integer.MIN_VALUE);
    }

    @NonNull public static HLog getInstance(@NonNull final Context context, @NonNull final String name) {
        HMergedStream.initializeDefaultFileOutputStream(new File(context.getExternalCacheDir(), "logs/" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss")) + ".log"));
        final HLog instance = HLog.getInstance(name);
        if (HLog.isLogTime()) {
            HLog.setLogTime(false);
            instance.log(HLogLevel.FINE, "Initialized HLog in thread: ", Thread.currentThread());
        }
        return instance;
    }

    public static void buildInstance(@NonNull final String name, final int level) {
        HLog.createInstance(name, level, false, false, new OutputStream() {
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
                if (message.contains("[VERBOSE]"))
                    priority = Log.VERBOSE;
                if (message.contains("[DEBUG]") || message.contains("[NETWORK]"))
                    priority = Log.DEBUG;
                if (message.contains("[LESS]") || message.contains("[FINE]") || message.contains("[INFO]") || message.contains("[ENHANCED]"))
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
