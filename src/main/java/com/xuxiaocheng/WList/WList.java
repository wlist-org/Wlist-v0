package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Configuration.FieldOrderRepresenter;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverConfiguration_123Pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverUtil_123pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public final class WList {
    private WList() {
        super();
    }

    public static final boolean DeepDebugMode = false;
    public static final boolean DebugMode = !new File(WList.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            WList.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getPriority() + 1,
            true, new HLoggerStream(true, !WList.DebugMode));

    @SuppressWarnings("SpellCheckingInspection")
    public static void main(final String[] args) throws IllegalParametersException, IOException, SQLException {
        final InputStream is = new BufferedInputStream(new FileInputStream("test.yml"));
        final DriverConfiguration_123Pan config = new Yaml().loadAs(is, DriverConfiguration_123Pan.class);
        is.close();

        final Driver_123Pan driver = new Driver_123Pan();
        driver.login(config);
        WList.logger.log(HLogLevel.INFO, config);

        final Representer representer = new FieldOrderRepresenter();
        config.setConfigClassTag(representer);
        final OutputStream os = new BufferedOutputStream(new FileOutputStream("test.yml"));
        os.write(new Yaml(representer).dump(config).getBytes(StandardCharsets.UTF_8));
        os.close();

        HLog.DefaultLogger.log("", DriverUtil_123pan.getFileInformation(config, new DrivePath("/AutoCopy"), false, null));

        if (false) {
            final byte[] context = new byte[]{10, 20, 30};
            final InputStream inputStream = new ByteArrayInputStream(context);
            DriverUtil_123pan.doUpload(config, new DrivePath("test/hello.text"), inputStream, 3, MiscellaneousUtil.getMd5(context), null);
        }
//        Operation.init();
//        WList.logger.log(HLogLevel.FINE, "Hello WList! Initializing...");
//        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("config.yml")));
//        UserHelper.init();
//        TokenHelper.init();
//        final WListServer server = WListServer.getInstance(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
//        server.start().syncUninterruptibly();
    }
}
