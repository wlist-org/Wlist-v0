package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.time.ZonedDateTime;

public class OperateSelfTest extends ServerWrapper {
    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(1)
    public void logon(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        Assumptions.assumeTrue(OperateSelfHelper.logon(client, this.username(), this.password()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.Logon, pair.getFirst());
            final VisibleUserInformation information = VisibleUserInformation.parse(pair.getSecond());
            Assertions.assertEquals(this.username(), information.username());
            Assertions.assertEquals(UserGroupManager.getInstance().getDefaultId(), information.groupId());
            Assertions.assertEquals(IdentifierNames.UserGroupName.Default.getIdentifier(), information.groupName());
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void login(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final Pair.ImmutablePair<String, ZonedDateTime> token = OperateSelfHelper.login(client, this.username(), this.password());
        Assertions.assertNotNull(token);
        this.token(token.getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(6)
    public void logoff(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateSelfHelper.logoff(client, this.token(), this.password()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.Logoff, pair.getFirst());
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(3)
    public void changeUsername(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changeUsername(client, this.token(), username));
        this.username(username);
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangeUsername, pair.getFirst());
            ByteBufIOUtil.readVariableLenLong(pair.getSecond());
            Assertions.assertEquals(username,  ByteBufIOUtil.readUTF(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(4)
    public void changePassword(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changePassword(client, this.token(), this.password(), password));
        this.password(password);
        // refresh token
        this.login(client);
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangePassword, pair.getFirst());
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(5)
    public void getSelfGroup(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final VisibleUserGroupInformation information = OperateSelfHelper.getSelfGroup(client, this.token());
        Assertions.assertNotNull(information);
        Assertions.assertEquals(UserGroupManager.getInstance().getDefaultId(),  information.id());
        Assertions.assertEquals(IdentifierNames.UserGroupName.Default.getIdentifier(),  information.name());
        Assertions.assertEquals(UserPermission.Default, information.permissions());
    }


    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminLogon(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertFalse(OperateSelfHelper.logon(client, this.adminUsername(), password));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminLogin(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final Pair.ImmutablePair<String, ZonedDateTime> token = OperateSelfHelper.login(client, this.adminUsername(), this.adminPassword());
        Assertions.assertNotNull(token);
        this.adminToken(token.getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void adminLogoff(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertFalse(OperateSelfHelper.logoff(client, this.adminToken(), this.adminPassword()));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void adminChangeUsername(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final String username = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertFalse(OperateSelfHelper.changeUsername(client, this.adminToken(), username));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(3)
    public void adminChangePassword(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateSelfHelper.changePassword(client, this.adminToken(), this.adminPassword(), password));
        this.adminPassword(password);
        // refresh token
        this.adminLogin(client);
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangePassword, pair.getFirst());
            Assertions.assertEquals(UserManager.getInstance().getAdminId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void adminGetSelfGroup(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final VisibleUserGroupInformation information = OperateSelfHelper.getSelfGroup(client, this.adminToken());
        Assertions.assertNotNull(information);
        Assertions.assertEquals(UserGroupManager.getInstance().getAdminId(),  information.id());
        Assertions.assertEquals(IdentifierNames.UserGroupName.Admin.getIdentifier(),  information.name());
        Assertions.assertEquals(UserPermission.All, information.permissions());
    }


    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void invalidLogon(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final String password = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertFalse(OperateSelfHelper.logon(client, this.username(), password));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void invalidLogoff(final WListClientInterface client) {
        Assertions.assertThrows(WrongStateException.class, () -> OperateSelfHelper.logoff(client, "", ""));
    }
}
