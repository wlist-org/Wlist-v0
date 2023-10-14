package com.xuxiaocheng.WListClientAndroid.UIs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.R;
import com.xuxiaocheng.WListClientAndroid.Utils.EnhancedRecyclerViewAdapter;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import com.xuxiaocheng.WListClientAndroid.databinding.ActivityFileChooserBinding;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ActivityFileChooser extends AppCompatActivity {
    public static final int Code = Math.abs("SelectFiles".hashCode());

    public static @NotNull Intent build(final @NotNull Context activity, final @NotNull File root, final @NotNull String pattern) {
        final Intent intent = new Intent(activity, ActivityFileChooser.class);
        intent.putExtra("root", root.getAbsolutePath()).putExtra("pattern", pattern);
        return intent;
    }

    protected @Nullable File extraRoot() {
        final Intent intent = this.getIntent();
        final String root = intent.getStringExtra("root");
        if (root == null) return null;
        final Pattern pattern = Pattern.compile(Objects.requireNonNullElse(intent.getStringExtra("pattern"), ".*"));
        this.pattern.reinitialize(pattern);
        return new File(root).getAbsoluteFile();
    }

    protected final @NotNull HInitializer<ActivityFileChooserBinding> activity = new HInitializer<>("ActivityFileChooserBinding");
    protected final @NotNull HInitializer<Pattern> pattern = new HInitializer<>("ActivityFileChooserPattern");
    protected final @NotNull Set<@NotNull File> files = ConcurrentHashMap.newKeySet();

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HLogManager.initialize(this, HLogManager.ProcessType.Activity);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        logger.log(HLogLevel.VERBOSE, "Creating ActivityFileChooser.");
        final ActivityFileChooserBinding activity = ActivityFileChooserBinding.inflate(this.getLayoutInflater());
        this.setContentView(activity.getRoot());
        this.activity.reinitialize(activity);
        final File root = this.extraRoot();
        if (root == null) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), new IllegalStateException("No root received."));
            this.finish();
            return;
        }
        activity.activityFileChooserCanceller.setOnClickListener(v -> this.finish());
        activity.activityFileChooserConfirm.setOnClickListener(v -> {
            final Intent intent = new Intent();
            intent.putExtra("files", AndroidSupporter.streamToList(this.files.stream().map(File::getAbsolutePath)).toArray(EmptyArrays.EMPTY_STRINGS));
            this.setResult(ActivityFileChooser.Code, intent);
            this.finish();
        });
        activity.activityFileChooserContentList.setLayoutManager(new LinearLayoutManager(this));
        activity.activityFileChooserContentList.setHasFixedSize(true);
        this.setList(root);
    }

    @UiThread
    protected void setList(final @NotNull File root) {
        final RecyclerView list = this.activity.getInstance().activityFileChooserContentList;
        final EnhancedRecyclerViewAdapter<File, ActivityFileChooserViewHolder> adapter = new EnhancedRecyclerViewAdapter<>() {
            @Override
            protected @NotNull ActivityFileChooserViewHolder createViewHolder(final @NotNull ViewGroup parent) {
                return new ActivityFileChooserViewHolder(EnhancedRecyclerViewAdapter.buildView(ActivityFileChooser.this.getLayoutInflater(), R.layout.activity_file_chooser_cell, list), (file, chose) -> {
                    if (chose.booleanValue())
                        ActivityFileChooser.this.files.add(file);
                    else
                        ActivityFileChooser.this.files.remove(file);
                });
            }
        };
        list.setAdapter(adapter);
        Main.runOnBackgroundThread(this, HExceptionWrapper.wrapRunnable(() -> {
            final List<File> files;
            try (final Stream<Path> stream = Files.list(root.toPath())) {
                files = AndroidSupporter.streamToList(stream.map(Path::toFile)
                        .filter(f -> f.isDirectory() || this.pattern.getInstance().matcher(f.getName()).matches())
                        .sorted(Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName)));
            }
            Main.runOnUiThread(this, () -> adapter.addDataRange(files));
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.VERBOSE, "Destroying ActivityFileChooser.");
    }

    @Override
    public @NotNull String toString() {
        return "ActivityFileChooser{" +
                "activity=" + this.activity +
                ", pattern=" + this.pattern +
                ", files=" + this.files +
                '}';
    }
}
