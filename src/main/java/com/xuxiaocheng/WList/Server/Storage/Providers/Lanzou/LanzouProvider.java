package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.CheckRules.CheckRuleSet;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
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
    protected @NotNull UnionPair<FileInformation, Boolean> update0(@NotNull final FileInformation oldInformation) throws Exception {
        return UnionPair.fail(Boolean.TRUE);
    }

    @Override
    protected void delete0(final @NotNull FileInformation information) throws Exception {

    }

    @Override
    protected @NotNull UnionPair<DownloadRequirements, FailureReason> download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull FileLocation location) throws Exception {
        return null;
    }

    protected static @NotNull CheckRule<@NotNull String> nameChecker = new CheckRuleSet<>(
        //TODO
    );

    @Override
    protected @NotNull CheckRule<@NotNull String> nameChecker() {
        return LanzouProvider.nameChecker;
    }

    @Override
    protected @NotNull UnionPair<FileInformation, FailureReason> createDirectory0(final long parentId, @NotNull final String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull FileLocation parentLocation) throws Exception {
        throw new UnsupportedOperationException();
    }
}
