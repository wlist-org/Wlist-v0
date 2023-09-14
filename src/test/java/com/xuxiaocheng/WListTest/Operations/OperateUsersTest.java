package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateUsersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupManager;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

public class OperateUsersTest extends ServerWrapper {
    @DisplayName("Prepare")
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(0)
    public void login(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final String token = OperateSelfHelper.login(client, this.adminUsername(), this.adminPassword());
        Assumptions.assumeTrue(token != null);
        this.adminToken(token);

        Assumptions.assumeTrue(OperateSelfHelper.logon(client, this.username(), this.password()));
        this.token(Objects.requireNonNull(OperateSelfHelper.login(client, this.username(), this.password())));
        this.userId(2);
    }

    protected static final @NotNull HInitializer<Long> UserId = new HInitializer<>("UserId");
    public long userId() {
        return OperateUsersTest.UserId.getInstance().longValue();
    }
    public void userId(final long userId) {
        OperateUsersTest.UserId.reinitialize(userId);
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(1)
    public void changeUserGroup(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateUsersHelper.changeUserGroup(client, this.adminToken(), this.userId(), UserGroupManager.getInstance().getAdminId()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangeUserGroup, pair.getFirst());
            Assertions.assertEquals(this.userId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
            Assertions.assertEquals(UserGroupManager.getInstance().getAdminId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
            Assertions.assertEquals(IdentifierNames.UserGroupName.Admin.getIdentifier(), ByteBufIOUtil.readUTF(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void getUser(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        final VisibleUserInformation information = OperateUsersHelper.getUser(client, this.adminToken(), this.userId());
        Assertions.assertNotNull(information);
        Assertions.assertEquals(this.userId(), information.id());
        Assertions.assertEquals(this.username(), information.username());
        Assertions.assertEquals(UserGroupManager.getInstance().getAdminId(), information.groupId());
        Assertions.assertEquals(IdentifierNames.UserGroupName.Admin.getIdentifier(), information.groupName());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void listUsers(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(2, OperateUsersHelper.listUsers(client, this.adminToken(), new LinkedHashMap<>(), 0, 2).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void listUsersInGroups(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(2, OperateUsersHelper.listUsersInGroups(client, this.adminToken(), Set.of(UserGroupManager.getInstance().getAdminId()),
                false, new LinkedHashMap<>(), 0, 3).getFirst());
        Assertions.assertEquals(0, OperateUsersHelper.listUsersInGroups(client, this.adminToken(), Set.of(UserGroupManager.getInstance().getAdminId()),
                true, new LinkedHashMap<>(), 0, 3).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(3)
    public void deleteUser(final WListClientInterface client, final WListClientInterface broadcast) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertTrue(OperateUsersHelper.deleteUser(client, this.adminToken(), this.userId()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.Logoff, pair.getFirst());
            Assertions.assertEquals(this.userId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void deleteUsersInGroup(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(0, OperateUsersHelper.deleteUsersInGroup(client, this.adminToken(), UserGroupManager.getInstance().getDefaultId()));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(2)
    public void searchGroupRegex(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(2, OperateUsersHelper.searchUsersRegex(client, this.adminToken(), ".*", new LinkedHashMap<>(), 0, 2).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(3)
    public void searchGroupName(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(1, OperateUsersHelper.searchUserpsName(client, this.adminToken(), Set.of(this.username()), 0, 2).getFirst());
    }


    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminChangeUserGroup(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertFalse(OperateUsersHelper.changeUserGroup(client, this.adminToken(), UserManager.getInstance().getAdminId(), UserGroupManager.getInstance().getDefaultId()));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminDeleteUsersInGroup(final WListClientInterface client) throws WrongStateException, IOException, InterruptedException {
        Assertions.assertEquals(-1, OperateUsersHelper.deleteUsersInGroup(client, this.adminToken(), UserGroupManager.getInstance().getAdminId()));
    }
}
