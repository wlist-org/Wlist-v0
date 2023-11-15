package com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Main.File;

import android.view.View;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HProcessingInitializer;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.Tasks.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.Tasks.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Pages.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationRenameBinding;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

class PartOperation extends SFragmentFilePart {
    protected PartOperation(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @UiThread
    protected void rootOperation(final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean clickable) {
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            throw new UnsupportedOperationException("WIP");
        }, () -> clickable.set(true))); // TODO
    }

    @UiThread
    protected void insideOperation(final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean c) {
        final PageFileOperationBinding operationBinding = PageFileOperationBinding.inflate(this.activity().getLayoutInflater());
        final AlertDialog modifier = new AlertDialog.Builder(this.activity())
                .setTitle(R.string.page_file_operation).setView(operationBinding.getRoot())
                .setOnCancelListener(d -> c.set(true))
                .setPositiveButton(R.string.cancel, (d, w) -> c.set(true)).create();
        operationBinding.pageFileOperationName.setText(FileInformationGetter.name(information));
        final long size = FileInformationGetter.size(information);
        final String unknown = this.activity().getString(R.string.unknown);
        operationBinding.pageFileOperationSize.setText(ViewUtil.formatSizeDetail(size, unknown));
        operationBinding.pageFileOperationCreate.setText(ViewUtil.formatTime(FileInformationGetter.createTime(information), unknown));
        operationBinding.pageFileOperationUpdate.setText(ViewUtil.formatTime(FileInformationGetter.updateTime(information), unknown));
        final FileLocation current = new FileLocation(storage, FileInformationGetter.id(information));
        final AtomicBoolean clickable = new AtomicBoolean(true);
        operationBinding.pageFileOperationRename.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            final PageFileOperationRenameBinding rename = PageFileOperationRenameBinding.inflate(this.activity().getLayoutInflater());
            ViewUtil.focusEdit(rename.pageFileRenameName, FileInformationGetter.name(information));
            new AlertDialog.Builder(this.activity()).setTitle(R.string.page_file_operation_rename).setView(rename.getRoot())
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        final String renamed = ViewUtil.getText(rename.pageFileRenameName);
                        if (AndroidSupporter.isBlank(renamed) || FileInformationGetter.name(information).equals(renamed)) return;

                        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                            throw new UnsupportedOperationException("WIP");
                        }, () -> clickable.set(true))); // TODO
//                        final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_rename);
//                        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
//                                    ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("renamed", renamed));
//                            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.rename(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), renamed,
//                                    Main.ClientExecutors, HExceptionWrapper.wrapPredicate(this::queryNotSupportedOperation));
//                            if (res == null) return;
//                            if (res.isFailure())
//                                Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
//                            else
//                                Main.showToast(this.pageFile.activity(), R.string.page_file_operation_rename_success);
//                        }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
                    }).show();
            modifier.cancel();
        });
        operationBinding.pageFileOperationRenameImage.setOnClickListener(u -> operationBinding.pageFileOperationRename.performClick());
        operationBinding.pageFileOperationMove.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
//            Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                final Pair.ImmutablePair<String, Long> pair = FileInformationGetter.isDirectory(information) ?
//                        this.queryTargetDirectory(R.string.page_file_operation_move, storage, FileInformationGetter.id(information)) :
//                        this.queryTargetDirectory(R.string.page_file_operation_move, null, 0);
//                if (pair == null) return;
//                final FileLocation target = new FileLocation(pair.getFirst(), pair.getSecond().longValue());
//                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Moving.",
//                        ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("target", target));
//                Main.runOnUiThread(this.pageFile.activity(), () -> {
//                    final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_move);
//                    Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                        final AtomicBoolean queried = new AtomicBoolean(false);
//                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.move(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), target, Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
//                            if (queried.getAndSet(true)) return true;
//                            return this.queryNotSupportedOperation(p);
//                        }));
//                        if (res == null) return;
//                        if (res.isFailure())
//                            Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
//                        else
//                            Main.showToast(this.pageFile.activity(), R.string.page_file_operation_move_success);
//                    }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
//                });
//            }));
        });
        operationBinding.pageFileOperationMoveImage.setOnClickListener(u -> operationBinding.pageFileOperationMove.performClick());
        operationBinding.pageFileOperationCopy.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
//            Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                final Pair.ImmutablePair<String, Long> pair = FileInformationGetter.isDirectory(information) ?
//                        this.queryTargetDirectory(R.string.page_file_operation_copy, storage, FileInformationGetter.id(information)) :
//                        this.queryTargetDirectory(R.string.page_file_operation_copy, null, 0);
//                if (pair == null) return;
//                final FileLocation target = new FileLocation(pair.getFirst(), pair.getSecond().longValue());
//                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Copying.",
//                        ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("target", target));
//                Main.runOnUiThread(this.pageFile.activity(), () -> {
//                    final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_copy);
//                    Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                        final AtomicBoolean queried = new AtomicBoolean(false);
//                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copy(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), target, FileInformationGetter.name(information), Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
//                            if (queried.getAndSet(true)) return true;
//                            return this.queryNotSupportedOperation(p);
//                        }));
//                        if (res == null) return;
//                        if (res.isFailure())
//                            Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
//                        else
//                            Main.showToast(this.pageFile.activity(), R.string.page_file_operation_copy_success);
//                    }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
//                });
//            }));
        });
        operationBinding.pageFileOperationCopyImage.setOnClickListener(u -> operationBinding.pageFileOperationCopy.performClick());
        operationBinding.pageFileOperationTrash.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
