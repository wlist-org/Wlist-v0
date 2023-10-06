package com.xuxiaocheng.WListTest.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqliteHelper;
import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
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
            final AbstractProviderFile child = this.children.get(FileSqliteHelper.getDoubleId(id, isDirectory));
            Assertions.assertNotNull(child, () -> id + (isDirectory ? " d" : " f"));
            return child;
        }

        public void add(final @NotNull AbstractProviderFile child) {
            Assertions.assertTrue(this.hasChildren());
            Assertions.assertEquals(this.get().id(), child.get().parentId());
            this.children.put(FileSqliteHelper.getDoubleId(child.get().id(), child.get().isDirectory()), child);
        }

        public void del(final long id, final boolean isDirectory) {
            Assertions.assertTrue(this.hasChildren());
            final AbstractProviderFile file = this.children.remove(FileSqliteHelper.getDoubleId(id, isDirectory));
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


//    @Override
//    protected void download0(final @NotNull FileInformation information, final long from, final long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) {
//        throw new UnsupportedOperationException("Not tested.");
//    }
//
//    @Override
//    protected @NotNull CheckRule<@NotNull String> directoryNameChecker() {
//        return CheckRule.allAllow();
//    }
//
//    @Override
//    protected void createDirectory0(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) {
//        final FileInformation information = AbstractProviderTest.this.create.getAndSet(null);
//        if (information == null) {
//            consumer.accept(UnionPair.ok(UnionPair.fail(FailureReason.byInvalidName(parentLocation, directoryName, "For test."))));
//            return;
//        }
//        Assertions.assertEquals(parentId, information.parentId());
//        Assertions.assertEquals(directoryName, information.name());
//        consumer.accept(UnionPair.ok(UnionPair.ok(information)));
//    }
//
//    @Override
//    protected @NotNull CheckRule<@NotNull String> fileNameChecker() {
//        return CheckRule.allAllow();
//    }
//
//    @Override
//    protected void uploadFile0(final long parentId, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) {
//        throw new UnsupportedOperationException("Not tested.");
//    }
//
//    @Override
//    protected boolean isSupportedCopyFileDirectly(final @NotNull FileInformation information, final long parentId) {
//        return true;
//    }
//
//    @Override
//    protected void copyFileDirectly0(final @NotNull FileInformation information, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) {
//        final FileInformation copied = AbstractProviderTest.this.copy.getAndSet(null);
//        Assertions.assertNotNull(copied);
//        Assertions.assertEquals(filename, copied.name());
//        Assertions.assertEquals(information.size(), copied.size());
//        consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(Optional.of(copied)))));
//    }
//
//    @Override
//    protected boolean isSupportedMoveDirectly(final @NotNull FileInformation information, final long parentId) {
//        return true;
//    }
//
//    @Override
//    protected void moveDirectly0(final @NotNull FileInformation information, final long parentId, final Options.@NotNull DuplicatePolicy ignoredPolicy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) {
//        final FileInformation moved = AbstractProviderTest.this.move.getAndSet(null);
//        Assertions.assertNotNull(moved);
//        Assertions.assertEquals(information.name(), moved.name());
//        Assertions.assertEquals(information.size(), moved.size());
//        Assertions.assertEquals(parentId, moved.parentId());
//        consumer.accept(UnionPair.ok(Optional.of(UnionPair.ok(Optional.of(moved)))));
//    }
}
