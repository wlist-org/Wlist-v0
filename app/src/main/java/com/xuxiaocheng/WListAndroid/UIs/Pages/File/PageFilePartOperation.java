package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.Manifest;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import com.qw.soul.permission.bean.Permissions;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.PermissionUtil;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileOperationBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileRenameBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PageFilePartOperation {
    protected final @NotNull PageFile pageFile;

    public PageFilePartOperation(final @NotNull PageFile pageFile) {
        super();
        this.pageFile = pageFile;
    }

    @UiThread
    protected void rootOperation(final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean clickable) {
        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
            throw new UnsupportedOperationException("WIP");
        }, () -> clickable.set(true))); // TODO
    }

    @UiThread
    protected void insideOperation(final @NotNull String storage, final @NotNull VisibleFileInformation information, final @NotNull AtomicBoolean c) {
        final PageFileOperationBinding operationBinding = PageFileOperationBinding.inflate(this.pageFile.activity().getLayoutInflater());
        final AlertDialog modifier = new AlertDialog.Builder(this.pageFile.activity())
                .setTitle(R.string.page_file_operation).setView(operationBinding.getRoot())
                .setOnCancelListener(d -> c.set(true))
                .setPositiveButton(R.string.cancel, (d, w) -> c.set(true)).create();
        operationBinding.pageFileOperationName.setText(FileInformationGetter.name(information));
        final long size = FileInformationGetter.size(information);
        final String unknown = this.pageFile.activity().getString(R.string.unknown);
        operationBinding.pageFileOperationSize.setText(ViewUtil.formatSizeDetail(size, unknown));
        operationBinding.pageFileOperationCreate.setText(ViewUtil.formatTime(FileInformationGetter.createTime(information), unknown));
        operationBinding.pageFileOperationUpdate.setText(ViewUtil.formatTime(FileInformationGetter.updateTime(information), unknown));
        final FileLocation current = new FileLocation(storage, FileInformationGetter.id(information));
        final AtomicBoolean clickable = new AtomicBoolean(true);
        operationBinding.pageFileOperationRename.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            final PageFileRenameBinding renamer = PageFileRenameBinding.inflate(this.pageFile.activity().getLayoutInflater());
            renamer.pageFileRenameName.setText(FileInformationGetter.name(information));
            if (renamer.pageFileRenameName.requestFocus()) {
                renamer.pageFileRenameName.setSelectAllOnFocus(true);
                renamer.pageFileRenameName.setSelection(Objects.requireNonNull(renamer.pageFileRenameName.getText()).length());
            }
            new AlertDialog.Builder(this.pageFile.activity()).setTitle(R.string.page_file_operation_rename).setView(renamer.getRoot())
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        final String renamed = ViewUtil.getText(renamer.pageFileRenameName);
                        if (AndroidSupporter.isBlank(renamed) || FileInformationGetter.name(information).equals(renamed)) return;
                        final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_rename);
                        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
                                    ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("renamed", renamed));
                            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.rename(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), renamed,
                                    Main.ClientExecutors, HExceptionWrapper.wrapPredicate(this::queryNotSupportedOperation));
                            if (res == null) return;
                            if (res.isFailure())
                                Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                            else
                                Main.showToast(this.pageFile.activity(), R.string.page_file_operation_rename_success);
                        }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
                    }).show();
        });
        operationBinding.pageFileOperationRenameImage.setOnClickListener(u -> operationBinding.pageFileOperationRename.performClick());
        operationBinding.pageFileOperationMove.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                final Pair.ImmutablePair<String, Long> pair = FileInformationGetter.isDirectory(information) ?
                        this.queryTargetDirectory(R.string.page_file_operation_move, storage, FileInformationGetter.id(information)) :
                        this.queryTargetDirectory(R.string.page_file_operation_move, null, 0);
                if (pair == null) return;
                final FileLocation target = new FileLocation(pair.getFirst(), pair.getSecond().longValue());
                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Moving.",
                        ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("target", target));
                Main.runOnUiThread(this.pageFile.activity(), () -> {
                    final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_move);
                    Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        final AtomicBoolean queried = new AtomicBoolean(false);
                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.move(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), target, Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
                            if (queried.getAndSet(true)) return true;
                            return this.queryNotSupportedOperation(p);
                        }));
                        if (res == null) return;
                        if (res.isFailure())
                            Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                        else
                            Main.showToast(this.pageFile.activity(), R.string.page_file_operation_move_success);
                    }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
                });
            }));
        });
        operationBinding.pageFileOperationMoveImage.setOnClickListener(u -> operationBinding.pageFileOperationMove.performClick());
        operationBinding.pageFileOperationCopy.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                final Pair.ImmutablePair<String, Long> pair = FileInformationGetter.isDirectory(information) ?
                        this.queryTargetDirectory(R.string.page_file_operation_copy, storage, FileInformationGetter.id(information)) :
                        this.queryTargetDirectory(R.string.page_file_operation_copy, null, 0);
                if (pair == null) return;
                final FileLocation target = new FileLocation(pair.getFirst(), pair.getSecond().longValue());
                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Copying.",
                        ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("target", target));
                Main.runOnUiThread(this.pageFile.activity(), () -> {
                    final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_copy);
                    Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        final AtomicBoolean queried = new AtomicBoolean(false);
                        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copy(this.pageFile.address(), this.pageFile.username(), current, FileInformationGetter.isDirectory(information), target, FileInformationGetter.name(information), Main.ClientExecutors, HExceptionWrapper.wrapPredicate(p -> {
                            if (queried.getAndSet(true)) return true;
                            return this.queryNotSupportedOperation(p);
                        }));
                        if (res == null) return;
                        if (res.isFailure())
                            Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res.getE()) + FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                        else
                            Main.showToast(this.pageFile.activity(), R.string.page_file_operation_copy_success);
                    }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
                });
            }));
        });
        operationBinding.pageFileOperationCopyImage.setOnClickListener(u -> operationBinding.pageFileOperationCopy.performClick());
        operationBinding.pageFileOperationTrash.setOnClickListener(u -> {
            if (!clickable.compareAndSet(true, false)) return;
            modifier.cancel();
            new AlertDialog.Builder(this.pageFile.activity()).setTitle(R.string.page_file_operation_trash)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        final AlertDialog dialog = this.pageFile.partUpload.loadingDialog(this.pageFile.activity(), R.string.page_file_operation_trash);
                        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
                                    ParametersMap.create().add("address", this.pageFile.address()).add("information", information));
                            if (FilesAssistant.trash(this.pageFile.address(), this.pageFile.username(), new FileLocation(storage, FileInformationGetter.id(information)), FileInformationGetter.isDirectory(information), HExceptionWrapper.wrapPredicate(unused -> this.queryNotSupportedOperation(null))))
                                Main.showToast(this.pageFile.activity(), R.string.page_file_operation_delete_success);
                        }, () -> Main.runOnUiThread(this.pageFile.activity(), dialog::cancel)));
                    }).show();
        });
        operationBinding.pageFileOperationTrashImage.setOnClickListener(u -> operationBinding.pageFileOperationTrash.performClick());
        if (FileInformationGetter.isDirectory(information)) {
            operationBinding.pageFileOperationDownload.setVisibility(View.GONE);
            operationBinding.pageFileOperationDownloadImage.setVisibility(View.GONE);
        } else {
            operationBinding.pageFileOperationDownload.setOnClickListener(u -> {
                if (!clickable.compareAndSet(true, false)) return;
                modifier.cancel();
                new AlertDialog.Builder(this.pageFile.activity()).setTitle(R.string.page_file_operation_download)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final AlertDialog loader = new AlertDialog.Builder(this.pageFile.activity()).setTitle(FileInformationGetter.name(information)).setCancelable(false).show();
                            Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
                                final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist/" + FileInformationGetter.name(information));
                                PermissionUtil.tryGetPermission(this.pageFile.activity(), Permissions.build(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), R.string.toast_no_read_permissions);
                                HFileHelper.ensureFileAccessible(file, true);
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
                                        ParametersMap.create().add("address", this.pageFile.address()).add("information", information).add("file", file));
                                final VisibleFailureReason res;
                                try {
                                    this.pageFile.partList.listLoadingAnimation("Downloading", true, 0, 0); // TODO: download progress.
                                    res = FilesAssistant.download(this.pageFile.address(), this.pageFile.username(), new FileLocation(storage, FileInformationGetter.id(information)), file, PredicateE.truePredicate(), s -> {
                                        long curr = 0, total = 0;
                                        for (final Pair.ImmutablePair<Long, Long> pair : InstantaneousProgressStateGetter.stages(s)) {
                                            curr += pair.getFirst().longValue();
                                            total += pair.getSecond().longValue();
                                        }
                                        final long l = curr, t = total;
                                        this.pageFile.partList.listLoadingAnimation("Downloading", true, l, t);
                                    });
                                } finally {
                                    this.pageFile.partList.listLoadingAnimation("Downloading", false, 0, 0);
                                }
                                if (res != null)
                                    Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.kind(res) + FailureReasonGetter.message(res), Toast.LENGTH_SHORT).show());
                                else
                                    Main.showToast(this.pageFile.activity(), R.string.page_file_operation_download_success);
                            }, () -> Main.runOnUiThread(this.pageFile.activity(), loader::cancel)));
                        }).show();
            });
            operationBinding.pageFileOperationDownloadImage.setOnClickListener(u -> operationBinding.pageFileOperationDownload.performClick());
        }
        modifier.show();
    }


    @WorkerThread
    public boolean queryNotSupportedOperation(final FilesAssistant.@Nullable NotDirectlyPolicy policy) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean continuer = new AtomicBoolean(false);
        Main.runOnUiThread(this.pageFile.activity(), () -> new AlertDialog.Builder(this.pageFile.activity())
                .setTitle(R.string.page_file_operation_complex)
                .setOnCancelListener(a -> latch.countDown())
                .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
                .setPositiveButton(R.string.confirm, (a, k) -> Main.runOnBackgroundThread(this.pageFile.activity(), () -> {
                    continuer.set(true);
                    latch.countDown();
                })).show());
        latch.await();
        return continuer.get();
    }

    @WorkerThread
    public Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> queryTargetDirectory(@StringRes final int title, final @Nullable String currentStorage, final long currentDirectoryId) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<FileLocation> result = new AtomicReference<>();
        final PageFile page = new PageFile() {
            @Override
            public @NotNull ActivityMain activity() {
                return PageFilePartOperation.this.pageFile.activity();
            }
        }; // TODO
        Main.runOnUiThread(this.pageFile.activity(), () -> {
            page.onCreateView(this.pageFile.activity().getLayoutInflater(), null, null);
            final View p = this.pageFile.getPage().getRoot();
            new AlertDialog.Builder(this.pageFile.activity())
                    .setTitle(title).setView(page.getPage().getRoot())
                    .setOnCancelListener(a -> latch.countDown())
                    .setNegativeButton(R.string.cancel, (a, b) -> latch.countDown())
                    .setPositiveButton(R.string.confirm, (a, k) -> {
                        result.set(page.partList.currentLocation());
                        latch.countDown();
                    }).show()
                    .getWindow().setLayout(p.getWidth(), p.getHeight());
            page.getPage().getRoot().setLayoutParams(new FrameLayout.LayoutParams(p.getWidth(), p.getHeight()));
            page.getPage().pageFileUploader.setVisibility(View.GONE);
        });
        latch.await();
        final FileLocation choice = result.get();
        if (choice == null)
            return null;
        return Pair.ImmutablePair.makeImmutablePair(FileLocationGetter.storage(choice), FileLocationGetter.id(choice));
    }
}
