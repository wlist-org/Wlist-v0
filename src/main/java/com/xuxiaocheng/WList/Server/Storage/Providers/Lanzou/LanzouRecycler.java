package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseRecycler;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import org.jetbrains.annotations.NotNull;

public class LanzouRecycler extends AbstractIdBaseRecycler<LanzouConfiguration> {
    @Override
    public @NotNull StorageTypes<LanzouConfiguration> getType() {
        return StorageTypes.Lanzou;
    }


}
