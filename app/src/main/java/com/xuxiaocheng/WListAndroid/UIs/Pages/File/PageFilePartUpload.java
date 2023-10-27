package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FailureReasonGetter;
import com.xuxiaocheng.WList.AndroidSupports.InstantaneousProgressStateGetter;
import com.xuxiaocheng.WList.AndroidSupports.StorageTypeGetter;
import com.xuxiaocheng.WList.Client.Assistants.FilesAssistant;
import com.xuxiaocheng.WList.Client.Assistants.TokenAssistant;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileDirectoryBinding;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class PageFilePartUpload {
    protected final @NotNull PageFile pageFile;

    public PageFilePartUpload(final @NotNull PageFile pageFile) {
        super();
        this.pageFile = pageFile;
    }

    private @NotNull ActivityMain activity() {
        return this.pageFile.activity();
    }

    private @NotNull FileLocation currentLocation() {
        return this.pageFile.partList.currentLocation();
    }


    private @NotNull ImageView loadingView() {
        final ImageView loading = new ImageView(this.activity());
        loading.setImageResource(R.drawable.loading);
        ViewUtil.startDrawableAnimation(loading);
        return loading;
    }

    @UiThread
    protected @NotNull AlertDialog loadingDialog(@StringRes final int title) {
        return new AlertDialog.Builder(this.activity()).setTitle(title).setView(this.loadingView()).setCancelable(false).show();
    }

    @UiThread
    protected void createDirectory() {
        final FileLocation location = this.currentLocation();
        final PageFileDirectoryBinding editor = PageFileDirectoryBinding.inflate(this.activity().getLayoutInflater());
        editor.pageFileDirectoryName.setText(R.string.page_file_upload_directory_name);
        editor.pageFileDirectoryName.setHint(R.string.page_file_upload_directory_hint);
        if (editor.pageFileDirectoryName.requestFocus()) {
            editor.pageFileDirectoryName.setSelectAllOnFocus(true);
            editor.pageFileDirectoryName.setSelection(Objects.requireNonNull(editor.pageFileDirectoryName.getText()).length());
        }
        new AlertDialog.Builder(this.activity()).setTitle(R.string.page_file_create_directory)
                .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    final String name = ViewUtil.getText(editor.pageFileDirectoryName);
                    final AlertDialog loading = new AlertDialog.Builder(this.activity())
                            .setTitle(R.string.page_file_create_directory).setView(this.loadingView()).setCancelable(false).show();
                    Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
                                ParametersMap.create().add("address", this.activity().address()).add("location", location).add("name", name));
                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.activity().address())) {
                            OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(this.activity().address(), this.activity().username()), location, name, DuplicatePolicy.ERROR);
                        }
                        Main.showToast(this.activity(), R.string.page_file_upload_success_directory);
                    }, () -> Main.runOnUiThread(this.activity(), loading::cancel)));
                }).show();
    }

    @UiThread
    @SuppressWarnings("unchecked")
    protected  <C extends StorageConfiguration> void addStorage() {
        final String[] storages = StorageTypeGetter.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
        final AtomicInteger choice = new AtomicInteger(-1);
        new AlertDialog.Builder(this.activity()).setTitle(R.string.page_file_create_storage)
                .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    if (choice.get() == -1) return;
                    final String identifier = storages[choice.get()];
                    final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypeGetter.get(identifier));
                    PageFileProviderConfigurations.getConfiguration(this.activity(), type, null, configuration -> Main.runOnUiThread(this.activity(), () -> {
                        final AlertDialog loading = new AlertDialog.Builder(this.activity())
                                .setTitle(R.string.page_file_create_storage).setView(this.loadingView())
                                .setCancelable(false).show();
                        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                            try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.activity().address())) {
                                OperateProvidersHelper.addProvider(client, TokenAssistant.getToken(this.activity().address(), this.activity().username()),
                                        configuration.getName(), type, configuration);
                            }
                        }, () -> Main.runOnUiThread(this.activity(), loading::cancel)));
                    }));
                }).show();
    }

    @UiThread
    protected void uploadFile(final @NotNull Uri uri) {
        Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
            final FileLocation location = this.currentLocation();
            // TODO: serialize uploading task.
            // uri.toString()  -->  Uri.parse(...)
            final String filename;
            final long size;
            try (final Cursor cursor = this.activity().getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) return;
                filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            }
            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading files.",
                    ParametersMap.create().add("address", this.activity().address()).add("location", location).add("filename", filename).add("size", size).add("uri", uri));
            this.pageFile.partList.listLoadingAnimation(true, 0, 0);
            Main.runOnUiThread(this.activity(), () -> {
                final AlertDialog loader = new AlertDialog.Builder(this.activity()).setTitle(filename).setCancelable(false).show();
                Main.runOnUiThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                    final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.uploadStream(this.activity().address(), this.activity().username(), HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
                        try (final InputStream stream = new BufferedInputStream(this.activity().getContentResolver().openInputStream(uri))) {
                            AndroidSupporter.skipNBytes(stream, pair.getFirst().longValue());
                            consumer.accept(stream);
                        }
                    }), size, filename, location, PredicateE.truePredicate(), s -> { // TODO upload progress.
                        long current = 0, total = 0;
                        for (final Pair.ImmutablePair<Long, Long> pair: InstantaneousProgressStateGetter.stages(s)) {
                            current += pair.getFirst().longValue();
                            total += pair.getSecond().longValue();
                        }
                        final long c = current, t = total;
                        this.pageFile.partList.listLoadingAnimation(true, c, t);
                    });
                    assert res != null;
                    if (res.isFailure()) // TODO
                        Main.runOnUiThread(this.activity(), () -> Toast.makeText(this.activity(), FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
                    else
                        Main.showToast(this.activity(), R.string.page_file_upload_success_file);
                }, () -> {
                    this.pageFile.partList.listLoadingAnimation(false, 0, 0);
                    Main.runOnUiThread(this.activity(), loader::cancel);
                }));
            });
        }));
    }
}
