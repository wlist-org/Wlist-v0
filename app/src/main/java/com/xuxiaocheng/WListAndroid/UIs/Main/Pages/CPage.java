package com.xuxiaocheng.WListAndroid.UIs.Main.Pages;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.viewbinding.ViewBinding;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.UIs.IPage;

public abstract class CPage<P extends ViewBinding> extends IPage<P> {
    @UiThread
    protected void cOnConnect() {
    }

    @UiThread
    protected void cOnDisconnect() {
    }

    @WorkerThread
    public void onConnect() {
        Main.runOnUiThread(this.activity(), this::cOnConnect);
    }

    @WorkerThread
    public void onDisconnect() {
        Main.runOnUiThread(this.activity(), this::cOnDisconnect);
    }
}
