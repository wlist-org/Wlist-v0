package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

public class LanzouProvider extends AbstractIdBaseProvider<LanzouConfiguration> {
    @Override
    public @NotNull ProviderTypes<LanzouConfiguration> getType() {
        return ProviderTypes.Lanzou;
    }

    @Override
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) {
        return Collections.emptyIterator();
    }

    @Override
    protected @NotNull UnionPair<FileInformation, Boolean> info0(@NotNull final FileInformation oldInformation) throws Exception {
        return UnionPair.fail(Boolean.TRUE);
    }

    @Override
    protected void delete0(final long id, final boolean isDirectory) throws Exception {

    }
}
