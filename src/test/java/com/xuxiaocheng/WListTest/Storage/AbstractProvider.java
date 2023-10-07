package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.CheckRules.CheckRule;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("PublicField")
public class AbstractProvider extends AbstractIdBaseProvider<AbstractProvider.AbstractConfiguration> {
    public static class AbstractConfiguration extends StorageConfiguration {
    }

    public static class AbstractProviderFile {
        protected final @NotNull FileInformation information;
        protected final @NotNull Map<@NotNull Long, @NotNull AbstractProviderFile> children = new ConcurrentHashMap<>();

        public AbstractProviderFile(final @NotNull FileInformation information) {
            super();
            this.information = information;
        }

        public @NotNull FileInformation get() {
            return this.information;
        }

        public boolean hasChildren(){
            return this.information.isDirectory();
        }

        public @NotNull @UnmodifiableView Collection<@NotNull AbstractProviderFile> children() {
            Assertions.assertTrue(this.hasChildren());
            return Collections.unmodifiableCollection(this.children.values());
        }

        public @NotNull AbstractProviderFile get(final long id, final boolean isDirectory) {
            Assertions.assertTrue(this.hasChildren());
            final AbstractProviderFile child = this.children.get((id << 1) + (isDirectory ? 0 : 1));
            Assertions.assertNotNull(child, () -> id + (isDirectory ? " d" : " f"));
            return child;
        }

        public void add(final @NotNull AbstractProviderFile child) {
            Assertions.assertTrue(this.hasChildren());
            Assertions.assertEquals(this.get().id(), child.get().parentId());
            this.children.put((child.get().id() << 1) + (child.get().isDirectory() ? 0 : 1), child);
        }

