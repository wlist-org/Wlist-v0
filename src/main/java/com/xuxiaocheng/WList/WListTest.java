package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverConfiguration_123Pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public final class WListTest {
    private WListTest() {
        super();
    }

    public static void main(final String[] args) throws IOException, SQLException, IllegalParametersException {
        GlobalConfiguration.init(null);
        final DriverConfiguration_123Pan config = new DriverConfiguration_123Pan();
        try (final InputStream stream = new BufferedInputStream(new FileInputStream("configs/123pan.yaml"))) {
            config.load(YamlHelper.loadYaml(stream), List.of()); // assert no error.
        }
        new Driver_123Pan().login(config);
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream("configs/123pan.yaml"))) {
            YamlHelper.dumpYaml(config.dump(), stream);
        }
//        DriverManager_123pan.recursiveRefreshDirectory(config, 0, new DrivePath("/"), null, WListServer.IOExecutors);
        HLog.DefaultLogger.log(HLogLevel.FINE, "Success");
    }
}
