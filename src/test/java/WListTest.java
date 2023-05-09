import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class WListTest {
    @Test
    public void globalConfig() throws IOException {
        GlobalConfiguration.init(new File("run/server.properties"));
        HLog.DefaultLogger.log(HLogLevel.INFO, GlobalConfiguration.getInstance());
    }
}
