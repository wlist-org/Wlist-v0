package com.xuxiaocheng.WListAndroid.UIs.Pages.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.R;
import com.xuxiaocheng.WListAndroid.UIs.ActivityMain;
import com.xuxiaocheng.WListAndroid.Utils.ViewUtil;
import com.xuxiaocheng.WListAndroid.databinding.PageFileBinding;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PageFilePartUpload {
    protected final @NotNull PageFile pageFile;

    public PageFilePartUpload(final @NotNull PageFile pageFile) {
        super();
        this.pageFile = pageFile;
    }

    private final @NotNull HInitializer<ActivityResultLauncher<String>> chooserLauncher = new HInitializer<>("PageFileChooserLauncher");

    @UiThread
    @SuppressLint("ClickableViewAccessibility")
    protected void onBind(final @NotNull PageFileBinding page) {
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
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, halfHeight, page.pageFileList.getHeight() - halfHeight) + parentY- halfHeight);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f
                            || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f
                            || Duration.between(startTime.get(), ZonedDateTime.now()).toMillis() >= 500) {
                        scrolling.set(true);
                        page.pageFileList.requestDisallowInterceptTouchEvent(true);
                    }
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get()) {
                        this.pageFile.activity().getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", v.getX()).putFloat("y", v.getY()).apply();
                        page.pageFileList.requestDisallowInterceptTouchEvent(false);
                    } else return v.performClick();
                }
            }
            return true;
        });
        final SharedPreferences preferences = this.pageFile.activity().getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
        final DisplayMetrics displayMetrics = this.pageFile.activity().getResources().getDisplayMetrics();
        Main.runOnBackgroundThread(this.pageFile.activity(), () -> {
            final float x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileUploader.getWidth()) * 0.8f);
            final float y = preferences.getFloat("y", (displayMetrics.heightPixels - page.pageFileUploader.getHeight()) * 0.7f);
            if (!preferences.contains("x") || !preferences.contains("y"))
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            Main.runOnUiThread(this.pageFile.activity(), () -> {
                page.pageFileUploader.setX(x);
                page.pageFileUploader.setY(y);
                page.pageFileUploader.setVisibility(View.VISIBLE);
            });
        }, 300, TimeUnit.MILLISECONDS);
//        page.pageFileUploader.setOnClickListener(u -> {
//            if (this.pageFile.partList.isOnRoot()) {
//                this.addStorage(this.pageFile.activity());
//                return;
//            }
//            final BottomSheetDialog dialog = new BottomSheetDialog(this.pageFile.activity(), R.style.BottomSheetDialog);
//            final PageFileUploadBinding uploader = PageFileUploadBinding.inflate(this.pageFile.activity().getLayoutInflater());
//            uploader.pageFileUploadCancel.setOnClickListener(v -> dialog.cancel());
//            final AtomicBoolean clickable = new AtomicBoolean(true);
//            uploader.pageFileUploadStorageImage.setOnClickListener(v -> {
//                if (!clickable.compareAndSet(true, false)) return;
//                dialog.cancel();
//                this.addStorage(this.pageFile.activity());
//            });
//            uploader.pageFileUploadStorageText.setOnClickListener(v -> uploader.pageFileUploadStorageImage.performClick());
//            uploader.pageFileUploadDirectoryImage.setOnClickListener(v -> {
//                if (this.pageFile.partList.isOnRoot() || !clickable.compareAndSet(true, false)) return;
//                dialog.cancel();
//                this.createDirectory(this.pageFile.activity());
//            });
//            uploader.pageFileUploadDirectoryText.setOnClickListener(v -> uploader.pageFileUploadDirectoryImage.performClick());
//            uploader.pageFileUploadFileImage.setOnClickListener(v -> {
//                if (this.pageFile.partList.isOnRoot() || !clickable.compareAndSet(true, false)) return;
//                dialog.cancel();
//                this.chooserLauncher.getInstance().launch("*/*");
//            });
//            uploader.pageFileUploadFileText.setOnClickListener(v -> uploader.pageFileUploadFileImage.performClick());
//            uploader.pageFileUploadPictureImage.setOnClickListener(v -> {
//                if (this.pageFile.partList.isOnRoot() || !clickable.compareAndSet(true, false)) return;
//                dialog.cancel();
//                this.chooserLauncher.getInstance().launch("image/*");
//            });
//            uploader.pageFileUploadPictureText.setOnClickListener(v -> uploader.pageFileUploadPictureImage.performClick());
//            uploader.pageFileUploadVideoImage.setOnClickListener(v -> {
//                if (this.pageFile.partList.isOnRoot() || !clickable.compareAndSet(true, false)) return;
//                dialog.cancel();
//                this.chooserLauncher.getInstance().launch("video/*");
//            });
//            uploader.pageFileUploadVideoText.setOnClickListener(v -> uploader.pageFileUploadVideoImage.performClick());
//            dialog.setCanceledOnTouchOutside(true);
//            dialog.setContentView(uploader.getRoot());
//            dialog.show();
//        });
    }

    protected void onActivityCreateHook(final @NotNull ActivityResultCaller activity) {
        this.chooserLauncher.reinitialize(activity.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
//            if (uri != null)
//                this.uploadFile(uri);
        }));
    }

