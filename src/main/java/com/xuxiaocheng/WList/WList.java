package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverConfiguration_123Pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverHelper_123pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverSQLHelper_123pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

    @SuppressWarnings("SpellCheckingInspection")
    public static void main(final String[] args) throws IOException, IllegalParametersException, SQLException {
        final InputStream is = new BufferedInputStream(new FileInputStream("test.yml"));
        final DriverConfiguration_123Pan config = new Yaml().loadAs(is, DriverConfiguration_123Pan.class);
        is.close();

        final Driver_123Pan driver = new Driver_123Pan();
        driver.login(config);
        DriverHelper_123pan.getDirectoryId(config, new DrivePath("/AutoCopy"), true, true);
        HLog.DefaultLogger.log("", DriverSQLHelper_123pan.countPath("123pan", new DrivePath("")));

//        DriverHelper_123pan.doListFiles(config, 0, 1);

//        final Representer representer = new FieldOrderRepresenter();
//        config.getDumpMapClasses().forEach(c -> representer.addClassTag(c, Tag.MAP));
//        final OutputStream os = new BufferedOutputStream(new FileOutputStream("test.yml"));
//        os.write(new Yaml(representer).dump(config).getBytes(StandardCharsets.UTF_8));
//        os.close();

//        Operation.init();
//        WList.logger.log(HLogLevel.FINE, "Hello WList! Initializing...");
//        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("config.yml")));
//        UserHelper.init();
//        TokenHelper.init();
//        final WListServer server = WListServer.getInstance(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
//        server.start().syncUninterruptibly();
    }
}
