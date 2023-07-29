package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Trash_123pan_NoCache implements DriverTrashInterface<Driver_123pan> {
    protected @NotNull Driver_123pan driver = new Driver_123pan();

    @Override
    public @NotNull Driver_123pan getDriver() {
        return this.driver;
    }

    @Override
    public void initialize(final @NotNull Driver_123pan driver) throws SQLException {
        this.driver = driver;
    }

    @Override
    public void uninitialize() throws IllegalParametersException, IOException, SQLException {
        // DriverHelper_123pan.logout(this.driver.configuration);
        this.driver.configuration.getCacheSide().setLastTrashIndexBuildTime(null);
        this.driver.configuration.getCacheSide().setModified(true);
    }

    @Override
    public void buildCache() throws SQLException {
    }

    @Override
    public void buildIndex() throws SQLException {
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException {
        return TrashHelper_123pan.listFiles(this.driver.configuration, limit, page, policy, direction);
    }

    @Override
    public @Nullable TrashedSqlInformation info(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
        final Long id = location.id();
        return TrashHelper_123pan.getFilesInformation(this.driver.configuration, List.of(id)).get(id);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final @NotNull FileLocation location, final long targetParentId, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        final Long id = location.id();
        if (DriverHelper_123pan.trashFiles(this.driver.configuration, List.of(id), false).isEmpty())
            return UnionPair.fail(FailureReason.byNoSuchFile("Restoring.", location));
        final FileSqlInformation information = DriverHelper_123pan.moveFiles(this.driver.configuration, List.of(id), targetParentId, policy).get(id);
        return information == null ? UnionPair.fail(FailureReason.byNoSuchFile("Restoring.", location)) : UnionPair.ok(information);
    }

    @Override
    public void delete(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
        TrashHelper_123pan.deleteFiles(this.driver.configuration, List.of(location.id()));
    }

    @Override
    public void deleteAll() throws IllegalParametersException, IOException, SQLException {
        TrashHelper_123pan.deleteAllFiles(this.driver.configuration);
    }

    @Override
    public @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final @NotNull FileLocation location, final @NotNull String name) throws IllegalParametersException, IOException, SQLException {
        final UnionPair<FileSqlInformation, FailureReason> renamer = DriverHelper_123pan.renameFile(this.driver.configuration, location.id(), name, Options.DuplicatePolicy.ERROR);
        if (renamer.isFailure())
            return UnionPair.fail(renamer.getE());
        final TrashedSqlInformation information = this.info(renamer.getT().location());
        if (information == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", renamer.getT().location()));
        return UnionPair.ok(information);
    }

    @Override
    public @NotNull String toString() {
        return "Trash_123pan_NoCache{" +
                "driver=" + this.driver +
                '}';
    }
}
