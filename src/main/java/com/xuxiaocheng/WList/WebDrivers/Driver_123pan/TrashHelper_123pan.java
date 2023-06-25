package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.IllegalResponseCodeException;
import com.xuxiaocheng.WList.Exceptions.WrongResponseException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.File.TrashedSqlInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrashHelper_123pan {
    private TrashHelper_123pan() {
        super();
    }

    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> DeleteFileURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/delete", "POST");
    static final Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> DeleteAllFilesURL = Pair.ImmutablePair.makeImmutablePair("https://www.123pan.com/api/file/trash_delete_all", "POST");

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull TrashedSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(7);
        request.put("Trashed", true);
        request.put("DriveId", 0);
        request.put("Limit", limit);
        request.put("OrderBy", DriverHelper_123pan.getOrderPolicy(policy));
        request.put("OrderDirection", DriverHelper_123pan.getOrderDirection(direction));
        request.put("ParentFileId", 0);
        request.put("Page", page + 1);
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.ListFilesURL, configuration, request, false);
        final Long total = data.getLong("Total");
        final JSONArray infos = data.getJSONArray("InfoList");
        if (total == null || infos == null)
            throw new WrongResponseException("Listing trashed file.", data);
        final List<TrashedSqlInformation> list = new ArrayList<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null)
                continue;
            final TrashedSqlInformation information = FileInformation_123pan.createTrashed(info);
            if (information != null)
                list.add(information);
        }
        return Pair.ImmutablePair.makeImmutablePair(total, list);
    }

    private static @Nullable JSONArray getFileInfos(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        request.put("FileIdList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id);
            return pair;
        }).toList());
        final JSONObject data =  DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.FilesInfoURL, configuration, request, false);
        return data.getJSONArray("infoList");
    }

    static @NotNull @UnmodifiableView Map<@NotNull Long, Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull FileSqlInformation>> getInformationParentIds(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final JSONArray infos = TrashHelper_123pan.getFileInfos(configuration, idList);
        if (infos == null)
            return Map.of();
        final Map<Long, Pair.ImmutablePair<Long, FileSqlInformation>> map = new HashMap<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null) continue;
            final Long id = info.getLong("FileId");
            final Long parent = info.getLong("ParentFileId");
            if (id == null || parent == null)
                continue;
            final FileSqlInformation information = FileInformation_123pan.create(new DrivePath(""), info);
            if (information != null)
                map.put(id, Pair.ImmutablePair.makeImmutablePair(parent, information));
        }
        return Collections.unmodifiableMap(map);
    }

    static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedSqlInformation> getFilesInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        final JSONArray infos = TrashHelper_123pan.getFileInfos(configuration, idList);
        if (infos == null)
            return Map.of();
        final Map<Long, TrashedSqlInformation> map = new HashMap<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null) continue;
            final TrashedSqlInformation information = FileInformation_123pan.createTrashed(info);
            if (information != null)
                map.put(information.id(), information);
        }
        return Collections.unmodifiableMap(map);
    }

    @SuppressWarnings({"UnusedReturnValue", "DuplicatedCode"})
    static @NotNull @UnmodifiableView Set<@NotNull Long> deleteFiles(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull Collection<@NotNull Long> idList) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(1);
        request.put("FileIdList", idList.stream().map(id -> {
            final JSONObject pair = new JSONObject(1);
            pair.put("FileId", id.longValue());
            return pair;
        }).toList());
        final JSONObject data = DriverHelper_123pan.sendRequestReceiveExtractedData(TrashHelper_123pan.DeleteFileURL, configuration, request, false);
        final JSONArray infos = data.getJSONArray("InfoList");
        if (infos == null)
            return Set.of();
        final Set<Long> set = new HashSet<>(infos.size());
        for (final JSONObject info: infos.toList(JSONObject.class)) {
            if (info == null)
                continue;
            final Long id = info.getLong("FileId");
            if (id != null)
                set.add(id);
        }
        return Collections.unmodifiableSet(set);
    }

    static void deleteAllFiles(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException {
        DriverHelper_123pan.ensureToken(configuration);
        DriverHelper_123pan.sendRequestReceiveExtractedData(TrashHelper_123pan.DeleteAllFilesURL, configuration, new LinkedHashMap<>(1), false);
    }

    static @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> renameFile(final @NotNull DriverConfiguration_123Pan configuration, final long id, final @NotNull String name) throws IllegalParametersException, IOException {
        if (!DriverHelper_123pan.filenamePredication.test(name))
            return UnionPair.fail(FailureReason.byInvalidName(name, new DrivePath(name)));
        DriverHelper_123pan.ensureToken(configuration);
        final Map<String, Object> request = new LinkedHashMap<>(4);
        request.put("DriveId", 0);
        request.put("FileId", id);
        request.put("FileName", name);
        final JSONObject data;
        try {
            data = DriverHelper_123pan.sendRequestReceiveExtractedData(DriverHelper_123pan.RenameFileURL, configuration, request, false);
        } catch (final IllegalResponseCodeException exception) {
            if (exception.getCode() == DriverHelper_123pan.InvalidFilenameResponseCode)
                return UnionPair.fail(FailureReason.byInvalidName(name, new DrivePath(name)));
            throw exception;
        }
        final TrashedSqlInformation information = FileInformation_123pan.createTrashed(data.getJSONObject("Info"));
        if (information == null)
            throw new WrongResponseException("Renaming file.", data);
        return UnionPair.ok(information);
    }
}
