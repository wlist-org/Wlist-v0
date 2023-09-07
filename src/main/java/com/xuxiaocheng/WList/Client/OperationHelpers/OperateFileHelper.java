package com.xuxiaocheng.WList.Client.OperationHelpers;

public final class OperateFileHelper {
    private OperateFileHelper() {
        super();
    }

//    private static @NotNull FailureReason handleFailureReason(final @NotNull String reason) {
//        if ("Parameters".equals(reason))
//            throw new IllegalArgumentException();
//        return FailureReason.parse(reason);
//    }
//
//    public static Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleFileInformation>> listFiles(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation directoryLocation, final Options.@NotNull DirectoriesOrFiles filter, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final boolean refresh) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListFiles, token);
//        FileLocation.dump(send, directoryLocation);
//        ByteBufIOUtil.writeByte(send, (byte) (filter.ordinal() + 1));
//        ByteBufIOUtil.writeVariableLenInt(send, limit);
//        ByteBufIOUtil.writeVariableLenInt(send, page);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        ByteBufIOUtil.writeUTF(send, direction.name());
//        ByteBufIOUtil.writeBoolean(send, refresh);
//        OperateHelper.logOperating(Operation.Type.ListFiles, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("directoryLocation", directoryLocation).add("filter", filter).add("limit", limit).add("page", page).add("policy", policy).add("direction", direction).add("refresh", refresh));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final long total = ByteBufIOUtil.readVariableLenLong(receive);
//                final long filtered = ByteBufIOUtil.readVariableLenLong(receive);
//                final int count = ByteBufIOUtil.readVariableLenInt(receive);
//                final List<VisibleFileInformation> list = new ArrayList<>(count);
//                for (int i = 0; i < count; ++i)
//                    list.add(VisibleFileInformation.parse(receive));
//                OperateHelper.logOperated(Operation.Type.ListFiles, () -> ParametersMap.create().add("success", true)
//                        .add("total", total).add("filtered", filtered).add("list", list));
//                return Triad.ImmutableTriad.makeImmutableTriad(total, filtered, Collections.unmodifiableList(list));
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.ListFiles, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("File".equals(reason))
//                return null;
//            throw new WrongStateException(Operation.State.DataError, reason + ParametersMap.create()
//                    .add("directoryLocation", directoryLocation).add("filter", filter).add("limit", limit).add("page", page).add("policy", policy).add("direction", direction).add("refresh", refresh));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> createDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CreateDirectory, token);
//        FileLocation.dump(send, parentLocation);
//        ByteBufIOUtil.writeUTF(send, directoryName);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(Operation.Type.CreateDirectory, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("parentLocation", parentLocation).add("directoryName", directoryName).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(Operation.Type.CreateDirectory, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.CreateDirectory, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFileHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean deleteFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteFile, token);
//        FileLocation.dump(send, fileLocation);
//        OperateHelper.logOperating(Operation.Type.DeleteFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("fileLocation", fileLocation));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(Operation.Type.DeleteFile, () -> ParametersMap.create().add("success", false));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> renameFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation, final @NotNull String newFilename, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RenameFile, token);
//        FileLocation.dump(send, fileLocation);
//        ByteBufIOUtil.writeUTF(send, newFilename);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(Operation.Type.RenameFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("fileLocation", fileLocation).add("newFilename", newFilename).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(Operation.Type.RenameFile, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.RenameFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFileHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull String> requestDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation, final long from, final long to) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestDownloadFile, token);
//        FileLocation.dump(send, fileLocation);
//        ByteBufIOUtil.writeVariableLenLong(send, from);
//        ByteBufIOUtil.writeVariable2LenLong(send, to);
//        OperateHelper.logOperating(Operation.Type.RequestDownloadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("fileLocation", fileLocation).add("from", from).add("to", to));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final long size = ByteBufIOUtil.readVariable2LenLong(receive);
//                final String id = ByteBufIOUtil.readUTF(receive);
//                OperateHelper.logOperated(Operation.Type.RequestDownloadFile, () -> ParametersMap.create().add("success", true)
//                        .add("size", size).add("id", id));
//                return Pair.ImmutablePair.makeImmutablePair(size, id);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.RequestDownloadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("File".equals(reason))
//                return null;
//            assert "Parameters".equals(reason);
//            throw new WrongStateException(Operation.State.DataError, reason + ParametersMap.create().add("fileLocation", fileLocation).add("from", from).add("to", to));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @Nullable ByteBuf downloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int chunk) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DownloadFile, token);
//        ByteBufIOUtil.writeUTF(send, id);
//        ByteBufIOUtil.writeVariableLenInt(send, chunk);
//        OperateHelper.logOperating(Operation.Type.DownloadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id).add("chunk", chunk));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                OperateHelper.logOperated(Operation.Type.DownloadFile, () -> ParametersMap.create().add("success", true)
//                        .add("chunkSize", receive.readableBytes()));
//                return receive.retain();
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.DownloadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("Id".equals(reason))
//                return null;
//            throw new WrongStateException(Operation.State.DataError, reason + ParametersMap.create().add("id", id).add("chunk", chunk));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean cancelDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelDownloadFile, token);
//        ByteBufIOUtil.writeUTF(send, id);
//        OperateHelper.logOperating(Operation.Type.CancelDownloadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(Operation.Type.CancelDownloadFile, () -> ParametersMap.create().add("success", success));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull String>, @NotNull FailureReason> requestUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestUploadFile, token);
//        FileLocation.dump(send, parentLocation);
//        ByteBufIOUtil.writeUTF(send, filename);
//        ByteBufIOUtil.writeVariable2LenLong(send, size);
//        ByteBufIOUtil.writeUTF(send, md5);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(Operation.Type.RequestUploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("parentLocation", parentLocation).add("filename", filename).add("size", size).add("md5", md5).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive))
//                if (ByteBufIOUtil.readBoolean(receive)) {
//                    final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                    OperateHelper.logOperated(Operation.Type.RequestUploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("reuse", true).add("information", information));
//                    return UnionPair.ok(UnionPair.ok(information));
//                } else {
//                    final String id = ByteBufIOUtil.readUTF(receive);
//                    OperateHelper.logOperated(Operation.Type.RequestUploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("reuse", false).add("id", id));
//                    return UnionPair.ok(UnionPair.fail(id));
//                }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.RequestUploadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFileHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    // Null: no id, failure.false: invalid content, failure.true: continue, success: complete
//    public static @Nullable UnionPair<@NotNull VisibleFileInformation, @NotNull Boolean> uploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int chunk, final @NotNull ByteBuf buffer) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
//        ByteBufIOUtil.writeUTF(prefix, Operation.Type.UploadFile.name());
//        ByteBufIOUtil.writeUTF(prefix, token);
//        ByteBufIOUtil.writeUTF(prefix, id);
//        ByteBufIOUtil.writeVariableLenInt(prefix, chunk);
//        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
//        OperateHelper.logOperating(Operation.Type.UploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id).add("chunk", chunk).add("chunkSize", buffer.readableBytes()));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive))
//                if (ByteBufIOUtil.readBoolean(receive)) {
//                    OperateHelper.logOperated(Operation.Type.UploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("continue", true));
//                    return UnionPair.fail(true);
//                } else {
//                    final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                    OperateHelper.logOperated(Operation.Type.UploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("continue", false).add("information", information));
//                    return UnionPair.ok(information);
//                }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.Type.UploadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("Id".equals(reason))
//                return null;
//            if ("Content".equals(reason))
//                return UnionPair.fail(false);
//            throw new WrongStateException(Operation.State.DataError, reason + ParametersMap.create().add("id", id).add("chunk", chunk));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean cancelUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelUploadFile, token);
//        ByteBufIOUtil.writeUTF(send, id);
//        OperateHelper.logOperating(Operation.Type.CancelUploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(Operation.Type.CancelUploadFile, () -> ParametersMap.create().add("success", success));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> copyFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation source, final @NotNull FileLocation target, final @NotNull String newFilename, final Options.@NotNull DuplicatePolicy policy, final boolean moveMode) throws IOException, InterruptedException, WrongStateException {
//        final Operation.Type type = moveMode ? Operation.Type.MoveFile : Operation.Type.CopyFile;
//        final ByteBuf send = OperateHelper.operateWithToken(type, token);
//        FileLocation.dump(send, source);
//        FileLocation.dump(send, target);
//        ByteBufIOUtil.writeUTF(send, newFilename);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(type, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("source", source).add("target", target).add("newFilename", newFilename).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(type, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(type, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFileHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
}
