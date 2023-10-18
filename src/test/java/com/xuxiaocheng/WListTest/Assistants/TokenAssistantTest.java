package com.xuxiaocheng.WListTest.Assistants;

import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import com.xuxiaocheng.WListTest.Operations.ServerWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Disabled
public class TokenAssistantTest extends ServerWrapper {
    @Test
    public void test() throws IOException, WrongStateException, InterruptedException {
        ServerConfiguration.set(ServerConfiguration.parse(new ByteArrayInputStream("token_expire_time: 60".getBytes(StandardCharsets.UTF_8))));

        TokenAssistant.login(this.address(), this.adminUsername(), this.adminPassword(), BackgroundTaskManager.BackgroundExecutors);
        final String token = TokenAssistant.getToken(this.address(), this.adminUsername());
        TimeUnit.SECONDS.sleep(60 - 30 + 1);
        Assertions.assertNotEquals(token,  TokenAssistant.getToken(this.address(), this.adminUsername()));
    }
}
