package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public final class ServerHandlerManager {
    private ServerHandlerManager() {
        super();
    }

    private static final @NotNull Map<Operation.@NotNull Type, @NotNull ServerHandler> map = new EnumMap<>(Operation.Type.class);

    static void register(final Operation.@NotNull Type operation, final @NotNull ServerHandler handler) {
        ServerHandlerManager.map.put(operation, handler);
    }

    public static void initialize() {
        ServerStateHandler.initialize();
        ServerUserHandler.initialize();
        ServerDriverHandler.initialize();
        ServerFileHandler.initialize();
        assert ServerHandlerManager.map.size() == Operation.Type.values().length;
    }

    public static @NotNull ServerHandler getHandler(final Operation.@NotNull Type operation) {
        return ServerHandlerManager.map.get(operation);
    }
}