//            new AlertDialog.Builder(this.pageFile.activity()).setTitle(R.string.page_file_operation_trash)
//                    .setNegativeButton(R.string.cancel, null)
//                    .setPositiveButton(R.string.confirm, (d, w) -> {
//                        final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_trash);
//                        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
//                                    ParametersMap.create().add("address", this.pageFile.address()).add("information", information));
//                            if (FilesAssistant.trash(this.pageFile.address(), this.pageFile.username(), new FileLocation(storage, FileInformationGetter.id(information)), FileInformationGetter.isDirectory(information), HExceptionWrapper.wrapPredicate(unused -> this.queryNotSupportedOperation(null))))
//                                Main.showToast(this.pageFile.activity(), R.string.page_file_operation_delete_success);
//                        }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
//                    }).show();
        });
        operationBinding.pageFileOperationTrashImage.setOnClickListener(u -> operationBinding.pageFileOperationTrash.performClick());
        if (FileInformationGetter.isDirectory(information)) {
            operationBinding.pageFileOperationDownload.setVisibility(View.GONE);
            operationBinding.pageFileOperationDownloadImage.setVisibility(View.GONE);
        } else {
            operationBinding.pageFileOperationDownload.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                final UnionPair<HProcessingInitializer.LoadingState, Throwable> state = AbstractTasksManager.managers.getLoadingState(PageTaskAdapter.Types.Download);
                if (state == HProcessingInitializer.USuccess) {
                    modifier.cancel();
                    new AlertDialog.Builder(this.activity()).setTitle(R.string.page_file_operation_download)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> DownloadTasksManager.getInstance()
                                    .addTask(this.activity(), new DownloadTasksManager.DownloadTask(this.address(), this.username(), MiscellaneousUtil.now(), current, FileInformationGetter.name(information))))))
                            .show();
                } else {
                    if (state.isSuccess())
                        Main.showToast(this.activity(), R.string.page_task_download_manager_waiting);
                    else {
                        Main.showToast(this.activity(), R.string.page_task_download_manager_failure);
                        this.fragment.partTask().initializeManagers();
                    }
                    clickable.set(true);
                }
            });
            operationBinding.pageFileOperationDownloadImage.setOnClickListener(u -> operationBinding.pageFileOperationDownload.performClick());
        }
        modifier.show();
    }


//    @WorkerThread
//    public boolean queryNotSupportedOperation(final FilesAssistant.@Nullable NotDirectlyPolicy policy) throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        final AtomicBoolean continuer = new AtomicBoolean(false);
//        Main.runOnUiThread(this.pageFile.activity(), () -> new AlertDialog.Builder(this.pageFile.activity())
//                .setTitle(R.string.page_file_operation_complex)
//                .setOnCancelListener(a -> latch.countDown())
//                .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
//                .setPositiveButton(R.string.confirm, (a, k) -> Main.runOnBackgroundThread(this.pageFile.activity(), () -> {
//                    continuer.set(true);
//                    latch.countDown();
//                })).show());
//        latch.await();
//        return continuer.get();
//    }
//
//    @WorkerThread
//    public Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> queryTargetDirectory(@StringRes final int title, final @Nullable String currentStorage, final long currentDirectoryId) throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        final AtomicReference<FileLocation> result = new AtomicReference<>();
//        final PageFile fragment = new PageFile() {
//            @Override
//            public @NotNull ActivityMain activity() {
//                return PageFilePartOperation.this.pageFile.activity();
//            }
//        }; // TODO
//        Main.runOnUiThread(this.pageFile.activity(), () -> {
//            fragment.onCreateView(this.pageFile.activity().getLayoutInflater(), null, null);
//            final View p = this.pageFile.fragment().getRoot();
//            new AlertDialog.Builder(this.pageFile.activity())
//                    .setTitle(title).setView(fragment.fragment().getRoot())
//                    .setOnCancelListener(a -> latch.countDown())
//                    .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
//                    .setPositiveButton(R.string.confirm, (a, k) -> {
////                        result.set(fragment.partList.currentLocation());
//                        latch.countDown();
//                    }).show()
//                    .getWindow().setLayout(p.getWidth(), p.getHeight());
//            fragment.fragment().getRoot().setLayoutParams(new FrameLayout.LayoutParams(p.getWidth(), p.getHeight()));
//            fragment.fragment().pageFileUploader.setVisibility(View.GONE);
//        });
//        latch.await();
//        final FileLocation choice = result.get();
//        if (choice == null)
//            return null;
//        return Pair.ImmutablePair.makeImmutablePair(FileLocationGetter.storage(choice), FileLocationGetter.id(choice));
//    }
}
