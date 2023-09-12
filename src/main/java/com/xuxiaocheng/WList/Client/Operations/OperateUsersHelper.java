package com.xuxiaocheng.WList.Client.Operations;

public final class OperateUsersHelper {
    private OperateUsersHelper() {
        super();
    }

//    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserInformation>> listUsers(final @NotNull WListClientInterface client, final @NotNull String token, final int limit, final int page, final Options.@NotNull OrderDirection direction) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListUsers, token);
//        ByteBufIOUtil.writeVariableLenInt(send, limit);
//        ByteBufIOUtil.writeVariableLenInt(send, page);
//        ByteBufIOUtil.writeUTF(send, direction.name());
//        OperateHelper.logOperating(OperationType.ListUsers, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("limit", limit).add("page", page).add("direction", direction));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final long total = ByteBufIOUtil.readVariableLenLong(receive);
//                final int length = ByteBufIOUtil.readVariableLenInt(receive);
//                final List<VisibleUserInformation> list = new ArrayList<>(count);
//                for (int i = 0; i < length; ++i)
//                    list.add(VisibleUserInformation.parse(receive));
//                OperateHelper.logOperated(OperationType.ListUsers, () -> ParametersMap.create().add("success", true)
//                        .add("total", total).add("list", list));
//                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.ListUsers, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("limit", limit).add("page", page).add("direction", direction));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean deleteUser(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String username) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DeleteUser, token);
//        ByteBufIOUtil.writeUTF(send, username);
//        OperateHelper.logOperating(OperationType.DeleteUser, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("username", username));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                OperateHelper.logOperated(OperationType.DeleteUser, () -> ParametersMap.create().add("success", true));
//                return true;
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.DeleteUser, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("User".equals(reason))
//                return false;
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("username", username));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleUserGroupInformation>> listGroups(final @NotNull WListClientInterface client, final @NotNull String token, final int limit, final int page, final Options.@NotNull OrderDirection direction) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListGroups, token);
//        ByteBufIOUtil.writeVariableLenInt(send, limit);
//        ByteBufIOUtil.writeVariableLenInt(send, page);
//        ByteBufIOUtil.writeUTF(send, direction.name());
//        OperateHelper.logOperating(OperationType.ListGroups, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("limit", limit).add("page", page).add("direction", direction));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final long total = ByteBufIOUtil.readVariableLenLong(receive);
//                final int length = ByteBufIOUtil.readVariableLenInt(receive);
//                final List<VisibleUserGroupInformation> list = new ArrayList<>(count);
//                for (int i = 0; i < length; ++i)
//                    list.add(VisibleUserGroupInformation.parse(receive));
//                OperateHelper.logOperated(OperationType.ListGroups, () -> ParametersMap.create().add("success", true)
//                        .add("total", total).add("list", list));
//                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.ListGroups, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("limit", limit).add("page", page).add("direction", direction));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean addGroup(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.AddGroup, token);
//        ByteBufIOUtil.writeUTF(send, groupName);
//        OperateHelper.logOperating(OperationType.AddGroup, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("groupName", groupName));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(OperationType.AddGroup, () -> ParametersMap.create().add("success", success));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @Nullable Boolean deleteGroup(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DeleteGroup, token);
//        ByteBufIOUtil.writeUTF(send, groupName);
//        OperateHelper.logOperating(OperationType.DeleteGroup, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("groupName", groupName));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                OperateHelper.logOperated(OperationType.DeleteGroup, () -> ParametersMap.create().add("success", true));
//                return true;
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.DeleteGroup, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("Users".equals(reason))
//                return false;
//            if ("Group".equals(reason))
//                return null;
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("groupName", groupName));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @Nullable Boolean changeGroup(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String username, final @NotNull String groupName) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ChangeGroup, token);
//        ByteBufIOUtil.writeUTF(send, username);
//        ByteBufIOUtil.writeUTF(send, groupName);
//        OperateHelper.logOperating(OperationType.ChangeGroup, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("username", username).add("groupName", groupName));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                OperateHelper.logOperated(OperationType.ChangeGroup, () -> ParametersMap.create().add("success", true));
//                return true;
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.ChangeGroup, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("User".equals(reason))
//                return false;
//            if ("Group".equals(reason))
//                return null;
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("username", username).add("groupName", groupName));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean changePermission(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String groupName, final boolean add, final @NotNull Iterable<Operation.@NotNull Permission> permissions) throws IOException, InterruptedException, WrongStateException {
//        final OperationType type = add ? OperationType.AddPermission : OperationType.RemovePermission;
//        final ByteBuf send = OperateHelper.operateWithToken(type, token);
//        ByteBufIOUtil.writeUTF(send, groupName);
//        ByteBufIOUtil.writeUTF(send, Operation.dumpPermissions(permissions));
//        OperateHelper.logOperating(type, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("groupName", groupName).add("permissions", permissions));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                OperateHelper.logOperated(type, () -> ParametersMap.create().add("success", true));
//                return true;
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.ChangeGroup, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("Group".equals(reason))
//                return false;
//            assert "Permissions".equals(reason);
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("groupName", groupName).add("permissions", permissions));
//        } finally {
//            receive.release();
//        }
//    }
}
