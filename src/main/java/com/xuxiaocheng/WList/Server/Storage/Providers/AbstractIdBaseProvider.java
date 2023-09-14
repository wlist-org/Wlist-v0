package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@SuppressWarnings("OverlyBroadThrowsClause")
public abstract class AbstractIdBaseProvider<C extends ProviderConfiguration> implements ProviderInterface<C> {
    protected @NotNull Supplier<@NotNull C> configurationSupplier;
    protected @NotNull C configuration;
    protected @NotNull HInitializer<FileManager> manager = new HInitializer<>("ProviderManager");

    protected AbstractIdBaseProvider(final @NotNull Supplier<@NotNull C> configuration) {
        super();
        this.configurationSupplier = configuration;
        this.configuration = configuration.get();
    }

    @Override
    public @NotNull C getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull C configuration) throws Exception {
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(StorageManager.getStorageDatabaseFile(configuration.getName()));
        FileManager.quicklyInitialize(configuration.getName(), database, configuration.getRootDirectoryId(), null);
        this.configuration = configuration;
        this.manager.reinitialize(FileManager.getInstance(configuration.getName()));
        final FileInformation information = this.manager.getInstance().selectFile(this.configuration.getRootDirectoryId(), true, null);
        assert information != null;
        assert information.createTime() != null;
        assert information.updateTime() != null;
        this.configuration.setSpaceUsed(information.size());
        this.configuration.setCreateTime(information.createTime());
        this.configuration.setUpdateTime(information.updateTime());
        this.configuration.markModified();
    }

    @Override
    public void uninitialize(final boolean dropIndex) throws Exception {
        this.configuration = this.configurationSupplier.get();
        this.manager.uninitializeNullable();
        if (dropIndex)
            FileManager.quicklyUninitialize(this.configuration.getName(), null);
    }

    @Override
    public @NotNull String toString() {
        return "AbstractIdBaseProvider{" +
                "configuration=" + this.configuration +
                '}';
    }
}