        public void del(final long id, final boolean isDirectory) {
            Assertions.assertTrue(this.hasChildren());
            final AbstractProviderFile file = this.children.remove((id << 1) + (isDirectory ? 0 : 1));
            Assertions.assertNotNull(file, () -> id + (isDirectory ? " d" : " f"));
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractProviderFile that)) return false;
            return this.information.equals(that.information) && this.children.equals(that.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.information, this.children);
        }

        @Override
        public @NotNull String toString() {
            return "AbstractProviderFile{" +
                    "information=" + this.information +
                    ", children=" + this.children +
                    '}';
        }
    }

    public static @NotNull AbstractProviderFile build(final long id, final long parentId, final boolean isDirectory) {
        return new AbstractProviderFile(new FileInformation(id, parentId, String.valueOf(id),
                isDirectory, isDirectory ? -1 : 0, null, null, null));
    }

    protected final @NotNull AbstractProviderFile root = AbstractProvider.build(0, 0, true);
    protected final @NotNull List<@NotNull String> operations = new ArrayList<>();

    public @NotNull AbstractProviderFile root() {
        return this.root;
    }

    public @NotNull List<@NotNull String> checkOperations() {
        final List<String> list = new ArrayList<>(this.operations);
        this.operations.clear();
        return list;
    }

    protected @Nullable AbstractProviderFile find(final long id, final boolean isDirectory) {
        final BlockingQueue<AbstractProviderFile> queue = new LinkedBlockingQueue<>();
        queue.add(this.root);
        while (!queue.isEmpty()) {
            final AbstractProviderFile file = queue.remove();
            if (file.get().id() == id && file.hasChildren() == isDirectory)
                return file;
            if (file.hasChildren())
                queue.addAll(file.children.values());
        }
        return null;
    }

    @Override
    public @NotNull StorageTypes<AbstractConfiguration> getType() {
        throw new RuntimeException("Unreachable.");
    }

    @Override
    protected void loginIfNot() {
        this.operations.add("Login.");
    }


    public final @NotNull HInitializer<Iterator<@NotNull FileInformation>> list = new HInitializer<>("ListIterator");

    @Override
    protected @Nullable Iterator<@NotNull FileInformation> list0(final long directoryId) {
        this.operations.add("List: " + directoryId);
        final Iterator<FileInformation> iterator = this.list.uninitializeNullable();
        if (iterator != null)
            return iterator;
        final AbstractProviderFile directory = this.find(directoryId, true);
        return directory == null ? null : directory.children.values().stream().map(AbstractProviderFile::get).iterator();
    }


    public final @NotNull AtomicBoolean requireUpdate = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<UnionPair<UnionPair<FileInformation, Boolean>, Throwable>>> update = new HInitializer<>("UpdateSupplier");

    @Override
    protected boolean doesRequireUpdate(@NotNull final FileInformation information) {
        return this.requireUpdate.get();
    }

    @Override
    protected void update0(final @NotNull FileInformation oldInformation, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> consumer) {
        this.operations.add("Update: " + oldInformation.id() + (oldInformation.isDirectory() ? " d" : " f"));
        final Supplier<UnionPair<UnionPair<FileInformation, Boolean>, Throwable>> supplier = this.update.uninitializeNullable();
        if (supplier != null) {
            consumer.accept(supplier.get());
            return;
        }
        final AbstractProviderFile file = this.find(oldInformation.id(), oldInformation.isDirectory());
        if (file == null) {
            consumer.accept(AbstractIdBaseProvider.UpdateNotExisted);
            return;
        }
        consumer.accept(UnionPair.ok(UnionPair.ok(file.get())));
    }


    public final @NotNull AtomicBoolean supportInfo = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<UnionPair<Optional<FileInformation>, Throwable>>> info = new HInitializer<>("InfoSupplier");

    @Override
    protected boolean doesSupportInfo(final boolean isDirectory) {
        return this.supportInfo.get();
    }

    @Override
    protected void info0(final long id, final boolean isDirectory, final @NotNull Consumer<? super UnionPair<Optional<FileInformation>, Throwable>> consumer) {
        this.operations.add("Info: " + id + (isDirectory ? " d" : " f"));
        final Supplier<UnionPair<Optional<FileInformation>, Throwable>> supplier = this.info.uninitializeNullable();
        if (supplier != null) {
            consumer.accept(supplier.get());
            return;
        }
        final AbstractProviderFile file = this.find(id, isDirectory);
        if (file == null) {
            consumer.accept(AbstractIdBaseProvider.InfoNotExist);
            return;
        }
        consumer.accept(UnionPair.ok(Optional.of(file.get())));
    }


    public final @NotNull AtomicBoolean supportTrashRecursively = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<UnionPair<Boolean, Throwable>>> trash = new HInitializer<>("TrashSupplier");

    @Override
    protected boolean doesSupportTrashNotEmptyDirectory() {
        return this.supportTrashRecursively.get();
    }

    @Override
    protected void trash0(final @NotNull FileInformation information, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) {
        this.operations.add("Trash: " + information.id() + (information.isDirectory() ? " d" : " f"));
        final Supplier<UnionPair<Boolean, Throwable>> supplier = this.trash.uninitializeNullable();
        if (supplier != null) {
            consumer.accept(supplier.get());
            return;
        }
        final AbstractProviderFile directory = this.find(information.parentId(), true);
        if (directory != null)
            directory.del(information.id(), information.isDirectory());
        consumer.accept(AbstractIdBaseProvider.TrashSuccess);
    }


    @Override
    protected boolean doesRequireLoginDownloading(final @NotNull FileInformation information) {
        return false;
    }

    @Override
    protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) {
        throw new UnsupportedOperationException("Not tested.");
    }


    public final @NotNull HInitializer<Supplier<FileInformation>> create = new HInitializer<>("CreateSupplier");

    @Override
    protected @NotNull CheckRule<@NotNull String> directoryNameChecker() {
        return CheckRule.allAllow();
    }

    @Override
    protected void createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) {
        this.operations.add("Create: " + parentId + " " + directoryName);
        final FileInformation information = this.create.uninitialize().get();
        Assertions.assertEquals(parentId, information.parentId());
        Assertions.assertEquals(directoryName, information.name());
        Objects.requireNonNull(this.find(parentId, true)).add(new AbstractProviderFile(information));
        consumer.accept(UnionPair.ok(UnionPair.ok(information)));
    }


    @Override
    protected @NotNull CheckRule<@NotNull String> fileNameChecker() {
        return CheckRule.allAllow();
    }

    @Override
    protected void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) {
        throw new UnsupportedOperationException("Not tested.");
    }


    public final @NotNull AtomicBoolean supportCopyDirectly = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<FileInformation>> copy = new HInitializer<>("CopySupplier");

    @Override
    protected boolean doesSupportCopyDirectly(final @NotNull FileInformation information, final long parentId) {
        return this.supportCopyDirectly.get();
    }

    @Override
    protected void copyDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        this.operations.add("Copy: " + information.id() + (information.isDirectory() ? " d " : " f ") + parentId + " " + name);
        final FileInformation info = this.copy.uninitialize().get();
        Assertions.assertEquals(information.isDirectory(), info.isDirectory());
        Assertions.assertEquals(parentId, info.parentId());
        Assertions.assertEquals(name, info.name());
        Assertions.assertEquals(information.size(), info.size());
        Objects.requireNonNull(this.find(parentId, true)).add(new AbstractProviderFile(info));
        consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(info))));
    }


    public final @NotNull AtomicBoolean supportMoveDirectly = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<FileInformation>> move = new HInitializer<>("MoveSupplier");

    @Override
    protected boolean doesSupportMoveDirectly(final @NotNull FileInformation information, final long parentId) {
        return this.supportMoveDirectly.get();
    }

    @Override
    protected void moveDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        this.operations.add("Move: " + information.id() + (information.isDirectory() ? " d " : " f ") + parentId + " " + name);
        final FileInformation info = this.move.uninitialize().get();
        Assertions.assertEquals(information.id(), info.id());
        Assertions.assertEquals(information.isDirectory(), info.isDirectory());
        Assertions.assertEquals(parentId, info.parentId());
        Assertions.assertEquals(name, info.name());
        Assertions.assertEquals(information.size(), info.size());
        Objects.requireNonNull(this.find(information.parentId(), true)).del(information.id(), information.isDirectory());
        Objects.requireNonNull(this.find(parentId, true)).add(new AbstractProviderFile(info));
        consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(info))));
    }


    public final @NotNull AtomicBoolean supportRenameDirectly = new AtomicBoolean(true);
    public final @NotNull HInitializer<Supplier<FileInformation>> rename = new HInitializer<>("RenameSupplier");

    @Override
    protected boolean doesSupportRenameDirectly(final @NotNull FileInformation information) {
        return this.supportRenameDirectly.get();
    }

    @Override
    protected void renameDirectly0(final @NotNull FileInformation information, final @NotNull String name, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) {
        this.operations.add("Rename: " + information.id() + (information.isDirectory() ? " d " : " f ") + name);
        final FileInformation info = this.rename.uninitialize().get();
        Assertions.assertEquals(information.id(), info.id());
        Assertions.assertEquals(information.isDirectory(), info.isDirectory());
        Assertions.assertEquals(information.parentId(), info.parentId());
        Assertions.assertEquals(name, info.name());
        Assertions.assertEquals(information.size(), info.size());
        Objects.requireNonNull(this.find(information.parentId(), true)).del(information.id(), information.isDirectory());
        Objects.requireNonNull(this.find(information.parentId(), true)).add(new AbstractProviderFile(info));
        consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(info))));
    }


    @Override
    public @NotNull String toString() {
        return "AbstractProvider{" +
                "root=" + this.root +
                ", operations=" + this.operations +
                ", list=" + this.list +
                ", requireUpdate=" + this.requireUpdate +
                ", update=" + this.update +
                ", supportInfo=" + this.supportInfo +
                ", info=" + this.info +
                ", supportTrashRecursively=" + this.supportTrashRecursively +
                ", trash=" + this.trash +
                ", create=" + this.create +
                ", supportCopyDirectly=" + this.supportCopyDirectly +
                ", copy=" + this.copy +
                ", supportMoveDirectly=" + this.supportMoveDirectly +
                ", move=" + this.move +
                ", supportRenameDirectly=" + this.supportRenameDirectly +
                ", rename=" + this.rename +
                ", super=" + super.toString() +
                '}';
    }
}
