package com.xuxiaocheng.WList;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HLoggerStream;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.UserManager;
import com.xuxiaocheng.WList.Server.UserSqlHelper;
import com.xuxiaocheng.WList.Server.UserTokenHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public static void main(final String[] args) throws IOException, SQLException, IllegalParametersException {
        WList.logger.log(HLogLevel.FINE, "Hello WList! Initializing...");
        GlobalConfiguration.init(new BufferedInputStream(new FileInputStream("config.yml")));
        UserSqlHelper.init(UserManager.DefaultPermission, UserManager.AdminPermission);

        final String token = UserTokenHelper.generateToken("admin");
        HLog.DefaultLogger.log("", token);
        HLog.DefaultLogger.log("", UserTokenHelper.resolveToken(token));

//        final WListServer server = WListServer.getInstance(new InetSocketAddress(GlobalConfiguration.getInstance().getPort()));
//        server.start().syncUninterruptibly();

//        final InputStream is = new BufferedInputStream(new FileInputStream("test.yml"));
//        final DriverConfiguration_123Pan config = new Yaml().loadAs(is, DriverConfiguration_123Pan.class);
//        is.close();
//
//        final Driver_123Pan driver = new Driver_123Pan();
//        driver.login(config);
//        WList.logger.log(HLogLevel.INFO, config);
//
//        final Representer representer = new FieldOrderRepresenter();
//        config.setConfigClassTag(representer);
//        final OutputStream os = new BufferedOutputStream(new FileOutputStream("test.yml"));
//        os.write(new Yaml(representer).dump(config).getBytes(StandardCharsets.UTF_8));
//        os.close();
    }
}
