package com.xuxiaocheng.WListAndroid.UIs.Main.Main.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hjq.toast.Toaster;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.StorageTypeGetter;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateProvidersHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.Main.ActivityMain;
import com.xuxiaocheng.WListAndroid.UIs.Main.Main.PageMainAdapter;
import com.xuxiaocheng.WListAndroid.UIs.Main.Provider.PageProvider;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.AbstractTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.Managers.UploadTasksManager;
import com.xuxiaocheng.WListAndroid.UIs.Main.Task.PageTaskAdapter;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileDirectoryBinding;
import com.xuxiaocheng.WListAndroid.databinding.PageFileUploadBinding;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class PartUpload extends SFragmentFilePart {
    protected PartUpload(final @NotNull FragmentFile fragment) {
        super(fragment);
    }

    @Override
    public void cOnConnect() {
        super.cOnConnect();
        final SharedPreferences preferences = this.activity().getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
        final float percentX = preferences.getFloat("x", 0.7f);
        final float percentY = preferences.getFloat("y", 0.7f);
        if (!preferences.contains("x") || !preferences.contains("y"))
            preferences.edit().putFloat("x", percentX).putFloat("y", percentY).apply();
        Main.runOnUiThread(this.activity(), () -> {
            final View v = this.fragmentContent().pageFileUploader, parent = this.fragmentContent().pageFileList;
            final float parentX = parent.getX(), parentY = parent.getY();
            final float width = parent.getWidth(), height = parent.getHeight();
            final float halfWidth = v.getWidth() / 2.0f, halfHeight = v.getHeight() / 2.0f;
            v.setX(HMathHelper.clamp(percentX * width + parentX - parentX, halfWidth, width - halfWidth) + parentX - halfWidth);
            v.setY(HMathHelper.clamp(percentY * height + parentY - parentY, halfHeight, height - halfHeight) + parentY - halfHeight);
            v.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void cOnDisconnect() {
        super.cOnDisconnect();
        Main.runOnUiThread(this.activity(), () -> this.fragmentContent().pageFileUploader.setVisibility(View.GONE));
    }

    @Override
    protected void sOnTypeChanged(final PageMainAdapter.@NotNull Types type) {
        super.sOnTypeChanged(type);
        if (type == PageMainAdapter.Types.File && this.isConnected())
            Main.runOnBackgroundThread(this.activity(), this::cOnConnect); // Fix position.
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void iOnBuildPage() {
        super.iOnBuildPage();
        final PageFileBinding page = this.fragmentContent();
        final AtomicBoolean scrolling = new AtomicBoolean();
        final AtomicInteger startX = new AtomicInteger(), startY = new AtomicInteger();
        final AtomicReference<ZonedDateTime> startTime = new AtomicReference<>();
        page.pageFileUploader.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    scrolling.set(false);
                    startX.set(Float.floatToIntBits(v.getX()));
                    startY.set(Float.floatToIntBits(v.getY()));
                    startTime.set(ZonedDateTime.now());
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (scrolling.get()) {
                        final float parentX = page.pageFileList.getX(), parentY = page.pageFileList.getY();
                        final float halfWidth = v.getWidth() / 2.0f, halfHeight = v.getHeight() / 2.0f;
                        v.setX(HMathHelper.clamp(v.getX() + e.getX() - parentX, halfWidth, page.pageFileList.getWidth() - halfWidth) + parentX - halfWidth);
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, halfHeight, page.pageFileList.getHeight() - halfHeight) + parentY - halfHeight);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f
                            || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f
                            || Duration.between(startTime.get(), ZonedDateTime.now()).toMillis() >= 500) {
                        scrolling.set(true);
                        page.pageFileList.requestDisallowInterceptTouchEvent(true);
                    }
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get()) {
                        final float percentX = (v.getX() - page.pageFileList.getX()) / page.pageFileList.getWidth();
                        final float percentY = (v.getY() - page.pageFileList.getY()) / page.pageFileList.getHeight();
                        this.activity().getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", percentX).putFloat("y", percentY).apply();
                        page.pageFileList.requestDisallowInterceptTouchEvent(false);
                    } else return v.performClick();
                }
            }
            return true;
        });
        page.pageFileUploader.setOnClickListener(Main.debounceClickListener(u -> {
            if (this.fragment.partList().isOnRoot()) {
                this.addStorage(this.activity());
                return;
            }
            final BottomSheetDialog dialog = new BottomSheetDialog(this.activity(), R.style.BottomSheetDialog);
            final PageFileUploadBinding uploader = PageFileUploadBinding.inflate(this.activity().getLayoutInflater());
            uploader.pageFileUploadCancel.setOnClickListener(v -> dialog.cancel());
            final AtomicBoolean clickable = new AtomicBoolean(true);
            uploader.pageFileUploadStorageImage.setOnClickListener(v -> {
                if (!clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.addStorage(this.activity());
            });
            uploader.pageFileUploadStorageText.setOnClickListener(v -> uploader.pageFileUploadStorageImage.performClick());
            uploader.pageFileUploadDirectoryImage.setOnClickListener(v -> {
                if (this.fragment.partList().isOnRoot() || !clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.createDirectory(this.activity());
            });
            uploader.pageFileUploadDirectoryText.setOnClickListener(v -> uploader.pageFileUploadDirectoryImage.performClick());
            uploader.pageFileUploadFileImage.setOnClickListener(v -> {
                if (this.fragment.partList().isOnRoot() || !clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("*/*");
            });
            uploader.pageFileUploadFileText.setOnClickListener(v -> uploader.pageFileUploadFileImage.performClick());
            uploader.pageFileUploadPictureImage.setOnClickListener(v -> {
                if (this.fragment.partList().isOnRoot() || !clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("image/*");
            });
            uploader.pageFileUploadPictureText.setOnClickListener(v -> uploader.pageFileUploadPictureImage.performClick());
            uploader.pageFileUploadVideoImage.setOnClickListener(v -> {
                if (this.fragment.partList().isOnRoot() || !clickable.compareAndSet(true, false)) return;
                dialog.cancel();
                this.chooserLauncher.getInstance().launch("video/*");
            });
            uploader.pageFileUploadVideoText.setOnClickListener(v -> uploader.pageFileUploadVideoImage.performClick());
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(uploader.getRoot());
            dialog.show();
        }, 300, TimeUnit.MILLISECONDS));
    }

    private final @NotNull HInitializer<ActivityResultLauncher<String>> chooserLauncher = new HInitializer<>("PageFileChooserLauncher");
    public static final @NotNull String UploadChooserTag = "wlist:activity_rq_for_result#upload_chooser";

    @Override
    public void onAttach() {
        super.onAttach();
        this.chooserLauncher.reinitialize(this.activity().getActivityResultRegistry().register(PartUpload.UploadChooserTag, new ActivityResultContract<String, List<Uri>>() {
            @Override
            public @NotNull Intent createIntent(final @NotNull Context context, final @NotNull String input) {
                return new Intent(Intent.ACTION_GET_CONTENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        .setType(input);
            }

            @Override
            public @NotNull @Unmodifiable List<@NotNull Uri> parseResult(final int resultCode, final @Nullable Intent intent) {
                if (intent == null || resultCode != Activity.RESULT_OK) return List.of();
                if (intent.getData() != null)
                    return Collections.singletonList(intent.getData());
                final ClipData clipData = intent.getClipData();
                if (clipData == null)
                    return List.of();
                final List<Uri> uris = new ArrayList<>(clipData.getItemCount());
                for (int i = 0; i < clipData.getItemCount(); ++i)
                    uris.add(clipData.getItemAt(i).getUri());
                return Collections.unmodifiableList(uris);
            }
        }, this::uploadFile));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        final ActivityResultLauncher<String> chooser = this.chooserLauncher.uninitializeNullable();
        if (chooser != null)
            chooser.unregister();
    }


    private @NotNull FileLocation currentLocation() {
        return this.fragment.partList().currentLocation();
    }

    @UiThread
    protected void createDirectory(final @NotNull ActivityMain activity) {
        final FileLocation location = this.currentLocation();
        final PageFileDirectoryBinding editor = PageFileDirectoryBinding.inflate(activity.getLayoutInflater());
        editor.pageFileDirectoryName.setText(R.string.page_file_upload_directory_name);
        editor.pageFileDirectoryName.setHint(R.string.page_file_upload_directory_hint);
        if (editor.pageFileDirectoryName.requestFocus()) {
            editor.pageFileDirectoryName.setSelectAllOnFocus(true);
            editor.pageFileDirectoryName.setSelection(Objects.requireNonNull(editor.pageFileDirectoryName.getText()).length());
        }
        new AlertDialog.Builder(activity).setTitle(R.string.page_file_create_directory)
                .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    final String name = ViewUtil.getText(editor.pageFileDirectoryName);
                    final AlertDialog loading = activity.createLoadingDialog(R.string.page_file_create_directory);
                    loading.show();
                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
                                ParametersMap.create().add("location", location).add("name", name));
                        try (final WListClientInterface client = this.client()) {
                            OperateFilesHelper.createDirectory(client, this.token(), location, name, DuplicatePolicy.ERROR);
                        }
                        Toaster.show(R.string.page_file_upload_success_directory);
                    }, () -> Main.runOnUiThread(activity, loading::cancel)));
                }).show();
    }

    @UiThread
    @SuppressWarnings("unchecked")
    protected <C extends StorageConfiguration> void addStorage(final @NotNull ActivityMain activity) {
        final String[] storages = StorageTypeGetter.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
        final AtomicInteger choice = new AtomicInteger(-1);
        new AlertDialog.Builder(activity).setTitle(R.string.page_file_create_storage)
                .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    if (choice.get() == -1) return;
                    final String identifier = storages[choice.get()];
                    final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypeGetter.get(identifier));
                    PageProvider.getConfiguration(activity, type, null, configuration -> Main.runOnUiThread(activity, () -> {
                        final ImageView loadingView = new ImageView(activity);
                        loadingView.setImageResource(R.drawable.loading);
                        ViewUtil.startDrawableAnimation(loadingView);
                        final AlertDialog loading = new AlertDialog.Builder(activity)
                                .setTitle(R.string.page_file_create_storage).setView(loadingView)
                                .setCancelable(false).show();
                        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
                            try (final WListClientInterface client = this.client()) {
                                OperateProvidersHelper.addProvider(client, this.token(), configuration.getName(), type, configuration);
                            }
                        }, () -> Main.runOnUiThread(activity, loading::cancel)));
                    }));
                }).show();
    }

    @UiThread
    protected void uploadFile(final @NotNull @Unmodifiable Collection<? extends @NotNull Uri> uris) {
        if (uris.isEmpty()) return;
        final FileLocation parent = this.currentLocation();
        final ZonedDateTime now = ZonedDateTime.now();
        final AtomicInteger count = new AtomicInteger(0);
        final ContentResolver resolver = this.activity().getContentResolver();
        uris.forEach(uri -> {
            final ZonedDateTime current = now.plusNanos(count.getAndIncrement());
            Main.runOnBackgroundThread(this.activity(), HExceptionWrapper.wrapRunnable(() -> {
                final String filename;
                final long filesize;
                try (final Cursor cursor = resolver.query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                    if (cursor == null || !cursor.moveToFirst()) return;
                    final String name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    final long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
                    filename = name == null ? "unknown" : name;
                    filesize = size < 0 ? 0 : size;
                }
                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading file.",
                        ParametersMap.create().add("parent", parent).add("filename", filename).add("filesize", filesize).add("uri", uri));
                UploadTasksManager.getInstance().addTask(this.activity(), new UploadTasksManager.UploadTask(new AbstractTasksManager.AbstractTask(
                        this.address(), this.username(), current, filename, PageTaskAdapter.Types.Upload), parent, filesize, uri));
            }));
        });
    }
}
