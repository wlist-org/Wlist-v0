import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.DriverConfiguration_123Pan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class WListTest {
    @Test
    public void driver_configuration_123_pan() throws IOException {
        GlobalConfiguration.init(null);
        final Collection<Object> list = new ArrayList<>();
        final DriverConfiguration_123Pan c = new DriverConfiguration_123Pan();
        c.load(Map.of(), list);
        HLog.DefaultLogger.log(HLogLevel.INFO, list);
    }
}
