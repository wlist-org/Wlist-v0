package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public final class ServerHandlerManager {
    private ServerHandlerManager() {
        super();
    }

    private static final @NotNull Map<@NotNull OperationType, @NotNull ServerHandler> map = new EnumMap<>(OperationType.class);

    static void register(final @NotNull OperationType operation, final @NotNull ServerHandler handler) {
        ServerHandlerManager.map.put(operation, handler);
    }

    public static void load() {
    } static {
        OperateSelfHandler.initialize();
        OperateServerHandler.initialize();
        OperateGroupsHandler.initialize();
        OperateUsersHandler.initialize();
        OperateProvidersHandler.initialize();
        OperateFilesHandler.initialize();
        OperateProgressHandler.initialize();
        assert ServerHandlerManager.map.size() == OperationType.values().length - 1; // #Undefined
    }


    public static @NotNull ServerHandler getHandler(final @NotNull OperationType operation) {
        return ServerHandlerManager.map.get(operation);
    }
}
