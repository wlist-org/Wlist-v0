package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HProcessingInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.DownloadTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.RenameTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.TrashTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationRenameBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
                                    ParametersMap.create().add("address", this.address()).add("information", information).add("renamed", renamed));
                            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> res;
                            try (final WListClientInterface client = this.client()) {
                                res = OperateFilesHelper.renameDirectly(client, this.token(), current, FileInformationGetter.isDirectory(information), renamed, DuplicatePolicy.KEEP);
                            }
                            if (res.isFailure()) {
                                Toaster.show(MessageFormat.format(this.activity().getString(R.string.page_file_operation_rename), FailureReasonGetter.toString(res.getE())));
                                return;
                            }
                            if (res.getT().isPresent()) {
                                Toaster.show(R.string.page_file_operation_rename_success);
                                return;
                            }
                            final UnionPair<HProcessingInitializer.LoadingState, Throwable> state = AbstractTasksManager.managers.getLoadingState(PageTaskAdapter.Types.Rename);
                            if (state == HProcessingInitializer.USuccess) {
                                Toaster.showShort(R.string.page_file_operation_complex);
                                RenameTasksManager.getInstance().addTask(this.activity(), new RenameTasksManager.RenameTask(new AbstractTasksManager.AbstractTask(
                                        this.address(), this.username(), ZonedDateTime.now(), FileInformationGetter.name(information), PageTaskAdapter.Types.Rename),
                                        current, FileInformationGetter.isDirectory(information), renamed));
                            } else {
                                if (state.isSuccess())
                                    Toaster.show(R.string.page_task_rename_manager_waiting);
                                else {
                                    Toaster.show(R.string.page_task_rename_manager_failure);
                                    this.fragment.partTask().initializeManagers();
                                }
                            }
                        }));
                    }).show();
        });
        operationBinding.pageFileOperationRenameImage.setOnClickListener(u -> operationBinding.pageFileOperationRename.performClick());
        operationBinding.pageFileOperationMove.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            // TODO
            Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                final Pair.ImmutablePair<String, Long> pair = FileInformationGetter.isDirectory(information) ?
                        this.queryTargetDirectory(R.string.page_file_operation_move, storage, FileInformationGetter.id(information)) :
                        this.queryTargetDirectory(R.string.page_file_operation_move, null, 0);
                if (pair == null) return;
                final FileLocation target = new FileLocation(pair.getFirst(), pair.getSecond().longValue());
                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Moving.",
                        ParametersMap.create().add("address", this.address()).add("information", information).add("target", target));
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
            }));
        });
        operationBinding.pageFileOperationMoveImage.setOnClickListener(u -> operationBinding.pageFileOperationMove.performClick());
        operationBinding.pageFileOperationCopy.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            // TODO
//            Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
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
            new AlertDialog.Builder(this.activity()).setTitle(R.string.page_file_operation_trash)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        final AlertDialog dialog = this.activity().createLoadingDialog(R.string.page_file_operation_trash);
                        dialog.show();
                        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
                                    ParametersMap.create().add("address", this.address()).add("information", information));
                            final FileLocation location = new FileLocation(storage, FileInformationGetter.id(information));
                            final boolean isDirectory = FileInformationGetter.isDirectory(information);
                            final Boolean success;
                            try (final WListClientInterface client = this.client()) {
                                success = OperateFilesHelper.trashFileOrDirectory(client, this.token(), location, isDirectory);
                            }
                            if (success == null || success.booleanValue()) {
                                Toaster.show(R.string.page_file_operation_trash_success);
                                return;
                            }
                            final UnionPair<HProcessingInitializer.LoadingState, Throwable> state = AbstractTasksManager.managers.getLoadingState(PageTaskAdapter.Types.Trash);
                            if (state == HProcessingInitializer.USuccess) {
                                Toaster.showShort(R.string.page_file_operation_complex);
                                TrashTasksManager.getInstance().addTask(this.activity(), new TrashTasksManager.TrashTask(new AbstractTasksManager.AbstractTask(
                                        this.address(), this.username(), ZonedDateTime.now(), FileInformationGetter.name(information), PageTaskAdapter.Types.Trash), location));
                            } else {
                                if (state.isSuccess())
                                    Toaster.show(R.string.page_task_trash_manager_waiting);
                                else {
                                    Toaster.show(R.string.page_task_trash_manager_failure);
                                    this.fragment.partTask().initializeManagers();
                                }
                            }
                        }, () -> Main.runOnUiThread(this.activity(), dialog::cancel)));
                    }).show();
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
                            .setPositiveButton(R.string.confirm, (d, w) -> Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() ->
                                    DownloadTasksManager.getInstance().addTask(this.activity(), new DownloadTasksManager.DownloadTask(new AbstractTasksManager.AbstractTask(
                                            this.address(), this.username(), MiscellaneousUtil.now(), FileInformationGetter.name(information),
                                            PageTaskAdapter.Types.Download), current)))))
                            .show();
                } else {
                    if (state.isSuccess())
                        Toaster.show(R.string.page_task_download_manager_waiting);
                    else {
                        Toaster.show(R.string.page_task_download_manager_failure);
                        this.fragment.partTask().initializeManagers();
                    }
                    clickable.set(true);
                }
            });
            operationBinding.pageFileOperationDownloadImage.setOnClickListener(u -> operationBinding.pageFileOperationDownload.performClick());
        }
        modifier.show();
    }

    @WorkerThread
    public Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> queryTargetDirectory(@StringRes final int title, final @Nullable String currentStorage, final long currentDirectoryId) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<FileLocation> result = new AtomicReference<>();
        final FragmentFile fragment = new FragmentFile();
        fragment.setSelectingMode(currentStorage, currentDirectoryId);
        Main.runOnUiThread(this.activity(), () -> {
            this.fragment.getChildFragmentManager().beginTransaction().add(fragment, "selecting").commitNow();
            final View p = this.fragmentContent().getRoot();
            new AlertDialog.Builder(this.activity())
                    .setTitle(title).setView(fragment.content().getRoot())
                    .setOnCancelListener(a -> latch.countDown())
                    .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
                    .setPositiveButton(R.string.confirm, (a, k) -> {
                        result.set(fragment.partList().currentLocation());
                        latch.countDown();
                    }).show()
                    .getWindow().setLayout(p.getWidth(), p.getHeight());
            fragment.content().getRoot().setLayoutParams(new FrameLayout.LayoutParams(p.getWidth(), p.getHeight()));
            Main.runOnBackgroundThread(this.activity(), fragment::cOnConnect);
        });
        latch.await();
        this.fragment.getChildFragmentManager().beginTransaction().remove(fragment).commit();
        final FileLocation choice = result.get();
        if (choice == null)
            return null;
        return Pair.ImmutablePair.makeImmutablePair(FileLocationGetter.storage(choice), FileLocationGetter.id(choice));
    }
}
