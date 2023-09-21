package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

@SuppressWarnings("SpellCheckingInspection")
final class DriverHelper_lanzou {
    private DriverHelper_lanzou() {
        super();
    }

//    static @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull LanzouConfiguration configuration, final String name, final long parentId) throws IOException {
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("parent_id", String.valueOf(parentId))
//                .add("folder_name", name);
//        final JSONObject json;
//        final ZonedDateTime now;
//        try {
//            json = DriverHelper_lanzou.task(configuration, 2, builder, 1);
//            now = MiscellaneousUtil.now();
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0 && "\u540D\u79F0\u542B\u6709\u7279\u6B8A\u5B57\u7B26".equals(exception.getMeaning()))
//                return UnionPair.fail(FailureReason.byInvalidName("Creating directory.", new FileLocation(configuration.getName(), parentId), name));
//            throw exception;
//        }
//        final String message = json.getString("info");
//        final Long id = json.getLong("text");
//        if (id == null || !"\u521B\u5EFA\u6210\u529F".equals(message))
//            throw new WrongResponseException("Creating directories.", message, ParametersMap.create()
//                    .add("configuration", configuration).add("name", name).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(new FileInformation(new FileLocation(configuration.getName(), id.longValue()), parentId, name,
//                FileSqlInterface.FileSqlType.EmptyDirectory, 0, now, now, "", null));
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> uploadFile(final @NotNull LanzouConfiguration configuration, final String name, final long parentId, final @NotNull ByteBuf content, final @NotNull String md5) throws IOException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
//        final int size = content.readableBytes();
//        if (size > configuration.getMaxSizePerFile())
//            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, configuration.getMaxSizePerFile(), new FileLocation(configuration.getName(), parentId), name));
//        DriverManager_lanzou.ensureLoggedIn(configuration);
//        final MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                .addFormDataPart("task", "1")
//                .addFormDataPart("ve", "2")
//                .addFormDataPart("folder_id_bb_n", String.valueOf(parentId))
//                .addFormDataPart("upload_file", name, DriverNetworkHelper.createOctetStreamRequestBody(content))
//                .build();
//        final JSONObject json = DriverNetworkHelper.extraJsonResponseBody(DriverNetworkHelper.postWithBody(configuration.getHttpClient(), DriverHelper_lanzou.UploadURL,
//                DriverHelper_lanzou.headers.newBuilder().set("cookie", "phpdisk_info=" + configuration.getIdentifier() + "; ").build(), body).execute());
//        final ZonedDateTime now = MiscellaneousUtil.now();
//        final int code = json.getIntValue("zt", -1);
//        if (code != 1)
//            throw new IllegalResponseCodeException(code, json.getString("info") == null ? json.getString("text") : json.getString("info"),
//                    ParametersMap.create().add("configuration", configuration).add("requireZt", 1).add("json", json));
//        final String message = json.getString("info");
//        final JSONArray array = json.getJSONArray("text");
//        if (!"\u4E0A\u4F20\u6210\u529F".equals(message) || array == null || array.isEmpty())
//            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
//                    .add("name", name).add("parentId", parentId).add("json", json));
//        final JSONObject info = array.getJSONObject(0);
//        if (info == null || info.getLong("id") == null)
//            throw new WrongResponseException("Uploading.", message, ParametersMap.create().add("configuration", configuration)
//                    .add("name", name).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(new FileInformation(new FileLocation(configuration.getName(), info.getLongValue("id")),
//                parentId, name, FileSqlInterface.FileSqlType.RegularFile, size, now, now, md5, null));
//    }
//
//    static @Nullable UnionPair<ZonedDateTime, FailureReason> moveFile(final @NotNull LanzouConfiguration configuration, final long fileId, final long parentId) throws IOException {
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("file_id", String.valueOf(fileId))
//                .add("folder_id", String.valueOf(parentId));
//        final JSONObject json;
//        final ZonedDateTime now;
//        try {
//            if (parentId == 0) throw new IllegalResponseCodeException(0, "\u6CA1\u6709\u627E\u5230\u6587\u4EF6", ParametersMap.create());
//            json = DriverHelper_lanzou.task(configuration, 20, builder, 1);
//            now = MiscellaneousUtil.now();
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0) {
//                if ("\u79FB\u52A8\u5931\u8D25\uFF0C\u6587\u4EF6\u5DF2\u5728\u6B64\u76EE\u5F55".equals(exception.getMeaning()))
//                    return null;
//                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6".equals(exception.getMeaning()))
//                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (source).", new FileLocation(configuration.getName(), fileId)));
//                if ("\u6CA1\u6709\u627E\u5230\u6587\u4EF6\u5939".equals(exception.getMeaning()))
//                    return UnionPair.fail(FailureReason.byNoSuchFile("Moving (target).", new FileLocation(configuration.getName(), parentId)));
//            }
//            throw exception;
//        }
//        final String message = json.getString("info");
//        if (!"\u79FB\u52A8\u6210\u529F".equals(message))
//            throw new WrongResponseException("Moving.", message, ParametersMap.create()
//                    .add("configuration", configuration).add("fileId", fileId).add("parentId", parentId).add("json", json));
//        return UnionPair.ok(now);
//    }
//
//    static @Nullable OptionalNullable<FailureReason> renameFile(final @NotNull LanzouConfiguration configuration, final long fileId, final @NotNull String name) throws IOException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return OptionalNullable.of(FailureReason.byInvalidName("Renaming.", new FileLocation(configuration.getName(), fileId), name));
//        final FormBody.Builder builder = new FormBody.Builder()
//                .add("file_id", String.valueOf(fileId))
//                .add("file_name", name)
//                .add("type", "2");
//        final JSONObject json;
//        try {
//            json = DriverHelper_lanzou.task(configuration, 46, builder, 1);
//        } catch (final IllegalResponseCodeException exception) {
//            if (exception.getCode() == 0 && "\u6B64\u529F\u80FD\u4EC5\u4F1A\u5458\u4F7F\u7528\uFF0C\u8BF7\u5148\u5F00\u901A\u4F1A\u5458".equals(exception.getMeaning()))
//                return null;
//            throw exception;
//        }
//        // TODO: Unchecked.
//HLog.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Driver lanzou record: Running in vip mode (rename): ", json);
//        return OptionalNullable.empty();
//    }
}
