package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.Server.DrivePath;
import com.xuxiaocheng.WListClient.Utils.MiscellaneousUtil;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.random.RandomGenerator;

public final class Main {
    private Main() {
        super();
    }

    public static final boolean DebugMode = !new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).isFile();

    private static final HLog logger = HLog.createInstance("DefaultLogger",
            Main.DebugMode ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1,
            true);

    public static final @NotNull BigInteger key;
    public static final @NotNull BigInteger vector;
    static {
        final RandomGenerator generator = new Random(5212);
        final byte[] bytes = new byte[512];
        generator.nextBytes(bytes);
        key = new BigInteger(bytes);
        generator.nextBytes(bytes);
        vector = new BigInteger(bytes);
    }

    static final int state = 0; static final String password = "123456";
    public static void main(final String[] args) throws IOException, InterruptedException {
        Main.logger.log(HLogLevel.FINE, "Hello WList Client!");
        final Properties properties = new Properties(); {
            final File path = new File("client.properties");
            final boolean e = !path.createNewFile();
            try (final @NotNull InputStream stream = new BufferedInputStream(new FileInputStream(path))) {
                properties.load(stream);
            } catch (final FileNotFoundException exception) {
                assert e;
                throw exception;
            }
        }
        final String ip = properties.getProperty("ip", "localhost");
        final int port = Integer.valueOf(properties.getProperty("port", "5212")).intValue();
        final WListClient client = new WListClient(new InetSocketAddress(ip, port));
        try {
            if ((Main.state & 1) > 0) {
                final String oldPassword = "5W4lrLcr";
                final String token = OperateHelper.login(client, "admin", oldPassword);
                OperateHelper.changePassword(client, token, oldPassword, Main.password);
            }
            final String token = OperateHelper.login(client, "admin", Main.password);
            {
                final byte[] content = "%CS WList Tester".getBytes(StandardCharsets.UTF_8);
                final Optional<String> id =
                        OperateHelper.requestUploadFile(client, token, new DrivePath("/123pan/test.txt"),
                                content.length, MiscellaneousUtil.getMd5(content));
                Main.logger.log(HLogLevel.FINE, id);
                if (id.isPresent()) {
                    OperateHelper.uploadFile(client, token, id.get(), 0, Unpooled.wrappedBuffer(content));
                }
            }
            OperateHelper.closeServer(client, token);
        } finally {
            client.stop();
        }
    }
}
