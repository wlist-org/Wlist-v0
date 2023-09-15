package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("OverlyBroadThrowsClause")
public abstract class AbstractIdBaseProvider<C extends ProviderConfiguration> implements ProviderInterface<C> {
    protected @NotNull HInitializer<C> configuration = new HInitializer<>("ProviderConfiguration");
    protected @NotNull HInitializer<FileManager> manager = new HInitializer<>("ProviderManager");

    @Override
    public @NotNull C getConfiguration() {
        return this.configuration.getInstance();
    }

    @Override
    public void initialize(final @NotNull C configuration) throws Exception {
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(StorageManager.getStorageDatabaseFile(configuration.getName()));
        FileManager.quicklyInitialize(configuration.getName(), database, configuration.getRootDirectoryId(), null);
        this.configuration.reinitialize(configuration);
        this.manager.reinitialize(FileManager.getInstance(configuration.getName()));
        final FileInformation information = this.manager.getInstance().selectFile(this.configuration.getInstance().getRootDirectoryId(), true, null);
        assert information != null;
        assert information.createTime() != null;
        assert information.updateTime() != null;
        configuration.setSpaceUsed(information.size());
        configuration.setCreateTime(information.createTime());
        configuration.setUpdateTime(information.updateTime());
        configuration.markModified();
    }

    @Override
    public void uninitialize(final boolean dropIndex) throws Exception {
        final C configuration = this.configuration.uninitializeNullable();
        this.manager.uninitializeNullable();
        if (configuration != null && dropIndex)
            FileManager.quicklyUninitialize(configuration.getName(), null);
    }

    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
