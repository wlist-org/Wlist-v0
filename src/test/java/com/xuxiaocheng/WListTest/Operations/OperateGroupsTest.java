package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateGroupsHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateSelfHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
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
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class OperateGroupsTest extends ServerWrapper {
    @DisplayName("Prepare")
    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(0)
    public void login(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final Pair.ImmutablePair<String,  ZonedDateTime> token = OperateSelfHelper.login(client, this.adminUsername(), this.adminPassword());
        Assumptions.assumeTrue(token != null);
        this.adminToken(token.getFirst());
    }

    protected static final @NotNull HInitializer<String> GroupName = new HInitializer<>("GroupName", "test");
    protected static final @NotNull HInitializer<Long> GroupId = new HInitializer<>("GroupId");
    public @NotNull String groupName() {
        return OperateGroupsTest.GroupName.getInstance();
    }
    public void groupName(final @NotNull String groupName) {
        OperateGroupsTest.GroupName.reinitialize(groupName);
    }
    public long groupId() {
        return OperateGroupsTest.GroupId.getInstance().longValue();
    }
    public void groupId(final long groupId) {
        OperateGroupsTest.GroupId.reinitialize(groupId);
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(1)
    public void addGroup(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertTrue(OperateGroupsHelper.addGroup(client, this.adminToken(), this.groupName()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.AddGroup, pair.getFirst());
            final VisibleUserGroupInformation information = VisibleUserGroupInformation.parse(pair.getSecond());
            Assertions.assertEquals(this.groupName(), information.name());
            this.groupId(information.id());
            Assertions.assertEquals(UserPermission.Empty, information.permissions());
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(2)
    public void changeGroupName(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        final String groupName = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertTrue(OperateGroupsHelper.changeGroupName(client, this.adminToken(), this.groupId(), groupName));
        this.groupName(groupName);
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangeGroupName, pair.getFirst());
            Assertions.assertEquals(this.groupId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
            Assertions.assertEquals(this.groupName(), ByteBufIOUtil.readUTF(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(3)
    public void changeGroupPermissions(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertTrue(OperateGroupsHelper.changeGroupPermissions(client, this.adminToken(), this.groupId(), UserPermission.Default));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangeGroupPermissions, pair.getFirst());
            Assertions.assertEquals(this.groupId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
            Assertions.assertEquals(UserPermission.Default, UserPermission.parse(ByteBufIOUtil.readUTF(pair.getSecond())));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void getGroup(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final VisibleUserGroupInformation information = OperateGroupsHelper.getGroup(client, this.adminToken(), this.groupId());
        Assertions.assertNotNull(information);
        Assertions.assertEquals(this.groupId(), information.id());
        Assertions.assertEquals(this.groupName(), information.name());
        Assertions.assertEquals(UserPermission.Default, information.permissions());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void listGroups(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertEquals(3, OperateGroupsHelper.listGroups(client, this.adminToken(), VisibleUserGroupInformation.emptyOrder(), 0, 3).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void listGroupsInPermissions(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertEquals(1, OperateGroupsHelper.listGroupsInPermissions(client, this.adminToken(),
                UserPermission.All.stream().collect(Collectors.toMap(p -> p, p -> true)), VisibleUserGroupInformation.emptyOrder(), 0, 3).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(6)
    public void deleteGroup(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertTrue(OperateGroupsHelper.deleteGroup(client, this.adminToken(), this.groupId()));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.DeleteGroup, pair.getFirst());
            Assertions.assertEquals(this.groupId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
        } finally {
            pair.getSecond().release();
        }
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void searchGroupRegex(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertEquals(3, OperateGroupsHelper.searchGroupsRegex(client, this.adminToken(), ".*", VisibleUserGroupInformation.emptyOrder(), 0, 3).getFirst());
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(4)
    public void searchGroupName(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertEquals(1, OperateGroupsHelper.searchGroupsName(client, this.adminToken(), Set.of(this.groupName()), 0, 3).getFirst());
    }


    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminAddGroup(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertFalse(OperateGroupsHelper.addGroup(client, this.adminToken(), IdentifierNames.UserGroupName.Admin.getIdentifier()));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminChangeGroupName(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final String groupName = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertFalse(OperateGroupsHelper.changeGroupName(client, this.adminToken(), UserGroupManager.getInstance().getAdminId(), groupName));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void adminChangeGroupPermissions(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertFalse(OperateGroupsHelper.changeGroupPermissions(client, this.adminToken(), UserGroupManager.getInstance().getAdminId(), UserPermission.Default));
    }


    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void defaultAddGroup(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertFalse(OperateGroupsHelper.addGroup(client, this.adminToken(), IdentifierNames.UserGroupName.Default.getIdentifier()));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("client")
    @Order(1)
    public void defaultChangeGroupName(final WListClientInterface client) throws IOException, InterruptedException, WrongStateException {
        final String groupName = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 20, null);
        Assertions.assertFalse(OperateGroupsHelper.changeGroupName(client, this.adminToken(), UserGroupManager.getInstance().getDefaultId(), groupName));
    }

    @ParameterizedTest(name = "running")
    @MethodSource("broadcast")
    @Order(5)
    public void defaultChangeGroupPermissions(final WListClientInterface client, final WListClientInterface broadcast) throws IOException, InterruptedException, WrongStateException {
        Assertions.assertTrue(OperateGroupsHelper.changeGroupPermissions(client, this.adminToken(), UserGroupManager.getInstance().getDefaultId(), UserPermission.All));
        final Pair.ImmutablePair<OperationType, ByteBuf> pair = OperateServerHelper.waitBroadcast(broadcast).getT();
        try {
            Assertions.assertEquals(OperationType.ChangeGroupPermissions, pair.getFirst());
            Assertions.assertEquals(UserGroupManager.getInstance().getDefaultId(), ByteBufIOUtil.readVariableLenLong(pair.getSecond()));
            Assertions.assertEquals(UserPermission.All, UserPermission.parse(ByteBufIOUtil.readUTF(pair.getSecond())));
        } finally {
            pair.getSecond().release();
        }
    }
}
