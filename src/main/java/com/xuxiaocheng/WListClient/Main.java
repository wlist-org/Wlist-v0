package com.xuxiaocheng.WListClient;

import com.xuxiaocheng.HeadLibs.DataStructures.OptionalNullable;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClient.OperationHelpers.OperateFileHelper;
import com.xuxiaocheng.WListClient.OperationHelpers.OperateServerHelper;
import com.xuxiaocheng.WListClient.OperationHelpers.OperateUserHelper;
import com.xuxiaocheng.WListClient.OperationHelpers.WrongStateException;
import com.xuxiaocheng.WListClient.Server.DrivePath;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClient.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
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
import java.util.List;
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

    static final int state = 4; static final String password = "123456";
    public static void main(final String[] args) throws IOException, InterruptedException, WrongStateException {
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
                final String oldPassword = "pFFPAKVh";
                final String token = OperateUserHelper.login(client, "admin", oldPassword);
                assert token != null;
                final boolean success = OperateUserHelper.changePassword(client, token, oldPassword, Main.password);
                assert success;
            }
            final String token = OperateUserHelper.login(client, "admin", Main.password);
            assert token != null;
            final Pair.ImmutablePair<Long, List<VisibleFileInformation>> list =
                    OperateFileHelper.listFiles(client, token, new DrivePath("/123pan/test"), 3, 0, Options.OrderPolicy.FileName, Options.OrderDirection.ASCEND);
            assert list != null;
            HLog.DefaultLogger.log("", list);
            if ((Main.state & 2) > 0) {
                final byte[] content = "CS WList Tester-Client".getBytes(StandardCharsets.UTF_8);
                final OptionalNullable<String> id = OperateFileHelper.requestUploadFile(client, token, new DrivePath("/123pan/test/t.txt"),
                                content.length, MiscellaneousUtil.getMd5(content), Options.DuplicatePolicy.ERROR);
                assert id != null;
                Main.logger.log(HLogLevel.FINE, id);
                if (id.isPresent()) {
                    final ByteBuf buffer = Unpooled.wrappedBuffer(content);
                    final OptionalNullable<VisibleFileInformation> info = OperateFileHelper.uploadFile(client, token, id.get(), 0, buffer);
                    assert info != null && info.isPresent();
                    Main.logger.log(HLogLevel.FINE, info);
                }
            }
            if ((Main.state & 4) > 0) {
                final boolean success = OperateServerHelper.closeServer(client, token);
                assert success;
            }
        } finally {
            client.stop();
        }
    }
}
