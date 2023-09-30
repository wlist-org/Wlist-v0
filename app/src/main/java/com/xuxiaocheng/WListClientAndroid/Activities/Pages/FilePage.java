package com.xuxiaocheng.WListClientAndroid.Activities.Pages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AIOStream;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMathHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.AndroidSupports.FileInformationGetter;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationSupporter;
import com.xuxiaocheng.WList.AndroidSupports.FilesListInformationGetter;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WListClientAndroid.Activities.CustomViews.MainTab;
import com.xuxiaocheng.WListClientAndroid.Activities.LoginActivity;
import com.xuxiaocheng.WListClientAndroid.Client.TokenManager;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileContentBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileDriverBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileEditorBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileOptionBinding;
import com.xuxiaocheng.WListClientAndroid.databinding.PageFileUploadBinding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class FilePage implements MainTab.MainTabPage {
    @NonNull protected final Activity activity;
    @NonNull protected final InetSocketAddress address;

    public FilePage(@NonNull final Activity activity, @NonNull final InetSocketAddress address) {
        super();
        this.activity = activity;
        this.address = address;
    }

    @NonNull protected final HInitializer<PageFileContentBinding> pageCache = new HInitializer<>("FilePage");
    @Override
    @NonNull public ConstraintLayout onShow() {
        final PageFileContentBinding cache = this.pageCache.getInstanceNullable();
        if (cache != null) return cache.getRoot();
        final PageFileContentBinding page = PageFileContentBinding.inflate(this.activity.getLayoutInflater());
        this.pageCache.initialize(page);
        page.pageFileContentName.setText(R.string.app_name);
        page.pageFileContentList.setLayoutManager(new LinearLayoutManager(this.activity));
        page.pageFileContentList.setHasFixedSize(true);
        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() ->
                        this.pushFileList(page.pageFileContentName.getText(), new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), 0))));
        this.buildUploader();
        return page.getRoot();
    }

    @NonNull protected final Deque<LocationStackRecord> locationStack = new ArrayDeque<>();
    protected static class LocationStackRecord {
        protected final boolean isRoot;
        @NonNull protected final CharSequence name;
        @NonNull protected final AtomicLong counter;
        @NonNull protected final FileLocation location;
        @NonNull protected final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter;
        @NonNull protected final RecyclerView.OnScrollListener listener;

        protected LocationStackRecord(final boolean isRoot, @NonNull final CharSequence name, @NonNull final AtomicLong counter, @NonNull final FileLocation location,
                                      @NonNull final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter,
                                      @NonNull final RecyclerView.OnScrollListener listener) {
            super();
            this.isRoot = isRoot;
            this.name = name;
            this.counter = counter;
            this.location = location;
            this.adapter = adapter;
            this.listener = listener;
        }

        @Override
        @NonNull public String toString() {
            return "LocationStackRecord{" +
                    "isRoot=" + this.isRoot +
                    ", name=" + this.name +
                    ", counter=" + this.counter +
                    ", location=" + this.location +
                    ", adapter=" + this.adapter +
                    ", listener=" + this.listener +
                    '}';
        }
    }

    private void setBacker(final boolean isRoot) {
        final ImageView backer = this.pageCache.getInstance().pageFileContentBacker;
        if (isRoot) {
            backer.setImageResource(R.mipmap.page_file_backer_nonclickable);
            backer.setOnClickListener(null);
            backer.setClickable(false);
        } else {
            backer.setImageResource(R.mipmap.page_file_backer);
            backer.setOnClickListener(v -> this.popFileList());
            backer.setClickable(true);
        }
    }

    protected void pushFileList(@NonNull final CharSequence name, @NonNull final FileLocation location) {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final boolean isRoot = IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(FileLocationSupporter.storage(location));
        final AtomicInteger currentPage = new AtomicInteger(0);
        final AtomicLong counter = new AtomicLong(0);
        final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapterWrapper = new EnhancedRecyclerViewAdapter<>() {
            @Override
            @NonNull protected CellViewHolder createViewHolder(@NonNull final ViewGroup parent) {
                return new CellViewHolder(EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_cell, page.pageFileContentList), information -> {
                   if (FileInformationGetter.isDirectory(information))
                        FilePage.this.pushFileList(FileInformationGetter.name(information),
                                FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.storage(location), FileInformationGetter.id(information)));
                    else {
                        Main.runOnBackgroundThread(FilePage.this.activity, () -> { // Prevent exit.
                            // TODO: show file.
                            throw new UnsupportedOperationException("Show file is unsupported now!");
                        });
                    }
                }, isRoot, FilePage.this);
            }

            @Override
            protected void bindViewHolder(@NonNull final CellViewHolder holder, @NonNull final VisibleFileInformation information) {
                holder.onBind(information);
            }
        };
        final RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
            @NonNull private final AtomicBoolean onLoading = new AtomicBoolean(false);
            @NonNull private final AtomicBoolean noMore = new AtomicBoolean(false);
            @UiThread
            @Override
            public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // TODO: Remove the pages on the top.
                // TODO: register broadcast listener. (auto add)
                if (!recyclerView.canScrollVertically(1) && !this.noMore.get() && this.onLoading.compareAndSet(false, true)) {
                    final ConstraintLayout loadingTailor = EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_loading, page.pageFileContentList);
                    final ImageView loading = (ImageView) loadingTailor.getViewById(R.id.page_file_tailor_loading_image);
                    FilePage.setLoading(loading);
                    adapterWrapper.addTailor(loadingTailor);
                    Main.runOnBackgroundThread(FilePage.this.activity, HExceptionWrapper.wrapRunnable(() -> {
                        final VisibleFilesListInformation list;
                        // TODO: loading progress.
                        this.noMore.set(false); // prevent retry forever when server error.
                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(FilePage.this.address)) {
                            // TODO: more configurable params.
                            list = OperateFilesHelper.listFiles(client, TokenManager.getToken(FilePage.this.address), location,
                                    Options.FilterPolicy.Both, new LinkedHashMap<>(0), 50, currentPage.getAndIncrement());
                        }
                        if (list == null) {
                            Main.runOnUiThread(FilePage.this.activity, () -> {
                                Main.showToast(FilePage.this.activity, R.string.page_file_unavailable_directory);
                                FilePage.this.popFileList();
                            });
                            return;
                        }
                        this.noMore.set(FilesListInformationGetter.informationList(list).isEmpty());
                        counter.set(FilesListInformationGetter.total(list));
                        Main.runOnUiThread(FilePage.this.activity, () -> {
                            page.pageFileContentCounter.setText(String.valueOf(FilesListInformationGetter.total(list)));
                            page.pageFileContentCounter.setVisibility(View.VISIBLE);
                            page.pageFileContentCounterText.setVisibility(View.VISIBLE);
                            adapterWrapper.addDataRange(FilesListInformationGetter.informationList(list));
                        });
                    }, () -> {
                        this.onLoading.set(false);
                        Main.runOnUiThread(FilePage.this.activity, () -> {
                            loading.clearAnimation();
                            if (this.noMore.get()) {
                                adapterWrapper.setTailor(0, EnhancedRecyclerViewAdapter.buildView(FilePage.this.activity.getLayoutInflater(), R.layout.page_file_tailor_no_more, page.pageFileContentList));
                                if (page.pageFileContentList.getAdapter() == adapterWrapper) // Confuse: Why must call 'setAdapter' again?
                                    page.pageFileContentList.setAdapter(adapterWrapper);
                            } else {
                                adapterWrapper.removeTailor(0);
                                if (page.pageFileContentList.getAdapter() == adapterWrapper) // Autoload more if still in this list page.
                                    this.onScrollStateChanged(recyclerView, newState);
                            }
                        });
                    }));
                }
            }
        };
        FilePage.this.locationStack.push(new LocationStackRecord(isRoot, name, counter, location, adapterWrapper, listener));
        Main.runOnUiThread(this.activity, () -> {
            this.setBacker(isRoot);
            page.pageFileContentName.setText(name);
            page.pageFileContentCounter.setVisibility(View.GONE);
            page.pageFileContentCounterText.setVisibility(View.GONE);
            final RecyclerView content = page.pageFileContentList;
            content.setAdapter(adapterWrapper);
            content.clearOnScrollListeners();
            content.addOnScrollListener(listener);
            listener.onScrollStateChanged(content, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        });
    }

    @UiThread
    protected boolean popFileList() {
        final PageFileContentBinding page = this.pageCache.getInstanceNullable();
        if (page == null || this.locationStack.size() < 2) return false;
        this.locationStack.pop();
        final LocationStackRecord record = this.locationStack.pop();
        this.pushFileList(record.name, record.location);
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void buildUploader() {
        final PageFileContentBinding page = this.pageCache.getInstance();
        final AtomicBoolean scrolling = new AtomicBoolean();
        final AtomicInteger startX = new AtomicInteger(), startY = new AtomicInteger();
        page.pageFileContentUploader.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    scrolling.set(false);
                    startX.set(Float.floatToIntBits(v.getX()));
                    startY.set(Float.floatToIntBits(v.getY()));
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (scrolling.get()) {
                        final float parentX = page.pageFileContentList.getX(), parentY = page.pageFileContentList.getY();
                        v.setX(HMathHelper.clamp(v.getX() + e.getX() - parentX, 0, page.pageFileContentList.getWidth()) + parentX - v.getWidth() / 2.0f);
                        v.setY(HMathHelper.clamp(v.getY() + e.getY() - parentY, -50, page.pageFileContentList.getHeight()) + parentY - v.getHeight() / 2.0f);
                    } else if (Math.abs(v.getX() + e.getX() - Float.intBitsToFloat(startX.get())) > v.getWidth() / 2.0f || Math.abs(v.getY() + e.getY() - Float.intBitsToFloat(startY.get())) > v.getHeight() / 2.0f)
                        scrolling.set(true);
                }
                case MotionEvent.ACTION_UP -> {
                    if (scrolling.get())
                        FilePage.this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE).edit()
                                .putFloat("x", v.getX()).putFloat("y", v.getY()).apply();
                    else return v.performClick();
                }
            }
            return true;
        });
        Main.runOnBackgroundThread(this.activity, () -> {
            final float x, y;
            final SharedPreferences preferences = this.activity.getSharedPreferences("page_file_uploader_position", Context.MODE_PRIVATE);
            if (preferences.contains("x") && preferences.contains("y")) {
                x = preferences.getFloat("x", 0);
                y = preferences.getFloat("y", 0);
            } else {
                final DisplayMetrics displayMetrics = this.activity.getResources().getDisplayMetrics();
                x = preferences.getFloat("x", (displayMetrics.widthPixels - page.pageFileContentUploader.getWidth()) * 0.7f);
                y = preferences.getFloat("y", displayMetrics.heightPixels * 0.6f);
                preferences.edit().putFloat("x", x).putFloat("y", y).apply();
            }
            Main.runOnUiThread(this.activity, () -> {
                page.pageFileContentUploader.setX(x);
                page.pageFileContentUploader.setY(y);
            });
        });
        page.pageFileContentUploader.setOnClickListener(u -> {
            if (this.locationStack.isEmpty()) return;
            if (this.locationStack.size() < 2) { // Root driver
                if (!this.address.equals(LoginActivity.internalServerAddress.getInstanceNullable())) {
                    Main.showToast(this.activity, R.string.page_file_upload_root);
                    return;
                }
                final String[] drivers = StorageTypes.getAll().keySet().toArray(new String[0]);
                final AtomicInteger choice = new AtomicInteger(-1);
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_driver_add)
                        .setSingleChoiceItems(drivers, -1, (d, w) -> choice.set(w))
                        .setNegativeButton(R.string.cancel, (d, w) -> {})
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final String identifier = drivers[choice.get()];
                            final PageFileDriverBinding driverBinding = PageFileDriverBinding.inflate(this.activity.getLayoutInflater());
                            driverBinding.pageFileDriverName.setText(identifier);
                            new AlertDialog.Builder(this.activity).setTitle(identifier).setView(driverBinding.getRoot())
                                    .setNegativeButton(R.string.cancel, (b, h) -> {})
                                    .setPositiveButton(R.string.confirm, (b, h) -> {
                                        final Editable name_e = driverBinding.pageFileDriverName.getText();
                                        final Editable passport_e = driverBinding.pageFileDriverPassport.getText();
                                        final Editable password_e = driverBinding.pageFileDriverPassword.getText();
                                        final String name = name_e == null ? "" : name_e.toString();
                                        final String passport = passport_e == null ? "" : passport_e.toString();
                                        final String password = password_e == null ? "" : password_e.toString();
                                        final ImageView loading = new ImageView(this.activity);
                                        loading.setImageResource(R.mipmap.page_file_loading);
                                        FilePage.setLoading(loading);
                                        final AlertDialog dialog = new AlertDialog.Builder(this.activity)
                                                .setTitle(R.string.page_file_driver_add).setView(loading).setCancelable(false).show();
                                        Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                            if (name.isEmpty() || passport.isEmpty() || password.isEmpty())
                                                throw new IllegalStateException("Empty input."); // TODO input checker
                                            // TODO add driver. (WIP)
                                            final String n = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 32, null);
                                            final File file = new File(this.activity.getExternalFilesDir("server"), "configs/" + n + ".yaml");
                                            HFileHelper.ensureFileExist(file.toPath(), false);
                                            try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                                                stream.write(String.format("local:\n  display_name: %s\nweb:\n  passport: '%s'\n  password: '%s'\n", name, passport, password).getBytes());
                                            }
                                            final String configuration;
                                            try (final InputStream stream = new BufferedInputStream(new FileInputStream(new File(this.activity.getExternalFilesDir("server"), "server.yaml")))) {
                                                final ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
                                                try (final OutputStream os = new ByteBufOutputStream(buf)) {
                                                    AIOStream.transferTo(stream, os);
                                                    configuration = buf.toString(StandardCharsets.UTF_8);
                                                } finally {
                                                    buf.release();
                                                }
                                            }
                                            try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(this.activity.getExternalFilesDir("server"), "server.yaml")))) {
                                                stream.write(configuration.replace(" {}", "").getBytes(StandardCharsets.UTF_8));
                                                stream.write(String.format("  %s: %s\n", n, identifier).getBytes());
                                            }
                                            Main.runOnUiThread(this.activity, () -> {
                                                Toast.makeText(this.activity, "Server needs to be restarted to adapt to changes.", Toast.LENGTH_SHORT).show();
                                            });
                                            try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
                                               OperateServerHelper.closeServer(client, TokenManager.getToken(this.address));
                                            }
                                        }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                                    }).show();
                        }).show();
                return;
            }
            final LocationStackRecord record = this.locationStack.getFirst();
            final FileLocation location = record.location;
            final PageFileUploadBinding upload = PageFileUploadBinding.inflate(this.activity.getLayoutInflater());
            final AlertDialog uploader = new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.page_file_upload).setView(upload.getRoot())
                    .setPositiveButton(R.string.cancel, (d, w) -> {}).create();
            final AtomicBoolean clickable = new AtomicBoolean(true);
            upload.pageFileUploadDirectory.setOnClickListener(v -> {
                if (!clickable.compareAndSet(true, false)) return;
                uploader.cancel();
                final PageFileEditorBinding editor = PageFileEditorBinding.inflate(this.activity.getLayoutInflater());
                editor.pageFileEditor.setText(R.string.page_file_upload_directory_name);
                editor.pageFileEditor.setHint(R.string.page_file_upload_directory_hint);
                if (editor.pageFileEditor.requestFocus()) {
                    editor.pageFileEditor.setSelectAllOnFocus(true);
                    editor.pageFileEditor.setSelection(Objects.requireNonNull(editor.pageFileEditor.getText()).length());
                }
                new AlertDialog.Builder(this.activity).setTitle(R.string.page_file_upload_directory)
                        .setIcon(R.mipmap.page_file_upload_directory).setView(editor.getRoot())
                        .setNegativeButton(R.string.cancel, (d, w) -> {})
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            final Editable editable = editor.pageFileEditor.getText();
                            final String name = editable == null ? "" : editable.toString();
                            final ImageView loading = new ImageView(this.activity);
                            loading.setImageResource(R.mipmap.page_file_loading);
                            FilePage.setLoading(loading);
                            final AlertDialog dialog = new AlertDialog.Builder(this.activity)
                                    .setTitle(R.string.page_file_upload_directory).setView(loading).setCancelable(false).show();
                            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
                                HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Creating directory.",
                                        ParametersMap.create().add("address", this.address).add("location", location).add("name", name));
                                try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
                                    OperateFilesHelper.createDirectory(client, TokenManager.getToken(this.address), location, name, Options.DuplicatePolicy.ERROR);
                                }
                                Main.runOnUiThread(this.activity, () -> {
                                    Main.showToast(this.activity, R.string.page_file_upload_success_directory);
                                    // TODO: auto add.
                                    this.popFileList();
                                    this.pushFileList(name, location);
                                });
                            }, () -> Main.runOnUiThread(this.activity, dialog::cancel)));
                        }).show();
            });
            upload.pageFileUploadDirectoryText.setOnClickListener(v -> upload.pageFileUploadDirectory.performClick());
            final Consumer<String> uploadFile = type -> {
                if (!clickable.compareAndSet(true, false)) return;
                uploader.cancel();
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(type);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                this.activity.startActivityForResult(intent, "SelectFiles".hashCode());
            };
            upload.pageFileUploadFile.setOnClickListener(v -> uploadFile.accept("*/*"));
            upload.pageFileUploadFileText.setOnClickListener(v -> upload.pageFileUploadFile.performClick());
            upload.pageFileUploadPicture.setOnClickListener(v -> uploadFile.accept("image/*"));
            upload.pageFileUploadPictureText.setOnClickListener(v -> upload.pageFileUploadPicture.performClick());
            upload.pageFileUploadVideo.setOnClickListener(v -> uploadFile.accept("video/*"));
            upload.pageFileUploadVideoText.setOnClickListener(v -> upload.pageFileUploadVideo.performClick());
            uploader.show();
        });
    }