//    private @NotNull FileLocation currentLocation() {
//        return this.pageFile.partList.currentLocation();
//    }

    private @NotNull ImageView loadingView(final @NotNull ActivityMain activity) {
        final ImageView loading = new ImageView(activity);
        loading.setImageResource(R.drawable.loading);
        ViewUtil.startDrawableAnimation(loading);
        return loading;
    }

    @UiThread
    protected @NotNull AlertDialog loadingDialog(final @NotNull ActivityMain activity, @StringRes final int title) {
        return new AlertDialog.Builder(activity).setTitle(title).setView(this.loadingView(activity)).setCancelable(false).show();
    }

//    @UiThread
//    protected void createDirectory(final @NotNull ActivityMain activity) {
//        final FileLocation location = this.currentLocation();
//        final PageFileDirectoryBinding editor = PageFileDirectoryBinding.inflate(activity.getLayoutInflater());
//        editor.pageFileDirectoryName.setText(R.string.page_file_upload_directory_name);
//        editor.pageFileDirectoryName.setHint(R.string.page_file_upload_directory_hint);
//        if (editor.pageFileDirectoryName.requestFocus()) {
//            editor.pageFileDirectoryName.setSelectAllOnFocus(true);
//            editor.pageFileDirectoryName.setSelection(Objects.requireNonNull(editor.pageFileDirectoryName.getText()).length());
//        }
//        new AlertDialog.Builder(activity).setTitle(R.string.page_file_create_directory)
//                .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
//                .setNegativeButton(R.string.cancel, null)
//                .setPositiveButton(R.string.confirm, (d, w) -> {
//                    final String name = ViewUtil.getText(editor.pageFileDirectoryName);
//                    final AlertDialog loading = new AlertDialog.Builder(activity)
//                            .setTitle(R.string.page_file_create_directory).setView(this.loadingView(activity)).setCancelable(false).show();
//                    Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
//                                ParametersMap.create().add("location", location).add("name", name));
//                        try (final WListClientInterface client = this.pageFile.client()) {
//                            OperateFilesHelper.createDirectory(client, this.pageFile.token(), location, name, DuplicatePolicy.ERROR);
//                        }
//                        Main.showToast(activity, R.string.page_file_upload_success_directory);
//                    }, () -> Main.runOnUiThread(activity, loading::cancel)));
//                }).show();
//    }
//
//    @UiThread
//    @SuppressWarnings("unchecked")
//    protected  <C extends StorageConfiguration> void addStorage(final @NotNull ActivityMain activity) {
//        final String[] storages = StorageTypeGetter.getAll().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
//        final AtomicInteger choice = new AtomicInteger(-1);
//        new AlertDialog.Builder(activity).setTitle(R.string.page_file_create_storage)
//                .setSingleChoiceItems(storages, -1, (d, w) -> choice.set(w))
//                .setNegativeButton(R.string.cancel, null)
//                .setPositiveButton(R.string.confirm, (d, w) -> {
//                    if (choice.get() == -1) return;
//                    final String identifier = storages[choice.get()];
//                    final StorageTypes<C> type = (StorageTypes<C>) Objects.requireNonNull(StorageTypeGetter.get(identifier));
//                    PageFileProviderConfigurations.getConfiguration(activity, type, null, configuration -> Main.runOnUiThread(activity, () -> {
//                        final AlertDialog loading = new AlertDialog.Builder(activity)
//                                .setTitle(R.string.page_file_create_storage).setView(this.loadingView(activity))
//                                .setCancelable(false).show();
//                        Main.runOnBackgroundThread(activity, HExceptionWrapper.wrapRunnable(() -> {
//                            try (final WListClientInterface client = this.pageFile.client()) {
//                                OperateProvidersHelper.addProvider(client, this.pageFile.token(), configuration.getName(), type, configuration);
//                            }
//                        }, () -> Main.runOnUiThread(activity, loading::cancel)));
//                    }));
//                }).show();
//    }
//
//    @UiThread
//    protected void uploadFile(final @NotNull Uri uri) {
//        Main.runOnBackgroundThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//            final FileLocation location = this.currentLocation();
//            // TODO: serialize uploading task.
//            // uri.toString()  -->  Uri.parse(...)
//            final String filename;
//            final long size;
//            try (final Cursor cursor = this.pageFile.activity().getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
//                if (cursor == null || !cursor.moveToFirst()) return;
//                filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
//                size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
//            }
//            HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading files.",
//                    ParametersMap.create().add("location", location).add("filename", filename).add("size", size).add("uri", uri));
//            this.pageFile.partList.listLoadingAnimation("Uploading", true, 0, 0);
//            Main.runOnUiThread(this.pageFile.activity(), () -> {
//                final AlertDialog loader = new AlertDialog.Builder(this.pageFile.activity()).setTitle(filename).setCancelable(false).show();
//                Main.runOnUiThread(this.pageFile.activity(), HExceptionWrapper.wrapRunnable(() -> {
//                    final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.uploadStream(this.pageFile.address(), this.pageFile.username(), HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
//                        try (final InputStream stream = new BufferedInputStream(this.pageFile.activity().getContentResolver().openInputStream(uri))) {
//                            AndroidSupporter.skipNBytes(stream, pair.getFirst().longValue());
//                            consumer.accept(stream);
//                        }
//                    }), size, filename, location, PredicateE.truePredicate(), s -> { // TODO upload progress.
//                        long current = 0, total = 0;
//                        for (final Pair.ImmutablePair<Long, Long> pair: InstantaneousProgressStateGetter.stages(s)) {
//                            current += pair.getFirst().longValue();
//                            total += pair.getSecond().longValue();
//                        }
//                        final long c = current, t = total;
//                        this.pageFile.partList.listLoadingAnimation("Uploading", true, c, t);
//                    });
//                    assert res != null;
//                    if (res.isFailure()) // TODO
//                        Main.runOnUiThread(this.pageFile.activity(), () -> Toast.makeText(this.pageFile.activity(), FailureReasonGetter.message(res.getE()), Toast.LENGTH_SHORT).show());
//                    else
//                        Main.showToast(this.pageFile.activity(), R.string.page_file_upload_success_file);
//                }, () -> {
//                    this.pageFile.partList.listLoadingAnimation("Uploading", false, 0, 0);
//                    Main.runOnUiThread(this.pageFile.activity(), loader::cancel);
//                }));
//            });
//        }));
//    }
}
