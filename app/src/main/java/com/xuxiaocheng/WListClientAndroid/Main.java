package com.xuxiaocheng.WListClientAndroid;

import android.app.Activity;
import android.app.Application;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FutureListener;

public final class Main extends Application {
    @NonNull public static final EventExecutorGroup AndroidExecutors =
            new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("AndroidExecutors"));

    @NonNull public static FutureListener<? super Object> exceptionListenerWithToast(@NonNull final Activity activity) {
        return f -> {
            final Throwable cause = f.cause();
            if (cause == null)
                return;
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), cause);
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), cause.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("HLog", "Hello WList Client (Android v0.1.0)." + ParametersMap.create().add("pid", Process.myPid()));
    }
}
