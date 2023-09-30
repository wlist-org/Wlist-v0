package com.xuxiaocheng.WListClientAndroid;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ResourceBundle;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assert.assertEquals("com.xuxiaocheng.wlist", appContext.getPackageName());
    }

    @Test
    public void serverI18n() {
        final ResourceBundle bundle = ResourceBundle.getBundle("lang/wlist");
        bundle.getString("client.network.closed_client");
    }
}
