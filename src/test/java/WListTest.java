import com.xuxiaocheng.WList.Server.Driver.DriverManager;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class WListTest {
    @Test
    public void driver_configuration_123_pan() throws IOException {
        GlobalConfiguration.init(null);
        DriverManager.init();
    }
}
