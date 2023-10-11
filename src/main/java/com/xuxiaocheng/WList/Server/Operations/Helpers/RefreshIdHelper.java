package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.RefreshRequirements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class RefreshIdHelper {
    private RefreshIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull RefreshRequirements> data = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final @NotNull RefreshRequirements requirements) {
        final String id = MiscellaneousUtil.randomKeyAndPut(RefreshIdHelper.data, IdsHelper::randomTimerId, requirements);
        IdsHelper.CleanerExecutors.schedule(() -> RefreshIdHelper.data.remove(id, requirements), ServerConfiguration.get().idIdleExpireTime(), TimeUnit.SECONDS)
                .addListener(IdsHelper.noCancellationExceptionListener());
        IdsHelper.setProgressBar(id);
        return id;
    }

    public static boolean cancel(final @NotNull String id) {
        final RefreshRequirements requirements = RefreshIdHelper.data.remove(id);
        if (requirements == null)
            return false;
        IdsHelper.removeProgressBar(id);
        requirements.canceller().run();
        return true;
    }

    public static boolean confirm(final @NotNull String id, final @NotNull Consumer<? super @Nullable Throwable> consumer) throws Exception {
        final RefreshRequirements requirements = RefreshIdHelper.data.remove(id);
        if (requirements == null)
            return true;
        try {
            requirements.runner().accept(consumer, IdsHelper.getProgressBar(id));
        } finally {
            IdsHelper.removeProgressBar(id);
        }
        return false;
    }
}