//    @Override
//    public boolean onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
//        if (resultCode == Activity.RESULT_OK && requestCode == "SelectFiles".hashCode() && data != null) {
//            final Collection<Uri> uris = new ArrayList<>();
//            if (data.getData() != null)
//                uris.add(data.getData());
//            else {
//                final ClipData clipData = data.getClipData();
//                for (int i = 0; i < clipData.getItemCount(); ++i)
//                    uris.add(clipData.getItemAt(i).getUri());
//            }
//            final ImageView loading = new ImageView(this.activity);
//            loading.setImageResource(R.mipmap.page_file_loading);
//            FilePage.setLoading(loading);
//            final AlertDialog dialog = new AlertDialog.Builder(this.activity)
//                    .setTitle(R.string.page_file_upload_file).setView(loading).setCancelable(false).show();
//            Main.runOnBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
//                final CountDownLatch latch = new CountDownLatch(uris.size());
//                for (final Uri uri: uris)
//                    Main.runOnNewBackgroundThread(this.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        // TODO: serialize uploading task.
//                        final String filename;
//                        final long size;
//                        try (final Cursor cursor = this.activity.getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
//                            if (cursor == null || !cursor.moveToFirst())
//                                return;
//                            filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
//                            size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
//                        }
//                        final MessageDigest digester = HMessageDigestHelper.MD5.getDigester();
//                        try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
//                            HMessageDigestHelper.updateMessageDigest(digester, stream);
//                        }
//                        final String md5 = HMessageDigestHelper.MD5.digest(digester);
//                        final LocationStackRecord record = this.locationStack.getFirst(); // .peek();
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Uploading file.",
//                                ParametersMap.create().add("address", this.address).add("location", record.location).add("uri", uri)
//                                        .add("filename", filename).add("size", size).add("md5", md5));
//                        final UnionPair<UnionPair<VisibleFileInformation, String>, FailureReason> request;
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.address)) {
//                            request = OperateFilesHelper.requestUploadFile(client, TokenManager.getToken(this.address), record.location, filename, size, md5, Options.DuplicatePolicy.KEEP);
//                            if (request.isFailure()) // TODO
//                                throw new RuntimeException(FailureReason.handleFailureReason(request.getE()));
//                            if (request.getT().isFailure()) {
//                                final String id = request.getT().getE();
//                                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
//                                try (final InputStream stream = new BufferedInputStream(this.activity.getContentResolver().openInputStream(uri))) {
//                                    int chunk = 0;
//                                    while (true) {
//                                        buffer.writeBytes(stream, NetworkTransmission.FileTransferBufferSize);
//                                        final UnionPair<VisibleFileInformation, Boolean> result = OperateFilesHelper.uploadFile(client, TokenManager.getToken(this.address), id, chunk++, buffer.retain());
//                                        if (result == null || result.isSuccess() || !result.getE().booleanValue() || stream.available() == 0)
//                                            break;
//                                        buffer.clear();
//                                    }
//                                } finally {
//                                    buffer.release();
//                                }
//                            }
//                        }
//            }, latch::countDown));
//                latch.await();
//                Main.runOnUiThread(this.activity, () -> {
//                    dialog.cancel();
//                    Main.showToast(this.activity, R.string.page_file_upload_success_file);
//                    // TODO: auto add.
//                    final LocationStackRecord record = this.locationStack.getFirst(); // .peek();
//                    this.popFileList();
//                    this.pushFileList(record.name, record.location);
//                });
//            }));
//            return true;
//        }
//        return false;
//    }

    @Override
    public boolean onBackPressed() {
        return this.popFileList();
    }

    @UiThread
    private static void setLoading(@NonNull final ImageView loading) {
        final Animation loadingAnimation = new RotateAnimation(0, 360 << 10, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        loadingAnimation.setDuration(500 << 10);
        loadingAnimation.setInterpolator(new LinearInterpolator());
        loadingAnimation.setRepeatCount(Animation.INFINITE);
        loading.startAnimation(loadingAnimation);
    }

    @Override
    @NonNull public String toString() {
        return "FilePage{" +
                "address=" + this.address +
                ", pageCache=" + this.pageCache +
                ", locationStack=" + this.locationStack +
                '}';
    }

    protected static class CellViewHolder extends EnhancedRecyclerViewAdapter.WrappedViewHolder<VisibleFileInformation, ConstraintLayout> {
        @NonNull protected final Consumer<VisibleFileInformation> clicker;
        protected final boolean isRoot;
        @NonNull protected final FilePage page;
        @NonNull protected final ImageView image;
        @NonNull protected final TextView name;
        @NonNull protected final TextView tips;
        @NonNull protected final View option;

        protected CellViewHolder(@NonNull final ConstraintLayout cell, @NonNull final Consumer<VisibleFileInformation> clicker, final boolean isRoot, @NonNull final FilePage page) {
            super(cell);
            this.clicker = clicker;
            this.isRoot = isRoot;
            this.page = page;
            this.image = (ImageView) cell.getViewById(R.id.page_file_cell_image);
            this.name = (TextView) cell.getViewById(R.id.page_file_cell_name);
            this.tips = (TextView) cell.getViewById(R.id.page_file_cell_tips);
            this.option = cell.getViewById(R.id.page_file_cell_option);
        }

        public void onBind(@NonNull final VisibleFileInformation information) {
            this.itemView.setOnClickListener(v -> this.clicker.accept(information)); // TODO: select on long click.
            CellViewHolder.setFileImage(this.image, information);
            this.name.setText(FileInformationGetter.name(information));
            this.tips.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown").replace('T', ' '));
            this.option.setOnClickListener(v -> {
                final PageFileOptionBinding optionBinding = PageFileOptionBinding.inflate(this.page.activity.getLayoutInflater());
                optionBinding.pageFileOptionName.setText(FileInformationGetter.name(information));
                final long size = FileInformationGetter.size(information);
                optionBinding.pageFileOptionSize.setText(size < 1 ? "unknown" : String.valueOf(size));
                optionBinding.pageFileOptionCreate.setText(FileInformationGetter.createTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown"));
                optionBinding.pageFileOptionUpdate.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown"));
                final AlertDialog modifier = new AlertDialog.Builder(this.page.activity)
                        .setTitle(R.string.page_file_option).setView(optionBinding.getRoot())
                        .setPositiveButton(R.string.cancel, (d, w) -> {}).create();
                final LocationStackRecord record = this.page.locationStack.getFirst(); // TODO: optimize
                final FileLocation location = new FileLocation(FileLocationSupporter.storage(record.location), FileInformationGetter.id(information));
                final AtomicBoolean clickable = new AtomicBoolean(true);
//                optionBinding.pageFileOptionRename.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final PageFileEditorBinding editor = PageFileEditorBinding.inflate(this.page.activity.getLayoutInflater());
//                    editor.pageFileEditor.setText(FileInformationGetter.name(information));
//                    if (editor.pageFileEditor.requestFocus()) {
//                        editor.pageFileEditor.setSelectAllOnFocus(true);
//                        editor.pageFileEditor.setSelection(Objects.requireNonNull(editor.pageFileEditor.getText()).length());
//                    }
//                    new AlertDialog.Builder(this.page.activity).setTitle(R.string.page_file_option_rename).setView(editor.getRoot())
//                            .setNegativeButton(R.string.cancel, (d, w) -> {})
//                            .setPositiveButton(R.string.confirm, (d, w) -> {
//                                final Editable editable = editor.pageFileEditor.getText();
//                                final String name = editable == null ? "" : editable.toString();
//                                if (FileInformationGetter.name(information).equals(name)) return;
//                                final ImageView loading = new ImageView(this.page.activity);
//                                loading.setImageResource(R.mipmap.page_file_loading);
//                                FilePage.setLoading(loading);
//                                final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                                        .setTitle(R.string.page_file_option_rename).setView(loading).setCancelable(false).show();
//                                Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                                    HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Renaming.",
//                                            ParametersMap.create().add("address", this.page.address).add("location", location).add("name", name));
//                                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                                        OperateFilesHelper.renameFile(client, TokenManager.getToken(this.page.address), location, name, Options.DuplicatePolicy.ERROR);
//                                    }
//                                    Main.runOnUiThread(this.page.activity, () -> {
//                                        Main.showToast(this.page.activity, R.string.page_file_option_rename_success);
//                                        // TODO: auto refresh.
//                                        this.page.popFileList();
//                                        this.page.pushFileList(record.name, record.location);
//                                    });
//                                }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                            }).show();
//                });
//                optionBinding.pageFileOptionRenameIcon.setOnClickListener(u -> optionBinding.pageFileOptionRename.performClick());
//                optionBinding.pageFileOptionMove.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    Main.runOnBackgroundThread(this.page.activity, () -> {
//                        // TODO: move file.
//                        throw new UnsupportedOperationException("Move file is unsupported now!");
//                    });
////                    final EnhancedRecyclerViewAdapter<VisibleFileInformation, CellViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
////                        @Override
////                        @NonNull protected CellViewHolder createViewHolder(@NonNull final ViewGroup parent) {
////                            final CellViewHolder holder = new CellViewHolder(EnhancedRecyclerViewAdapter.buildView(CellViewHolder.this.page.activity.getLayoutInflater(), R.layout.page_file_cell, (RecyclerView) parent), information -> {
//////                                FilePage.this.pushFileList(isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information),
//////                                        FileLocationSupporter.create(isRoot ? FileInformationGetter.name(information) : FileLocationSupporter.driver(location), FileInformationGetter.id(information)));
////                            }, isRoot, CellViewHolder.this.page);
////                            holder.option.setVisibility(View.GONE);
////                            return holder;
////                        }
////
////                        @Override
////                        protected void bindViewHolder(@NonNull final CellViewHolder holder, @NonNull final VisibleFileInformation information) {
////                            holder.itemView.setOnClickListener(v -> holder.clicker.accept(information));
////                            CellViewHolder.setFileImage(holder.image, information);
////                            holder.name.setText(holder.isRoot ? FileInformationGetter.md5(information) : FileInformationGetter.name(information));
////                            holder.tips.setText(FileInformationGetter.updateTimeString(information, DateTimeFormatter.ISO_DATE_TIME, "unknown").replace('T', ' '));
////                            // TODO: same as 'onBind' except 'option'.
////                        }
////                    };
////                    final AlertDialog.Builder chooser = new AlertDialog.Builder(this.page.activity).setTitle(R.string.page_file_option_move)
////                            .setView()
//                });
//                optionBinding.pageFileOptionMoveIcon.setOnClickListener(u -> optionBinding.pageFileOptionMove.performClick());
//                optionBinding.pageFileOptionCopy.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    Main.runOnBackgroundThread(this.page.activity, () -> {
//                        // TODO: copy file.
//                        throw new UnsupportedOperationException("Copy file is unsupported now!");
//                    });
//                });
//                optionBinding.pageFileOptionCopyIcon.setOnClickListener(u -> optionBinding.pageFileOptionCopy.performClick());
//                optionBinding.pageFileOptionDelete.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final ImageView loading = new ImageView(this.page.activity);
//                    loading.setImageResource(R.mipmap.page_file_loading);
//                    FilePage.setLoading(loading);
//                    final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                            .setTitle(R.string.page_file_option_delete).setView(loading).setCancelable(false).show();
//                    Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Deleting.",
//                                ParametersMap.create().add("address", this.page.address).add("location", location));
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                            OperateFilesHelper.trashFileOrDirectory(client, TokenManager.getToken(this.page.address), location);
//                        }
//                        Main.runOnUiThread(this.page.activity, () -> {
//                            Main.showToast(this.page.activity, R.string.page_file_option_delete_success);
//                            // TODO: auto remove
//                            this.page.popFileList();
//                            this.page.pushFileList(record.name, record.location);
//                        });
//                    }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                });
//                optionBinding.pageFileOptionDeleteIcon.setOnClickListener(u -> optionBinding.pageFileOptionDelete.performClick());
//                optionBinding.pageFileOptionDownload.setOnClickListener(u -> {
//                    if (!clickable.compareAndSet(true, false)) return;
//                    modifier.cancel();
//                    final ImageView loading = new ImageView(this.page.activity);
//                    loading.setImageResource(R.mipmap.page_file_loading);
//                    FilePage.setLoading(loading);
//                    final AlertDialog dialog = new AlertDialog.Builder(this.page.activity)
//                            .setTitle(R.string.page_file_option_download).setView(loading).setCancelable(false).show();
//                    Main.runOnBackgroundThread(this.page.activity, HExceptionWrapper.wrapRunnable(() -> {
//                        HLogManager.getInstance("ClientLogger").log(HLogLevel.INFO, "Downloading.",
//                                ParametersMap.create().add("address", this.page.address).add("location", location));
//                        try (final WListClientInterface client = WListClientManager.quicklyGetClient(this.page.address)) {
//                            final Pair.ImmutablePair<Long, String> id = OperateFilesHelper.requestDownloadFile(client, TokenManager.getToken(this.page.address), location, 0, Long.MAX_VALUE);
//                            if (id == null)
//                                throw new IllegalStateException("File not exist.");
//                            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wlist/" + FileInformationGetter.name(information));
//                            HFileHelper.ensureFileExist(file.toPath(), false);
//                            try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
//                                int chunk = 0;
//                                while (true) {
//                                    final ByteBuf buffer = OperateFilesHelper.downloadFile(client, TokenManager.getToken(this.page.address), id.getSecond(), chunk++);
//                                    if (buffer == null) break;
//                                    try (final InputStream buf = new ByteBufInputStream(buffer)) {
//                                        AIOStream.transferTo(buf, stream);
//                                    } finally {
//                                        buffer.release();
//                                    }
//                                }
//                            }
//                        }
//                        Main.showToast(this.page.activity, R.string.page_file_option_download_success);
//                    }, () -> Main.runOnUiThread(this.page.activity, dialog::cancel)));
//                });
//                optionBinding.pageFileOptionDownloadIcon.setOnClickListener(u -> optionBinding.pageFileOptionDownload.performClick());
                modifier.show();
            });
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof CellViewHolder holder)) return false;
            return this.image.equals(holder.image) && this.name.equals(holder.name) && this.tips.equals(holder.tips) && this.option.equals(holder.option);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.image, this.name, this.tips, this.option);
        }

        @Override
        @NonNull public String toString() {
            return "FilePage$CellViewHolder{" +
                    "clicker=" + this.clicker +
                    ", image=" + this.image +
                    ", name=" + this.name +
                    ", tips=" + this.tips +
                    ", option=" + this.option +
                    '}';
        }

        protected static void setFileImage(@NonNull final ImageView image, @NonNull final VisibleFileInformation information) {
            if (FileInformationGetter.isDirectory(information)) {
                image.setImageResource(R.mipmap.page_file_image_directory);
                return;
            }
            final String name = FileInformationGetter.name(information);
            final int index = name.lastIndexOf('.');
            // TODO: cached Drawable.
            image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT)) {
                case "bat", "cmd", "sh", "run" -> R.mipmap.page_file_image_bat;
                case "doc", "docx" -> R.mipmap.page_file_image_docx;
                case "exe", "bin" -> R.mipmap.page_file_image_exe;
                case "jpg", "jpeg", "png", "bmp", "psd", "tga" -> R.mipmap.page_file_image_jpg;
                case "mp3", "flac", "wav", "wma", "aac", "ape" -> R.mipmap.page_file_image_mp3;
                case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
                case "txt", "log" -> R.mipmap.page_file_image_txt;
                case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
                case "zip", "7z", "rar", "gz", "tar" -> R.mipmap.page_file_image_zip;
                default -> R.mipmap.page_file_image_file;
            });
        }
    }
}
